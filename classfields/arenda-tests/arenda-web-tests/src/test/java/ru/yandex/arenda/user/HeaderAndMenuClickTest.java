package ru.yandex.arenda.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.ApiSteps;
import ru.yandex.arenda.steps.MainSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static ru.yandex.arenda.constants.UriPath.LK_PERSONAL_DATA_EDIT;
import static ru.yandex.arenda.constants.UriPath.LK_SELECTION;
import static ru.yandex.arenda.pages.BasePage.MY_FLATS_LINK;
import static ru.yandex.arenda.pages.BasePage.PERSONAL_DATA_LINK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1662")
@DisplayName("Переходы по ссылкам меню. Данные не подтверждены")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class HeaderAndMenuClickTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private MainSteps mainSteps;

    @Inject
    private ApiSteps apiSteps;

    @Before
    public void before() {
        apiSteps.createYandexAccount(account);
    }

    @Test
    @DisplayName("Клик по «Мой кабинет»")
    public void shouldSeeMyCabinetClick() {
        urlSteps.testing().open();
        mainSteps.onBasePage().myCabinet().click();
        mainSteps.onBasePage().myCabinetPopupDesktop().should(isDisplayed());
        urlSteps.path("/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Клик по «Мой кабинет» -> «Мои квартиры» -> переходим на разводку")
    public void shouldSeeMyFlatsClick() {
        urlSteps.testing().open();
        mainSteps.onBasePage().myCabinet().click();
        mainSteps.onBasePage().myCabinetPopupDesktop().link(MY_FLATS_LINK).click();
        urlSteps.testing().path(LK_SELECTION).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Клик по «Мой кабинет» -> «Личные данные» -> переходим на редактирование данных")
    public void shouldSeePersonalDataClick() {
        urlSteps.testing().open();
        mainSteps.onBasePage().myCabinet().click();
        mainSteps.onBasePage().myCabinetPopupDesktop().link(PERSONAL_DATA_LINK).click();
        urlSteps.testing().path(LK_PERSONAL_DATA_EDIT).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Клик по лого Яндекса")
    public void shouldSeeYandexLogoClick() {
        urlSteps.testing().open();
        mainSteps.onBasePage().header().yandexLogo().click();
        urlSteps.fromUri("https://yandex.ru/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Клик по лого Арнеда")
    public void shouldSeeArendaLogoClick() {
        urlSteps.testing().open();
        mainSteps.onBasePage().header().arendaLogo().click();
        urlSteps.testing().shouldNotDiffWithWebDriverUrl();
    }
}
