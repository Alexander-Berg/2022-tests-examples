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
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка новостройки")
@Feature(NEWBUILDING_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ExpandableFlatsNewBuildingCardTest {

    private static final String SECTION = "Квартиры";

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
        mockRuleConfigurable.siteWithOffersStatStub(mockSiteWithOffersStatTemplate().build()).createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Сворачиваем и разворчиваем секцию Квартиры аккордеона")
    public void shouldSeeNewBuildingCardWithFoldedCards() {
        urlSteps.testing().newbuildingSiteMobile().open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onNewBuildingCardPage().cardSection(SECTION));
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingCardPage().cardSection(SECTION));
        basePageSteps.onNewBuildingCardPage().cardSection(SECTION).newbuildingContent().should(exists());
        basePageSteps.moveCursorAndClick(basePageSteps.onNewBuildingCardPage().cardSection(SECTION));
        basePageSteps.onNewBuildingCardPage().cardSection(SECTION).newbuildingContentHidden().should(exists());
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingCardPage().cardSection(SECTION));
        basePageSteps.moveCursorAndClick(basePageSteps.onNewBuildingCardPage().cardSection(SECTION));
        basePageSteps.onNewBuildingCardPage().cardSection(SECTION).newbuildingContent().should(exists());

    }
}
