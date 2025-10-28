# SocketIO Java Server Framework

[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)](https://maven.apache.org/)
[![Netty](https://img.shields.io/badge/Netty-4.x-green.svg)](https://netty.io/)

A Java server framework designed based on the Socket.IO protocol, built on top of the high-performance Netty network framework. This project is inspired by the [netty-socketio](https://github.com/mrniko/netty-socketio) open-source project and adds annotation-driven development and multi-endpoint support features.

## 📋 Table of Contents

- [Features](#features)
- [Quick Start](#quick-start)
- [Core Concepts](#core-concepts)
- [Annotations](#annotations)
- [Usage Examples](#usage-examples)
- [Configuration](#configuration)
- [Advanced Features](#advanced-features)
- [Performance Optimization](#performance-optimization)
- [FAQ](#faq)
- [References](#references)

## ✨ Features

- 🚀 **High Performance**: Built on Netty asynchronous I/O framework, supporting high concurrent connections
- 📡 **Socket.IO Compatible**: Fully compatible with Socket.IO protocol, supporting WebSocket and HTTP long polling
- 🎯 **Annotation-Driven**: Annotation-based event handling, simplifying development workflow
- 🔄 **Multi-Endpoint Support**: Support for multiple WebSocket endpoints with flexible routing configuration
- 🏠 **Rooms & Namespaces**: Complete room management and namespace isolation mechanism
- 📢 **Broadcast Operations**: Support for global broadcast, room broadcast, and various message distribution methods
- 🔐 **Interceptor Mechanism**: Support for connection authentication, message interception, and middleware functionality
- 📦 **Binary Data**: Support for binary data transmission
- 🎛️ **Flexible Configuration**: Rich configuration options to adapt to different business scenarios

## 🚀 Quick Start

### Requirements

- Java 8+
- Maven 3.6+
- [Teambeit Cloud Microservices Framework 1.0+](https://github.com/wayken/cloud)

### Add Dependency

```xml
<dependency>
    <groupId>cloud.apposs</groupId>
    <artifactId>teambeit-socketio</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Create Server Endpoint

```java
import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.annotation.*;

@ServerEndpoint("/socket.io")
public class ChatEndpoint {
    @OnConnect
    public void onConnect(SocketIOSession session) {
        System.out.println("Client connected: " + session.getSessionId());
    }
    
    @OnEvent("message")
    public void onMessage(SocketIOSession session, String message) {
        System.out.println("Received message: " + message);
        // Reply with message
        session.sendEvent("message", "Server reply: " + message);
    }
    
    @OnDisconnect
    public void onDisconnect(SocketIOSession session) {
        System.out.println("Client disconnected: " + session.getSessionId());
    }
}
```

### Start Server

```java
import cloud.apposs.socketio.SocketIOApplication;

public class ChatApplication {
    public static void main(String[] args) throws Exception {
        SocketIOApplication.run(ChatApplication.class, args);
    }
}
```

### Client Connection

```javascript
const socket = io('http://localhost:9092');

socket.on('connect', () => {
    console.log('Connected successfully');
    socket.emit('message', 'Hello, server!');
});

socket.on('message', (data) => {
    console.log('Received message:', data);
});
```

## 🔧 Core Concepts

### ApplicationContext

The application context is the core container of the entire SocketIO service, responsible for managing the service lifecycle, configuration information, and component registration.

### Namespace

Namespaces provide logical isolation mechanisms where clients in different namespaces are independent of each other. The default namespace is `/`.

### Room

Rooms are a further grouping mechanism within namespaces, allowing clients to join or leave rooms for precise message broadcasting.

### Session

Each connected client corresponds to a Session object containing connection information, authentication status, etc.

## 📝 Annotations

### @ServerEndpoint

Defines WebSocket server endpoints, supporting multi-path matching.

```java
@ServerEndpoint({"/socket.io", "/ws"})
public class MultiPathEndpoint {
    // Endpoint logic
}
```

**Attributes:**
- `value`: Path array, supporting multiple path mappings to the same endpoint
- `host`: Host matching rule, default is `*` (matches all hosts)

### @OnConnect

Marks callback methods for connection establishment.

```java
@OnConnect
public void handleConnect(SocketIOSession session) {
    // Connection establishment handling logic
}
```

### @OnEvent

Marks event handling methods, supporting multiple parameter types.

```java
@OnEvent("chat")
public void handleChat(SocketIOSession session, ChatMessage message) {
    // Event handling logic
}

@OnEvent("multidata")
public void handleMultiData(SocketIOSession session, String text, Integer count, ChatObject obj) {
    // Supports multiple parameters
}
```

### @OnDisconnect

Marks callback methods for connection disconnection.

```java
@OnDisconnect
public void handleDisconnect(SocketIOSession session) {
    // Disconnection handling logic
}
```

### @OnError

Marks error handling methods.

```java
@OnError
public void handleError(Throwable error) {
    // Error handling logic
}
```

### @Order

Controls the execution order of multiple handling methods for the same event.

```java
@OnConnect
@Order(1)
public void firstHandler(SocketIOSession session) {
    // Execute first
}

@OnConnect
@Order(2)
public void secondHandler(SocketIOSession session) {
    // Execute second
}
```

## 🎯 Usage Examples

### Basic Chat Room

```java
@ServerEndpoint("/chat")
public class ChatRoomEndpoint {
    @OnConnect
    public void onConnect(SocketIOSession session) {
        // Join default room
        session.joinRoom("lobby");
        session.getRoomOperations("lobby").sendEvent("userJoin", 
            "User " + session.getSessionId() + " joined the chat room");
    }
    
    @OnEvent("sendMessage")
    public void onSendMessage(SocketIOSession session, ChatMessage message) {
        // Broadcast message to room
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

### Binary Data Transmission

```java
@ServerEndpoint("/binary")
public class BinaryEndpoint {
    @OnEvent("uploadFile")
    public void onUploadFile(SocketIOSession session, byte[] fileData) {
        // Handle binary file data
        System.out.println("Received file, size: " + fileData.length + " bytes");
        
        // Return processing result
        session.sendEvent("uploadResult", "File upload successful");
    }
}
```

### Interceptor Usage

```java
@Component
public class AuthInterceptor implements CommandarInterceptor {
    @Override
    public boolean isAuthorized(HandshakeData handshakeData) throws Exception {
        // Pre-connection authentication
        String token = handshakeData.getSingleUrlParam("token");
        return validateToken(token);
    }
    
    @Override
    public boolean onEvent(Commandar commandar, SocketIOSession session, List<Object> arguments) {
        // Event interception
        System.out.println("Intercepting event: " + commandar.getEventName());
        return true;
    }
    
    @Override
    public void afterCompletion(Commandar commandar, SocketIOSession session, Throwable throwable) {
        // Post-completion handling
        if (throwable != null) {
            System.err.println("Event handling error: " + throwable.getMessage());
        }
    }
    
    private boolean validateToken(String token) {
        // Implement token validation logic
        return token != null && !token.isEmpty();
    }
}
```

## ⚙ Configuration

### Basic Configuration

```java
public class CustomApplication {
    public static void main(String[] args) throws Exception {
        SocketIOConfig config = new SocketIOConfig();
        config.setPort(9092);                    // Port number
        config.setHostname("0.0.0.0");          // Bind address
        config.setMaxFramePayloadLength(65536); // Maximum frame size
        config.setMaxHttpContentLength(65536);  // Maximum HTTP content length
        config.setWorkerThreads(0);             // Worker thread count, 0 means auto
        
        SocketIOApplication.run(CustomApplication.class, config, args);
    }
}
```

### Configuration File

Supports configuration through configuration files:

```xml
<!-- application.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<socketio-config>
    <!-- Basic configuration -->
    <!-- Base package scan, must be configured, framework will automatically scan annotation classes -->
    <property name="basePackage">cloud.apposs.socketio.sample</property>
    <!-- Project input/output encoding -->
    <property name="charset">utf-8</property>
    <!-- Bind host and port -->
    <property name="host">0.0.0.0</property>
    <property name="port">9088</property>
    <property name="keepAlive">true</property>
    <!-- Log related configuration -->
    <!-- Log output level, can be FATAL, ERROR, WARN, INFO, DEBUG, TRACE, OFF -->
    <property name="logLevel">info</property>
</socketio-config>
```

## 🏗️ Advanced Features

### Broadcast Operations

```java
// Global broadcast
session.getBroadcastOperations().sendEvent("globalMessage", "Global message");

// Room broadcast
session.getRoomOperations("room1").sendEvent("roomMessage", "Room message");

// Namespace broadcast
session.getNamespace().getBroadcastOperations().sendEvent("namespaceMessage", "Namespace message");
```

### Multi-Namespace Support

```java
@ServerEndpoint("/admin")
public class AdminEndpoint {
    // Admin endpoint
}

@ServerEndpoint("/user")
public class UserEndpoint {
    // User endpoint
}
```

### Custom Data Types

```java
public class ChatMessage {
    private String username;
    private String content;
    private long timestamp;
    
    // Must have default constructor
    public ChatMessage() {}
    
    // getter/setter methods
}

@OnEvent("chat")
public void handleChat(SocketIOSession session, ChatMessage message) {
    // Automatically deserialized to ChatMessage object
}
```

## 🔧 Performance Optimization

### Connection Pool Configuration

```java
SocketIOConfig config = new SocketIOConfig();
config.setWorkerCount(Runtime.getRuntime().availableProcessors() * 2);
```

### Memory Optimization

```java
// Set appropriate buffer sizes
config.setMaxFramePayloadLength(1024 * 64);  // 64KB
config.setMaxHttpContentLength(1024 * 64);   // 64KB
```

### Monitoring and Statistics

```java
@Component
public class PerformanceInterceptor implements CommandarInterceptor {
    @Override
    public void afterCompletion(Commandar commandar, SocketIOSession session, Throwable throwable) {
        // Record performance metrics
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Event " + commandar.getEventName() + " took: " + duration + "ms");
    }
}
```

## ❓ FAQ

### Q: How to implement load balancing?

A: You can use reverse proxies like Nginx for load balancing.

### Q: Which clients are supported?

A: All standard Socket.IO clients are supported, including:
- JavaScript (browser)
- Node.js
- Java
- Python
- Swift (iOS)
- Kotlin/Java (Android)

### Q: How to debug connection issues?

A: Enable debug logging:

```java
config.setLogLevel(LogLevel.DEBUG);
```

## 🔗 References

- [Socket.IO Official Documentation](https://socket.io/docs/v4/)
- [Netty-SocketIO Project](https://github.com/mrniko/netty-socketio)
- [Socket.IO Protocol Details](https://github.com/socketio/socket.io-protocol)
- [Netty Official Documentation](https://netty.io/wiki/)

## 📄 License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Issues and Pull Requests are welcome!

1. Fork the project
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Create a Pull Request

---

If you have any questions or suggestions, please contact us through GitHub Issues.
