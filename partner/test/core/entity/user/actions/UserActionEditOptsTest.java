package ru.yandex.partner.core.entity.user.actions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.user.actions.factories.UserEditFactory;
import ru.yandex.partner.core.entity.user.model.User;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
class UserActionEditOptsTest {
    @Autowired
    UserEditFactory userEditFactory;

    @Test
    void testCorrectOpts() {
        UserActionEdit userActionEdit = userEditFactory.edit(List.of(
                new ModelChanges<>(0L, User.class).process("test", User.COOPERATION_FORM)));

        var opts = userActionEdit.getSerializedOpts(0L);
        assertThat(opts).isEqualTo("{\"cooperation_form\":\"test\"}");
    }

}
