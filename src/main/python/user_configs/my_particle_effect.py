# -*- coding: utf-8 -*-
# 烟花/彩虹粒子配置文件

EFFECT_TYPE = "particle"

BASE_CONFIG = {
    "enabled": True,
    "max_length": 50,
    "opacity": 1.0
}

PARTICLE_CONFIG = {
    # 增加数量，让“爆炸”更密集
    "count": 12,

    # 【重点】：烟花色盘 (高饱和度霓虹色 + 金色闪光)
    "color": [
        "#FF0000", # 红 (Red)
        "#FFD700", # 金 (Gold) - 烟花必备
        "#00FF00", # 亮绿 (Lime)
        "#00FFFF", # 青 (Cyan)
        "#FF00FF", # 这里的洋红 (Magenta)
        "#FF4500", # 橘红 (OrangeRed)
        "#9400D3", # 紫罗兰 (DarkViolet)
        "#1E90FF", # 闪电蓝 (DodgerBlue)
        "#FFFFFF"  # 白闪光 (White)
    ],

    # 粒子大小差异大一点，模拟碎屑
    "size_range": [2, 7],

    # 速度要快！才有“炸开”的感觉
    "speed_range": [3, 9],

    # 存活时间短一点，像烟花一样转瞬即逝
    "life_range": [20, 45],

    # 重力稍微大一点，模拟火星下坠
    "gravity": 0.6,

    "wind": 0
}

def custom_particle_move(particle, mouse_x, mouse_y):
    # 这里不需要额外逻辑，直线飞溅最像烟花
    return particle

config = {
    "type": EFFECT_TYPE,
    "opacity": BASE_CONFIG["opacity"],
    "particle_count": PARTICLE_CONFIG["count"],
    "particle_color": PARTICLE_CONFIG["color"],
    "size_range": PARTICLE_CONFIG["size_range"],
    "speed_range": PARTICLE_CONFIG["speed_range"],
    "gravity": PARTICLE_CONFIG["gravity"],
    "wind": PARTICLE_CONFIG["wind"],
    "update_behavior": custom_particle_move
}