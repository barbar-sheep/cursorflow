import { useState, useEffect } from 'react'
import EffectCanvas from './components/EffectCanvas'
import ConfigPanel from './components/ConfigPanel'
import './App.css'

// 默认特效配置
const defaultConfig = `// 粒子特效配置
export default {
  type: "particle",
  
  // 每帧生成的粒子数量
  particle_count: 8,
  
  // 粒子生命周期范围 [最小, 最大] (帧数)
  life_range: [20, 50],
  
  // 粒子大小范围 [最小, 最大] (像素)
  size_range: [2, 6],
  
  // 粒子速度范围 [最小, 最大]
  speed_range: [1, 3],
  
  // 粒子颜色 (支持单色或多色数组)
  particle_color: ["#ff6b6b", "#4ecdc4", "#45b7d1", "#f7b731", "#5f27cd"],
  
  // 重力加速度
  gravity: 0.3,
  
  // 全局不透明度
  opacity: 0.9
}`;

function App() {
  const [config, setConfig] = useState(defaultConfig)
  const [parsedConfig, setParsedConfig] = useState(null)
  const [error, setError] = useState(null)
  const [showPanel, setShowPanel] = useState(true)

  // 解析用户配置
  const parseConfig = (code) => {
    try {
      // 移除 export default 并执行配置代码
      const cleanCode = code.replace(/export\s+default\s+/, 'return ')
      const configFunction = new Function(cleanCode)
      const result = configFunction()
      
      setParsedConfig(result)
      setError(null)
      return true
    } catch (e) {
      setError(`配置解析错误: ${e.message}`)
      return false
    }
  }

  // 初始化解析默认配置
  useEffect(() => {
    parseConfig(config)
  }, [])

  // 应用配置
  const handleApplyConfig = (newConfig) => {
    setConfig(newConfig)
    parseConfig(newConfig)
  }

  return (
    <div className="app">
      {/* 全屏特效画布 */}
      <EffectCanvas config={parsedConfig} />
      
      {/* 配置面板 */}
      <ConfigPanel
        config={config}
        onApply={handleApplyConfig}
        error={error}
        visible={showPanel}
        onToggle={() => setShowPanel(!showPanel)}
      />
      
      {/* 切换按钮 */}
      {!showPanel && (
        <button 
          className="toggle-button"
          onClick={() => setShowPanel(true)}
          title="打开配置面板"
        >
          ⚙️
        </button>
      )}
    </div>
  )
}

export default App
