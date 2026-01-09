import { useState, useEffect } from 'react'
import './ConfigPanel.css'

// 预设模板
const templates = {
  default: `// 默认彩虹粒子
export default {
  type: "particle",
  particle_count: 8,
  life_range: [20, 50],
  size_range: [2, 6],
  speed_range: [1, 3],
  particle_color: ["#ff6b6b", "#4ecdc4", "#45b7d1", "#f7b731", "#5f27cd"],
  gravity: 0.3,
  opacity: 0.9
}`,
  
  fire: `// 火焰效果
export default {
  type: "particle",
  particle_count: 12,
  life_range: [15, 40],
  size_range: [3, 8],
  speed_range: [0.5, 2],
  particle_color: ["#ff4500", "#ff6347", "#ffa500", "#ffff00"],
  gravity: -0.2,  // 负重力，向上飘
  opacity: 0.8
}`,
  
  snow: `// 雪花效果
export default {
  type: "particle",
  particle_count: 5,
  life_range: [40, 80],
  size_range: [1, 4],
  speed_range: [0.2, 1],
  particle_color: ["#ffffff", "#e3f2fd", "#bbdefb"],
  gravity: 0.5,
  opacity: 0.7
}`,

  neon: `// 霓虹灯效果
export default {
  type: "particle",
  particle_count: 15,
  life_range: [10, 30],
  size_range: [2, 5],
  speed_range: [2, 4],
  particle_color: ["#00ffff", "#ff00ff", "#00ff00", "#ffff00"],
  gravity: 0.1,
  opacity: 1.0
}`,

  galaxy: `// 星系效果
export default {
  type: "particle",
  particle_count: 6,
  life_range: [30, 70],
  size_range: [1, 3],
  speed_range: [0.5, 1.5],
  particle_color: ["#9b59b6", "#3498db", "#e74c3c", "#f39c12"],
  gravity: 0.05,
  opacity: 0.95
}`
}

function ConfigPanel({ config, onApply, error, visible, onToggle }) {
  const [code, setCode] = useState(config)
  const [selectedTemplate, setSelectedTemplate] = useState('default')

  useEffect(() => {
    setCode(config)
  }, [config])

  const handleApply = () => {
    onApply(code)
  }

  const handleLoadTemplate = (templateName) => {
    setSelectedTemplate(templateName)
    setCode(templates[templateName])
  }

  if (!visible) return null

  return (
    <div className="config-panel">
      <div className="panel-header">
        <h2>⚙️ 特效配置编辑器</h2>
        <button className="close-button" onClick={onToggle}>✕</button>
      </div>

      <div className="panel-content">
        {/* 模板选择 */}
        <div className="template-section">
          <label>预设模板：</label>
          <div className="template-buttons">
            {Object.keys(templates).map(name => (
              <button
                key={name}
                className={`template-btn ${selectedTemplate === name ? 'active' : ''}`}
                onClick={() => handleLoadTemplate(name)}
              >
                {name === 'default' ? '默认' : 
                 name === 'fire' ? '火焰' : 
                 name === 'snow' ? '雪花' : 
                 name === 'neon' ? '霓虹' : '星系'}
              </button>
            ))}
          </div>
        </div>

        {/* 代码编辑器 */}
        <div className="editor-section">
          <label>配置代码：</label>
          <textarea
            className="code-editor"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            spellCheck={false}
          />
        </div>

        {/* 错误提示 */}
        {error && (
          <div className="error-message">
            ⚠️ {error}
          </div>
        )}

        {/* 应用按钮 */}
        <button className="apply-button" onClick={handleApply}>
          ✨ 应用特效
        </button>

        {/* 使用说明 */}
        <div className="help-section">
          <h3>📖 参数说明</h3>
          <ul>
            <li><code>particle_count</code> - 每帧生成粒子数量</li>
            <li><code>life_range</code> - 粒子生命周期 [最小, 最大]</li>
            <li><code>size_range</code> - 粒子大小 [最小, 最大]</li>
            <li><code>speed_range</code> - 粒子速度 [最小, 最大]</li>
            <li><code>particle_color</code> - 颜色（单色或数组）</li>
            <li><code>gravity</code> - 重力（负值向上）</li>
            <li><code>opacity</code> - 不透明度 (0-1)</li>
          </ul>
        </div>
      </div>
    </div>
  )
}

export default ConfigPanel
