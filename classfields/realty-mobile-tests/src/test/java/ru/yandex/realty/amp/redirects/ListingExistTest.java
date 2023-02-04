package ru.yandex.realty.amp.redirects;

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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.AMP;
import static ru.yandex.realty.consts.RealtyFeatures.AMP_FEATURE;

@Link("https://st.yandex-team.ru/VERTISTEST-2036")
@Feature(AMP_FEATURE)
@DisplayName("amp. Проверка существования фильтров")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingExistTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String path;

    @Parameterized.Parameters(name = "Есть листинг {0}")
    public static Collection<String> testParams() {
        return asList(
                "/sankt-peterburg/kupit/dom/blochniy/",
                "/sankt-peterburg/kupit/dom/monolitniy/",
                "/sankt-peterburg/kupit/dom/kirpich-monolit/",
                "/sankt-peterburg/kupit/dom/panelniy/",
                "/sankt-peterburg/kupit/dom/sip-panel/",
                "/sankt-peterburg/kupit/dom/iz-brusa/",
                "/sankt-peterburg/kupit/dom/s-kaminom/",
                "/sankt-peterburg/kupit/dom/iz-kleenogo-brusa/",
                "/sankt-peterburg/snyat/dom/dacha-na-leto/",
                "/sankt-peterburg/snyat/dom/s-panoramnymi-oknami/",
                "/sankt-peterburg/snyat/dom/s-garazhom/",
                "/sankt-peterburg/kupit/kvartira/pod-kluch/",
                "/sankt-peterburg/kupit/kvartira/s-otdelkoy/",
                "/sankt-peterburg/kupit/kvartira/v-mnogoetazhnom-dome/",
                "/sankt-peterburg/kupit/kvartira/1-etazh/",
                "/sankt-peterburg/kupit/kvartira/novostroyki-s-basseinom/",
                "/sankt-peterburg/kupit/kvartira/novostroyki-s-terrasoi/",
                "/sankt-peterburg/kupit/kvartira/penthouse-s-terrasoy/",
                "/sankt-peterburg/snyat/kvartira/v-novostroyke/",
                "/sankt-peterburg/snyat/kvartira/bez-komissii/"
        );
    }


    @Test
    @Owner(KANTEMIROV)
    public void shouldNotSeeJkKpLinkAmp() {
        urlSteps.testing().path(AMP).path(path).open();
        basePageSteps.onAmpSaleAdsPage().content().should(isDisplayed());
    }
}
