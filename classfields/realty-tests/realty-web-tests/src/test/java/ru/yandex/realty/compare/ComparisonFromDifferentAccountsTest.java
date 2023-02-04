package ru.yandex.realty.compare;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.COMPARISON;
import static ru.yandex.realty.element.saleads.ActionBar.ADD_TO_COMPARISON;
import static ru.yandex.realty.step.CommonSteps.FIRST;

/**
 * Created by kopitsa on 30.06.17.
 */
@DisplayName("Ссылка на список сравнения")
@Feature(COMPARISON)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ComparisonFromDifferentAccountsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Account account;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Ссылка на список сравнения")
    @Description("Формируем список сравнения. Логинимся под другим пользователем. " +
            "Заходим по ссылке в список сравнения первого пользователя. Проверяем, что все хорошо")
    public void shouldSendComparisonLink() {
        basePageSteps.onOffersSearchPage().offersList().waitUntil(hasSize(greaterThan(1)));
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offersList().get(0));
        basePageSteps.onOffersSearchPage().offer(FIRST).actionBar().buttonWithTitle(ADD_TO_COMPARISON).click();
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offersList().get(1));
        basePageSteps.onOffersSearchPage().offer(1).actionBar().buttonWithTitle(ADD_TO_COMPARISON).click();
        urlSteps.testing().path(Pages.COMPARISON).open();

        basePageSteps.onComparisonPage().savedItemsTable().should(isDisplayed());
        String firstOfferId = basePageSteps.getOfferId(basePageSteps.onComparisonPage()
                .savedItemsTable().nameDescription().offerLinkList().get(0));
        String secondOfferId = basePageSteps.getOfferId(basePageSteps.onComparisonPage()
                .savedItemsTable().nameDescription().offerLinkList().get(1));


        apiSteps.createYandexAccount(account);
        urlSteps.open();
        basePageSteps.onComparisonPage().savedItemsTable().should(isDisplayed());
        String firstIdFromComparisonPage = basePageSteps.getOfferId(basePageSteps.onComparisonPage()
                .savedItemsTable().nameDescription().offerLinkList().get(0));
        String secondIdFromComparisonPage = basePageSteps.getOfferId(basePageSteps.onComparisonPage()
                .savedItemsTable().nameDescription().offerLinkList().get(1));

        basePageSteps.shouldEqual("Id двух офферов должны совпадать", firstIdFromComparisonPage, firstOfferId);
        basePageSteps.shouldEqual("Id двух офферов должны совпадать", secondIdFromComparisonPage, secondOfferId);
    }
}
