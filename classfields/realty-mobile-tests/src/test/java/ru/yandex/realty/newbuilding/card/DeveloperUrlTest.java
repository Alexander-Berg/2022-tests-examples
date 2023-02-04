package ru.yandex.realty.newbuilding.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Ссылка на карточку застройщика")
@Feature(NEWBUILDING_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
@Issue("VERTISTEST-1461")
public class DeveloperUrlTest {

    private static final String NAME = "Кекс";
    private static final String NAME_TRANSLIT = "keks";
    private static final String ID = "26500";

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

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на карточку застройщика")
    public void shouldSeeDeveloperUrl() {
        mockRuleConfigurable.siteWithOffersStatStub(
                mockSiteWithOffersStatTemplate().setDeveloperId(ID).setDeveloperName(NAME).build()).createWithDefaults();
        urlSteps.testing().newbuildingSiteMobile().open();
basePageSteps.scrollUntilExists(() -> basePageSteps.onNewBuildingCardPage().cardsDevInfo().get(FIRST).link());
        basePageSteps.onNewBuildingCardPage().cardsDevInfo().get(FIRST).link().should(hasHref(equalTo(
                urlSteps.testing().path(SPB_I_LO).developerPath(NAME_TRANSLIT, ID).toString())));
    }

}
