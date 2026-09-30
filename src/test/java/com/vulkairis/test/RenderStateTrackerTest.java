package com.vulkairis.test;

import com.vulkairis.rendering.RenderStateTracker;
import com.vulkairis.vulkan.pipeline.PipelineKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RenderStateTrackerTest {

    @Test
    public void testPipelineKeyGeneration() {
        RenderStateTracker tracker = new RenderStateTracker();

        tracker.setBlend(true, 1);
        tracker.setDepth(true, true);
        tracker.setCulling(true);
        PipelineKey key1 = tracker.createPipelineKey(100L, 200L, "POSITION_COLOR");

        tracker.setBlend(false, 0);
        PipelineKey key2 = tracker.createPipelineKey(100L, 200L, "POSITION_COLOR");

        Assertions.assertNotEquals(key1, key2, "Keys with different blend states must not be equal");
        Assertions.assertEquals(key1, new PipelineKey(100L, 200L, "POSITION_COLOR", 1, true, true, true));
    }

    @Test
    public void testViewportTracking() {
        RenderStateTracker tracker = new RenderStateTracker();
        tracker.setViewport(2560, 1440);
        Assertions.assertEquals(2560, tracker.getViewportWidth());
        Assertions.assertEquals(1440, tracker.getViewportHeight());
    }
}
