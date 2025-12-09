# 易修 - 简易图片编辑应用

## 📱 项目简介

**易修** 是一个功能完备的 Android 图片编辑应用，基于 MVVM 架构开发，采用 Kotlin 语言和 Jetpack 组件构建。该项目涵盖了从基础 UI 展示到复杂 OpenGL ES 图像处理的全流程实现，是学习现代 Android 开发的完整案例。

### 🎯 核心特性

- **现代化架构**：基于 MVVM + LiveData + ViewModel，实现清晰的数据驱动 UI
- **高性能图像渲染**：使用 OpenGL ES 2.0 实现实时图像处理
- **完整编辑功能**：支持裁剪、滤镜（灰度、对比度、饱和度）、缩放、平移
- **手势交互**：双指缩放、单指拖拽、裁剪框拖动等自然交互
- **撤销/重做系统**：支持编辑操作的无限次撤销与重做
- **多媒体支持**：支持 GIF 动图播放和本地相册浏览
- **多设备适配**：已适配多种品牌 Android 设备

## 📸 核心功能截图

### 登录界面
<img src="screenshots/login_screen.png" width="280" alt="登录界面"/>

### 首页 - Banner 轮播与特效
<img src="screenshots/home_banner.png" width="280" alt="首页Banner"/>

### 相册浏览
<img src="screenshots/gallery.png" width="280" alt="相册浏览"/>

### 图片编辑 - 裁剪功能
<img src="screenshots/editor_crop.png" width="280" alt="裁剪编辑"/>

### 图片编辑 - 滤镜调整
<img src="screenshots/editor_filter.png" width="280" alt="滤镜调整"/>

### 个人中心
<img src="screenshots/profile.png" width="280" alt="个人中心"/>

## 🛠️ 技术栈

- **语言**：Kotlin 100%
- **架构**：MVVM (Model-View-ViewModel)
- **Jetpack 组件**：
    - ViewModel + LiveData - 状态管理与数据观察
    - Lifecycle - 生命周期感知
    - Navigation - 简化导航逻辑
- **图像处理**：OpenGL ES 2.0
- **异步处理**：Kotlin 协程
- **图片加载**：Glide
- **权限管理**：AndroidX Permission
- **构建工具**：Gradle 7.0+

## 📋 构建与运行说明

### 环境要求

- **Android Studio**：Arctic Fox 2020.3.1 或更高版本
- **Android SDK**：API 31 (Android 12) 或更高
- **Java**：JDK 11 或更高
- **设备/模拟器**：Android 8.0 (API 26) 或更高版本

### 克隆项目

```bash
git clone https://github.com/yourusername/simple-editing-picture-app.git
cd simple-editing-picture-app
```

### 导入项目

1. 打开 Android Studio
2. 选择 "Open" 或 "Import Project"
3. 导航到项目目录并选择 `build.gradle` 文件
4. 等待 Gradle 同步完成（首次同步可能需要下载依赖）

### 配置与运行

#### 方式一：使用模拟器

1. **创建模拟器**：
    - 打开 Android Studio 的 AVD Manager
    - 创建新的虚拟设备（推荐 Pixel 5，API 31+）
    - 确保开启 GPU 模拟（OpenGL ES 需要）

2. **运行应用**：
    - 选择目标设备
    - 点击运行按钮 (▶️) 或使用快捷键 `Shift + F10`
    - 等待应用构建和安装

#### 方式二：使用真机调试

1. **启用开发者选项**：
    - 进入手机设置 > 关于手机 > 连续点击版本号 7 次
    - 返回设置，进入开发者选项
    - 开启 "USB 调试"

2. **连接设备**：
    - 使用 USB 数据线连接手机和电脑
    - 在手机上确认调试授权

3. **运行应用**：
    - 在 Android Studio 中选择连接的设备
    - 点击运行按钮
    - 应用将自动安装到手机

### 构建配置

#### 调试版本 (Debug)

```bash
# 使用 Gradle 命令行
./gradlew assembleDebug

# 或直接使用 Android Studio
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

#### 发布版本 (Release)

```bash
# 生成 APK
./gradlew assembleRelease

