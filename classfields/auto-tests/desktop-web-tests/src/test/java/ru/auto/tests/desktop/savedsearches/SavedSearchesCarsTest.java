package ru.auto.tests.desktop.savedsearches;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SAVE_SEARCHES;
import static ru.auto.tests.desktop.consts.Notifications.SEARCH_SAVED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.KOPITSA;
import static ru.auto.tests.desktop.element.listing.StickySaveSearchPanel.SAVED;
import static ru.auto.tests.desktop.element.listing.StickySaveSearchPanel.SAVE_SEARCH;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг объявлений легковых - сохраненные поиски")
@Feature(SAVE_SEARCHES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SavedSearchesCarsTest {

    private static final int PRICE_FROM = 5;
    private static final int PRICE_TO = 100005;
    private static final int PXLS_TO_STICKY_SAVE_SEARCH = 700;
    private static final String POPUP_TEXT = "Audi A5 I (8T) Рестайлинг\nС пробегом, 2005 — 2015, 1 — 200 000 км, " +
            "5 — 100 005 ₽, Седан, Робот, Передний, Бензин, 0.2 — 10.0 л, Серебристый, " +
            "1 владелец + 39\nЭлектронная почта\nПолучать на почту\nУдалить";
    private static final String FULL_SEARCH_URL = "%s/moskva/cars/audi/a5/7721546/used/engine-benzin/" +
            "?body_type_group=SEDAN&price_from=5&transmission=ROBOT&gear_type=FORWARD_CONTROL&displacement_from=200&" +
            "displacement_to=10000&year_from=2005&year_to=2015&owning_time_group=LESS_THAN_YEAR&" +
            "steering_wheel=LEFT&catalog_equipment=airbag-driver&catalog_equipment=condition&" +
            "catalog_equipment=wheel-configuration1&catalog_equipment=leather&catalog_equipment=electro-window-front&" +
            "catalog_equipment=driver-seat-updown&catalog_equipment=passenger-seat-updown&" +
            "catalog_equipment=light-interior&catalog_equipment=abs&catalog_equipment=esp&" +
            "catalog_equipment=asr&catalog_equipment=cruise-control&catalog_equipment=wheel-power&" +
            "catalog_equipment=computer&catalog_equipment=park-assist-r&catalog_equipment=tinted-glass&" +
            "catalog_equipment=navigation&catalog_equipment=rain-sensor&catalog_equipment=light-sensor&" +
            "catalog_equipment=xenon&catalog_equipment=mirrors-heat&catalog_equipment=light-cleaner&" +
            "catalog_equipment=electro-mirrors&catalog_equipment=alarm&catalog_equipment=lock&" +
            "catalog_equipment=wheel-heat&catalog_equipment=front-seats-heat&catalog_equipment=hatch&" +
            "catalog_equipment=seats-2&catalog_equipment=armored&km_age_from=1&km_age_to=200000&power_from=1&" +
            "power_to=500&acceleration_from=1&acceleration_to=50&seller_group=PRIVATE&owners_count_group=ONE&" +
            "pts_status=1&with_warranty=true&exchange_group=POSSIBLE&has_video=true&geo_radius=200&" +
            "color=CACECB&price_to=100005";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.fromUri(format(FULL_SEARCH_URL, urlSteps.getConfig().getTestingURI(), PRICE_FROM, PRICE_TO)).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(KOPITSA)
    @DisplayName("Текст поп-апа сохраненного поиска")
    public void shouldSeeSavedSearchPopup() {
        saveSearch();

        basePageSteps.onListingPage().activePopup().waitUntil(isDisplayed()).should(hasText(POPUP_TEXT));
    }

    @Test
    @Category({Regression.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст поп-апа сохраненного поиска, при сохранении из прилипшей плашки")
    public void shouldSeeSavedSearchPopupFromFloat() {
        basePageSteps.scrollDown(PXLS_TO_STICKY_SAVE_SEARCH);
        basePageSteps.onListingPage().stickySaveSearchPanel().saveButton().waitUntil(hasText(SAVE_SEARCH)).click();

        basePageSteps.onListingPage().notifier(SEARCH_SAVED).should(isDisplayed());
        basePageSteps.onListingPage().stickySaveSearchPanel().saveButton().should(hasText(SAVED));
        basePageSteps.onListingPage().activePopup().waitUntil(isDisplayed()).should(hasText(POPUP_TEXT));
    }

    @Test
    @Category({Regression.class})
    @Owner(KOPITSA)
    @DisplayName("Клик по сохраненному поиску")
    public void shouldClickSavedSearch() {
        saveSearch();
        basePageSteps.onListingPage().activePopupLink().waitUntil(isDisplayed()).click();

        basePageSteps.onListingPage().activePopup().should(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
    }

    @Step("Сохраняем поиск")
    private void saveSearch() {
        basePageSteps.onListingPage().filter().saveSearchButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().notifier(SEARCH_SAVED).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().saveSearchButton().waitUntil(isDisplayed()).waitUntil(hasText(SAVED));
    }

}
