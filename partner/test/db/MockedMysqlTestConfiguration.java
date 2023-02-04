package ru.yandex.partner.test.db;

import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

@TestConfiguration
@EnableTransactionManagement
public class MockedMysqlTestConfiguration {
    public static final String MYSQL_DATASOURCE_BEAN_NAME = "mysqlDataSource";

    @Primary
    @Bean(name = MYSQL_DATASOURCE_BEAN_NAME)
    public DataSource mysqlDataSource() {
        return mock(DataSource.class);
    }

    @Bean
    DSLContext dslContext() {
        return mock(DSLContext.class, RETURNS_DEEP_STUBS);
    }

    @Bean
    public PlatformTransactionManager txManager() {
        return mock(PlatformTransactionManager.class);
    }

}
