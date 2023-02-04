package ru.yandex.partner.core.utils;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.model.Model;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.user.model.CommonUser;
import ru.yandex.partner.core.entity.user.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelPropertyUtilsTest {

    @Test
    void allModelProperties() {
        assertEquals(RtbBlock.allModelProperties(), ModelPropertyUtils.allModelProperties(RtbBlock.class));

        assertEquals(User.allModelProperties(), ModelPropertyUtils.allModelProperties(User.class));

        assertEquals(CommonUser.allModelProperties(), ModelPropertyUtils.allModelProperties(CommonUser.class));

        assertThrows(RuntimeException.class, () -> ModelPropertyUtils.allModelProperties(TestModel.class));

    }

    private static class TestModel implements Model {

    }
}
