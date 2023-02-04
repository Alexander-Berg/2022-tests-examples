package ru.yandex.payments.fnsreg;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.context.annotation.Value;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.AllArgsConstructor;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@MicronautTest(propertySources = "classpath:application-cache-test.yml")
class HazelcastTest {
    static {
        HazelcastTimeTravelClock.install();
    }

    @FunctionalInterface
    public interface Source {
        int getValue();
    }

    @Singleton
    @AllArgsConstructor(onConstructor_ = @Inject)
    public static class CacheableBean {
        private final Source source;

        @Cacheable(cacheNames = "permanent", atomic = true)
        public int readPermanent(int value) {
            return value + source.getValue();
        }

        @Cacheable(cacheNames = "temporary", atomic = true)
        public String readTemporary(String value) {
            return value + source.getValue();
        }
    }

    @Inject
    CacheableBean bean;

    @Inject
    Source sourceMock;

    @Value("${hazelcast-cache.caches.temporary.ttl}")
    Duration temporaryCacheTtl;

    @MockBean(Source.class)
    public Source mockSource() {
        val value = new AtomicInteger(0);

        val source = mock(Source.class);
        when(source.getValue()).thenAnswer(invocation -> value.incrementAndGet());
        return source;
    }

    @Test
    @DisplayName("Verify that permanent hazelcast cache stores values forever")
    void testPermanentCache() {
        val value = bean.readPermanent(1);
        assertThat(value)
                .isEqualTo(bean.readPermanent(1));

        HazelcastTimeTravelClock.moveForward(Duration.ofDays(2));

        assertThat(value)
                .isEqualTo(bean.readPermanent(1));

        verify(sourceMock, only()).getValue();
        verifyNoMoreInteractions(sourceMock);
    }

    @Test
    @DisplayName("Verify that hazelcast cache with configured records ttl stores the values during proper time")
    void testTemporaryCache() {
        val value = bean.readTemporary("foo");
        assertThat(value)
                .isEqualTo(bean.readTemporary("foo"));

        HazelcastTimeTravelClock.moveForward(temporaryCacheTtl);

        assertThat(value)
                .isNotEqualTo(bean.readTemporary("foo"));

        verify(sourceMock, times(2)).getValue();
        verifyNoMoreInteractions(sourceMock);
    }
}
