package com.cursorflow.effect;

import java.util.Map;

/**
 * 特效工厂：根据特效类型创建实例
 */
public class EffectFactory {
    // 特效类型枚举（与服务端配置对应）
    public enum EffectType {
        SNAKE_LINE("snake_line", SnakeLineEffect.class),
        PARTICLE("particle", ParticleEffect.class),
        PYTHON_DRIVEN("python_driven", PythonDrivenEffect.class);

        private final String type;
        private final Class<? extends ITrailEffect> clazz;

        EffectType(String type, Class<? extends ITrailEffect> clazz) {
            this.type = type;
            this.clazz = clazz;
        }

        public static EffectType fromType(String type) {
            for (EffectType effectType : values()) {
                if (effectType.type.equals(type)) {
                    return effectType;
                }
            }
            return SNAKE_LINE; // 默认特效
        }
    }

    /**
     * 创建特效实例（支持 Python 驱动特效）
     * @param type 特效类型
     * @param config 配置（Python 驱动时需包含 configFilePath）
     */
    public static ITrailEffect createEffect(String type, Map<String, Object> config) {
        try {
            EffectType effectType = EffectType.fromType(type);
            if (effectType == EffectType.PYTHON_DRIVEN) {
                // Python 驱动特效：从配置中获取用户配置文件路径
                String configFilePath = (String) config.getOrDefault("configFilePath", "src/main/python/user_configs/my_particle_effect.py");
                return new PythonDrivenEffect(configFilePath);
            } else {
                // 原有 Java 实现的特效
                ITrailEffect effect = effectType.clazz.getDeclaredConstructor().newInstance();
                effect.init(config);
                return effect;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 异常时返回默认特效
            ITrailEffect defaultEffect = new SnakeLineEffect();
            defaultEffect.init(config);
            return defaultEffect;
        }
    }
}