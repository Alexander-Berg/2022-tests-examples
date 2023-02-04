package ru.yandex.realty.advertising;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.atlas.webdriver.AtlasWebElement;
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.consts.Counters.AN_YANDEX_RU_COUNT;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.ADVERTISING;

@DisplayName("Реклама. Выдача, страница")
@Feature(ADVERTISING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
@Issue("VERTISTEST-942")
public class ListingPageBannersOnListingTest {

    private static final int COUNT_IDENTIFIER_LENGTH = 5;

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
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.scrolling(10000, 200);
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Production.class})
    @DisplayName("Проверяем рекламные места на выдаче на наличие пикселей видимости")
    public void shouldSeeListingAdCounters() {
        basePageSteps.onOffersSearchPage().adList().should(hasSize(5));
        proxy.shouldSeeRequestInLog(containsString(getPixelIdUrl(basePageSteps.onOffersSearchPage().adList().get(0).link())),
                equalTo(5));
    }

    @Step("Получаем идентификатор пикселя")
    private String getPixelIdUrl(AtlasWebElement element) {
        return element.getAttribute("href").substring(0, AN_YANDEX_RU_COUNT.length() + COUNT_IDENTIFIER_LENGTH);
    }
}
