package com.cursorflow.effect;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 粒子特效：鼠标移动时生成粒子（火花效果）
 */
public class ParticleEffect implements ITrailEffect {
    // 粒子列表
    private List<Particle> particles = new ArrayList<>();
    // 粒子数量
    private int particleCount = 15;
    // 粒子颜色
    private Color particleColor = Color.ORANGE;
    // 粒子最大生命周期
    private int maxLife = 60;
    // 随机数生成器
    private Random random = new Random();

    @Override
    public void init(Map<String, Object> config) {
        if (config != null) {
            particleCount = config.getOrDefault("particleCount", 15) instanceof Integer ? (Integer) config.get("particleCount") : 15;
            maxLife = config.getOrDefault("maxLife", 60) instanceof Integer ? (Integer) config.get("maxLife") : 60;
            particleColor = config.getOrDefault("color", "#FF9800") instanceof String ? Color.web((String) config.get("color")) : Color.ORANGE;
        }
    }

    @Override
    public void render(GraphicsContext gc, int mouseX, int mouseY) {
        // 生成新粒子
        for (int i = 0; i < particleCount; i++) {
            particles.add(createParticle(mouseX, mouseY));
        }

        // 绘制并更新粒子
        gc.setFill(particleColor);
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();

            // 绘制粒子（圆形）
            gc.fillOval(p.x, p.y, p.size, p.size);

            // 移除死亡粒子
            if (p.life <= 0) {
                particles.remove(i);
            }
        }
    }

    @Override
    public void dispose() {
        particles.clear();
    }

    // 创建单个粒子
    private Particle createParticle(int x, int y) {
        double dx = (random.nextDouble() - 0.5) * 4; // X方向速度（-2 ~ 2）
        double dy = (random.nextDouble() - 0.5) * 4; // Y方向速度
        double size = random.nextDouble() * 3 + 1; // 粒子大小（1 ~ 4）
        int life = random.nextInt(maxLife) + 30; // 生命周期（30 ~ 90帧）
        return new Particle(x, y, dx, dy, size, life);
    }

    // 粒子内部类
    private static class Particle {
        double x, y; // 坐标
        double dx, dy; // 速度
        double size; // 大小
        int life; // 生命周期

        Particle(double x, double y, double dx, double dy, double size, int life) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.size = size;
            this.life = life;
        }

        // 更新粒子状态（移动+生命周期减少）
        void update() {
            x += dx;
            y += dy;
            life--;
            // 生命周期越短，粒子越小（渐变消失）
            size *= 0.98;
        }
    }
}