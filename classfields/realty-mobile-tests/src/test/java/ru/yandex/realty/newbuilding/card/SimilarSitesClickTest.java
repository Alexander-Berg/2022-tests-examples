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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.NEWBUILDING_DEV_OBJECTS_VALUE;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка новостройки")
@Feature(NEWBUILDING_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SimilarSitesClickTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на первый в списке похожих карточек новостроек")
    public void shouldSeeSimilarSiteClick() {
        urlSteps.testing().newbuildingSiteMobile().open();
        basePageSteps.scrollUntilExistsTouch(() ->
                basePageSteps.onNewBuildingCardPage().similarSitesList().get(0)
        );
        String title = basePageSteps.onNewBuildingCardPage().similarSitesList()
                .waitUntil(hasSize(greaterThan(0))).get(0).title().getText();
        String link = basePageSteps.onNewBuildingCardPage().similarSitesList()
                .waitUntil(hasSize(greaterThan(0))).get(0).link().getAttribute("href");
        basePageSteps.moveCursorAndClick(
                basePageSteps.onNewBuildingCardPage().similarSitesList().get(0).link());
        basePageSteps.switchToNextTab();
        urlSteps.fromUri(link).shouldNotDiffWithWebDriverUrl();
        basePageSteps.onNewBuildingCardPage().h1().should(hasText(title));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на первый в списке карточек новостроек от застройщика")
    public void shouldSeeFromDeveloperSiteClick() {
        urlSteps.testing().newbuildingSiteMobile().open();
                basePageSteps.scrollUntilExistsTouch(() ->
                basePageSteps.onNewBuildingCardPage().fromDeveloperSitesList().get(FIRST));
                basePageSteps.scrollElementToCenter(
                        basePageSteps.onNewBuildingCardPage().similarSitesList().get(FIRST).offerLink());
       basePageSteps.moveCursorAndClick(
                basePageSteps.onNewBuildingCardPage().similarSitesList().get(FIRST).offerLink());
        basePageSteps.switchToNextTab();
        urlSteps.shouldUrl(containsString(NEWBUILDING_DEV_OBJECTS_VALUE));
    }
}
