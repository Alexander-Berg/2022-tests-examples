package ru.yandex.solomon.alert;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.solomon.config.protobuf.alert.TAlertingConfig;
import ru.yandex.solomon.main.SpringContexts;

import static ru.yandex.solomon.config.SolomonConfigs.parseConfigWithoutInclude;

/**
 * @author Sergey Polovko
 */
@RunWith(Parameterized.class)
public class AlertingMainContextTest {

    @Rule
    public Timeout timeout = Timeout.builder()
            .withLookingForStuckThread(true)
            .withTimeout(1, TimeUnit.MINUTES)
            .build();

    @Parameterized.Parameter
    public String configFile;

    @Parameterized.Parameters(name = "{0}")
    public static String[] configs() {
        return new String[]{
                "configs/testing/alerting.conf",
                "configs/prestable/alerting.conf",
                "configs/production/alerting.conf",
                "configs/cloud-preprod/alerting.conf",
                "configs/cloud-prod/alerting.conf",
                "configs/israel/alerting.conf",
        };
    }

    @Test
    public void testConfiguration() {
        if (!Files.exists(Path.of(configFile))) {
            Assert.fail("cannot find file " + configFile + ", CWD: " + Path.of(".").toAbsolutePath().normalize());
        }

        var config = parseConfigWithoutInclude(configFile, TAlertingConfig.getDefaultInstance());
        Assert.assertNotEquals(TAlertingConfig.getDefaultInstance(), config);

        try (var ctx = SpringContexts.createSimple(AlertingMainContext.class, config)) {
            Assert.assertEquals(config, ctx.getBean(TAlertingConfig.class));
        }
    }
}
