package ru.yandex.partner.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.partner.core.configuration.AdfoxGraphqlTestConfig;
import ru.yandex.partner.core.configuration.AuthTestConfiguration;
import ru.yandex.partner.core.configuration.BalanceTestConfiguration;
import ru.yandex.partner.core.configuration.MemcachedTestConfiguration;
import ru.yandex.partner.core.configuration.MockFormatSystemConfiguration;
import ru.yandex.partner.core.configuration.TestOperationContainerConfiguration;
import ru.yandex.partner.core.entity.action.ThreadScopeErrorBeansCollector;
import ru.yandex.partner.core.entity.queue.TestTaskConfiguration;
import ru.yandex.partner.libs.multistate.configration.MultistateConfiguration;
import ru.yandex.partner.test.db.MysqlTestConfiguration;
import ru.yandex.partner.unifiedagent.UnifiedAgentTestConfig;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
        properties = {"spring.main.allow-bean-definition-overriding=true"},
        classes = {TestApplication.class, MysqlTestConfiguration.class, BalanceTestConfiguration.class,
                MultistateConfiguration.class, MemcachedTestConfiguration.class, TestTaskConfiguration.class,
                ThreadScopeErrorBeansCollector.class, MockFormatSystemConfiguration.class,
                TestOperationContainerConfiguration.class, AuthTestConfiguration.class, AdfoxGraphqlTestConfig.class,
                UnifiedAgentTestConfig.class}
)
@Execution(ExecutionMode.SAME_THREAD)
public @interface CoreTest {
}
