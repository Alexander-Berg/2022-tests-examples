package ru.yandex.partner.test.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;

import javax.sql.DataSource;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.ExecuteListener;
import org.jooq.ExecuteListenerProvider;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.jooq.tools.LoggerListener;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ru.yandex.direct.mysql.MySQLInstance;
import ru.yandex.direct.test.mysql.TestMysqlConfig;
import ru.yandex.partner.defaultconfiguration.JooqSettingsConfigurer;
import ru.yandex.partner.defaultconfiguration.MysqlProps;
import ru.yandex.partner.test.db.utils.MySQLRefresherService;

import static ru.yandex.partner.defaultconfiguration.MysqlUtils.setupCommonHikariConfig;

@TestConfiguration
@EnableTransactionManagement
public class MysqlTestConfiguration implements BeanPostProcessor {
    public static final String MYSQL_DATASOURCE_BEAN_NAME = "mysqlDataSource";

    public static final TestMysqlConfig MYSQL_CONFIG = new TestMysqlConfig(
            "partner/java/libs/test-db", "mysql-server", "mysql-test-data",
            "/ru/yandex/partner/test/db/dbschema_docker_image.txt",
            "partner/java/libs/test-db/src/main/resources//ru/yandex/partner/test/db/dbschema_docker_image.txt"
    );

    @Bean
    public MysqlProps mysqlProps() {
        return new MysqlProps();
    }

    @Bean
    public TestDbInitializer testDbInitializer() {
        return new TestDbInitializer(MYSQL_CONFIG);
    }

    @Primary
    @Bean(name = MYSQL_DATASOURCE_BEAN_NAME)
    public DataSource mysqlDataSource(
            MysqlProps mysqlProps,
            TestDbInitializer testDbInitializer
    ) throws SQLException {
        MySQLInstance mysql = testDbInitializer.getConnector();

        String testUrl = String.format("jdbc:mysql://%s:%s/%s?%s",
                mysql.getHost(),
                mysql.getPort(), mysqlProps.getSchema(), mysqlProps.getParams());

        MysqlDataSource mysqlDataSource = new MysqlDataSource();
        mysqlDataSource.setUrl(testUrl);
        mysqlDataSource.setUser(mysql.getUsername());
        mysqlDataSource.setPassword(mysql.getPassword());

        try (Connection connection = mysqlDataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SET sql_mode = ''")) {
            preparedStatement.execute();
        }

        mysqlDataSource.getUrl();

        return new HikariDataSource(setupCommonHikariConfig(
                mysqlDataSource,
                mysqlProps.getConnectionTimeout(),
                mysqlProps.getLockWaitTimeout(),
                mysqlProps.getMaxPoolSize()
        ));
    }

    @Bean
    public ExecuteListener sqlQueriesLogger() {
        return new LoggerListener();
    }

    @Bean
    public QueryListener queryListener() {
        return new QueryListener(new LinkedList<>());
    }

    @Bean
    public QueryLogService queryLogService(QueryListener queryListener) {
        return new QueryLogService(queryListener);
    }

    @Bean
    public ExecuteListenerProvider executeListenerProviders(QueryListener queryListener) {
        return new DefaultExecuteListenerProvider(queryListener);
    }

    @Bean
    public MySQLRefresherService mySQLRefresherService(DSLContext dslContext, QueryLogService queryLogService) {
        return new MySQLRefresherService(dslContext, queryLogService);
    }

    @Bean
    JooqSettingsConfigurer loggingConfigurer() {
        return settings -> settings.withRenderFormatted(true);
    }
}
