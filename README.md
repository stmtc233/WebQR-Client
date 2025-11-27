# WebQR Client

WebQR Client 是一个 Android 应用程序，旨在扫描二维码并将扫描内容实时上传至指定的 Web 服务器。该应用适用于需要通过移动设备扫描二维码并在网页端进行展示或处理的场景，例如动态二维码签到、库存管理或会议入场验证等。

WebQR Client is an Android application designed to scan QR codes and upload the content to a specified web server in real-time. It is suitable for scenarios requiring QR code scanning via mobile devices with display or processing on a web page, such as dynamic QR code check-ins, inventory management, or event entry verification.

## 后端项目 / Backend Project

本项目需要配合后端服务使用。后端项目地址如下：
This project requires a backend service. The backend project repository is located at:

**[https://github.com/stmtc233/WebQR-server](https://github.com/stmtc233/WebQR-server)**

## 功能特点 / Features

*   **实时扫码 (Real-time Scanning):** 使用 Android CameraX 快速识别二维码。
*   **自动上传 (Auto Upload):** 扫描成功后自动将数据发送至配置的服务器接口。
*   **API 切换 (API Switching):** 支持在应用内动态切换 API 端点（如正式环境与备份环境）。
*   **变焦控制 (Zoom Control):** 提供滑动条控制相机变焦，适应不同距离的扫码需求。
*   **Material You 设计:** 支持动态取色 (Dynamic Colors)，界面现代美观。
*   **防重复 (Anti-duplication):** 防止连续重复上传相同的二维码数据。

## 编译与配置 / Build & Configuration

**注意：本项目需要自行编译。** 请确保您已安装 Android Studio 和 JDK 环境。
**Note: This project requires self-compilation.** Please ensure you have Android Studio and a JDK environment installed.

### 1. 克隆项目 / Clone Project

```bash
git clone https://github.com/stmtc233/WebQR-client.git
```

### 2. 配置 API 地址 / Configure API URLs

为了保护您的服务器地址不被泄露，本项目将 API 配置从代码中剥离。您需要创建一个 `local.properties` 文件来指定您的后端地址。

To protect your server addresses, API configuration is separated from the codebase. You need to create a `local.properties` file to specify your backend endpoints.

1.  在项目根目录下找到或新建 `local.properties` 文件。
    Locate or create the `local.properties` file in the project root directory.

2.  添加以下配置信息：
    Add the following configuration:

    ```properties
    # API Configuration
    # 请将 URL 替换为您自己的 API 地址 (必须以 / 结尾)
    # Please replace the URLs with your own API addresses (must end with /)
    
    API_DEFAULT_URL="https://your-api-domain.com/"
    API_BACKUP_URL="https://your-backup-api-domain.com/"
    ```

3.  点击 Android Studio 中的 **"Sync Project with Gradle Files"** 按钮应用更改。
    Click **"Sync Project with Gradle Files"** in Android Studio to apply changes.

### 3. 编译运行 / Build & Run

*   连接您的 Android 设备或启动模拟器。
*   在 Android Studio 中点击 **Run** (绿色播放按钮) 即可安装并运行应用。

## 截图 / Screenshots

*(此处可以预留截图位置 / Space for screenshots)*

## 许可证 / License

[MIT License](LICENSE)
