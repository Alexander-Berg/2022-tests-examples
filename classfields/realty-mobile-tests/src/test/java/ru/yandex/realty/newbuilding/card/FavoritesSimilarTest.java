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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.isActive;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка новостройки")
@Feature(NEWBUILDING_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class FavoritesSimilarTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().newbuildingSiteMobile().open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим попап избранного для похожих офферов")
    public void shouldSeeFavPopupFromSimilar() {
        basePageSteps.scrollUntilExistsTouch(() ->
                basePageSteps.onNewBuildingCardPage().similarSitesList().get(FIRST).offerLink());
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingCardPage().similarSitesList().get(FIRST).offerLink());
        basePageSteps.onNewBuildingCardPage().similarSitesList().get(FIRST).addToFav().click();
        basePageSteps.onNewBuildingCardPage().similarSitesList().get(FIRST).addToFav().waitUntil(isActive());
        basePageSteps.onNewBuildingCardPage().favPopup().should(isDisplayed());
    }
}
