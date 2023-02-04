package ru.yandex.realty.newbuilding.card;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.matchers.IsSelectedMatcher.isSelected;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка новостройки")
@Feature(NEWBUILDING_CARD)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ShortcutsNewBuildingTest {
    private static final String LOCATION = "Расположение";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String shortcut;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Расположение"},
                {"Доступность Яндекс.Драйва"},
                {"Транспортная доступность"},
                {"Инфраструктура"},
                {"Цена продажи"},
                {"Цена аренды"},
                {"Прогноз окупаемости"},
        });
    }

    @Before
    public void before() {
        mockRuleConfigurable.siteWithOffersStatStub(mockSiteWithOffersStatTemplate().build()).createWithDefaults();
        urlSteps.testing().newbuildingSiteMobile().open();
        basePageSteps.scrollUntilExistsTouch(() -> basePageSteps.onNewBuildingCardPage().accordionBlock(LOCATION));
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingCardPage().accordionBlock(LOCATION));
        basePageSteps.moveCursorAndClick(basePageSteps.onNewBuildingCardPage().accordionBlock(LOCATION));
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingCardPage().heatMapSlider());
        basePageSteps.slidingUntil(()-> basePageSteps.onNewBuildingCardPage().heatMapSlider(),
                () -> basePageSteps.onNewBuildingCardPage().heatMapSnippet("Яндекс.Драйв"), isDisplayed(), 10);
        basePageSteps.onNewBuildingCardPage().heatMapSnippet("Яндекс.Драйв").click();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по шорткату")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeNewBuildingShortcut() {
        basePageSteps.onNewBuildingCardPage().heatMapShortcut(shortcut).click();
        basePageSteps.onNewBuildingCardPage().heatMapShortcut(shortcut).should(isSelected());
    }
}
