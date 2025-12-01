import sys
import os
import time
import json
import traceback
import importlib.util
import random # 粒子随机生成需要

# -----------------------------------------------------
# CursorFlow 核心逻辑 (完整版)
# -----------------------------------------------------
class EffectProcessor:
    def __init__(self, config_path):
        self.config_path = config_path.strip()
        self.config = {}
        self.effect_type = "unknown"

        # [状态管理] 存储当前屏幕上所有活着的粒子
        self.active_particles = []
        self.running = True

        print(f"[Py] Init Processor with config: {self.config_path}")
        self._load_user_config()

    def _load_user_config(self):
        """加载用户配置文件"""
        if not os.path.exists(self.config_path):
            print(f"[Py Error] File not found: {self.config_path}")
            return

        try:
            # 动态加载模块
            spec = importlib.util.spec_from_file_location("user_effect_module", self.config_path)
            if spec is None or spec.loader is None:
                print(f"[Py Error] Cannot load spec from: {self.config_path}")
                return

            user_module = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(user_module)

            # 读取 config 变量
            if hasattr(user_module, "config"):
                self.config = user_module.config
                self.effect_type = self.config.get("type", "unknown")
                print(f"[Py Success] Config loaded! Type: {self.effect_type}")
                # Debug
                keys = list(self.config.keys())
                print(f"[Py Debug] Config keys: {keys}")
            else:
                print(f"[Py Error] Variable 'config' MISSING in {os.path.basename(self.config_path)}")

        except Exception:
            print("[Py Fatal Error] Exception during import:")
            traceback.print_exc()
            sys.stdout.flush()

    def process_line(self, line):
        try:
            if not line or not line.startswith("{"): return
            data = json.loads(line)
            mouse_x = data.get("x", 0)
            mouse_y = data.get("y", 0)

            # 获取参数
            p_count = self.config.get("particle_count", 5)
            life_min, life_max = self.config.get("life_range", [20, 50])
            size_min, size_max = self.config.get("size_range", [2, 6])
            speed_min, speed_max = self.config.get("speed_range", [1, 3])
            color_config = self.config.get("particle_color", "#ff0000")
            gravity = self.config.get("gravity", 0.5)

            # ============================================================
            # 【优化改动】：在生成循环 之前 决定颜色
            # 这样这一帧生成的所有 5-10 颗粒子都是同一个颜色，形成“一簇”的感觉
            # ============================================================
            current_frame_color = "#FFFFFF"

            # 方案 A：完全随机（每帧变一个色，像霓虹灯闪烁）
            if isinstance(color_config, list):
                current_frame_color = random.choice(color_config)
            else:
                current_frame_color = color_config

            # 方案 B：(可选) 如果想要彩虹流光效果（Razer Chroma 风格），请取消下面这段注释覆盖上面的逻辑
            # import colorsys
            # import time
            # hue = (time.time() * 0.5) % 1.0  # 0.5 是变色速度
            # r, g, b = colorsys.hsv_to_rgb(hue, 1.0, 1.0)
            # current_frame_color = '#%02x%02x%02x' % (int(r*255), int(g*255), int(b*255))
            # ============================================================

            # --- 生成新粒子 ---
            for _ in range(p_count):
                angle = random.uniform(0, 360)
                speed = random.uniform(speed_min, speed_max)

                # 计算向量
                import math
                rad = math.radians(angle)
                vx = math.cos(rad) * speed
                vy = math.sin(rad) * speed

                self.active_particles.append({
                    "x": mouse_x,
                    "y": mouse_y,
                    "vx": vx,
                    "vy": vy,
                    "size": random.uniform(size_min, size_max),
                    "life": random.randint(life_min, life_max),
                    "max_life": float(life_max),
                    # 【使用刚才外面决定好的统一颜色】
                    "color": current_frame_color
                })

            # --- 下面是物理更新和发送逻辑 (保持不变) ---
            alive_particles = []
            output_particles = []
            global_opacity = self.config.get("opacity", 1.0)

            for p in self.active_particles:
                p["x"] += p["vx"]
                p["y"] += p["vy"]
                p["vy"] += gravity
                p["life"] -= 1

                if p["life"] > 0:
                    alive_particles.append(p)
                    life_ratio = p["life"] / p["max_life"]
                    current_size = p["size"] * life_ratio
                    # 这里添加了 平方 运算，让渐隐更自然（非线性），避免结束时还有灰蒙蒙的影子
                    current_alpha = global_opacity * (life_ratio * life_ratio)

                    output_particles.append({
                        "x": p["x"],
                        "y": p["y"],
                        "size": max(0.1, current_size),
                        "color": p["color"],
                        "alpha": current_alpha
                    })

            self.active_particles = alive_particles

            if alive_particles:
                response = {
                    "type": "particle",
                    "particles": output_particles
                }
                print(json.dumps(response))
                sys.stdout.flush()

        except Exception:
            pass

    def start(self):
        """
        这就是你之前缺失的方法
        """
        print("[Py] Loop started. Waiting for input...")
        while self.running:
            try:
                # 从标准输入读取一行 (Java 发来的 {"x":..., "y":...})
                line = sys.stdin.readline()
                if not line:
                    break #如果 Java 关闭了流，Python 退出
                self.process_line(line.strip())
            except KeyboardInterrupt:
                break
            except Exception:
                break

if __name__ == "__main__":
    # 强制 UTF-8 输出
    sys.stdout.reconfigure(encoding='utf-8')

    if len(sys.argv) < 2:
        print("[Py] Missing config path argument.")
        sys.exit(1)

    config_file_path = sys.argv[1]
    processor = EffectProcessor(config_file_path)
    # 这行代码调用 start()，如果你没有 start 方法就会报错
    processor.start()