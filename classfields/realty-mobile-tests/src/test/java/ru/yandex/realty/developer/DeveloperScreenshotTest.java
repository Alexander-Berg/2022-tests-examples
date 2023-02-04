package ru.yandex.realty.developer;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockDeveloper;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.ZASTROYSCHIK;
import static ru.yandex.realty.consts.RealtyFeatures.DEVELOPER_CARD;
import static ru.yandex.realty.mock.MockDeveloper.BASIC_DEVELOPER;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEVELOPER;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_GEO_ID_PATH;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_ID;
import static ru.yandex.realty.mock.MockDeveloper.mockDeveloper;
import static ru.yandex.realty.mock.MockSite.SITE_TEMPLATE;
import static ru.yandex.realty.mock.MockSite.mockSite;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;

@Issue("VERTISTEST-1461")
@Feature(DEVELOPER_CARD)
@DisplayName("Скриншоты обычной и расширенной карточек застройщика")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DeveloperScreenshotTest {

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

    @Parameterized.Parameter(1)
    public MockDeveloper developer;

    @Parameterized.Parameter(2)
    public String id;

    @Parameterized.Parameter(3)
    public String path;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Карточка застройщика", mockDeveloper(BASIC_DEVELOPER), "2000", MOSKVA},
                {"Расширенная карточка застройщика ", mockDeveloper(ENHANCED_DEVELOPER), ENHANCED_DEV_ID, ENHANCED_DEV_GEO_ID_PATH}
        });
    }

    @Before
    public void before() {
        compareSteps.resize(375, 4000);
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                mockSite(SITE_TEMPLATE))).buildSite())
                .developerStub(id, developer.setId(id).build()).createWithDefaults();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Видим скриншот карточки застройщика")
    public void shouldSeeDeveloperCardScreenshot() {
        urlSteps.testing().path(path).path(ZASTROYSCHIK).path(id).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onDeveloperPage().pageRoot());

        urlSteps.setMobileProductionHost().open();
        basePageSteps.onDeveloperPage().h1().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onDeveloperPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
