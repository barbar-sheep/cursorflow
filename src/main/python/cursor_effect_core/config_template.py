# ==================== 用户配置模板 ====================
# 特效类型（必须，对应 effect_renderer 中的实现）
EFFECT_TYPE = "particle"  # 可选：particle（粒子）、snake_line（线条）、sprite（贴图）

# 基础参数（所有特效通用）
BASE_CONFIG = {
    "enabled": True,  # 是否启用该特效
    "max_length": 30,  # 拖尾最大长度（帧）
    "opacity": 0.8,    # 整体透明度（0-1）
    "z_index": 1       # 渲染层级（多个特效时生效）
}

# 特效专属参数（根据 EFFECT_TYPE 选择配置）
# ------------------- 粒子特效参数 -------------------
PARTICLE_CONFIG = {
    "count": 15,          # 每帧生成粒子数量
    "color": "#FF9800",   # 粒子颜色（支持 RGB/RGBA/十六进制）
    "size_range": [1, 4], # 粒子大小范围（最小，最大）
    "speed_range": [1, 3],# 粒子速度范围（最小，最大）
    "life_range": [30, 90],# 粒子生命周期（帧）
    "gravity": 0.1,       # 重力系数（正数向下，负数向上）
    "wind": 0.05          # 风力系数（正数向右，负数向左）
}

# ------------------- 线条特效参数 -------------------
SNAKE_LINE_CONFIG = {
    "color": "#9C27B0",   # 线条颜色
    "width": 3.0,         # 线条宽度
    "round_cap": True,    # 是否圆角端点
    "fade_out": True      # 是否渐变消失
}

# ------------------- 贴图特效参数 -------------------
SPRITE_CONFIG = {
    "image_path": "sprite.png",  # 贴图路径（相对/绝对路径）
    "size": [32, 32],            # 贴图大小（宽，高）
    "rotate": True,              # 是否跟随鼠标旋转
    "alpha_range": [0.3, 0.8]    # 贴图透明度范围
}