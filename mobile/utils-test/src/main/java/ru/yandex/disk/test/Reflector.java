package ru.yandex.disk.test;

import ru.yandex.disk.util.Exceptions;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Reflector {

    private final Class<?> clazz;
    private final Object targetObject;

    public Reflector(String className, Object targetObject) {
        this(classForName(className), targetObject);
    }

    private static Class<?> classForName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return Exceptions.crashValue(e);
        }
    }

    public Reflector(Class<?> clazz, Object targetObject) {
        this.clazz = clazz;
        this.targetObject = targetObject;
    }

    public void set(String fieldName, Object fieldValue) {
        try {
            Field field = getField(fieldName);
            field.set(targetObject, fieldValue);
        } catch (Exception e) {
            Exceptions.crash(e);
        }
    }

    private Field getField(String fieldName) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        return field;
    }

    public Object invoke(String methodName, Object... params) {
        try {
            Class<?>[] paramTypes = new Class<?>[params.length];
            for (int i = 0; i < paramTypes.length; i++) {
                Class<? extends Object> paramType = params[i].getClass();
                if (paramType.equals(Integer.class)) {
                    paramType = int.class;
                } else if (paramType.equals(Boolean.class)) {
                    paramType = boolean.class;
                }
                paramTypes[i] = paramType;
            }

            Method method = getMethod(methodName, paramTypes);
            return method.invoke(targetObject, params);
        } catch (InvocationTargetException e) {
            return Exceptions.crash(e.getCause());
        } catch (Exception e) {
            return Exceptions.crash(e);
        }

    }

    private Method getMethod(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = clazz.getDeclaredMethod(methodName, paramTypes);
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        return method;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String fieldName) {
        try {
            Field field = getField(fieldName);
            return (T) field.get(targetObject);
        } catch (Exception e) {
            return Exceptions.crashValue(e);
        }

    }

    public static <T> T getField(Object targetObject, String fieldName, Class<?> clazz) {
        return new Reflector(clazz, targetObject).get(fieldName);
    }

    public static <T> T getField(Object targetObject, String fieldName, String className) {
        try {
            Class<?> clazz = classForName(className);
            return getField(targetObject, fieldName, clazz);
        } catch (Exception e) {
            return Exceptions.crashValue(e);
        }
    }

    public static Object instanse(String className) {
        try {
            Class<?> clazz = classForName(className);
            Constructor<?> c = clazz.getDeclaredConstructors()[0];
            if (!c.isAccessible()) {
                c.setAccessible(true);
            }
            return c.newInstance();
        } catch (Exception e) {
            return Exceptions.crash(e);
        }
    }

    public static void scrub(Object object) throws Exception {
        Class<?> clazz = object.getClass();
        while (!clazz.equals(Object.class)) {
            scrub(clazz, object);
            clazz = clazz.getSuperclass();
        }
    }

    private static void scrub(Class<?> clazz, Object object) throws Exception {
        final Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                field.setAccessible(true);
                field.set(object, null);
            }
        }
    }
}
