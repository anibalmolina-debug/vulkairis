package com.vulkairis.client;

import com.vulkairis.client.mixin.ext.VulkairisMixinExtension;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class VulkairisPreLaunch implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LogManager.getLogger("Vulkairis/PreLaunch");

    private static final Set<String> CONFLICTING_IRIS_MIXINS = Set.of(
            "MixinRenderSystem",
            "MixinShaderInstance",
            "MixinProgram",
            "MixinProgramManager",
            "MixinProgramType",
            "MixinUniform",
            "MixinVertexBuffer",
            "MixinGlStateManager",
            "MixinLevelRenderer",
            "MixinGameRenderer",
            "MixinChunkBorderRenderer",
            "MixinBufferBuilder",
            "MixinByteBufferBuilder",
            "MixinChunkVertex",
            "MixinRenderRegion",
            "MixinSodiumOptions",
            "MixinShaderChunkRenderer",
            "MixinSodiumWorldRenderer",
            "MixinSodiumGameOptions",
            "EnumOptionBuilderImplAccessor",
            "IntegerOptionBuilderImplAccessor",
            "MixinHumanoidArmorLayer",
            "MixinHorseArmorLayer",
            "MixinCapeLayer",
            "MixinElytraLayer",
            "MixinItemRenderer",
            "MixinEntityRenderer",
            "MixinEntityRenderDispatcher",
            "MixinBlockEntityRenderDispatcher",
            "MixinEnderDragonRenderer"
    );

    @Override
    public void onPreLaunch() {
        LOGGER.info("[Vulkairis] Pre-launch: configuring Iris <-> VulkanMod Mixin compatibility bridge...");
        applyCompat();
    }

    public static void applyCompat() {
        registerExtension();
        sanitizeConfigs();
    }

    private static void registerExtension() {
        try {
            Object transformer = MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
            if (transformer == null) {
                transformer = MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
            }

            if (transformer == null) {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                Field delegateField = classLoader.getClass().getDeclaredField("delegate");
                delegateField.setAccessible(true);
                Object delegate = delegateField.get(classLoader);
                if (delegate != null) {
                    Field transformerField = delegate.getClass().getDeclaredField("mixinTransformer");
                    transformerField.setAccessible(true);
                    transformer = transformerField.get(delegate);
                }
            }

            if (transformer != null) {
                Method getExtMethod = null;
                Class<?> cur = transformer.getClass();
                while (cur != null && getExtMethod == null) {
                    try {
                        getExtMethod = cur.getDeclaredMethod("getExtensions");
                    } catch (NoSuchMethodException e) {
                        cur = cur.getSuperclass();
                    }
                }

                if (getExtMethod != null) {
                    getExtMethod.setAccessible(true);
                    Object extRegistry = getExtMethod.invoke(transformer);
                    if (extRegistry != null) {
                        Method addMethod = extRegistry.getClass().getMethod("add", IExtension.class);
                        addMethod.setAccessible(true);
                        addMethod.invoke(extRegistry, new VulkairisMixinExtension());
                        LOGGER.info("[Vulkairis] Successfully registered VulkairisMixinExtension with Sponge Mixin!");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[Vulkairis] Extension registration notice: {}", e.getMessage());
        }
    }

    private static void sanitizeConfigs() {
        try {
            for (Config config : Mixins.getConfigs()) {
                String configName = config.getName();
                if (configName != null && (configName.contains("iris") || configName.contains("sodium"))) {
                    Object mixinConfig = config.getConfig();
                    if (mixinConfig != null) {
                        stripConflictingEntries(mixinConfig);
                    }
                }
            }
            LOGGER.info("[Vulkairis] Iris mixin configurations sanitized successfully.");
        } catch (Exception e) {
            LOGGER.warn("[Vulkairis] Error sanitizing mixin configs: {}", e.getMessage());
        }
    }

    private static void stripConflictingEntries(Object mixinConfig) {
        try {
            Field[] fields = mixinConfig.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object val = field.get(mixinConfig);
                if (val instanceof List<?> list) {
                    list.removeIf(item -> {
                        if (item instanceof String s) {
                            for (String conflicting : CONFLICTING_IRIS_MIXINS) {
                                if (s.contains(conflicting)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    });
                }
            }
        } catch (Exception ignored) {
        }
    }
}
