package com.yandex.mail.storage;

import com.yandex.mail.storage.MessageStatus.Type;
import com.yandex.mail.storage.MessageType.TypeFlags;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageTypeTest {

    @Test
    public void getSomeTypeFromMask() {
        int mask = TypeFlags.DELIVERY_TYPE_FLAG | TypeFlags.BOUNCE_TYPE_FLAG | TypeFlags.S_GROUPONSITE_TYPE_FLAG;
        Type expectedType = Type.fromFlag(TypeFlags.DELIVERY_TYPE_FLAG);
        Type actualType = MessageType.getSomeTypeFromMask(mask);
        assertThat(actualType).isEqualTo(expectedType);
    }

    @Test
    public void formFilteringMask_handleNullType() {
        int[] typeIds = new int[]{Type.DELIVERY.getId(), Type.BOUNCE.getId(), Type.S_GROUPONSITE.getId(), 0};
        int expectingMask = TypeFlags.DELIVERY_TYPE_FLAG | TypeFlags.BOUNCE_TYPE_FLAG | TypeFlags.S_GROUPONSITE_TYPE_FLAG;
        assertThat(MessageType.formFilteringMask(typeIds)).isEqualTo(expectingMask);
    }
}