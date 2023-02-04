package ru.yandex.partner.core.entity.user.actions.factories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
class UserBlockFactoryTest {
    @Autowired
    UserBlockFactory userBlockFactory;
    @Autowired
    UserEditFactory userEditFactory;

    @Test
    void testNestedActionsResolved() {
        assertThat(userBlockFactory.getActionConfiguration().getAllowedNestedActions())
                .contains(userEditFactory.getActionConfiguration());

        assertThat(userBlockFactory.getActionConfiguration().getDependsOn())
                .containsAll(userEditFactory.getActionConfiguration().getDependsOn());
    }
}
