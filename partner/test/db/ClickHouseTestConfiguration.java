package ru.yandex.partner.test.db;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import ru.yandex.clickhouse.ClickHouseDataSource;

@TestConfiguration
public class ClickHouseTestConfiguration {
    public static final String CLICKHOUSE_DATASOURCE_BEAN_NAME = "clickhouseDataSource";
    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseTestConfiguration.class);

    @Bean(name = CLICKHOUSE_DATASOURCE_BEAN_NAME)
    ClickHouseDataSource clickhouseDataSource(
            @Value("${clickhouse.url:}") String jdbcUrl
    ) {
        String host = System.getenv("RECIPE_CLICKHOUSE_HOST");
        String port = System.getenv("RECIPE_CLICKHOUSE_HTTP_PORT");
        String username = System.getenv("RECIPE_CLICKHOUSE_USER");
        String password = System.getenv("RECIPE_CLICKHOUSE_PASSWORD");

        Pattern hostPort = Pattern.compile("//(.+?)/");
        String testUrl = hostPort.matcher(jdbcUrl)
                .replaceFirst("//" + host + ":" + port + "/")
                .replace("/partner?", "/default?");
        LOGGER.info(">>>> {}", testUrl);
        //localClickHouseTest
//        todo:testUrl = jdbcUrl;
        ClickHouseDataSource clickHouseDataSource = new ClickHouseDataSource(testUrl);
        clickHouseDataSource.getProperties().setUser(username);
        clickHouseDataSource.getProperties().setPassword(password);
        return clickHouseDataSource;
    }
}
