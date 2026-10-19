# 最终版测试记录
本记录对应应用代码基准 83d8071；文档与交付脚本另行提交。

| 检查 | 结果 | 证据 |
|---|---|---|
| 后端 JUnit 与真实 MySQL 集成 | 102 项，失败/错误/跳过均为 0 | coverage 下各套件 JSON |
| 前端 Vitest | 17 项通过 | frontend/src/**/*.test.ts |
| 外部 HTTP 黑盒 | 15 项通过 | blackbox.json |
| Chromium 端到端 | 3 条场景通过 | browser-tests.json |
| 生产构建 | 通过，保留依赖大包提示 | npm run build |
| 真实 DeepSeek | NEEDS_CONFIRMATION，确认后落库 | live-model.json、persistence.json |
| 本机读取性能 | 50 次 P50 30.31ms / P95 34.36ms；8 并发 80 次全 200 | performance.json |
| 服务重启恢复 | 未完成，自动审批拒绝服务重启 | 不计入通过项 |

白盒指标由 JaCoCo XML 统计，不以用例数量替代覆盖率。

| 模块 | 行覆盖 | 分支覆盖 |
|---|---|---|
| backend | 88.18% | 66.04% |
| backend-hjh | 49.36% | 43.57% |

范围：本机 Windows、真实 MySQL、Chromium。没有宣称其他浏览器、真机、生产容量、OCR/ASR 或正式认证通过。原始测试状态与时间保留在 evidence；Surefire 导出仅保留套件和用例属性，不发布完整 JVM 环境。
