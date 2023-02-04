package ru.auto.tests.desktop.listing;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
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
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг объявлений - бейдж «Только на Auto.ru»")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
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
    public MockRuleConfigurable mockRule;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/SearchCarsAutoruExclusive")
        ).create();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam(SEARCH_TAG, "autoru_exclusive").open();
        basePageSteps.onListingPage().getSale(0).autoruOnlyBadge().waitUntil(isDisplayed()).hover();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Отображение поп-апа «Только на Auto.ru»")
    public void shouldSeeAutoruExclusivePopup() {
        basePageSteps.onListingPage().popup().waitUntil(isDisplayed())
                .should(hasText("Только на Авто.ру\n" +
                        "Не пропустите — продавец указал, что объявление размещается только на Авто.ру\n" +
                        "Подробнее"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке в поп-апе «Только на Auto.ru»")
    public void shouldClickAutoruExclusivePopupUrl() {
        basePageSteps.onListingPage().popup().button().waitUntil(isDisplayed()).click();

        urlSteps.switchToNextTab();
        urlSteps.fromUri(format("%s%s", YANDEX_SUPPORT_AUTORU_LEGAL_TERMS_OF_SERVICE, ONLY_AUTORU_FRAGMENT)).shouldNotSeeDiff();
    }
}
