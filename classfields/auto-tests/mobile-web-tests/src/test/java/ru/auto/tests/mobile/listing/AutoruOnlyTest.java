package ru.auto.tests.mobile.listing;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.QueryParams.SEARCH_TAG;
import static ru.auto.tests.desktop.consts.Urls.ONLY_AUTORU_FRAGMENT;
import static ru.auto.tests.desktop.consts.Urls.YANDEX_SUPPORT_AUTORU_LEGAL_TERMS_OF_SERVICE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг объявлений - бейдж «Только на Auto.ru»")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class AutoruOnlyTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsAutoruExclusive");

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam(SEARCH_TAG, "autoru_exclusive")
                .open();
        basePageSteps.onListingPage().getSale(0).badge("Только на Авто.ру").waitUntil(isDisplayed()).click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Отображение поп-апа «Только на Авто.ру»")
    public void shouldSeeAutoruOnlyPopup() {
        basePageSteps.onListingPage().popup().waitUntil(isDisplayed()).should(hasText("Только на Авто.ру\n" +
                "Не пропустите — продавец указал, что объявление размещается только на Авто.ру.\nПодробнее"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке в поп-апе «Только на Авто.ру»")
    public void shouldClickAutoruOnlyPopupUrl() {
        basePageSteps.onListingPage().popup().button("Подробнее").click();

        urlSteps.switchToNextTab();
        urlSteps.fromUri(format("%s%s", YANDEX_SUPPORT_AUTORU_LEGAL_TERMS_OF_SERVICE, ONLY_AUTORU_FRAGMENT)).shouldNotSeeDiff();
    }
}
