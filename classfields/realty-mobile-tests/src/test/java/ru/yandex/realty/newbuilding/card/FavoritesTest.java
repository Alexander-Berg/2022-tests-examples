package ru.yandex.realty.newbuilding.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.isActive;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка новостройки")
@Feature(NEWBUILDING_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class FavoritesTest {

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

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        mockRuleConfigurable.siteWithOffersStatStub(mockSiteWithOffersStatTemplate().build()).siteLikeSearchStub()
                .createWithDefaults();
        compareSteps.resize(400, 10000);
        urlSteps.testing().newbuildingSiteMobile().open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка избранного становится активной")
    public void shouldSeeFavButtonActive() {
        basePageSteps.onNewBuildingCardPage().headerFavIcon().click();
        basePageSteps.onNewBuildingCardPage().headerFavIcon().should(isActive());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим попап избранного")
    public void shouldSeeFavPopup() {
        basePageSteps.onNewBuildingCardPage().headerFavIcon().click();
        basePageSteps.onNewBuildingCardPage().favPopup().waitUntil(isDisplayed());
        basePageSteps.onNewBuildingCardPage().favPopup().should(hasText(containsString("Новостройка уже\nв избранном")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Закрываем попап избранного")
    public void shouldNotSeeFavPopup() {
        basePageSteps.onNewBuildingCardPage().headerFavIcon().click();
        basePageSteps.onNewBuildingCardPage().favPopup().waitUntil(isDisplayed());
        basePageSteps.onNewBuildingCardPage().favPopup().closeButton().waitUntil(isDisplayed())
                .clickWhile(not(isDisplayed()));
        basePageSteps.onNewBuildingCardPage().favPopup().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим скриншот попапа избранного")
    public void shouldSeeFavPopupScreenshot() {
        basePageSteps.onNewBuildingCardPage().headerFavIcon().click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onNewBuildingCardPage().favPopup());
        basePageSteps.onNewBuildingCardPage().favPopup().closeButton().click();
        basePageSteps.onNewBuildingCardPage().headerFavIcon().clickIf(isActive());
        urlSteps.setMobileProductionHost().open();
        basePageSteps.onNewBuildingCardPage().headerFavIcon().waitUntil(isDisplayed()).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onNewBuildingCardPage().favPopup());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим попапа избранного при добавлении в блоке от застройщика")
    public void shouldSeeFavPopupFromDev() {
        basePageSteps.scrollUntilExistsTouch(() -> basePageSteps.onNewBuildingCardPage().fromDeveloperSitesList().get(FIRST));
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingCardPage().fromDeveloperSitesList().get(FIRST));
        basePageSteps.onNewBuildingCardPage().fromDeveloperSitesList().get(FIRST).addToFav().click();
        basePageSteps.onNewBuildingCardPage().fromDeveloperSitesList().get(FIRST).addToFav().waitUntil(isActive());
        basePageSteps.onNewBuildingCardPage().favPopup().should(isDisplayed());
    }
}
