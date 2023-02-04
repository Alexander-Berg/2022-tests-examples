package ru.yandex.realty.archive;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.consts.Filters.KOMNATA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OTSENKA_KVARTIRY;
import static ru.yandex.realty.consts.RealtyFeatures.ARCHIVE;

@DisplayName("Страница архива")
@Feature(ARCHIVE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ArchivePageTest {

    private static final String ADDRESS = "Новочерёмушкинская улица, 50";
    private static final String PARSED_ADDRESS = "Россия, Москва, Новочерёмушкинская улица, 50";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот страницы архива")
    public void shouldSeeArchiveScreenshot() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).open();
        compareSteps.resize(375, 10000);
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onArchivePage().pageRoot());
        urlSteps.production().path(OTSENKA_KVARTIRY).open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onArchivePage().pageRoot());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим архив офферов дома")
    public void shouldSeeArchiveOffers() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).open();
        basePageSteps.onArchivePage().address().click();
        basePageSteps.onArchivePage().address().sendKeys(ADDRESS);
        basePageSteps.onArchivePage().suggestListItems().waitUntil(hasSize(greaterThan(0))).get(0).click();
        basePageSteps.onArchivePage().archiveOffers().waitUntil(hasSize(greaterThan(0)));
        urlSteps.path(PARSED_ADDRESS).path(KUPIT).path(KVARTIRA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Жмем кнопку «Аренда»")
    public void shouldSeeArchiveOffersRent() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(PARSED_ADDRESS).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onArchivePage().button("Аренда").click();
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(PARSED_ADDRESS).path(SNYAT).path(KVARTIRA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Жмем кнопку «Продажа»")
    public void shouldSeeArchiveOffersSell() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(PARSED_ADDRESS).path(SNYAT).path(KVARTIRA).open();
        basePageSteps.onArchivePage().button("Продажа").click();
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(PARSED_ADDRESS).path(KUPIT).path(KVARTIRA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем колличество комнат")
    public void shouldSeeArchiveOffersByRooms() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(PARSED_ADDRESS).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onArchivePage().roomsCollapser().click();
        basePageSteps.onArchivePage().roomsSelector().click();
        basePageSteps.onArchivePage().option("3 комнаты").click();
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(PARSED_ADDRESS).path(KUPIT).path(KVARTIRA)
                .queryParam("roomsTotal", "3").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Жмем кнопку «Комната»")
    public void shouldSeeArchiveOffersRentRoom() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(PARSED_ADDRESS).path(SNYAT).path(KVARTIRA).open();
        basePageSteps.onArchivePage().roomsCollapser().click();
        basePageSteps.onArchivePage().button("Комната").click();
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(PARSED_ADDRESS).path(SNYAT).path(KOMNATA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Жмем кнопку «Квартира»")
    public void shouldSeeArchiveOffersRentFlat() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(PARSED_ADDRESS).path(SNYAT).path(KOMNATA).open();
        basePageSteps.onArchivePage().roomsCollapser().click();
        basePageSteps.onArchivePage().button("Квартира").click();
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(PARSED_ADDRESS).path(SNYAT).path(KVARTIRA)
                .shouldNotDiffWithWebDriverUrl();
    }
}
