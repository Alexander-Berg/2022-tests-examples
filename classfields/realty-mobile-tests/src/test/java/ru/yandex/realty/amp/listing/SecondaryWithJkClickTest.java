package ru.yandex.realty.amp.listing;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.AMP;
import static ru.yandex.realty.consts.RealtyFeatures.AMP_FEATURE;

@Link("https://st.yandex-team.ru/VERTISTEST-1637")
@Feature(AMP_FEATURE)
@DisplayName("amp. Поиск в ЖК. Клик по сниппету")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SecondaryWithJkClickTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

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
                {"Регион с метро", SPB_I_LO, "/zhk-tarmo-549393/"},
                {"С акциями и скидками", SPB_I_LO, "/zhk-tarmo-549393/"},
                {"В регионе без метро и шоссе", "/omsk/", "/zhk-sadovyj-661432/"},
//                amp пока не ходит в мокрицу
//                {"ЖК без фото", ""},
        });
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeSClick() {
        urlSteps.testing().path(AMP).path(regionPath).path(KUPIT).path(KVARTIRA).path(zhkPath).open();
        basePageSteps.onAmpSaleAdsPage().ampSiteSnippet().click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(regionPath).path(KUPIT).path(NOVOSTROJKA).path(zhkPath.replace("zhk-", ""))
                .ignoreParam(UrlSteps.AMP_PARAMETER).shouldNotDiffWithWebDriverUrl();
    }
}
