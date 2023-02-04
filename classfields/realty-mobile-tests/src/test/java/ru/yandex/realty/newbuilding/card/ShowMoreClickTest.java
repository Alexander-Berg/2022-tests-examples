package ru.yandex.realty.newbuilding.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.mobile.element.newbuilding.NewbuildingCardMortgages.ALL_MORTGAGES;
import static ru.yandex.realty.mobile.page.NewBuildingCardPage.SHOW_ALL;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.SITE_WITH_OFFERS_STAT_FOR_NB;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка новостройки")
@Feature(NEWBUILDING_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ShowMoreClickTest {

    private static final String SHOW_MORE = "Показать ещё";
    private static final String MORE = "Ещё";
    private static final String FULL_DESCRIPTION = "Полное описание";
    private static final String OPISANIE_ZJK = "Описание ЖК";
    private static final String IPOTEKA_I_SKIDKI = "Ипотека и скидки";

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

    @Before
    public void before() {
        mockRuleConfigurable.mockNewBuilding().createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по показать ещё в новостройках от застройщика")
    public void shouldSeeMoreFromDeveloperSites() {
        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onNewBuildingCardPage().link(SHOW_ALL));
        basePageSteps.onNewBuildingCardPage().link(SHOW_ALL).click();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(NOVOSTROJKA).queryParam("developerId", "52308");
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по «Ещё (N) особенностей»")
    public void shouldSeeMoreDescriptionFeatures() {
        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.scrollUntilExistsTouch(() -> basePageSteps.onNewBuildingCardPage().accordionBlock(OPISANIE_ZJK));
        basePageSteps.moveCursorAndClick(basePageSteps.onNewBuildingCardPage().accordionBlock(OPISANIE_ZJK));
        basePageSteps.scrollUntilExists(() -> basePageSteps.onNewBuildingCardPage().featuresBlock().button(MORE));
        int initialSitesSize = basePageSteps.onNewBuildingCardPage().featuresBlock().featuresList().size();
        basePageSteps.onNewBuildingCardPage().featuresBlock().button(MORE).click();
        basePageSteps.onNewBuildingCardPage().featuresBlock().featuresList()
                .should(hasSize(greaterThan(initialSitesSize)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по «Полное описание»")
    public void shouldSeeFullDescription() {
        String fullDescription = new GsonBuilder().create().fromJson(
                getResourceAsString(SITE_WITH_OFFERS_STAT_FOR_NB), JsonObject.class)
                .getAsJsonObject("response").getAsJsonObject("site").getAsJsonPrimitive("description").getAsString();
        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.scrollUntilExistsTouch(() -> basePageSteps.onNewBuildingCardPage().accordionBlock(OPISANIE_ZJK));
        basePageSteps.moveCursorAndClick(basePageSteps.onNewBuildingCardPage().accordionBlock(OPISANIE_ZJK));
        basePageSteps.scrollUntilExists(() -> basePageSteps.onNewBuildingCardPage().description());
        basePageSteps.onNewBuildingCardPage().description().spanLink(FULL_DESCRIPTION).click();
        basePageSteps.onNewBuildingCardPage().description().should(hasText(fullDescription));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по «Все ипотечные программы» в ипотеках переходит на страничку с ипотекой")
    public void shouldSeeMoreMortgages() {
        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.scrollUntilExistsTouch(() -> basePageSteps.onNewBuildingCardPage().accordionBlock(IPOTEKA_I_SKIDKI));
        basePageSteps.moveCursorAndClick(basePageSteps.onNewBuildingCardPage().accordionBlock(IPOTEKA_I_SKIDKI));
        basePageSteps.scrollUntilExists(() ->
                basePageSteps.onNewBuildingCardPage().newbuildingCardMortgagesBlock());
        basePageSteps.onNewBuildingCardPage().newbuildingCardMortgagesBlock().link(ALL_MORTGAGES).click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.shouldCurrentUrlContains("/ipoteka/");
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по «Показать ещё (N) документа»")
    public void shouldSeeMoreDocuments() {
        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.scrollUntilExistsTouch(() -> basePageSteps.onNewBuildingCardPage().accordionBlock(OPISANIE_ZJK));
        basePageSteps.moveCursorAndClick(basePageSteps.onNewBuildingCardPage().accordionBlock(OPISANIE_ZJK));
        basePageSteps.scrollUntilExists(() ->
                basePageSteps.onNewBuildingCardPage().newbuildingCardDocuments().link(SHOW_MORE));
        int initialSitesSize = basePageSteps.onNewBuildingCardPage().newbuildingCardDocuments().docs().size();
        basePageSteps.onNewBuildingCardPage().newbuildingCardDocuments().link(SHOW_MORE).click();
        basePageSteps.onNewBuildingCardPage().newbuildingCardDocuments().docs()
                .should(hasSize(greaterThan(initialSitesSize)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по «Ещё (N) предложения» в акциях")
    public void shouldSeeMorePromos() {
        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.scrollUntilExistsTouch(() -> basePageSteps.onNewBuildingCardPage().accordionBlock(IPOTEKA_I_SKIDKI));
        basePageSteps.moveCursorAndClick(basePageSteps.onNewBuildingCardPage().accordionBlock(IPOTEKA_I_SKIDKI));
        basePageSteps.scrollUntilExists(() ->
                basePageSteps.onNewBuildingCardPage().newbuildingCardProposals().button(MORE));
        int initialSitesSize = basePageSteps.onNewBuildingCardPage().newbuildingCardProposals().promosList().size();
        basePageSteps.onNewBuildingCardPage().newbuildingCardProposals().button(MORE).click();
        basePageSteps.onNewBuildingCardPage().newbuildingCardProposals().promosList()
                .should(hasSize(greaterThan(initialSitesSize)));
    }
}
