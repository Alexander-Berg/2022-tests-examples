package ru.auto.tests.cabinet.header;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROFILE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.03.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Шапка. Персональное меню")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class PersonalMenuOfDealerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/DealerInfoMultipostingDisabled",
                "cabinet/ClientsGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Блок «Персональное меню пользователя»")
    public void shouldSeePersonalMenuBlock() {
        steps.moveCursor(steps.onCabinetOffersPage().header().personalMenuOfDealer());
        steps.onCabinetOffersPage().header().personalMenuOfDealer()
                .waitUntil(hasText("msk2418\nНастройки учётной записи\nВыход"));
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Переход на страницу с персональными данными с блока персонального меню")
    public void shouldSeePersonalDataPageFromPersonalMenuButton() {
        steps.onCabinetOffersPage().header().personalMenuOfDealer().click();
        urlSteps.testing().path(MY).path(PROFILE).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Кнопка «Настройки»")
    public void shouldSeePersonalSettingsPage() {
        steps.moveCursor(steps.onCabinetOffersPage().header().personalMenuOfDealer());
        steps.onCabinetOffersPage().header().personalMenuOfDealer().menu("Настройки").click();
        urlSteps.testing().path(MY).path(PROFILE).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Кнопка «Выход»")
    public void shouldSeeAuthorizationPage() {
        mockRule.delete();

        String currentUrl = urlSteps.getCurrentUrl();
        steps.moveCursor(steps.onCabinetOffersPage().header().personalMenuOfDealer());
        steps.onCabinetOffersPage().header().personalMenuOfDealer().menu("Выход").waitUntil(isDisplayed())
                .click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN).addParam("r", encode(currentUrl)).shouldNotSeeDiff();
    }
}
