package studio.ellan.anticheatbridge.paper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class ReflectionUtil {
    private ReflectionUtil() {
    }

    static Class<?> findClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    static Class<?> findClass(String name, ClassLoader classLoader) {
        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    static Object staticField(Class<?> owner, String fieldName) {
        try {
            Field field = owner.getField(fieldName);
            return field.get(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    static String string(Object target, String methodName, String fallback) {
        Object value = invoke(target, methodName);
        return value == null ? fallback : String.valueOf(value);
    }

    static double number(Object target, String methodName, double fallback) {
        Object value = invoke(target, methodName);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    static boolean bool(Object target, String methodName, boolean fallback) {
        Object value = invoke(target, methodName);
        return value instanceof Boolean bool ? bool : fallback;
    }
}
