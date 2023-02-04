package ru.yandex.realty.wizard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.SEARCH_LIST;

/**
 * Created by kopitsa on 24.08.17.
 */
@DisplayName("Работа параметра pinnedOfferId")
@Issue("VERTISTEST-542")
@Feature(SEARCH_LIST)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PinnedOfferIdTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Проверяем параметр pinnedOfferId")
    public void shouldSeePinnedOfferFirst() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        String offerId = basePageSteps.getOfferId(basePageSteps.onOffersSearchPage().offersList()
                .should(hasSize(greaterThanOrEqualTo(2))).get(1).offerLink());
        urlSteps.queryParam("pinnedOfferId", offerId).open();
        String offerIdOnPinneOfferPage = basePageSteps.getOfferId(basePageSteps.onOffersSearchPage().offersList()
                .should(hasSize(greaterThanOrEqualTo(1))).get(0).offerLink());
        basePageSteps.shouldEqual("Id первого оффера на странице должно быть тем, который записан в параметре pinnedOfferId",
                offerId, offerIdOnPinneOfferPage);
    }
}
