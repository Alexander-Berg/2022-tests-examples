package ru.yandex.realty.advertising;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.atlas.webdriver.AtlasWebElement;
import net.lightbody.bmp.core.har.HarNameValuePair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.consts.Counters.AN_YANDEX_RU_COUNT;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.KOMNATA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Filters.UCHASTOK;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.ADVERTISING;
import static ru.yandex.realty.step.ProxySteps.AN_YANDEX_RU_META;
import static ru.yandex.realty.step.ProxySteps.IMP_ID;

@DisplayName("Реклама. Выдача, страница")
@Feature(ADVERTISING)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithProxyModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@Issue("VERTISTEST-942")
public class ListingPageBannersTest {

    private static final int COUNT_IDENTIFIER_LENGTH = 5;
    private static final String C1_BANNER_IMP_ID = "36";
    private static final String C2_BANNER_IMP_ID = "9";
    private static final String C3_BANNER_IMP_ID = "19";
    private static final String R1_BANNER_IMP_ID_1 = "17";
    private static final String R1_BANNER_IMP_ID_2 = "35";
    private static final String REALTY_TYPE_SECOND = "SECOND";
    private static final String REALTY_TYPE_COMMERCIAL = "COMMERCIAL";
    private static final String CITY_SPB_ID = "2";
    private static final String CITY_MOSCOW_ID = "213";
    private static final String EMPTY_PARAM = "";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ProxySteps proxy;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String path;

    @Parameterized.Parameter(1)
    public String paramName;

    @Parameterized.Parameter(2)
    public String paramValue;

    @Parameterized.Parameter(3)
    public String cityId;

    @Parameterized.Parameter(4)
    public String realtyType;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {SANKT_PETERBURG + KUPIT + KVARTIRA, "newFlat", "NO", CITY_SPB_ID, REALTY_TYPE_SECOND},
                {SANKT_PETERBURG + KUPIT + KOMNATA, EMPTY_PARAM, EMPTY_PARAM, CITY_SPB_ID, REALTY_TYPE_SECOND},
                {MOSKVA + KUPIT + DOM, EMPTY_PARAM, EMPTY_PARAM, CITY_MOSCOW_ID, REALTY_TYPE_SECOND},
                {SANKT_PETERBURG + KUPIT + UCHASTOK, EMPTY_PARAM, EMPTY_PARAM, CITY_SPB_ID, REALTY_TYPE_SECOND},
                {SANKT_PETERBURG + SNYAT + KVARTIRA, EMPTY_PARAM, EMPTY_PARAM, CITY_SPB_ID, REALTY_TYPE_SECOND},
                {MOSKVA + SNYAT + COMMERCIAL, EMPTY_PARAM, EMPTY_PARAM, CITY_MOSCOW_ID, REALTY_TYPE_COMMERCIAL}
        });
    }

    @Before
    public void openMain() {
        basePageSteps.setWindowSize(1200, 1200);
        urlSteps.testing().path(path).queryParam(paramName, paramValue).open();
        proxy.clearHarUntilThereAreNoHarEntries();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Проверяем рекламные места на выдаче на наличие пикселей видимости")
    public void shouldSeeListingAdCounters() {
        basePageSteps.scrolling(10000, 200);
        basePageSteps.onOffersSearchPage().adList().should(hasSize(5));
        proxy.shouldSeeRequestInLog(containsString(getPixelIdUrl(basePageSteps.onOffersSearchPage().adList().get(0).link())),
                equalTo(5));
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Проверяем что нет других «imp-id» кроме заданных")
    public void shouldNotSeeOtherImpIds() {
        proxy.shouldNotSeeQueryExcept(asList(C3_BANNER_IMP_ID, R1_BANNER_IMP_ID_1, R1_BANNER_IMP_ID_2,
                C1_BANNER_IMP_ID, C2_BANNER_IMP_ID));
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Проверяем что запросов с «imp-id» заданное количество")
    public void shouldSeeNumberOfImpIds() {
        proxy.shouldSeeRequestWithQueryInLog(containsString(AN_YANDEX_RU_META), equalTo(IMP_ID), anything(), equalTo(4));
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Проверяем видим C1 баннер")
    public void shouldSeeC1Params() {
        proxy.shouldSeeRequestsWithQueriesInLog(containsString(AN_YANDEX_RU_META),
                equalTo(IMP_ID), equalTo(C1_BANNER_IMP_ID), getHarNameValuePairs(cityId, realtyType));
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Проверяем видим C2 баннер")
    public void shouldSeeC2Params() {
        proxy.shouldSeeRequestsWithQueriesInLog(containsString(AN_YANDEX_RU_META),
                equalTo(IMP_ID), equalTo(C2_BANNER_IMP_ID), getHarNameValuePairs(cityId, realtyType));
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Проверяем видим C3 баннер")
    public void shouldSeeC3Params() {
        proxy.shouldSeeRequestsWithQueriesInLog(containsString(AN_YANDEX_RU_META),
                equalTo(IMP_ID), equalTo(C3_BANNER_IMP_ID), getHarNameValuePairs(cityId, realtyType));
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Проверяем видим что уходит запрос R1 баннер и кликаем ")
    public void shouldSeeR1Params() {
        proxy.shouldSeeRequestsWithQueriesInLog(containsString(AN_YANDEX_RU_META),
                equalTo(IMP_ID), anyOf(equalTo(R1_BANNER_IMP_ID_1), equalTo(R1_BANNER_IMP_ID_2)),
                getHarNameValuePairs(cityId, realtyType));
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Кликаем на баннер R1, проверяем что открывается вкладка и есть урл")
    public void shouldSeeR1Click() {

        basePageSteps.shouldSeeNotNullClick(basePageSteps.onOffersSearchPage().r1Banner());
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Кликаем на баннер C1, проверяем что открывается вкладка и есть урл")
    public void shouldSeeC1Click() {
        basePageSteps.shouldSeeNotNullClick(basePageSteps.onOffersSearchPage().c1Banner());
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Кликаем на баннер C2, проверяем что открывается вкладка и есть урл")
    public void shouldSeeC2Click() {
        basePageSteps.shouldSeeNotNullClick(basePageSteps.onOffersSearchPage().c2Banner());
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Кликаем на баннер C3, проверяем что открывается вкладка и есть урл")
    public void shouldSeeC3Click() {
        basePageSteps.shouldSeeNotNullClick(basePageSteps.onOffersSearchPage().c3Banner());
    }

    @Step("Получаем идентификатор пикселя")
    private String getPixelIdUrl(AtlasWebElement element) {
        return element.getAttribute("href").substring(0, AN_YANDEX_RU_COUNT.length() + COUNT_IDENTIFIER_LENGTH);
    }

    @Step("Получаем параметры региона и типа недвижимости")
    private static List<HarNameValuePair> getHarNameValuePairs(String city, String category) {
        return asList(
                new HarNameValuePair("ascii", format("%s|%s", category, city)),
                new HarNameValuePair("city", city),
                new HarNameValuePair("category", category));
    }
}
