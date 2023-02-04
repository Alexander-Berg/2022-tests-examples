package ru.auto.tests.desktop.lk.header;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.TestData.USER_2_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROFILE;

@DisplayName("Подшапка - ссылки")
@Feature(HEADER)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SubHeaderUrlsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String startUrl;

    @Parameterized.Parameter(1)
    public String subHeaderUrlTitle;

    @Parameterized.Parameter(2)
    public String subHeaderUrl;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"", "Объявления", "https://%s/my/all/"},
                {"", "Безопасная сделка", "https://%s/my/deals/"},
                {"", "Отзывы", "https://%s/my/reviews/"},
                {"", "Сообщения", "https://forum.%s/messages/"},
                {"", "Отчёты", "https://%s/history/"},
                {"", "Кошелёк", "https://%s/my/wallet/"},
                {"", "Заявки на кредит", "https://%s/my/credits/draft/"},
                {"", "Настройки", "https://%s/my/profile/"},

                {PROFILE, "Объявления", "https://%s/my/all/"},
                {PROFILE, "Безопасная сделка", "https://%s/my/deals/"},
                {PROFILE, "Отзывы", "https://%s/my/reviews/"},
                {PROFILE, "Сообщения", "https://forum.%s/messages/"},
                {PROFILE, "Отчёты", "https://%s/history/"},
                {PROFILE, "Кошелёк", "https://%s/my/wallet/"},
                {PROFILE, "Заявки на кредит", "https://%s/my/credits/draft/"},
                {PROFILE, "Настройки", "https://%s/my/profile/"}
        });
    }

    @Before
    public void before() throws IOException {
        loginSteps.loginAs(USER_2_PROVIDER.get());
        urlSteps.testing().path(MY).path(startUrl).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке в подшапке")
    public void shouldClickUrl() {
        basePageSteps.onLkSalesPage().subHeader().button(subHeaderUrlTitle).click();
        urlSteps.fromUri(format(subHeaderUrl, urlSteps.getConfig().getBaseDomain())).shouldNotSeeDiff();
    }
}