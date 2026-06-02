# Luno

[English](README_EN.md)

Luno 是 Android 侧边手势、快捷启动与系统动作增强工具。

当前版本：`v1.6.2`

- 手势触钮（位置/大小/颜色/角度/镜像均可自定义）
- 子手势（每个触钮 8 方向子手势）
- 动作面板（弧形/网格/饼图布局）
- 快捷启动（应用搜索/快捷方式/频率记录）
- 虚拟鼠标（连续模式/灵敏度/回缩动画）
- 动作库（Shell/链接/Activity 集中管理）
- 冻结管理（Shizuku 冻结/解冻/保护名单）
- 个性化（Material You 配色/动画/振动定制）

## 系统要求

Android 13+，需无障碍服务，冻结功能需 Shizuku。

## 构建

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## License

参见 [`LICENSE`](LICENSE)。

基于 [SideGesture](https://github.com/aaronzzx/SideGesture) 修改。
