package ru.yandex.stockpile.ser.test;

import com.google.protobuf.Message;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.commune.protobuf.ProtobufUtils;
import ru.yandex.misc.io.file.File2;
import ru.yandex.solomon.codec.serializer.StockpileDeserializer;
import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.solomon.util.protobuf.ProtobufText;

/**
 * @author Stepan Koltsov
 */
@RunWith(Parameterized.class)
public class SerializeStableTest {

    @Parameterized.Parameter
    public StockpileFormat format;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] data() {
        return StockpileFormat.values();
    }

    private <J, M extends Message> void runTest(
        String base, StockpileFormat format, SerializeStableContext<J, M> serializeStableContext)
    {
        M m = ProtobufText.readFromTextFile(SerializeStableCommon.protoFile(base), serializeStableContext.protoTemplate);
        m = serializeStableContext.patchMessage.patch(m, format);

        byte[] bytes = new File2(SerializeStableCommon.binFile(base, format)).readBytes();
        J j = serializeStableContext.serializerFactoryByFormat.makeSerializer(format)
            .deserializeToEof(new StockpileDeserializer(bytes));

        M mFromBin = serializeStableContext.toProto.apply(j);

        Assert.assertEquals(base + " " + format,
            ProtobufUtils.clearDefaultFields(m),
            ProtobufUtils.clearDefaultFields(mFromBin));
    }

    @Test
    public void test1() {
        for (SerializeStableTestDef testDef : SerializeStableCommon.tests()) {
            if (testDef.firstAvailableFormat.le(format)) {
                System.out.println(String.format("TestCase=%s, format=%s", testDef.base, format));
                runTest(testDef.base, format, testDef.serializeStableContext);
            }
        }
    }
}