# 生成 AAB (用于 Google Play)
./gradlew bundleRelease
```

**发布版本需要签名**：配置 `app/keystore.properties` 文件或使用默认调试密钥：

```properties
storePassword=android
keyPassword=android
keyAlias=AndroidDebugKey
storeFile=debug.keystore
```

### 项目结构

```
app/
├── src/main/
│   ├── java/com/example/simpleeditingpictureapp/
│   │   ├── activity/          # Activity 类
│   │   ├── viewmodel/         # ViewModel 类
│   │   ├── model/            # Model 类
│   │   ├── opengl_es/        # OpenGL ES 渲染相关
│   │   ├── recyclerview/     # RecyclerView 适配器
│   │   ├── widget/           # 自定义 View
│   │   └── gesture/          # 手势处理
│   ├── res/
│   │   ├── drawable/         # 图片资源
│   │   ├── layout/           # 布局文件
│   │   ├── raw/             # 着色器文件
│   │   └── values/          # 颜色、字符串等资源
│   └── AndroidManifest.xml  # 应用清单
```

### 关键文件说明

| 文件 | 说明 |
|------|------|
| `MainActivity.kt` | 主页面，展示 Banner 和推荐 |
| `EditorActivity.kt` | 图片编辑页面，OpenGL ES 渲染 |
| `GalleryActivity.kt` | 相册浏览页面 |
| `EditorRenderer.kt` | OpenGL ES 渲染器 |
| `CropFrameGLSurfaceView.kt` | 裁剪框视图 |
| `EditorViewModel.kt` | 编辑业务逻辑 |
| `editor_fragment_shader.glsl` | OpenGL 片段着色器 |
| `crop_fragment_shader.glsl` | 裁剪框着色器 |

### 常见问题解决

#### 1. Gradle 同步失败
- 检查网络连接，确保可以访问 Google Maven 仓库
- 尝试使用阿里云镜像：修改 `build.gradle` 中的仓库地址
- 清理项目：`File > Invalidate Caches and Restart`

#### 2. 应用崩溃：OpenGL 初始化失败
- 确保模拟器或设备支持 OpenGL ES 2.0
- 在模拟器设置中启用 "Use dedicated graphics card"
- 真机上确保 GPU 驱动正常

#### 3. 相册权限被拒绝
- 首次进入相册需要授予存储权限
- 如果测试时拒绝权限，需要在系统设置中手动开启
- 对于 Android 10+，确保已适配分区存储

#### 4. 图片加载失败
- 检查是否授予了存储权限
- 确认测试设备上有图片文件
- 查看 Logcat 中的 Glide 错误日志

#### 5. 编译错误：找不到资源
- 运行 `Build > Clean Project` 然后 `Build > Rebuild Project`
- 确保所有资源文件命名合法（无空格、无中文）

### 测试账号

为了方便测试，应用内置了一个测试账号：
- **用户名**：`admin`
- **密码**：`123456`

### 支持的设备

项目已在以下设备上测试通过：
- **小米 11** (Android 12, MIUI 13)
- **华为 P40** (Android 10, EMUI 11)
- **Google Pixel 5 模拟器** (Android 13)
- **Samsung Galaxy S21 模拟器** (Android 12)

### 性能优化建议

1. **大图处理**：当前版本对大图（>10MB）可能存在内存压力，建议：
    - 启用图片采样（downsampling）
    - 实现分块加载（tiling）大型图片

2. **相册加载**：当前为全量加载，可优化为：
    - 分页加载（pagination）
    - 按需加载（on-demand）

3. **内存管理**：确保及时释放 OpenGL 资源，防止内存泄漏

### 代码规范

- **Kotlin 风格**：遵循官方 Kotlin 编码约定
- **命名规范**：
    - 类名：大驼峰，如 `EditorActivity`
    - 变量名：小驼峰，如 `imageList`
    - 资源文件：小写+下划线，如 `activity_editor.xml`
- **注释**：关键算法和复杂逻辑添加详细注释
- **日志**：使用 `Log.d()` 输出调试信息，发布时关闭

### 下一步计划

- [ ] 添加更多编辑工具（旋转、文字添加等）
- [ ] 优化相册加载性能

### 许可证

```
Copyright 2024 Your Name

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 📞 联系与反馈

如果在构建或使用过程中遇到任何问题，欢迎：

1. 查看项目的 [Issues](https://github.com/yourusername/simple-editing-picture-app/issues) 页面
2. 提交新的 Issue 描述问题
3. 通过邮件联系开发者[email](ljy.sbp@gmail.com)

---

**项目状态**：✅ 稳定可用 | 🚀 持续维护中

---

*最后更新：2025年12月*