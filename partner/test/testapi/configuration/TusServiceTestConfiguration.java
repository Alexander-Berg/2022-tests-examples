package ru.yandex.partner.testapi.configuration;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import ru.yandex.partner.testapi.fixture.service.tus.TusService;
import ru.yandex.partner.testapi.fixture.service.tus.TusServiceImpl;
import ru.yandex.partner.testapi.fixture.service.tus.TusUser;

@TestConfiguration
public class TusServiceTestConfiguration {

    @Bean(TusServiceImpl.BEAN_NAME)
    public TusService tusService() {
        return new TusService() {
            private AtomicInteger position = new AtomicInteger(1);

            @Override
            public void clearUserCachePosition() {
                position.set(1);
            }

            @Override
            public void clearUserCache() {
                clearUserCachePosition();
            }

            @Override
            public TusUser getOrCreateUser() {
                return createUser();
            }

            @Override
            public TusUser createUser() {
                int index = position.getAndIncrement();
                var user = new TusUser();
                user.setUid(index);
                user.setLogin("login" + index);
                user.setPassword("password" + index);
                return user;
            }
        };
    }
}
