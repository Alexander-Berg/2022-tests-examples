package ru.auto.tests.desktop.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(DEALERS)
@DisplayName("Сортировки")
@Story(DEALER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CardSortScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/SearchCarsBreadcrumbsMercedes"),
                stub("desktop/Salon"),
                stub("desktop/SearchCarsCountDealerId"),
                stub("desktop/SearchCarsMarkModelFiltersAllDealerIdOneMark"),
                stub("desktop/SearchCarsAllDealerId")
        ).create();

        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL)
                .path(CARS_OFFICIAL_DEALER).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение панели сортировок")
    public void shouldSeeSortBar() {
        basePageSteps.onDealerCardPage().sortBar().should(hasText("Сортировка\nВремя размещения\nПродажа с НДС"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Проверяем отображение выпадушки сортировок")
    public void shouldSeeSortPopup() {
        basePageSteps.onDealerCardPage().sortBar().select("Сортировка").selectButton().click();
        basePageSteps.onDealerCardPage().sortBar().selectPopup().waitUntil(isDisplayed())
                .should(hasText("По актуальности\nПо дате размещения\nПо возрастанию цены\nПо убыванию цены\n" +
                        "По году: новее\nПо году: старше\nПо пробегу\nПо названию\nПо уникальности\n" +
                        "По оценке стоимости"));
    }
}
