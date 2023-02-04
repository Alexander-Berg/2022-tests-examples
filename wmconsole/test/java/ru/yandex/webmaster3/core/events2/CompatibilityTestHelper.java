package ru.yandex.webmaster3.core.events2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

class CompatibilityTestHelper {
    private final ObjectMapper om = new ObjectMapper();
    private final Set<HostEventType> testedTypes = EnumSet.noneOf(HostEventType.class);
    private final Set<HostEventType> testedCurrentFormat = EnumSet.noneOf(HostEventType.class);

    private <D extends HostEventData> void test(boolean oldFormat, Class<D> clazz, String serializedString, Consumer<D> testBlock) throws Exception {
        HostEventType type = null;
        for (HostEventType t : HostEventType.values()) {
            if (t.getDataClass().equals(clazz)) {
                type = t;
            }
        }
        Assert.assertNotNull(type);
        testedTypes.add(type);
        if (!oldFormat) {
            testedCurrentFormat.add(type);
        }

        HostEventData data = HostEventJsonUtils.deserialize(type, serializedString);
        Assert.assertTrue(clazz.isInstance(data));
        D d = (D) data;
        Assert.assertEquals(type, d.getType());
        testBlock.accept(d);
        if (!oldFormat) {
            // Для актуального формата проверяем, что при сериализации обратно получается эквивалентный json
            String serializedTwice = HostEventJsonUtils.serialize(d);
            Assert.assertEquals(om.readTree(serializedString), om.readTree(serializedTwice));
            Assert.assertFalse(om.readTree(serializedTwice).has("type"));
        }
    }

    <D extends HostEventData> void testOld(Class<D> clazz, String serializedString, Consumer<D> testBlock) throws Exception {
        test(true, clazz, serializedString, testBlock);
    }

    <D extends HostEventData> void testCurrent(Class<D> clazz, String serializedString, Consumer<D> testBlock) throws Exception {
        test(false, clazz, serializedString, testBlock);
    }

    void assertEverythingTested() {
        List<String> notTested = new ArrayList<>();
        List<String> notTestedCurrentFormat = new ArrayList<>();
        for (HostEventType type : HostEventType.values()) {
            if (!testedTypes.contains(type)) {
                notTested.add(type.toString());
            } else {
                if (!testedCurrentFormat.contains(type)) {
                    notTestedCurrentFormat.add(type.toString());
                }
            }
        }
        if (!notTested.isEmpty() || !notTestedCurrentFormat.isEmpty()) {
            StringBuffer msg = new StringBuffer("Incomplete test: ");
            boolean needDelim = false;
            if (!notTested.isEmpty()) {
                msg.append("types not covered: " + String.join(", ", notTested));
                needDelim = true;
            }
            if (!notTestedCurrentFormat.isEmpty()) {
                if (needDelim) {
                    msg.append("; ");
                }
                msg.append("types with no current format test: " + String.join(", ", notTestedCurrentFormat));
            }
            Assert.fail(msg.toString());
        }
    }
}