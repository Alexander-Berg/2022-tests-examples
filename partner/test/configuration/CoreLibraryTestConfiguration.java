package ru.yandex.partner.coreexperiment.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

import ru.yandex.partner.defaultconfiguration.TimeZoneConfiguration;
import ru.yandex.partner.test.db.MysqlTestConfiguration;

@TestConfiguration
@Import({CoreConfiguration.class, MysqlTestConfiguration.class, TimeZoneConfiguration.class})
public class CoreLibraryTestConfiguration {
}
