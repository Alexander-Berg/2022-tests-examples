package com.yandex.maps.testapp.common.internal;

import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mrc.LocalRideIdentifier;
import com.yandex.runtime.bindings.ClassHandler;
import com.yandex.runtime.bindings.Marshalling;
import com.yandex.runtime.bindings.Serializable;

import java.nio.ByteBuffer;

public class Serialization {
    public static <T extends Serializable> byte[] serialize(T object, Class<T> itemClass) {
        ByteBuffer buf = Marshalling.serialize(object, new ClassHandler<T>(itemClass));
        buf.rewind();
        byte[] byteArray = new byte[buf.remaining()];
        buf.get(byteArray, 0, byteArray.length);
        return byteArray;
    }

    public static <T extends Serializable> T deserialize(byte[] bytes, Class<T> itemClass) {
        return Marshalling.deserialize(ByteBuffer.wrap(bytes), new ClassHandler<T>(itemClass));
    }
}
