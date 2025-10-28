package cloud.apposs.socketio.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 排序注解，提供各组件列表的排序添加，
 * Order的值越小优先级越高
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Order {
    int value() default Integer.MAX_VALUE;
}
