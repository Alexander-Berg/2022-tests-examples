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
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.ExternalLinks.TERMS_LINK;
import static ru.yandex.general.consts.ExternalLinks.YANDEX_LINK;
import static ru.yandex.general.consts.GeneralFeatures.EDIT_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.element.MyOfferSnippet.EDIT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(EDIT_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация на форме")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class NavigationEditFormTests {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Before
    public void before() {
        offerAddSteps.setMoscowCookie();
        passportSteps.accountWithOffersLogin();
        urlSteps.testing().path(MY).path(OFFERS).open();
        offerAddSteps.onMyOffersPage().snippetFirst().spanLink(EDIT).click();
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
    @DisplayName("Переход на условия использования снизу страницы")
    public void shouldSeeGoToTerms() {
        offerAddSteps.onFormPage().link("по ссылке").click();
        offerAddSteps.switchToNextTab();

        offerAddSteps.onBasePage().h1().should(hasText("Условия использования сервиса «Яндекс.Объявления»"));
        urlSteps.fromUri(TERMS_LINK).shouldNotDiffWithWebDriverUrl();
        ;
    }

}
