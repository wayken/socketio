package cloud.apposs.socketio.commandar;

import cloud.apposs.socketio.SocketIOSession;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 方法参数解析器
 */
public final class ParameterResolver {
    /**
     * 解析方法参数
     *
     * @param commandar 当前方法
     * @param session   当前会话
     * @param data      方法参数值
     * @return 返回解析后的参数值
     */
    public static Object[] resolveParameterArguments(Commandar commandar, SocketIOSession session, List<Object> data) {
        Method method = commandar.getMethod();
        Class<?>[] parameters = method.getParameterTypes();
        Object[] arguments = new Object[parameters.length];
        List<Integer> dataIndexes = getDataIndexes(method);
        int socketIOSessionIndex = getParameterIndex(method, SocketIOSession.class);
        if (socketIOSessionIndex != -1) {
            arguments[socketIOSessionIndex] = session;
        }
        int i = 0;
        for (int index : dataIndexes) {
            if (data.size() <= i) {
                arguments[index] = null;
            } else {
                arguments[index] = data.get(i);
            }
            i++;
        }
        return arguments;
    }

    /**
     * 解析方法 JSON Object 参数类型
     *
     * @param  commandar 当前指令方法
     * @return 返回解析后的参数类型
     */
    public static List<Class<?>> resolveParameterTypes(Commandar commandar) {
        Method method = commandar.getMethod();
        Class<?>[] parameters = method.getParameterTypes();
        List<Class<?>> types = new ArrayList<Class<?>>();
        for (Class<?> type : parameters) {
            if (type.equals(SocketIOSession.class) || type.equals(Throwable.class) || type.equals(Exception.class)) {
                continue;
            }
            types.add(type);
        }
        return types;
    }

    private static int getParameterIndex(Method method, Class<?> clazz) {
        int index = 0;
        for (Class<?> type : method.getParameterTypes()) {
            if (type.equals(clazz)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static List<Integer> getDataIndexes(Method method) {
        List<Integer> result = new ArrayList<Integer>();
        int index = 0;
        for (Class<?> type : method.getParameterTypes()) {
            if (!type.equals(SocketIOSession.class)) {
                result.add(index);
            }
            index++;
        }
        return result;
    }
}
