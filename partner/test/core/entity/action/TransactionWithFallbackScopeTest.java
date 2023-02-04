package ru.yandex.partner.core.entity.action;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionContextFacade;
import ru.yandex.partner.core.action.ActionModelContainer;
import ru.yandex.partner.core.entity.user.model.User;

@CoreTest
public class TransactionWithFallbackScopeTest {

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void proxyProvideRealObjectWithoutTransactionTest() {
        var facadeProxy = applicationContext.getBean(ActionContextFacade.class);

        Assertions.assertDoesNotThrow(() -> {
            facadeProxy.init();
        });
    }

    @Test
    void canGetContainersWithoutTransaction() {
        ActionContextFacade facade = applicationContext.getBean(ActionContextFacade.class);

        var userActionContext = facade.getActionContext(User.class);
        var containers = userActionContext.getContainersWithoutErrors(List.of(1009L, 1010L, 1011L));

        Set<Long> expected = Set.of(1009L, 1010L, 1011L);
        Set<Long> actual = containers.stream()
                .map(ActionModelContainer::getItem)
                .map(User::getId)
                .collect(Collectors.toSet());

        Assertions.assertEquals(expected, actual);
    }
}
