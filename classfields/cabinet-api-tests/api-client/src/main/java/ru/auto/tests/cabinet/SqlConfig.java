package ru.auto.tests.cabinet;

import org.aeonbits.owner.Config;

import static org.aeonbits.owner.Config.DisableableFeature.PARAMETER_FORMATTING;

@Config.Sources({"classpath:testing.properties"})
public interface SqlConfig extends Config {
    @Key("sql.login")
    @DefaultValue("auto")
    String login();

    @Key("sql.password")
    @DefaultValue("KiX1euph")
    String password();

    @Key("sql.driver.class.name")
    @DefaultValue("com.mysql.cj.jdbc.Driver")
    String driver();

    @Key("sql.database.url")
    @DisableFeature(PARAMETER_FORMATTING)
    @DefaultValue("jdbc:mysql://mysql.dev.vertis.yandex.net:%s?" +
        "useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC")
    String sqlDataBaseUrl();
}
