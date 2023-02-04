package ru.yandex.realty.developer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.ZASTROYSCHIK;
import static ru.yandex.realty.consts.RealtyFeatures.DEVELOPER_CARD;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEVELOPER;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_GEO_ID_PATH;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_ID;
import static ru.yandex.realty.mock.MockDeveloper.mockDeveloper;
import static ru.yandex.realty.mock.MockSite.SITE_TEMPLATE;
import static ru.yandex.realty.mock.MockSite.mockSite;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;

@Issue("VERTISTEST-1461")
@Feature(DEVELOPER_CARD)
@DisplayName("Скриншот обычной и расширенной карточек застройщика")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DeveloperExtendedScreenshotTest {

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

    @Parameterized.Parameter
    public String name;

    @Before
    public void before() {
        compareSteps.resize(1920, 5000);
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                mockSite(SITE_TEMPLATE))).buildSite())
                .developerStub(ENHANCED_DEV_ID, mockDeveloper(ENHANCED_DEVELOPER).setId(ENHANCED_DEV_ID).build())
                .createWithDefaults();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Расширенная карточка застройщика")
    public void shouldSeeDeveloperCardScreenshot() {
        urlSteps.testing().path(ENHANCED_DEV_GEO_ID_PATH).path(ZASTROYSCHIK).path(ENHANCED_DEV_ID).open();
        clickOnBullet();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onDeveloperPage().pageBody());

        urlSteps.setProductionHost().open();
        clickOnBullet();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onDeveloperPage().pageBody());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Step("Кликаем на первый буллет")
    private void clickOnBullet() {
        basePageSteps.onDeveloperPage().firstBullet().click();
        waitSomething(3, TimeUnit.SECONDS);
    }
}
