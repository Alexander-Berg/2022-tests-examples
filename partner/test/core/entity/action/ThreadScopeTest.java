package ru.yandex.partner.core.entity.action;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.partner.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.partner.core.entity.action.ThreadScopeErrorBeansCollector.THREAD_LOCAL_CLASSES;

@CoreTest
class ThreadScopeTest {

    @Autowired
    private ThreadScopeErrorBeansCollector errorBeansCollector;
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testScopePresent() {
        // wake up all lazy initialized beans
        THREAD_LOCAL_CLASSES.forEach(applicationContext::getBeansOfType);

        assertThat(errorBeansCollector.getBrokenBeans())
                .describedAs("all context beans should be thread-local")
                .isEmpty();

        assertThat(errorBeansCollector.getGoodBeans()).isNotEmpty();
    }
}
