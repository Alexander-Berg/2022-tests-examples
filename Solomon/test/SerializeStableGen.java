package ru.yandex.stockpile.ser.test;

import java.io.File;
import java.io.FileOutputStream;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.solomon.codec.serializer.naked.NakedSerializer;
import ru.yandex.solomon.util.protobuf.ProtobufText;

/**
 * @author Stepan Koltsov
 */
public class SerializeStableGen {

    public static void main(String[] args) {
        for (SerializeStableTestDef testDef : SerializeStableCommon.tests()) {
            try {
                gen(testDef.base, testDef.serializeStableContext, StockpileFormat.MAX);
            } catch (Exception e) {
                throw new RuntimeException("failed to gen " + testDef.base, e);
            }
        }
    }

    private static <J, M extends Message> void gen(String base, SerializeStableContext<J, M> serializeStableContext, StockpileFormat format) {

        File proto = SerializeStableCommon.protoFile(base);
        M multiArchiveProto = ProtobufText.readFromTextFile(proto, serializeStableContext.protoTemplate);

        multiArchiveProto = serializeStableContext.patchMessage.patch(multiArchiveProto, format);

        File bin = SerializeStableCommon.binFile(base, format);

        J multiArchive = serializeStableContext.fromProto.apply(multiArchiveProto);

        NakedSerializer<J> serializer = serializeStableContext.serializerFactoryByFormat.makeSerializer(format);
        ByteString serialized = serializer.serializeToByteString(multiArchive);
        try {
            serialized.writeTo(new FileOutputStream(bin));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
