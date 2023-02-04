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
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.element.offercard.MorePopup;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.VTORICHNIY_RYNOK;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.COMPARISON;
import static ru.yandex.realty.element.saleads.ActionBar.ADD_TO_COMPARISON;
import static ru.yandex.realty.step.CommonSteps.FIRST;

/**
 * Created by kopitsa on 29.06.17.
 */
@DisplayName("Работы страницы сравнения")
@Feature(COMPARISON)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ComparisonTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path(VTORICHNIY_RYNOK).open();
    }

    @Test
    @Owner(KOPITSA)
    @DisplayName("Добавление двух предложений на сравнение")
    @Description("Выбираем одно объявление из списка и добавляем его, второе из списка по клику на его карточку." +
            " Переходим на страницу сравнения и проверяем, что они появились." +
            " Одно предложение из списка, второе из карточки предложения.")
    public void shouldAddItemsToComparisonFromCard() {
        basePageSteps.onOffersSearchPage().offersList().waitUntil(hasSize(greaterThanOrEqualTo(1)));
        String firstOfferId = basePageSteps.getOfferId(basePageSteps.onOffersSearchPage().offersList().get(0).offerLink());
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offersList().get(0).actionBar());
        basePageSteps.onOffersSearchPage().offer(FIRST).actionBar().buttonWithTitle(ADD_TO_COMPARISON).click();

        String secondOfferId = basePageSteps.getOfferId(basePageSteps.onOffersSearchPage().offersList().get(1).offerLink());
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offersList().get(1));
        basePageSteps.onOffersSearchPage().offersList().get(1).offerLink().click();
        basePageSteps.switchToTab(1);
        basePageSteps.onOfferCardPage().offerCardSummary().moreButton().click();
        basePageSteps.moveCursorAndClick(
                basePageSteps.onOfferCardPage().topMorePopup().actionRow(MorePopup.ADD_TO_COMPARISON));
        urlSteps.testing().path(Pages.COMPARISON).open();

        basePageSteps.onComparisonPage().savedItemsTable().should(isDisplayed());

        List<String> listOfOfferIdsFromComparisonPage = basePageSteps.onComparisonPage()
                .savedItemsTable().nameDescription().offerLinkList()
                .stream().map(offerLink -> basePageSteps.getOfferId(offerLink))
                .collect(toList());

        basePageSteps.shouldMatchLists(asList(firstOfferId, secondOfferId), listOfOfferIdsFromComparisonPage);
    }

    @Test
    @Owner(KOPITSA)
    @DisplayName("Проверка кнопки показа контактов на странице сравнения")
    @Description("Добавляем оффер, кликаем на показ контактов и проверяем их наличие.")
    public void shouldComparisonPageShowContactsButton() {
        basePageSteps.onOffersSearchPage().offersList().waitUntil(hasSize(greaterThanOrEqualTo(1)));
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offersList().get(0));
        basePageSteps.onOffersSearchPage().offer(FIRST).actionBar().buttonWithTitle(ADD_TO_COMPARISON).click();
        urlSteps.testing().path(Pages.COMPARISON).open();

        basePageSteps.onComparisonPage().savedItemsTable().should(isDisplayed());
        basePageSteps.onComparisonPage().savedItemsTable().contactsList().get(0).showContactsButton().should(isDisplayed());
        basePageSteps.onComparisonPage().savedItemsTable().contactsList().get(0).showContactsButton().click();
        basePageSteps.onComparisonPage().savedItemsTable().contactsList().get(0).shownContacts().should(isDisplayed());
    }

    @Test
    @Owner(KOPITSA)
    @DisplayName("Проверка открытия оффера со страницы сравнения")
    @Description("Добавляем оффер и сохраняем его айди. Переходим на страницу сравнения, кликаем по картинке оффера." +
            " Проверяем, что открылся оффер с нужным айди.")
    public void shouldOpenOfferFromComparisonPage() {
        basePageSteps.onOffersSearchPage().offersList().waitUntil(hasSize(greaterThanOrEqualTo(1)));
        String offerId = basePageSteps.getOfferId(basePageSteps.onOffersSearchPage().offersList().get(0).offerLink());
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offersList().get(0));
        basePageSteps.onOffersSearchPage().offer(FIRST).actionBar().buttonWithTitle(ADD_TO_COMPARISON).click();
        urlSteps.testing().path(Pages.COMPARISON).open();

        basePageSteps.onComparisonPage().savedItemsTable().nameDescription().offerLinkList().get(0).click();

        basePageSteps.switchToTab(1);
        urlSteps.testing().path(OFFER).path(offerId).path("/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KOPITSA)
    @DisplayName("Удаление офферов со страницы сравнения -> видим зашлушку и кнопку «Начать новый поиск»")
    public void shouldAddItemsToComparisonAndDeleteThem() {
        basePageSteps.onOffersSearchPage().offersList().waitUntil(hasSize(greaterThanOrEqualTo(1)));
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offersList().get(0));
        basePageSteps.onOffersSearchPage().offer(FIRST).actionBar().buttonWithTitle(ADD_TO_COMPARISON).click();
        urlSteps.testing().path(Pages.COMPARISON).open();

        basePageSteps.onComparisonPage().savedItemsTable().nameDescription().should(isDisplayed());
        basePageSteps.onComparisonPage().savedItemsTable().nameDescription().offerDeleteButton().get(0).click();

        basePageSteps.onComparisonPage().emptyTableMarkingMessage().link("Начать новый поиск").should(isDisplayed());
    }

    @Test
    @Owner(KOPITSA)
    @DisplayName("Проверка кнопки изменения типа недвижимости")
    public void shouldAddDifferentTypesOfRealty() {
        basePageSteps.onOffersSearchPage().offersList().waitUntil(hasSize(greaterThanOrEqualTo(1)));
        String firstOfferId = basePageSteps.getOfferId(basePageSteps.onOffersSearchPage().offersList().get(0).offerLink());
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offersList().get(0));
        basePageSteps.onOffersSearchPage().offer(FIRST).actionBar().buttonWithTitle(ADD_TO_COMPARISON).click();

        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(DOM).open();

        String secondOfferId = basePageSteps.getOfferId(basePageSteps.onOffersSearchPage().offersList().get(0).offerLink());
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offersList().get(0));
        basePageSteps.onOffersSearchPage().offer(FIRST).actionBar().buttonWithTitle(ADD_TO_COMPARISON).click();

        urlSteps.testing().path(Pages.COMPARISON).open();

        basePageSteps.onComparisonPage().savedItemsTable().should(isDisplayed());
        String firstIdFromComparisonPage = basePageSteps.getOfferId(basePageSteps.onComparisonPage()
                .savedItemsTable().nameDescription().offerLinkList().get(0));
        urlSteps.testing().path(Pages.COMPARISON).queryParam("type", "SELL")
                .queryParam("category", "HOUSE").open();
        basePageSteps.onComparisonPage().savedItemsTable().nameDescription().offerLinkList().get(0).should(isDisplayed());
        String secondIdFromComparisonPage = basePageSteps.getOfferId(basePageSteps.onComparisonPage()
                .savedItemsTable().nameDescription().offerLinkList().get(0));

        basePageSteps.shouldEqual("Id двух офферов должны совпадать", firstIdFromComparisonPage,
                firstOfferId);
        basePageSteps.shouldEqual("Id двух офферов должны совпадать", secondIdFromComparisonPage,
                secondOfferId);
    }
}
