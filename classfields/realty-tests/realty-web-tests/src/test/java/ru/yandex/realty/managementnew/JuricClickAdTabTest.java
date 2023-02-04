package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.page.BasePage.AD_IN_NEWBUILDINGS;

@Tag(JURICS)
@DisplayName("Юрик. Клик по табу «Реклама»")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class JuricClickAdTabTest {

    private static final String LOGIN_URL = "https://passport.yandex.ru/auth" +
            "?origin=realty-partner&retpath=https%3A%2F%2Fpartner.realty.test.vertis.yandex.ru%2F";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        apiSteps.createRealty3JuridicalAccount(account);
        urlSteps.testing().open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по табу «Реклама» в саджесте попапе юзера")
    public void shouldSeeClickAddTabInUserPopup() {
        basePageSteps.moveCursor(basePageSteps.onManagementNewPage().headerMain().userAccount());
        basePageSteps.onBasePage().userNewPopup().waitUntil(isDisplayed());
        basePageSteps.onBasePage().userNewPopup().link(AD_IN_NEWBUILDINGS).should(isDisplayed()).click();
        urlSteps.fromUri(LOGIN_URL).shouldNotDiffWithWebDriverUrl();
    }
}
