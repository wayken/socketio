package cloud.apposs.socketio.commandar;

import cloud.apposs.ioc.BeanFactory;
import cloud.apposs.util.ReflectUtil;

import java.lang.reflect.Method;

public class CommandarInvocation {
    private final BeanFactory beanFactory;

    public CommandarInvocation(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public Object invoke(Commandar commandar, Object... arguments) throws Exception {
        if (commandar == null) {
            throw new NullPointerException("commandar");
        }

        Object target = beanFactory.getBean(commandar.getClazz());
        Method beanMethod = commandar.getMethod();
        // 调用方法
        return ReflectUtil.invokeMethod(target, beanMethod, arguments);
    }
}
