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

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.NEW4NEW;
import static ru.auto.tests.desktop.consts.Pages.TAG;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Новинки автопрома")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class New4NewTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionUnauth"),
                stub("desktop/SearchCarsBreadcrumbsNew4New"),
                stub("desktop/SearchCarsNew4New"),
                stub("desktop/ReferenceCatalogTagsV1")
        ).create();

        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).path(TAG).path(NEW4NEW).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Отображение новинок")
    public void shouldSeeNew() {
        basePageSteps.onListingPage().groupSalesList().should(hasSize(18));
        basePageSteps.onListingPage().getGroupSale(0).groupBadges().should(hasText("Новинка"));
        basePageSteps.onListingPage().getGroupSale(1).groupBadges().should(hasText("Новинка"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        basePageSteps.onListingPage().getGroupSale(0).hover();
        basePageSteps.scrollUp(basePageSteps.onListingPage().getGroupSale(0).getSize().getHeight());
        basePageSteps.onListingPage().getGroupSale(0).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).path(GROUP).path("/volkswagen/teramont/22970558-22970602/")
                .addParam("search_tag", "new4new")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сохранение тега при переключении вкладок")
    public void shouldSaveTag() {
        mockRule.setStubs(stub("desktop/UserFavoritesAllSubscriptionsEmpty"),
                stub("desktop/ProxyPublicApi")).update();

        basePageSteps.onListingPage().filter().radioButton("С пробегом").click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).path(TAG).path(NEW4NEW).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().should(isDisplayed());
    }
}
