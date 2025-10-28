package cloud.apposs.socketio.annotation;

import cloud.apposs.ioc.annotation.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求定义注解，它的功能主要是将目前的类定义成一个WebSocket服务器端,
 * 注解的值将被用于监听用户连接的终端访问URL地址,客户端可以通过这个URL来连接到WebSocket服务器端
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface ServerEndpoint {
    /** 请求的匹配路径列表 */
    String[] value() default {};

    /** 请求的匹配主机，默认是所有host匹配 */
    String host() default "*";
}
