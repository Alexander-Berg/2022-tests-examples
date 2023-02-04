package ru.yandex.realty.compare;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.element.saleads.NewListingOffer;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.GARAZH;
import static ru.yandex.realty.consts.Filters.KOMNATA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Filters.UCHASTOK;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.COMPARISON;
import static ru.yandex.realty.element.saleads.ActionBar.ADD_TO_COMPARISON;

/**
 * Created by kopitsa on 27.06.17.
 */
@DisplayName("Добавление оффера во вкладку сравнений")
@Feature(COMPARISON)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ComparisonOfDifferentRealtyTest {
    private final static int COUNT_OF_OFFERS_TO_COMPARE = 2;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String typeOfDealUrl;

    @Parameterized.Parameter(1)
    public String typeOfRealtyUrl;

    @Parameterized.Parameters(name = "{0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {KUPIT, KVARTIRA},
                {KUPIT, DOM},
                {KUPIT, KOMNATA},
                {KUPIT, UCHASTOK},
                {KUPIT, GARAZH},
                {KUPIT, COMMERCIAL},
                {SNYAT, KVARTIRA},
                {SNYAT, DOM},
                {SNYAT, KOMNATA},
                {SNYAT, GARAZH},
                {SNYAT, COMMERCIAL}
        });
    }

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA).path(typeOfDealUrl).path(typeOfRealtyUrl).open();
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Добавление двух предложений на сравнение")
    @Description("Выбираем из списка объявлений два и добавляем их к сравнению." +
            " Переходим на станицу сравнения и проверяем, что они появились")
    public void shouldAddItemsToComparison() {
        List<String> listOfOfferIdsFromSaleAdsPage = basePageSteps.onOffersSearchPage().offersList()
                .should(hasSize(greaterThanOrEqualTo(COUNT_OF_OFFERS_TO_COMPARE))).stream()
                .peek(offer -> {
                    basePageSteps.moveCursor(offer);
                    offer.actionBar().buttonWithTitle(ADD_TO_COMPARISON).click();
                })
                .map(NewListingOffer::offerLink)
                .map(offerLink -> basePageSteps.getOfferId(offerLink))
                .limit(COUNT_OF_OFFERS_TO_COMPARE)
                .collect(toList());

        urlSteps.testing().path(Pages.COMPARISON).open();

        basePageSteps.onComparisonPage().savedItemsTable().should(exists());

        List<String> listOfOfferIdsFromComparisonPage = basePageSteps.onComparisonPage()
                .savedItemsTable().nameDescription().offerLinkList()
                .stream().map(offerLink -> basePageSteps.getOfferId(offerLink))
                .collect(toList());

        basePageSteps.shouldMatchLists(listOfOfferIdsFromSaleAdsPage, listOfOfferIdsFromComparisonPage);
    }
}