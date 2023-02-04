package ru.yandex.stockpile.ser.test;

import java.util.function.Function;

import com.google.protobuf.Message;

import ru.yandex.solomon.codec.serializer.NakedSerializerFactoryByFormat;
import ru.yandex.solomon.codec.serializer.StockpileFormat;

/**
 * @author Stepan Koltsov
 */
public class SerializeStableContext<J, M extends Message> {

    public final NakedSerializerFactoryByFormat<J> serializerFactoryByFormat;
    public final M protoTemplate;
    public final Function<J, M> toProto;
    public final Function<M, J> fromProto;
    public final PatchMessage<M> patchMessage;

    public SerializeStableContext(
            NakedSerializerFactoryByFormat<J> serializerFactoryByFormat,
            M protoTemplate,
            Function<J, M> toProto,
            Function<M, J> fromProto,
            PatchMessage<M> patchMessage)
    {
        this.serializerFactoryByFormat = serializerFactoryByFormat;
        this.protoTemplate = protoTemplate;
        this.toProto = toProto;
        this.fromProto = fromProto;
        this.patchMessage = patchMessage;
    }

    public interface PatchMessage<M extends Message> {
        M patch(M message, StockpileFormat format);
    }
}
