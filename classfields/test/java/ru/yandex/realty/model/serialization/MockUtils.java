package ru.yandex.realty.model.serialization;

import com.google.protobuf.Message;
import org.junit.Assert;
import ru.yandex.common.util.collections.Cf;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author aherman
 */
public class MockUtils {
    public static <T> void assertEquals(T expected, T actual) throws Exception {
        if (expected == null) {
            Assert.assertNull(actual);
        }
        assertEquals("this(" + expected.getClass().getSimpleName() + ")", expected, actual, expected.getClass());
    }

    private static void assertEquals(String path, Object expected, Object actual, Class<?> clazz) throws
            IllegalAccessException
    {
        if (expected == null) {
            Assert.assertNull("Actual in " + path + " is not null", actual);
            return;
        }
        Assert.assertNotNull("Actual in " + path + " is null", actual);
        if (List.class.isAssignableFrom(clazz)) {
            assertListEquals(path, (List) expected, (List) actual);
        } else if (Message.Builder.class.isAssignableFrom(clazz)) {
            Assert.assertEquals(
                    path + " not equals",
                    ((Message.Builder) expected).build(),
                    ((Message.Builder) actual).build()
            );
        } else if (!isRealtyClass(clazz) || clazz.isEnum()) {
            Assert.assertEquals(path + " not equals", expected, actual);
        } else {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                declaredField.setAccessible(true);
                if (Modifier.isStatic(declaredField.getModifiers())) {
                    continue;
                }
                Object expectedFieldValue = declaredField.get(expected);
                Object actualFieldValue = declaredField.get(actual);

                String fieldPath = path + ":" + fieldToPath(declaredField);
                assertEquals(fieldPath, expectedFieldValue, actualFieldValue, declaredField.getType());
            }

            Class<?> superClazz = clazz.getSuperclass();
            if (isRealtyClass(superClazz)) {
                assertEquals(path, expected, actual, superClazz);
            }
        }

//        Assert.assertTrue("Strange! Objects are not equals: path=" + path, EqualsBuilder.reflectionEquals(expected, actual));
    }

    private static void assertListEquals(String fieldPath, List expectedFieldValue,
            List actualFieldValue) throws IllegalAccessException
    {
        Assert.assertEquals("List size differ for " + fieldPath, expectedFieldValue.size(), actualFieldValue.size());
        for (int i = 0; i < expectedFieldValue.size(); i++) {
            String fp = fieldPath + "[" + i + "]";
            Object expected = expectedFieldValue.get(i);
            Object actual = actualFieldValue.get(i);
            if (expected == null) {
                Assert.assertNull("Expected null " + fp, actual);
            } else {
                assertEquals(fp, expected, actual, expected.getClass());
            }
        }
    }

    private static String fieldToPath(Field field) {
        return field.getName() + "(" + field.getType().getSimpleName() + ")";
    }

    private static boolean isRealtyClass(Class<?> clazz) {
        Package pack = clazz.getPackage();
        return pack != null && pack.getName().startsWith("ru.yandex.realty");
    }

    public static <K, V> Map<K, V> map(Object... values) {
        if (values == null || values.length == 0) {
            return Collections.emptyMap();
        }

        Assert.assertTrue(values.length % 2 == 0);
        Map<K, V> result = Cf.newHashMap();
        int i = 0;
        while (i < values.length) {
            result.put((K) values[i], (V) values[i + 1]);
            i += 2;
        }
        return result;
    }
}
