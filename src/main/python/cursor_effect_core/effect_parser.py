import importlib.util
import os
from dataclasses import dataclass, asdict
from typing import Dict, Optional, Any  # 新增：导入 Any

# 定义统一的参数数据类（避免魔法值）
@dataclass
class BaseConfig:
    enabled: bool = True
    max_length: int = 30
    opacity: float = 0.8
    z_index: int = 1

@dataclass
class ParticleConfig(BaseConfig):
    count: int = 15
    color: str = "#FF9800"
    size_range: list = None
    speed_range: list = None
    life_range: list = None
    gravity: float = 0.0
    wind: float = 0.0
    custom_move_func: Optional[Any] = None  # 可选自定义运动函数

@dataclass
class SnakeLineConfig(BaseConfig):
    color: str = "#9C27B0"
    width: float = 3.0
    round_cap: bool = True
    fade_out: bool = True

@dataclass
class SpriteConfig(BaseConfig):
    image_path: str = ""
    size: list = None
    rotate: bool = False
    alpha_range: list = None

class EffectParser:
    def __init__(self, config_file_path: str):
        self.config_file_path = config_file_path
        self.config_module = None
        self.effect_type = None
        self.parsed_config = None

    def load_config(self) -> bool:
        """加载并验证用户配置文件"""
        if not os.path.exists(self.config_file_path):
            print(f"错误：配置文件不存在 - {self.config_file_path}")
            return False

        # 动态导入 Python 配置文件
        spec = importlib.util.spec_from_file_location("user_config", self.config_file_path)
        self.config_module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.config_module)

        # 验证必填项
        if not hasattr(self.config_module, "EFFECT_TYPE"):
            print("错误：配置文件缺少 EFFECT_TYPE")
            return False

        self.effect_type = self.config_module.EFFECT_TYPE
        return self._parse_config()

    def _parse_config(self) -> bool:
        """根据特效类型解析配置"""
        try:
            # 解析基础配置
            base_config = BaseConfig()
            if hasattr(self.config_module, "BASE_CONFIG"):
                base_dict = self.config_module.BASE_CONFIG
                for key, value in base_dict.items():
                    if hasattr(base_config, key):
                        setattr(base_config, key, value)

            # 根据特效类型解析专属配置
            if self.effect_type == "particle":
                self.parsed_config = self._parse_particle_config(base_config)
            elif self.effect_type == "snake_line":
                self.parsed_config = self._parse_snake_line_config(base_config)
            elif self.effect_type == "sprite":
                self.parsed_config = self._parse_sprite_config(base_config)
            else:
                print(f"错误：不支持的特效类型 - {self.effect_type}")
                return False
            return True
        except Exception as e:
            print(f"配置解析失败：{str(e)}")
            return False

    def _parse_particle_config(self, base_config: BaseConfig) -> ParticleConfig:
        """解析粒子特效配置"""
        config = ParticleConfig(**asdict(base_config))
        if hasattr(self.config_module, "PARTICLE_CONFIG"):
            particle_dict = self.config_module.PARTICLE_CONFIG
            for key, value in particle_dict.items():
                if hasattr(config, key):
                    setattr(config, key, value)
        # 加载自定义运动函数（如果存在）
        if hasattr(self.config_module, "custom_particle_move"):
            config.custom_move_func = self.config_module.custom_particle_move
        # 设置默认值（避免 None）
        config.size_range = config.size_range or [1, 4]
        config.speed_range = config.speed_range or [1, 3]
        config.life_range = config.life_range or [30, 90]
        return config

    def _parse_snake_line_config(self, base_config: BaseConfig) -> SnakeLineConfig:
        """解析线条特效配置"""
        config = SnakeLineConfig(**asdict(base_config))
        if hasattr(self.config_module, "SNAKE_LINE_CONFIG"):
            line_dict = self.config_module.SNAKE_LINE_CONFIG
            for key, value in line_dict.items():
                if hasattr(config, key):
                    setattr(config, key, value)
        return config

    def _parse_sprite_config(self, base_config: BaseConfig) -> SpriteConfig:
        """解析贴图特效配置"""
        config = SpriteConfig(**asdict(base_config))
        if hasattr(self.config_module, "SPRITE_CONFIG"):
            sprite_dict = self.config_module.SPRITE_CONFIG
            for key, value in sprite_dict.items():
                if hasattr(config, key):
                    setattr(config, key, value)
        # 设置默认值
        config.size = config.size or [32, 32]
        config.alpha_range = config.alpha_range or [0.3, 0.8]
        return config

    def get_config(self) -> Optional[Any]:
        """获取解析后的配置对象"""
        return self.parsed_config

    def get_effect_type(self) -> Optional[str]:
        """获取特效类型"""
        return self.effect_type