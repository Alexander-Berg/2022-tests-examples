package ru.yandex.webmaster3.storage.util;

import junit.framework.TestCase;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.webmaster3.core.util.enums.IntEnum;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author akhazhoyan 06/2018
 */
public final class IntEnumSubclassesTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(IntEnumSubclassesTest.class);

    public void testAllKeysAreUnique() {
        Reflections reflections = new Reflections("ru.yandex.webmaster3");
        Set<Class<? extends IntEnum>> enumClasses = reflections.getSubTypesOf(IntEnum.class);
        for (Class<? extends IntEnum> enumClass : enumClasses) {
            log.info("Going to test class {}", enumClass);
            assertTrue("Only enums are expected to implement the IntEnum interface", enumClass.isEnum());
            assertAllInstancesHaveDifferentKeys(enumClass);
        }
    }

    private static void assertAllInstancesHaveDifferentKeys(Class<? extends IntEnum> enumClass) {
        Collection<? extends IntEnum> instances = Arrays.asList(enumClass.getEnumConstants());
        Set<Integer> ids = instances.stream().map(i -> i.value()).collect(Collectors.toSet());
        assertEquals("All keys must be unique in class " + enumClass, instances.size(), ids.size());
    }
}
