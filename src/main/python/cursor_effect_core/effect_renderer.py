import random
from dataclasses import asdict
from typing import List, Dict, Optional, Any  # 新增：导入 Any
import numpy as np

# 粒子对象（用于内部计算）
class Particle:
    def __init__(self, x: float, y: float, config: "ParticleConfig"):
        self.x = x
        self.y = y
        self.size = random.uniform(*config.size_range)
        self.speed = random.uniform(*config.speed_range)
        self.dx = (random.random() - 0.5) * self.speed
        self.dy = (random.random() - 0.5) * self.speed
        self.life = random.randint(*config.life_range)
        self.max_life = self.life
        self.color = config.color
        self.custom_move_func = config.custom_move_func

class EffectRenderer:
    def __init__(self, config: Any, effect_type: str):  # 现在 Any 已导入，无报错
        self.config = config
        self.effect_type = effect_type
        self.particles: List[Particle] = []  # 粒子缓存
        self.trail_points: List[Dict[str, int]] = []  # 线条轨迹缓存

    def update(self, mouse_x: int, mouse_y: int) -> Dict[str, Any]:
        """根据鼠标坐标更新特效状态，返回渲染指令"""
        if not self.config.enabled:
            return {"type": "empty"}

        if self.effect_type == "particle":
            return self._update_particle(mouse_x, mouse_y)
        elif self.effect_type == "snake_line":
            return self._update_snake_line(mouse_x, mouse_y)
        elif self.effect_type == "sprite":
            return self._update_sprite(mouse_x, mouse_y)
        else:
            return {"type": "empty"}

    def _update_particle(self, mouse_x: int, mouse_y: int) -> Dict[str, Any]:
        """更新粒子特效，返回渲染指令"""
        config = self.config

        # 生成新粒子
        for _ in range(config.count):
            self.particles.append(Particle(mouse_x, mouse_y, config))

        # 更新粒子状态（运动、生命周期）
        alive_particles = []
        for p in self.particles:
            # 应用重力和风力
            p.dy += config.gravity
            p.dx += config.wind

            # 应用自定义运动函数（如果存在）
            if p.custom_move_func:
                p = p.custom_move_func(p, mouse_x, mouse_y)

            # 更新位置
            p.x += p.dx
            p.y += p.dy
            p.life -= 1

            # 过滤存活的粒子
            if p.life > 0:
                alive_particles.append(p)

        # 限制粒子数量（避免内存溢出）
        self.particles = alive_particles[-int(config.max_length * config.count):]

        # 生成渲染指令（给 Java 客户端）
        render_data = {
            "type": "particle",
            "opacity": config.opacity,
            "particles": [
                {
                    "x": round(p.x, 2),
                    "y": round(p.y, 2),
                    "size": round(p.size * (p.life / p.max_life), 2),  # 生命周期渐变
                    "color": p.color
                } for p in self.particles
            ]
        }
        return render_data

    def _update_snake_line(self, mouse_x: int, mouse_y: int) -> Dict[str, Any]:
        """更新线条特效，返回渲染指令"""
        config = self.config

        # 添加当前鼠标位置到轨迹
        self.trail_points.append({"x": mouse_x, "y": mouse_y})

        # 限制轨迹长度
        if len(self.trail_points) > config.max_length:
            self.trail_points = self.trail_points[-config.max_length:]

        # 生成渲染指令
        render_data = {
            "type": "snake_line",
            "opacity": config.opacity,
            "color": config.color,
            "width": config.width,
            "round_cap": config.round_cap,
            "fade_out": config.fade_out,
            "points": self.trail_points
        }
        return render_data

    def _update_sprite(self, mouse_x: int, mouse_y: int) -> Dict[str, Any]:
        """更新贴图特效，返回渲染指令"""
        config = self.config

        # 计算贴图透明度（随机在范围内）
        alpha = random.uniform(*config.alpha_range)

        # 计算旋转角度（如果启用）
        rotate = random.randint(0, 360) if config.rotate else 0

        # 生成渲染指令
        render_data = {
            "type": "sprite",
            "opacity": config.opacity * alpha,
            "x": mouse_x - config.size[0] / 2,  # 居中对齐鼠标
            "y": mouse_y - config.size[1] / 2,
            "width": config.size[0],
            "height": config.size[1],
            "image_path": config.image_path,
            "rotate": rotate
        }
        return render_data