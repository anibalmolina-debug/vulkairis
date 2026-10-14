package com.vulkairis.translation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Bounded background compile worker (Phase 12).
 *
 * <p>CPU translation and shaderc compilation may run here instead of the render thread: the
 * queue is bounded (64), threads are daemon (they never block shutdown), and every task
 * carries the pipeline generation it was submitted under. Reloads purge queued tasks from
 * older generations, and results are only installed when the task generation still matches —
 * a stale compile can never install a pipeline after a newer pack or dimension replaced it.
 * Vulkan object creation, submission and live-state mutation stay on the render thread;
 * only pure CPU work (translation, compilation to bytes) runs here.</p>
 */
public final class TranslationWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/CompileWorker");
    private static final int QUEUE_CAPACITY = 64;

    private static final ThreadFactory DAEMON_FACTORY = new ThreadFactory() {
        private final AtomicInteger index = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "vulkairis-compile-" + index.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    };

    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            1, 2, 30L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
            DAEMON_FACTORY,
            new ThreadPoolExecutor.DiscardPolicy());

    /** Outstanding tasks, so reloads can cancel exactly the stale ones. */
    private static final java.util.Set<Task<?>> OUTSTANDING =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    private TranslationWorker() {
    }

    /** A submitted unit of background work with its pipeline generation. */
    public record Task<T>(Future<T> future, long generation, String name) {
    }

    /**
     * Submits background work. A saturated queue discards (with a diagnostic) instead of
     * stalling the submitting thread.
     */
    public static <T> Task<T> submit(String name, long generation, Supplier<T> work) {
        Future<T> future = EXECUTOR.submit(() -> {
            try {
                return work.get();
            } catch (Throwable t) {
                LOGGER.warn("[Vulkairis/CompileWorker] Background task '{}' failed: {}",
                        name, t.getMessage());
                throw t;
            }
        });
        Task<T> task = new Task<>(future, generation, name);
        OUTSTANDING.add(task);
        return task;
    }

    /**
     * Cancels outstanding tasks from older generations. Queued tasks are dropped; running
     * tasks finish harmlessly because installation still checks the generation.
     *
     * @return how many stale tasks were dropped
     */
    public static int purgeStale(long currentGeneration) {
        int dropped = 0;
        for (Task<?> task : OUTSTANDING.toArray(new Task<?>[0])) {
            if (task.future().isDone() || task.future().isCancelled()) {
                OUTSTANDING.remove(task);
                continue;
            }
            if (task.generation() != currentGeneration && task.future().cancel(false)) {
                OUTSTANDING.remove(task);
                dropped++;
            }
        }
        if (dropped > 0) {
            LOGGER.info("[Vulkairis/CompileWorker] Dropped {} stale task(s) for generation {}",
                    dropped, currentGeneration);
        }
        return dropped;
    }

    /**
     * Takes a task result only when its generation is still current.
     *
     * @return the value, or empty when stale, failed, timed out or cancelled
     */
    public static <T> Optional<T> takeIfCurrent(Task<T> task, long currentGeneration,
                                                long timeoutMs) {
        if (task == null || task.future() == null) {
            return Optional.empty();
        }
        if (task.generation() != currentGeneration) {
            task.future().cancel(false);
            OUTSTANDING.remove(task);
            return Optional.empty();
        }
        try {
            Optional<T> result = Optional.ofNullable(task.future().get(timeoutMs, TimeUnit.MILLISECONDS));
            OUTSTANDING.remove(task);
            return result;
        } catch (TimeoutException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Throwable t) {
            LOGGER.warn("[Vulkairis/CompileWorker] Task '{}' failed: {}",
                    task.name(), t.getMessage());
            OUTSTANDING.remove(task);
            return Optional.empty();
        }
    }

    /** @return queued (not yet started) task count, for diagnostics */
    public static int queuedCount() {
        return EXECUTOR.getQueue().size();
    }
}
