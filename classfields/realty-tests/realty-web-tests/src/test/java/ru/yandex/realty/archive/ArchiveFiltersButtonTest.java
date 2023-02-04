package ru.yandex.realty.archive;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.consts.Filters;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OTSENKA_KVARTIRY;

/**
 * @author kantemirov
 */
@DisplayName("Страница архива. Проверка кнопок фильтров")
@Feature(RealtyFeatures.ARCHIVE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ArchiveFiltersButtonTest {

    private static final String ADDRESS = "Россия, Санкт-Петербург, Суворовский проспект, 47";
    private static final String KOMNATA = "Комната";
    private static final String KVARTIRA = "Квартира";
    private static final String PRODAZHA = "Продажа";
    private static final String ARENDA = "Аренда";
    private static final String KOLVO_KOMNAT = "Кол-во комнат";
    private static final String DVE_KOMNATY = "2 комнаты";

    private List<String> offersInfo = newArrayList();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем «количество комнат», видим в урле")
    public void shouldSeeRoomCountInUrl() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(ADDRESS).path(KUPIT).path(Filters.KVARTIRA).open();
        basePageSteps.onArchivePage().filterButton(KOLVO_KOMNAT).waitUntil(isDisplayed()).click();
        basePageSteps.onArchivePage().selectorPopup().option(DVE_KOMNATY).waitUntil(isDisplayed()).click();
        basePageSteps.onArchivePage().paranja().waitUntil(not(isDisplayed()));
        urlSteps.queryParam("roomsTotal", "2").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем количество комнат, видим в результатах только 2-ух комнатные")
    public void shouldSeeOnly2RoomOffers() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(ADDRESS).path(KUPIT).path(Filters.KVARTIRA).open();
        basePageSteps.onArchivePage().filterButton(KOLVO_KOMNAT).waitUntil(isDisplayed()).click();
        basePageSteps.onArchivePage().selectorPopup().option(DVE_KOMNATY).waitUntil(isDisplayed()).click();
        basePageSteps.onArchivePage().paranja().waitUntil(not(isDisplayed()));
        basePageSteps.onArchivePage().searchResultBlock().archiveOffers().waitUntil(hasSize(greaterThan(0)))
                .forEach(offer -> offersInfo.add(offer.mainOfferInfo().getText()));
        assertThat(offersInfo).allSatisfy(info -> assertThat(info).contains("2-комнатная"));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем «Комната», видим в урле")
    public void shouldSeeRoomInUrl() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(ADDRESS).path(KUPIT).path(Filters.KVARTIRA).open();
        basePageSteps.onArchivePage().filterButton(KOMNATA).waitUntil(isDisplayed()).click();
        basePageSteps.onArchivePage().paranja().waitUntil(not(isDisplayed()));
        urlSteps.testing()
                .path(OTSENKA_KVARTIRY).path(ADDRESS).path(KUPIT).path(Filters.KOMNATA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем «Комната», видим в результатах только комнаты")
    public void shouldSeeOnlyRoomOffers() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(ADDRESS).path(KUPIT).path(Filters.KVARTIRA).open();
        basePageSteps.onArchivePage().filterButton(KOMNATA).waitUntil(isDisplayed()).click();
        basePageSteps.onArchivePage().paranja().waitUntil(not(isDisplayed()));
        basePageSteps.onArchivePage().searchResultBlock().archiveOffers().waitUntil(hasSize(greaterThan(0)))
                .forEach(offerInfo -> offersInfo.add(offerInfo.mainOfferInfo().getText()));
        assertThat(offersInfo).allSatisfy(info -> assertThat(info).contains("комната"));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем «Квартира», видим в урле")
    public void shouldSeeFlatInUrl() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(ADDRESS).path(KUPIT).path(Filters.KOMNATA).open();
        basePageSteps.onArchivePage().filterButton(KVARTIRA).waitUntil(isDisplayed()).click();
        basePageSteps.onArchivePage().paranja().waitUntil(not(isDisplayed()));
        urlSteps.testing()
                .path(OTSENKA_KVARTIRY).path(ADDRESS).path(KUPIT).path(Filters.KVARTIRA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем «Квартира», не видим в результатах комнаты")
    public void shouldSeeOnlyFlatOffers() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(ADDRESS).path(KUPIT).path(Filters.KOMNATA).open();
        basePageSteps.onArchivePage().filterButton(KVARTIRA).waitUntil(isDisplayed()).click();
        basePageSteps.onArchivePage().paranja().waitUntil(not(isDisplayed()));
        basePageSteps.onArchivePage().searchResultBlock().archiveOffers().waitUntil(hasSize(greaterThan(0)))
                .forEach(archiveOffer -> offersInfo.add(archiveOffer.mainOfferInfo().getText()));
        assertThat(offersInfo).allSatisfy(info -> assertThat(info).doesNotEndWith("комната"));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем «Продажа», видим в урле")
    public void shouldSeeBuyInUrl() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(ADDRESS).path(SNYAT).path(Filters.KVARTIRA).open();
        basePageSteps.onArchivePage().filterButton(PRODAZHA).waitUntil(isDisplayed()).click();
        basePageSteps.onArchivePage().paranja().waitUntil(not(isDisplayed()));
        urlSteps.testing()
                .path(OTSENKA_KVARTIRY).path(ADDRESS).path(KUPIT).path(Filters.KVARTIRA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем «Продажа», не видим в результатах цены «за месяц»")
    public void shouldNotSeePricePerMonth() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(ADDRESS).path(SNYAT).path(Filters.KVARTIRA).open();
        basePageSteps.onArchivePage().filterButton(PRODAZHA).waitUntil(isDisplayed()).click();
        basePageSteps.onArchivePage().paranja().waitUntil(not(isDisplayed()));
        basePageSteps.onArchivePage().searchResultBlock().archiveOffers().waitUntil(hasSize(greaterThan(0)))
                .forEach(archiveOffer -> offersInfo.add(archiveOffer.priceOfferInfo().getText()));
        assertThat(offersInfo).allSatisfy(info -> assertThat(info).doesNotContain("в месяц"));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем «Аренда», видим в урле")
    public void shouldSeeRentInUrl() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(ADDRESS).path(KUPIT).path(Filters.KVARTIRA).open();
        basePageSteps.onArchivePage().filterButton(ARENDA).waitUntil(isDisplayed()).click();
        basePageSteps.onArchivePage().paranja().waitUntil(not(isDisplayed()));
        urlSteps.testing()
                .path(OTSENKA_KVARTIRY).path(ADDRESS).path(SNYAT).path(Filters.KVARTIRA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем «Аренда», видим в результатах все цены «за месяц»")
    public void shouldSeePricePerMonth() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(ADDRESS).path(KUPIT).path(Filters.KVARTIRA).open();
        basePageSteps.onArchivePage().filterButton(ARENDA).waitUntil(isDisplayed()).click();
        basePageSteps.onArchivePage().paranja().waitUntil(not(isDisplayed()));
        basePageSteps.onArchivePage().searchResultBlock().archiveOffers().waitUntil(hasSize(greaterThan(0)))
                .forEach(archiveOffer -> offersInfo.add(archiveOffer.priceOfferInfo().getText()));
        assertThat(offersInfo).allSatisfy(info -> assertThat(info).contains("/ мес."));
    }
}
