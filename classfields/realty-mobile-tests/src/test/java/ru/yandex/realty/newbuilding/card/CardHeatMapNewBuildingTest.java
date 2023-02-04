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
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.matchers.IsSelectedMatcher.isSelected;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;

@DisplayName("Карточка новостройки")
@Feature(NEWBUILDING_CARD)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardHeatMapNewBuildingTest {
    private static final String LOCATION = "Расположение";

    private MockOffer offer;

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

    @Parameterized.Parameters(name = "Сниппет тепловой карты. «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Яндекс.Драйв"},
                {"Инфраструктура"},
                {"Цена аренды"},
                {"Цена продажи"},
                {"Прогноз окупаемости"},
                {"Транспортная доступность"},
        });
    }

    @Before
    public void before() {
        mockRuleConfigurable.siteWithOffersStatStub(mockSiteWithOffersStatTemplate().build()).createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем по сниппету тепловой карты")
    public void shouldSeeHeatMap() {
        urlSteps.testing().newbuildingSiteMobile().open();
        basePageSteps.scrollUntilExistsTouch(() -> basePageSteps.onNewBuildingCardPage().accordionBlock(LOCATION));
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingCardPage().accordionBlock(LOCATION));
        basePageSteps.moveCursorAndClick(basePageSteps.onNewBuildingCardPage().accordionBlock(LOCATION));
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingCardPage().heatMapSlider());
        basePageSteps.slidingUntil(()-> basePageSteps.onNewBuildingCardPage().heatMapSlider(),
                () -> basePageSteps.onNewBuildingCardPage().heatMapSnippet(shortcut), isDisplayed(), 10);
        basePageSteps.onNewBuildingCardPage().heatMapSnippet(shortcut).click();
        basePageSteps.onNewBuildingCardPage().heatMapShortcut(shortcut).should(isSelected());
    }
}
