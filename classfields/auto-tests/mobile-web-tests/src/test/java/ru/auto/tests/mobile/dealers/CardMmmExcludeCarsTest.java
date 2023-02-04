package ru.auto.tests.mobile.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.QueryParams.SORT;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Карточка дилера - легковые, исключение марки")
@Epic(DEALERS)
@Feature(DEALER_CARD)
@Story(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class CardMmmExcludeCarsTest {

    private static final String DEALER = "/avilon_mercedes_benz_moskva_vozdvizhenka/";
    private static final String MARK = "Audi";

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.setStubs(stub("desktop/Salon"),
                stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/SearchCarsBreadcrumbsAudi"),
                stub("desktop/SearchCarsCountDealerId"),
                stub("mobile/SearchCarsAllDealerId"),
                stub("mobile/SearchCarsAllDealerIdExcludeAudi")).create();

        urlSteps.testing().path(DILER).path(CARS).path(ALL).path(DEALER).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Исключение марки из списка популярных")
    public void shouldExcludeMarkFromPopular() {
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().mmm());
        basePageSteps.onDealerCardPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.onDealerCardPage().mmmPopup().popularMark(MARK).click();
        basePageSteps.onDealerCardPage().mmmPopup().applyFiltersButton().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.title().should(not(hasText(startsWith(MARK)))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Исключение марки из списка всех")
    public void shouldExcludeMarkFromAll() {
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().mmm());
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onDealerCardPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.onDealerCardPage().mmmPopup().allMark(MARK).click();
        basePageSteps.onDealerCardPage().mmmPopup().applyFiltersButton().click();

        urlSteps.addParam("exclude_catalog_filter", format("mark=%s", MARK.toUpperCase()))
                .ignoreParam(SORT).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.title().should(not(hasText(startsWith(MARK)))));
    }
}
