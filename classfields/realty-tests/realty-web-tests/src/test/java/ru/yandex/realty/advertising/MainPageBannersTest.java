package ru.yandex.realty.advertising;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import net.lightbody.bmp.core.har.HarNameValuePair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.anything;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Counters.AN_YANDEX_RU_COUNT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.ADVERTISING;
import static ru.yandex.realty.step.ProxySteps.ADS_ADFOX_RU;
import static ru.yandex.realty.step.ProxySteps.AN_YANDEX_RU_META;
import static ru.yandex.realty.step.ProxySteps.IMP_ID;

@DisplayName("Реклама. Выдача, страница")
@Feature(ADVERTISING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
@Issue("VERTISTEST-948")
public class MainPageBannersTest {

    private static final int COUNT_IDENTIFIER_LENGTH = 5;
    private static final String C3_BANNER_IMP_ID = "21";
    private static final String R1_BANNER_IMP_ID = "5";
    private static final String CITY_MOSCOW_ID = "213";
    private static final String ASCII = "empty";
    private static final List<HarNameValuePair> paramsForRegion = asList(
            new HarNameValuePair("ascii", ASCII),
            new HarNameValuePair("city", CITY_MOSCOW_ID));
    private static final List<HarNameValuePair> paramsForRegionWithAd = asList(
            new HarNameValuePair("pp", "g"),
            new HarNameValuePair("ps", "cjtp"),
            new HarNameValuePair("p2", "fmmm"));
    private static final List<HarNameValuePair> paramsForPikBanner = asList(
            new HarNameValuePair("pp", "irt"),
            new HarNameValuePair("ps", "cjtp"),
            new HarNameValuePair("p2", "fhwz"));

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ProxySteps proxy;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void openMain() {
        proxy.clearHar();
        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Проверяем рекламное место места на выдаче на наличие пикселей видимости")
    public void shouldSeeListingAdCounters() {
        urlSteps.replacePath("lipetsk").open();
        proxy.proxyServerManager.getServer().newHar();
        basePageSteps.scrolling(5000, 200);
        String hrefC3 = basePageSteps.onMainPage().c3Banner().link().waitUntil(isDisplayed()).getAttribute("href");
        proxy.shouldSeeRequestInLog(containsString(getPixelCountIdUrlFrom(AN_YANDEX_RU_COUNT, hrefC3)), equalTo(1));
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Проверяем что нет других «imp-id» кроме заданных")
    public void shouldNotSeeOtherImpIds() {
        proxy.shouldNotSeeQueryExcept(asList(C3_BANNER_IMP_ID, R1_BANNER_IMP_ID));
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Проверяем видим запросы с параметрами для баннера С3")
    public void shouldSeeC3Params() {
        proxy.shouldSeeRequestsWithQueriesInLog(containsString(AN_YANDEX_RU_META),
                equalTo(IMP_ID), equalTo(C3_BANNER_IMP_ID), paramsForRegion);
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Проверяем видим запросы с параметрами для баннера R1")
    public void shouldSeeR1Params() {
        basePageSteps.setWindowSize(1200, 1800);
        basePageSteps.onMainPage().r1Banner().waitUntil(isDisplayed());
        proxy.shouldSeeRequestsWithQueriesInLog(containsString(AN_YANDEX_RU_META),
                equalTo(IMP_ID), equalTo(R1_BANNER_IMP_ID), paramsForRegion);
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Проверяем что запросов с «imp-id» заданное количество")
    public void shouldSeeNumberOfImpIds() {
        proxy.shouldSeeRequestWithQueryInLog(containsString(AN_YANDEX_RU_META), equalTo(IMP_ID), anything(),
                equalTo(2));
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Кликаем на баннер C3, проверяем что открывается вкладка и есть урл")
    public void shouldSeeC3Click() {
        basePageSteps.shouldSeeNotNullClick(basePageSteps.onMainPage().c3Banner());
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Кликаем на баннер R1, проверяем что открывается вкладка и есть урл")
    public void shouldSeeR1Click() {
        basePageSteps.setWindowSize(1200, 1800);
        basePageSteps.shouldSeeNotNullClick(basePageSteps.onMainPage().r1Banner());
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Подложка для региона с рекламой. Уходят нужные параметры")
    public void shouldSeeParamsForRegion() {
        proxy.shouldSeeRequestsWithQueriesInLog(containsString(ADS_ADFOX_RU), anything(), anything(),
                paramsForRegionWithAd);
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Подложка для баннера пик. Уходят нужные параметры")
    public void shouldSeeParamsForPikBanner() {
        proxy.shouldSeeRequestsWithQueriesInLog(containsString(ADS_ADFOX_RU), anything(), anything(),
                paramsForPikBanner);
    }

    @Step("Получаем идентификатор пикселя")
    private String getPixelCountIdUrlFrom(String bannerUrl, String href) {
        return href.substring(0, bannerUrl.length() + COUNT_IDENTIFIER_LENGTH);
    }
}
