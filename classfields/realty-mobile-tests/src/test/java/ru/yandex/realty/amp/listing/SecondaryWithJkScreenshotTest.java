package ru.yandex.realty.amp.listing;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.AMP;
import static ru.yandex.realty.consts.RealtyFeatures.AMP_FEATURE;

@Link("https://st.yandex-team.ru/VERTISTEST-1637")
@Feature(AMP_FEATURE)
@DisplayName("amp. Поиск в ЖК. Скриншот")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SecondaryWithJkScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String regionPath;

    @Parameterized.Parameter(2)
    public String zhkPath;

    @Parameterized.Parameters(name = "Ссылка на «{0}»")
    public static Collection<Object[]> rentType() {
        return asList(new Object[][]{
                {"Регион с метро", SANKT_PETERBURG, "/zhk-petr-velikij-i-ekaterina-velikaya-73111/"},
                {"Замороженный ЖК", SPB_I_LO, "/zhk-na-zarechnoj-49046/"},
                {"С акциями и скидками", SPB_I_LO, "/zhk-tarmo-549393/"},
                {"В регионе без метро и шоссе", "/omsk/", "/zhk-sadovyj-661432/"},
        });
    }

    @Test
    @Description("КУКА isAdDisabledTest=1 НЕ РАБОТАЕТ")
    @Owner(KANTEMIROV)
    public void shouldSeeScreenshot() {
        compareSteps.resize(390, 5000);
        basePageSteps.disableAd();
        urlSteps.testing().path(AMP).path(regionPath).path(KUPIT).path(KVARTIRA).path(zhkPath).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onAmpSaleAdsPage().pageRoot());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onAmpSaleAdsPage().pageRoot());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
