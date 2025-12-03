# 前后端通信模块使用指南

本指南说明了如何使用新添加的前后端通信模块，并如何启动模拟服务器进行测试。

## 模块功能

1.  **Axios 封装 (`api.js`)**:
    *   统一配置了 API 请求的基础 URL (`http://localhost:3000/`)。
    *   通过拦截器实现了请求状态的自动监控。

2.  **请求类 (`request.js`)**:
    *   提供 `Request` 类，用于创建和管理 API 请求。
    *   实例化时需提供一个“请求名称”（用于显示）和一个“API 路径”（URL 的后半部分）。

3.  **请求状态胶囊 (`request-capsule.css` & `api.js`)**:
    *   当发起 API 请求时，页面右下角会自动出现一个状态胶囊。
    *   胶囊会实时显示请求的加载进度。
    *   请求成功或失败后，胶囊会显示最终状态，并在 3 秒后自动消失。
    *   胶囊的颜色会根据日间/夜间模式自动调整。

## 如何使用

### 1. 引入脚本和样式

在你的 HTML 文件中，确保按顺序引入了以下文件：

```html
<head>
  <!-- ... other head elements -->
  <link rel="stylesheet" href="css/theme.css">
  <link rel="stylesheet" href="css/request-capsule.css">
</head>
<body>
  <!-- ... your content ... -->

  <!-- 脚本引入 -->
  <script src="js/axios.min.js"></script>
  <script src="js/api.js"></script>
  <script src="js/request.js"></script>
  <script src="js/theme-manager.js"></script>
</body>
```

### 2. 发起一个 API 请求

你可以使用 `Request` 类来方便地发起请求。

**示例：发起一个 GET 请求**

```javascript
// 创建一个请求实例
// '获取用户列表' 是用于显示在状态胶囊中的名称
// '/users' 是 API 路径，会自动与 api.js 中配置的 baseURL 拼接
const userRequest = new Request('获取用户列表', '/users');

// 发起请求
userRequest.get()
  .then(response => {
    console.log('成功:', response.data);
  })
  .catch(error => {
    console.error('失败:', error);
  });
```

**示例：发起一个 POST 请求**

```javascript
const productRequest = new Request('创建新产品', '/products');

productRequest.post({ name: '新手机', price: 6999 })
  .then(response => {
    console.log('创建成功:', response.data);
  })
  .catch(error => {
    console.error('创建失败:', error);
  });
```

## 如何测试

为了方便前端测试，项目根目录提供了一个 `mock-server.js` 文件。

### 1. 准备环境

*   确保你的电脑上已经安装了 [Node.js](https://nodejs.org/)。
*   在项目根目录 (`D:\test\sem\`) 打开终端。

### 2. 安装依赖

*   如果你是第一次运行，需要先初始化 npm 并安装依赖。在终端中执行以下命令：

    ```bash
    npm init -y
    npm install express cors
    ```

### 3. 启动模拟服务器

*   在终端中执行以下命令：

    ```bash
    node mock-server.js
    ```

*   看到 `Mock API server listening at http://localhost:3000` 消息即表示服务器已成功启动。

### 4. 查看效果

*   用浏览器打开 `untitled/web/simple.html` 文件。
*   点击页面上的 **"发起测试请求"** 或 **"发起错误请求"** 按钮，观察右下角出现的请求状态胶囊。
