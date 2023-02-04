package ru.yandex.realty.amp.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.AMP;
import static ru.yandex.realty.consts.RealtyFeatures.AMP_FEATURE;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatWithoutPhotoTemplate;

@Link("https://st.yandex-team.ru/VERTISTEST-1637")
@Feature(AMP_FEATURE)
@DisplayName("amp. Поиск в ЖК. Скриншот")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SecondaryWithJkWithoutPhotoScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим ЖК без фото")
    public void shouldSeeScreenshotWithoutPhoto() {
        mockRuleConfigurable.siteWithOffersStatStub(mockSiteWithOffersStatWithoutPhotoTemplate().build())
                .createWithDefaults();
        compareSteps.resize(390, 2000);
        urlSteps.testing().path(AMP).path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path("/zhk-rr-73111/").open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onAmpSaleAdsPage().pageRoot());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onAmpSaleAdsPage().pageRoot());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
