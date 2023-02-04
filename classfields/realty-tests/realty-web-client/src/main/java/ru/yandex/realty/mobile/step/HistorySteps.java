package ru.yandex.realty.mobile.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.yandex.realty.mobile.page.HistoryPage;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.RealtyUtils;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Pages.FILTERS;

/**
 * Created by kopitsa on 22.08.17.
 */
public class HistorySteps extends WebDriverSteps {

    private static final String SPB_RGID = "417899";

    public HistoryPage onHistoryPage() {
        return on(HistoryPage.class);
    }

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Step("Создаём заданное количество разных событий {0}")
    public List<String> addSearchHistoryItems(int count) {
        List<String> result = newArrayList();
        for (int i = 0; i < count; i++) {
            result.add(addSearchHistoryItem(RealtyUtils.getRandomPrice()));
        }
        return result;
    }

    @Step("Создаём одно событие в истории")
    public String addSearchHistoryItem(int priceMin) {
        urlSteps.testing().path(FILTERS).queryParam("type", "SELL").queryParam("category", "APARTMENT")
                .queryParam("rgid", SPB_RGID).queryParam("priceMin", Integer.toString(priceMin)).open();
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).queryParam("priceMin",
                Integer.toString(priceMin));
        return urlSteps.toString();
    }
}
