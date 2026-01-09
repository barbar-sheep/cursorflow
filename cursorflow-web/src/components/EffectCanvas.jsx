import { useEffect, useRef } from 'react'
import './EffectCanvas.css'

function EffectCanvas({ config }) {
  const canvasRef = useRef(null)
  const particlesRef = useRef([])
  const animationFrameRef = useRef(null)
  const mouseRef = useRef({ x: 0, y: 0 })

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    const ctx = canvas.getContext('2d')
    
    // 设置画布尺寸为窗口大小
    const resizeCanvas = () => {
      // 获取设备像素比
      const dpr = window.devicePixelRatio || 1
      
      // 设置显示大小（CSS像素）
      canvas.style.width = window.innerWidth + 'px'
      canvas.style.height = window.innerHeight + 'px'
      
      // 设置实际渲染大小（物理像素）
      canvas.width = window.innerWidth * dpr
      canvas.height = window.innerHeight * dpr
      
      // 缩放上下文以匹配DPI
      ctx.scale(dpr, dpr)
    }
    resizeCanvas()
    window.addEventListener('resize', resizeCanvas)

    // 鼠标移动监听 - 直接使用clientX/clientY，这是相对于视口的坐标
    const handleMouseMove = (e) => {
      mouseRef.current = { 
        x: e.clientX, 
        y: e.clientY 
      }
    }
    
    // 在document上监听，确保能捕获所有鼠标移动
    document.addEventListener('mousemove', handleMouseMove)

    // 渲染循环
    const render = () => {
      if (!config) {
        animationFrameRef.current = requestAnimationFrame(render)
        return
      }

      // 清空画布（带透明度实现拖尾效果）
      ctx.fillStyle = 'rgba(0, 0, 0, 0.05)'
      ctx.fillRect(0, 0, window.innerWidth, window.innerHeight)

      const mouse = mouseRef.current
      const particles = particlesRef.current

      // 生成新粒子
      const particleCount = config.particle_count || 5
      const [lifeMin, lifeMax] = config.life_range || [20, 50]
      const [sizeMin, sizeMax] = config.size_range || [2, 6]
      const [speedMin, speedMax] = config.speed_range || [1, 3]
      const colorConfig = config.particle_color || '#ff0000'
      const gravity = config.gravity || 0.5
      const globalOpacity = config.opacity || 1.0

      // 选择颜色（支持单色或多色）
      let currentColor
      if (Array.isArray(colorConfig)) {
        currentColor = colorConfig[Math.floor(Math.random() * colorConfig.length)]
      } else {
        currentColor = colorConfig
      }

      // 生成新粒子
      for (let i = 0; i < particleCount; i++) {
        const angle = Math.random() * Math.PI * 2
        const speed = speedMin + Math.random() * (speedMax - speedMin)
        
        particles.push({
          x: mouse.x,
          y: mouse.y,
          vx: Math.cos(angle) * speed,
          vy: Math.sin(angle) * speed,
          size: sizeMin + Math.random() * (sizeMax - sizeMin),
          life: lifeMin + Math.floor(Math.random() * (lifeMax - lifeMin)),
          maxLife: lifeMax,
          color: currentColor
        })
      }

      // 更新并渲染粒子
      for (let i = particles.length - 1; i >= 0; i--) {
        const p = particles[i]
        
        // 更新位置
        p.x += p.vx
        p.y += p.vy
        p.vy += gravity
        p.life--

        // 移除死亡粒子
        if (p.life <= 0) {
          particles.splice(i, 1)
          continue
        }

        // 计算渐变参数
        const lifeRatio = p.life / p.maxLife
        const currentSize = p.size * lifeRatio
        const currentAlpha = globalOpacity * (lifeRatio * lifeRatio)

        // 绘制粒子
        ctx.fillStyle = hexToRgba(p.color, currentAlpha)
        ctx.beginPath()
        ctx.arc(p.x, p.y, currentSize, 0, Math.PI * 2)
        ctx.fill()
      }

      animationFrameRef.current = requestAnimationFrame(render)
    }

    // 启动渲染
    render()

    // 清理
    return () => {
      window.removeEventListener('resize', resizeCanvas)
      document.removeEventListener('mousemove', handleMouseMove)
      if (animationFrameRef.current) {
        cancelAnimationFrame(animationFrameRef.current)
      }
    }
  }, [config])

  return <canvas ref={canvasRef} className="effect-canvas" />
}

// 工具函数：hex颜色转rgba
function hexToRgba(hex, alpha) {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

export default EffectCanvas
