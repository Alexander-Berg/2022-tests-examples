package ru.yandex.partner.core.entity.user.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.ModelWithId;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.ModelQueryService;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.tasks.doaction.DoActionFilterEnum;
import ru.yandex.partner.core.entity.user.filter.UserFilters;
import ru.yandex.partner.core.entity.user.model.User;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.junit.MySqlRefresher;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MySqlRefresher.class)
@CoreTest
class UserServiceTest {
    @Autowired
    UserService userService;

    @Test
    void findAll() {
        assertThat(userService.findAll(QueryOpts.forClass(User.class)
        ))
                .as("all users with class User")
                .isNotEmpty();
    }

    @Test
    void count() {
        assertThat(userService.count(QueryOpts.forClass(User.class)
                .withFilter(CoreFilterNode.neutral())))
                .isEqualTo(45);
    }

    @Test
    void getFilterByName() {
        ModelQueryService<? extends ModelWithId> modelQueryService = userService;

        var filter = modelQueryService.getMetaFilterForDoAction(DoActionFilterEnum.MULTISTATE);

        Assertions.assertEquals(UserFilters.MULTISTATE, filter);
    }
}
