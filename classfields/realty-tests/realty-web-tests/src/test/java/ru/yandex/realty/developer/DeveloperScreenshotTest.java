package ru.yandex.realty.developer;

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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.ZASTROYSCHIK;
import static ru.yandex.realty.consts.RealtyFeatures.DEVELOPER_CARD;
import static ru.yandex.realty.mock.MockDeveloper.BASIC_DEVELOPER;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_GEO_ID_PATH;
import static ru.yandex.realty.mock.MockDeveloper.mockDeveloper;
import static ru.yandex.realty.mock.MockSite.SITE_TEMPLATE;
import static ru.yandex.realty.mock.MockSite.mockSite;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;

@Issue("VERTISTEST-1461")
@Feature(DEVELOPER_CARD)
@DisplayName("Скриншот обычной и расширенной карточек застройщика")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DeveloperScreenshotTest {

    private static String DEVELOPER_ID = "2000";

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
        compareSteps.resize(1920, 3000);
        mockRuleConfigurable.offerWithSiteSearchStub(
                offerWithSiteSearchTemplate().sites(asList(mockSite(SITE_TEMPLATE))).buildSite())
                .developerStub(DEVELOPER_ID, mockDeveloper(BASIC_DEVELOPER).setId(DEVELOPER_ID).build())
                .createWithDefaults();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Видим скриншот карточки застройщика")
    public void shouldSeeDeveloperCardScreenshot() {
        urlSteps.testing().path(ENHANCED_DEV_GEO_ID_PATH).path(ZASTROYSCHIK).path(DEVELOPER_ID).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onDeveloperPage().pageBody());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onDeveloperPage().pageBody());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
