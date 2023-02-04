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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.beans.developer.office.OfficeResponse.tyumenOffice;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.ZASTROYSCHIK;
import static ru.yandex.realty.consts.RealtyFeatures.DEVELOPER_CARD;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_GEO_ID_PATH;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_ID;
import static ru.yandex.realty.mock.MockDeveloper.mockEnhancedDeveloper;
import static ru.yandex.realty.page.DeveloperPage.OFFICES;

@Issue("VERTISTEST-1461")
@Feature(DEVELOPER_CARD)
@DisplayName("Карточка застройщика. Скриншоты сниппетов на карте")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MapSnippetScreenshotTest {

    private static final String PHONE = "+7 (800) 555-35-35";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Before
    public void before() {
        mockRuleConfigurable.developerStub(ENHANCED_DEV_ID,
                mockEnhancedDeveloper().setOffices(
                        tyumenOffice().setPhones(asList(PHONE))).build()).createWithDefaults();
        urlSteps.testing().path(ENHANCED_DEV_GEO_ID_PATH).path(ZASTROYSCHIK).path(ENHANCED_DEV_ID).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Видим скриншот сниппета новостройки на карте")
    public void shouldSeeSiteSnippetScreenshot() {
        basePageSteps.onDeveloperPage().map().pins().get(0).hover();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onDeveloperPage().siteSnippet());

        urlSteps.setProductionHost().open();
        basePageSteps.onDeveloperPage().map().pins().get(0).hover();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onDeveloperPage().siteSnippet());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Видим скриншот сниппета офиса на карте")
    public void shouldSeeOfficeSnippetScreenshot() {
        officesButtonClick();
        basePageSteps.onDeveloperPage().map().pins().get(0).hover();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onDeveloperPage().officeSnippet());

        urlSteps.setProductionHost().open();
        officesButtonClick();
        basePageSteps.onDeveloperPage().map().pins().get(0).hover();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onDeveloperPage().officeSnippet());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    private void officesButtonClick() {
        basePageSteps.scrollToElement(basePageSteps.onDeveloperPage().map());
        basePageSteps.onDeveloperPage().map().waitUntil(isDisplayed());
        basePageSteps.onDeveloperPage().button(OFFICES).click();
    }
}
