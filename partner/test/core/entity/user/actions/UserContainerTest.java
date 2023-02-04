package ru.yandex.partner.core.entity.user.actions;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.direct.model.Model;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionModelContainerImpl;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.entity.user.model.User;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
class UserContainerTest {
    private static final Logger logger = LoggerFactory.getLogger(UserContainerTest.class);
    @Autowired
    private ActionPerformer actionPerformer;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void testContainerIndependence() throws InterruptedException {
        AtomicInteger size = new AtomicInteger(0);
        var t1 = new Thread(() -> makeWorkWithContainer(0L, size, Set.of(User.ID, User.LOGIN,
                User.MULTISTATE, User.NAME)));
        var t2 = new Thread(() -> makeWorkWithContainer(1008L, size, Set.of(User.ID, User.LOGIN,
                User.MULTISTATE, User.NAME, User.EMAIL)));
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertThat(size.get()).isEqualTo(2);

    }

    public void makeWorkWithContainer(Long id, AtomicInteger size, Set<ModelProperty<? extends Model, ?>> fields) {
        var facade = actionPerformer.getActionContextFacade();
        logger.info(facade.toString());
        var userActionContext = facade.getActionContext(User.class, UserActionContext.class);
        userActionContext.init();
        var containers = getContainers(userActionContext, List.of(id), fields);
        size.addAndGet(userActionContext.getContextSize());
        var user = containers.get(0).getItem();
        assertThat(user.getId()).isEqualTo(id);
        assertThat(userActionContext.getRequiredFields()).containsAll(fields);
    }

    private synchronized List<ActionModelContainerImpl<User>> getContainers(
            UserActionContext userActionContext, List<Long> ids, Set<ModelProperty<?
            extends Model, ?>> fields) {
        return transactionTemplate
                .execute(status -> userActionContext.getContainers(ids, fields, false));
    }
}
