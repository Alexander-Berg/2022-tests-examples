package ru.yandex.partner.testapi.configuration;

import java.util.List;
import java.util.Set;

import org.jooq.DSLContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import ru.yandex.partner.libs.memcached.MemcachedService;
import ru.yandex.partner.testapi.fixture.Fixture;
import ru.yandex.partner.testapi.fixture.service.idprovider.IdProviderService;
import ru.yandex.partner.testapi.fixture.service.tus.TusService;
import ru.yandex.partner.testapi.jooq.LoggingExecuteListener;
import ru.yandex.partner.testapi.service.testcase.BaseTestApiService;
import ru.yandex.partner.testapi.service.testcase.ConcurrentTestApiService;
import ru.yandex.partner.testapi.service.testcase.TestApiService;

@TestConfiguration
public class TestApiServiceTestConfiguration {

    @Bean
    @DependsOn("mySQLRefresherService")
    public TestApiService testApiService(DSLContext dsl,
                                         LoggingExecuteListener loggingExecuteListener,
                                         List<Fixture> fixtures,
                                         TusService tusService,
                                         Set<String> dictionaryTables,
                                         IdProviderService idProviderService,
                                         MemcachedService memcachedService) {
        return new ConcurrentTestApiService(
                new BaseTestApiService(
                        dsl,
                        loggingExecuteListener,
                        fixtures,
                        tusService,
                        dictionaryTables,
                        idProviderService,
                        memcachedService
                )
        );
    }
}
