package ru.yandex.general.form;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.OfferAddSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static ru.yandex.general.consts.ExternalLinks.DESKTOP_SUPPORT_RULES_LINK;
import static ru.yandex.general.consts.ExternalLinks.YANDEX_LINK;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.element.Header.LOGIN;
import static ru.yandex.general.page.BasePage.LOGIN_WITH_YANDEX_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(ADD_FORM_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация на форме")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class NavigationFormTests {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Before
    public void before() {
        offerAddSteps.setMoscowCookie();
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Yandex» через лого")
    public void shouldSeeGoToYandexFromLogo() {
        offerAddSteps.onFormPage().yLogo().click();
        offerAddSteps.switchToNextTab();

        urlSteps.fromUri(YANDEX_LINK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную через лого")
    public void shouldSeeGoToHomepageFromLogo() {
        offerAddSteps.onFormPage().oLogo().click();

        offerAddSteps.onListingPage().h1().should(hasText("Объявления в Москве"));
        urlSteps.testing().shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по кнопке «Войти» с формы")
    public void shouldSeeGoToLoginOnForm() {
        offerAddSteps.onFormPage().header().link(LOGIN).click();
        offerAddSteps.onBasePage().h1().waitUntil(hasText(LOGIN_WITH_YANDEX_ID));

        urlSteps.shouldNotDiffWith(format("https://passport.yandex.ru/auth?mode=auth&retpath=%s", urlSteps));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по «Загрузить фид» с формы неавторизованным юзером")
    public void shouldSeeGoToUploadFeed() {
        offerAddSteps.onFormPage().link("Загрузить фид").click();
        offerAddSteps.onBasePage().h1().waitUntil(hasText(LOGIN_WITH_YANDEX_ID));

        urlSteps.shouldNotDiffWith(
                format("https://passport.yandex.ru/auth?mode=auth&retpath=%s&backpath=https://o.test.vertis.yandex.ru",
                        urlSteps.testing().path(MY).path(FEED).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Авто» в пресетах формы")
    public void shouldSeePresetLinkAuto() {
        offerAddSteps.onFormPage().sectionLink("Авто и мото").click();
        offerAddSteps.switchToNextTab();

        urlSteps.fromUri("https://auto.ru/cars/used/add/?from=classified&utm_content=form&utm_source=yandex_ads")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Недвижимость» в пресетах формы")
    public void shouldSeePresetLinkRealty() {
        offerAddSteps.onFormPage().sectionLink("Недвижимость").click();
        offerAddSteps.switchToNextTab();

        urlSteps.fromUri("https://realty.yandex.ru/management-new/add/?from=classified&utm_content=form&utm_source=yandex_ads")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по «Как правильно заполнить» с формы")
    public void shouldSeeGoToHowFill() {
        offerAddSteps.onFormPage().link("Как правильно заполнить").click();
        offerAddSteps.switchToNextTab();

        offerAddSteps.onBasePage().h1().waitUntil(hasText("Как правильно заполнить объявление"));
        urlSteps.fromUri(DESKTOP_SUPPORT_RULES_LINK).shouldNotDiffWithWebDriverUrl();
    }

}
