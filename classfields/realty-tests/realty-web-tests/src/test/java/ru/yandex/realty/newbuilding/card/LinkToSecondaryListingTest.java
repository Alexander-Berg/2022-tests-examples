package ru.yandex.realty.newbuilding.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;
import static ru.yandex.realty.rules.MockRuleConfigurable.NB_ID;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_SITE_OFFER_STAT;

@DisplayName("Карточка новостройки. ссылка на предложение частных лиц")
@Feature(NEWBUILDING_CARD)
@Link("https://st.yandex-team.ru/VERTISTEST-2037")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class LinkToSecondaryListingTest {

    public static final String TEST_STRING = "https://realty.test.vertis.yandex.ru/" +
            "sankt-peterburg_i_leningradskaya_oblast/kupit/kvartira/zhk-valerinskij-gorod-2002000/" +
            "?primarySale=NO&showSimilar=NO";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public CompareSteps compareSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRuleConfigurable
                .siteWithOffersStatStub(mockSiteWithOffersStatTemplate().build())
                .getSiteOfferStat(NB_ID, PATH_TO_SITE_OFFER_STAT).createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим нужную ссылку")
    public void shouldSeeLinkUrl() {
        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.onNewBuildingSitePage().secondarySearchLink().link().should(hasHref(equalTo(TEST_STRING)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот ссылки на предложения от застройщиков")
    public void shouldSeeLinkToOfferScreenshot() {
        basePageSteps.resize(1600, 3000);
        urlSteps.testing().newbuildingSiteMock().open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onNewBuildingSitePage().secondarySearchLink());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onNewBuildingSitePage().secondarySearchLink());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
