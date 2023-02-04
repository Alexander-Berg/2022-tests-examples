package ru.yandex.realty.mytarget;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.step.ProxySteps.AD_MAIL_RU;

@DisplayName("Mytarget для ЖК и КП")
@Feature(GOALS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class MyTargetTagsKPTest {

    private static final String KP_ID = "1869360";
    private static final String KP_LIST_PARAM = "8";
    private static final String SHOW_PHONE_PARAM = "purchase";
    private static final String OPEN_PAGE_PARAM = "product";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ProxySteps proxy;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        proxy.clearHar();
        urlSteps.testing().path("/vyborgskiy_rayon/").path("/kupit/").path("/kottedzhnye-poselki/")
                .path("/vysokoe-simagino/").queryParam("id", KP_ID).open();
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeOpenPageTarget() {
        proxy.shouldSeeRequestInLog(allOf(containsString(AD_MAIL_RU),
                containsString(format("productid=%s", KP_ID)),
                containsString(format("list=%s", KP_LIST_PARAM)),
                containsString(format("pagetype=%s", OPEN_PAGE_PARAM))), equalTo(1));
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeShowPhoneTarget() {
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onVillageSitePage().villageCardAbout().showPhoneButton().click();
        proxy.shouldSeeRequestInLog(allOf(containsString(AD_MAIL_RU),
                containsString(format("productid=%s", KP_ID)),
                containsString(format("list=%s", KP_LIST_PARAM)),
                containsString(format("pagetype=%s", SHOW_PHONE_PARAM))), equalTo(1));
    }
}
