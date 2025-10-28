# SocketIO Java 服务端框架

[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)](https://maven.apache.org/)
[![Netty](https://img.shields.io/badge/Netty-4.x-green.svg)](https://netty.io/)

基于 Socket.IO 协议设计的 Java 服务端框架，底层采用 Netty 高性能网络框架。该项目参考了 [netty-socketio](https://github.com/mrniko/netty-socketio) 开源项目，并在此基础上新增了注解驱动开发和多端点支持等特性。

## 📋 目录

- [特性](#特性)
- [快速开始](#快速开始)
- [核心概念](#核心概念)
- [注解说明](#注解说明)
- [使用示例](#使用示例)
- [配置说明](#配置说明)
- [高级功能](#高级功能)
- [性能优化](#性能优化)
- [FAQ](#faq)
- [参考资料](#参考资料)

## ✨ 特性

- 🚀 **高性能**: 基于 Netty 异步 I/O 框架，支持高并发连接
- 📡 **Socket.IO 兼容**: 完全兼容 Socket.IO 协议，支持 WebSocket 和 HTTP 长轮询
- 🎯 **注解驱动**: 基于注解的事件处理，简化开发流程
- 🔄 **多端点支持**: 支持多个 WebSocket 端点，灵活的路由配置
- 🏠 **房间与命名空间**: 完整的房间管理和命名空间隔离机制
- 📢 **广播操作**: 支持全局广播、房间广播等多种消息分发方式
- 🔐 **拦截器机制**: 支持连接认证、消息拦截等中间件功能
- 📦 **二进制数据**: 支持二进制数据传输
- 🎛️ **灵活配置**: 丰富的配置选项，适应不同业务场景

## 🚀 快速开始

### 环境要求

- Java 8+
- Maven 3.6+
- [Teambeit Cloud Microservices Framework 1.0+](https://github.com/wayken/cloud)

### 添加依赖

```xml
<dependency>
    <groupId>cloud.apposs</groupId>
    <artifactId>teambeit-socketio</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 创建服务端点

```java
import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.annotation.*;

@ServerEndpoint("/socket.io")
public class ChatEndpoint {
    @OnConnect
    public void onConnect(SocketIOSession session) {
        System.out.println("客户端连接: " + session.getSessionId());
    }
    
    @OnEvent("message")
    public void onMessage(SocketIOSession session, String message) {
        System.out.println("收到消息: " + message);
        // 回复消息
        session.sendEvent("message", "服务器回复: " + message);
    }
    
    @OnDisconnect
    public void onDisconnect(SocketIOSession session) {
        System.out.println("客户端断开连接: " + session.getSessionId());
    }
}
```

### 启动服务器

```java
import cloud.apposs.socketio.SocketIOApplication;

public class ChatApplication {
    public static void main(String[] args) throws Exception {
        SocketIOApplication.run(ChatApplication.class, args);
    }
}
```

### 客户端连接

```javascript
const socket = io('http://localhost:9092');

socket.on('connect', () => {
    console.log('连接成功');
    socket.emit('message', '你好，服务器！');
});

socket.on('message', (data) => {
    console.log('收到消息:', data);
});
```

## 🔧 核心概念

### ApplicationContext（应用上下文）

应用上下文是整个 SocketIO 服务的核心容器，负责管理服务的生命周期、配置信息和组件注册。

### Namespace（命名空间）

命名空间提供了逻辑隔离机制，不同命名空间下的客户端之间相互独立。默认命名空间为 `/`。

### Room（房间）

房间是命名空间内的进一步分组机制，客户端可以加入或离开房间，实现精准的消息广播。

### Session（会话）

每个连接的客户端都对应一个 Session 对象，包含连接信息、认证状态等。

## 📝 注解说明

### @ServerEndpoint

定义 WebSocket 服务端点，支持多路径匹配。

```java
@ServerEndpoint({"/socket.io", "/ws"})
public class MultiPathEndpoint {
    // 端点逻辑
}
```

**属性说明：**
- `value`: 路径数组，支持多个路径映射到同一个端点
- `host`: 主机匹配规则，默认为 `*`（匹配所有主机）

### @OnConnect

标记连接建立时的回调方法。

```java
@OnConnect
public void handleConnect(SocketIOSession session) {
    // 连接建立处理逻辑
}
```

### @OnEvent

标记事件处理方法，支持多种参数类型。

```java
@OnEvent("chat")
public void handleChat(SocketIOSession session, ChatMessage message) {
    // 事件处理逻辑
}

@OnEvent("multidata")
public void handleMultiData(SocketIOSession session, String text, Integer count, ChatObject obj) {
    // 支持多个参数
}
```

### @OnDisconnect

标记连接断开时的回调方法。

```java
@OnDisconnect
public void handleDisconnect(SocketIOSession session) {
    // 断开连接处理逻辑
}
```

### @OnError

标记错误处理方法。

```java
@OnError
public void handleError(Throwable error) {
    // 错误处理逻辑
}
```

### @Order

控制同一事件多个处理方法的执行顺序。

```java
@OnConnect
@Order(1)
public void firstHandler(SocketIOSession session) {
    // 优先执行
}

@OnConnect
@Order(2)
public void secondHandler(SocketIOSession session) {
    // 后续执行
}
```

## 🎯 使用示例

### 基础聊天室

```java
@ServerEndpoint("/chat")
public class ChatRoomEndpoint {
    @OnConnect
    public void onConnect(SocketIOSession session) {
        // 加入默认房间
        session.joinRoom("lobby");
        session.getRoomOperations("lobby").sendEvent("userJoin", 
            "用户 " + session.getSessionId() + " 加入聊天室");
    }
    
    @OnEvent("sendMessage")
    public void onSendMessage(SocketIOSession session, ChatMessage message) {
        // 广播消息到房间
        session.getRoomOperations("lobby").sendEvent("receiveMessage", message);
    }
    
    @OnEvent("joinRoom")
    public void onJoinRoom(SocketIOSession session, String roomName) {
        session.leaveRoom("lobby");
        session.joinRoom(roomName);
        session.sendEvent("roomChanged", roomName);
    }
}
```

### 二进制数据传输

```java
@ServerEndpoint("/binary")
public class BinaryEndpoint {
    @OnEvent("uploadFile")
    public void onUploadFile(SocketIOSession session, byte[] fileData) {
        // 处理二进制文件数据
        System.out.println("接收到文件，大小: " + fileData.length + " 字节");
        
        // 返回处理结果
        session.sendEvent("uploadResult", "文件上传成功");
    }
}
```

### 拦截器使用

```java
@Component
public class AuthInterceptor implements CommandarInterceptor {
    @Override
    public boolean isAuthorized(HandshakeData handshakeData) throws Exception {
        // 连接前认证
        String token = handshakeData.getSingleUrlParam("token");
        return validateToken(token);
    }
    
    @Override
    public boolean onEvent(Commandar commandar, SocketIOSession session, List<Object> arguments) {
        // 事件拦截
        System.out.println("拦截事件: " + commandar.getEventName());
        return true;
    }
    
    @Override
    public void afterCompletion(Commandar commandar, SocketIOSession session, Throwable throwable) {
        // 完成后处理
        if (throwable != null) {
            System.err.println("事件处理出错: " + throwable.getMessage());
        }
    }
    
    private boolean validateToken(String token) {
        // 实现 token 验证逻辑
        return token != null && !token.isEmpty();
    }
}
```

## ⚙ 配置说明

### 基础配置

```java
public class CustomApplication {
    public static void main(String[] args) throws Exception {
        SocketIOConfig config = new SocketIOConfig();
        config.setPort(9092);                    // 端口号
        config.setHostname("0.0.0.0");          // 绑定地址
        config.setMaxFramePayloadLength(65536); // 最大帧大小
        config.setMaxHttpContentLength(65536);  // 最大 HTTP 内容长度
        config.setWorkerThreads(0);             // 工作线程数，0 表示自动
        
        SocketIOApplication.run(CustomApplication.class, config, args);
    }
}
```

### 配置文件

支持通过配置文件进行配置：

```xml
<!-- application.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<socketio-config>
    <!-- 基础配置 -->
    <!-- 扫描基础包，必须配置，框架会自动扫描注解类 -->
    <property name="basePackage">cloud.apposs.socketio.sample</property>
    <!-- 项目输入输出编码 -->
    <property name="charset">utf-8</property>
    <!-- 绑定主机和端口 -->
    <property name="host">0.0.0.0</property>
    <property name="port">9088</property>
    <property name="keepAlive">true</property>
    <!-- 日志相关配置 -->
    <!-- 日志输出级别，可为FATAL（致命）、ERROR（错误）、WARN（警告）、INFO（信息）、DEBUG（调试）、TRACE（追踪）、OFF（关闭） -->
    <property name="logLevel">info</property>
</socketio-config>
```

## 🏗️ 高级功能

### 广播操作

```java
// 全局广播
session.getBroadcastOperations().sendEvent("globalMessage", "全局消息");

// 房间广播
session.getRoomOperations("room1").sendEvent("roomMessage", "房间消息");

// 命名空间广播
session.getNamespace().getBroadcastOperations().sendEvent("namespaceMessage", "命名空间消息");
```

### 多命名空间支持

```java
@ServerEndpoint("/admin")
public class AdminEndpoint {
    // 管理员端点
}

@ServerEndpoint("/user")
public class UserEndpoint {
    // 用户端点
}
```

### 自定义数据类型

```java
public class ChatMessage {
    private String username;
    private String content;
    private long timestamp;
    
    // 必须有默认构造函数
    public ChatMessage() {}
    
    // getter/setter 方法
}

@OnEvent("chat")
public void handleChat(SocketIOSession session, ChatMessage message) {
    // 自动反序列化为 ChatMessage 对象
}
```

## 🔧 性能优化

### 连接池配置

```java
SocketIOConfig config = new SocketIOConfig();
config.setWorkerCount(Runtime.getRuntime().availableProcessors() * 2);
```

### 内存优化

```java
// 设置合适的缓冲区大小
config.setMaxFramePayloadLength(1024 * 64);  // 64KB
config.setMaxHttpContentLength(1024 * 64);   // 64KB
```

### 监控和统计

```java
@Component
public class PerformanceInterceptor implements CommandarInterceptor {
    @Override
    public void afterCompletion(Commandar commandar, SocketIOSession session, Throwable throwable) {
        // 记录性能指标
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("事件 " + commandar.getEventName() + " 耗时: " + duration + "ms");
    }
}
```

## ❓ FAQ

### Q: 如何实现负载均衡？

A: 可以使用 Nginx 等反向代理进行负载均衡。

### Q: 支持哪些客户端？

A: 支持所有标准的 Socket.IO 客户端，包括：
- JavaScript (浏览器)
- Node.js
- Java
- Python
- Swift (iOS)
- Kotlin/Java (Android)

### Q: 如何调试连接问题？

A: 启用调试日志：

```java
config.setLogLevel(LogLevel.DEBUG);
```

## 🔗 参考资料

- [Socket.IO 官方文档](https://socket.io/zh-CN/docs/v4/)
- [Netty-SocketIO 项目](https://github.com/mrniko/netty-socketio)
- [Socket.IO 协议详解](https://github.com/socketio/socket.io-protocol)
- [Netty 官方文档](https://netty.io/wiki/)

## 📄 许可证

本项目采用 Apache License 2.0 许可证，详见 [LICENSE](LICENSE) 文件。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

如有问题或建议，请通过 GitHub Issues 联系我们。
