package ru.yandex.infra.controller.util;

import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.yp.client.api.Enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ProtobufEnumWrapperTest {

    @Test
    void parseEnumValueTest() {
        var wrapper = new ProtobufEnumWrappers.Wrapper<>(Enums.EPodSshKeySet.getDescriptor(), Enums.EPodSshKeySet::forNumber);
        assertEquals(Enums.EPodSshKeySet.PSKS_ALL, wrapper.tryParseEnumValue(YTree.stringNode("all")));
        assertEquals(Enums.EPodSshKeySet.PSKS_ALL, wrapper.tryParseEnumValue(YTree.integerNode(1)));
        assertNull(wrapper.tryParseEnumValue(YTree.stringNode("wrongvalue")));
        assertNull(wrapper.tryParseEnumValue(YTree.integerNode(777)));
        assertNull(wrapper.tryParseEnumValue(null));
        assertNull(wrapper.tryParseEnumValue(YTree.builder().beginMap().endMap().build()));
    }
}
