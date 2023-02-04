package ru.auto.tests.amp;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.matchers.BrowserLogMatcher.hasLogItem;

@DisplayName("Проверка валидации AMP")
@Feature(AMP)
@GuiceModules(MobileTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AmpValidationTest {

    private static final String URL = "https://cdn.ampproject.org/v0/validator_wasm.js";
    private static final String MESSAGE = "AMP validation successful";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private WebDriverManager driverManager;

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"https://%s/amp/catalog/cars"},
                {"https://%s/amp/cars/audi/all/"},
                {"https://mag.%s/amp/article/sonata8rus"},
                {"https://%s/amp/review/cars/vaz/granta/7684102/61162975/"}
        });
    }

    @Before
    public void before() {
        urlSteps.fromUri(format(url, urlSteps.getConfig().getBaseDomain())).fragment("development=1").open();
    }

    @Test
    @Category({Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Проверка валидации AMP")
    public void shouldValidationSuccess() {
        await().pollInterval(1, SECONDS)
                .atMost(30, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    LogEntries logEntries = driverManager.getDriver().manage().logs().get(LogType.BROWSER);
                    Assert.assertThat("Валидация AMP зафейлилась", logEntries, hasLogItem(URL, MESSAGE));
                });
    }
}
