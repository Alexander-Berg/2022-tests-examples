package ru.yandex.arenda.admin.flat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.CompareSteps;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.arenda.constants.UriPath.FLATS;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.element.lk.admin.ManagerFlatFilters.CONFIRMED_STATUS;
import static ru.yandex.arenda.element.lk.admin.ManagerFlatFilters.DENIED_STATUS;
import static ru.yandex.arenda.element.lk.admin.ManagerFlatFilters.LOOKING_FOR_TENANT_STATUS;
import static ru.yandex.arenda.element.lk.admin.ManagerFlatFilters.RENTED_STATUS;
import static ru.yandex.arenda.element.lk.admin.ManagerFlatFilters.SEARCH_BUTTON;
import static ru.yandex.arenda.element.lk.admin.ManagerFlatFilters.WAITING_FOR_CONFIRMATION_STATUS;
import static ru.yandex.arenda.element.lk.admin.ManagerFlatFilters.WORK_IN_PROGRESS_STATUS;
import static ru.yandex.arenda.matcher.AttributeMatcher.isChecked;
import static ru.yandex.arenda.steps.MainSteps.FIRST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1728")
@DisplayName("[Админка] Листинг квартир")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class FlatFilterSearchTest {

    private static final String TEST_STREET = "Новолитовская";
    private static final String STRICT_TEST_STREET = "ул Новолитовская, д 10 стр 1";
    private static final String WRONG_TEXT = "полнаячушьвполезапроса";
    private static final String NAME_TEXT = "Кузнецов Илья Эдуардович";
    private static final String NAME_TEXT2 = "Кузнецов Илья 1";
    private static final String NAME_PARTIAL = "Илья";
    private static final String NAME_OPPOSITE = "Илья Кузнецов";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        passportSteps.adminLogin();
    }

    @Test
    @DisplayName("В фильтрах заданы все статусы квартир кроме Черновиков")
    public void shouldSeeAllStatusExceptDraft() {
        List<String> checkedStatuses = asList(WAITING_FOR_CONFIRMATION_STATUS, CONFIRMED_STATUS, WORK_IN_PROGRESS_STATUS,
                RENTED_STATUS, DENIED_STATUS, LOOKING_FOR_TENANT_STATUS);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersButton().click();
        checkedStatuses.forEach(status ->
                lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersPopup().divWithLabel(status).should(isChecked()));
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersPopup().divWithLabel("Черновик").should(not(isChecked()));
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersPopup().divWithLabel("Все").should(not(isChecked()));
    }

    @Test
    @DisplayName("В поле с поиском вводим улицу -> url поменялся.")
    public void shouldSeeStreetFilterUrl() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        lkSteps.onAdminListingPage().managerFlatFilters().queryFilter().sendKeys(TEST_STREET);
        lkSteps.onAdminListingPage().managerFlatFilters().button(SEARCH_BUTTON).click();
        urlSteps.queryParam("query", TEST_STREET).ignoreParam("status").ignoreParam("page").ignoreParam("sort")
                .ignoreParam("direction").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("В поле с поиском вводим улицу -> видим квартиры только на этой улице")
    public void shouldSeeStreetFilter() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        lkSteps.onAdminListingPage().managerFlatFilters().queryFilter().sendKeys(TEST_STREET);
        lkSteps.onAdminListingPage().managerFlatFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().managerFlatsItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        lkSteps.onAdminListingPage().managerFlatsItem().forEach(flat ->
                flat.link().should(hasText(containsString(TEST_STREET))));
    }

    @Test
    @DisplayName("В поле с поиском вводим точный адрес -> url поменялся.")
    public void shouldSeeStreetFilterStrictUrl() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        lkSteps.onAdminListingPage().managerFlatFilters().queryFilter().sendKeys(STRICT_TEST_STREET);
        lkSteps.onAdminListingPage().managerFlatFilters().button(SEARCH_BUTTON).click();
        urlSteps.queryParam("query", STRICT_TEST_STREET).ignoreParam("status");
    }

    @Test
    @DisplayName("В поле с поиском вводим адрес -> видим только одно предложение")
    public void shouldSeeStreetStrictFilter() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        lkSteps.onAdminListingPage().managerFlatFilters().queryFilter().sendKeys(STRICT_TEST_STREET);
        lkSteps.onAdminListingPage().managerFlatFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().managerFlatsItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        lkSteps.onAdminListingPage().managerFlatsItem().forEach(flat ->
                flat.link().should(hasText(containsString(STRICT_TEST_STREET))));
    }

    @Test
    @DisplayName("В поле с поиском вводим любую чушь -> пустая выдача")
    public void shouldSeeEmptyListing() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        lkSteps.onAdminListingPage().managerFlatFilters().queryFilter().sendKeys(WRONG_TEXT);
        lkSteps.onAdminListingPage().managerFlatFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().managerFlatsItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        lkSteps.onAdminListingPage().flatNotFoundListing().should(isDisplayed());
        lkSteps.onAdminListingPage().managerFlatsItem().should(hasSize(0));
    }

    @Test
    @DisplayName("В поле с поиском вводим любую чушь -> скриншот")
    public void shouldSeeEmptyScreenshot() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        lkSteps.onAdminListingPage().managerFlatFilters().queryFilter().sendKeys(WRONG_TEXT);
        lkSteps.onAdminListingPage().managerFlatFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().flatNotFoundListing().waitUntil(isDisplayed());
        Screenshot testing = compareSteps.takeScreenshot(lkSteps.onAdminListingPage().root());
        urlSteps.setProductionHost().open();
        lkSteps.onAdminListingPage().managerFlatFilters().queryFilter().sendKeys(WRONG_TEXT);
        lkSteps.onAdminListingPage().managerFlatFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().flatNotFoundListing().waitUntil(isDisplayed());
        Screenshot production = compareSteps.takeScreenshot(lkSteps.onAdminListingPage().root());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @DisplayName("Поиск по ФИО")
    public void shouldSeeByAuthor() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        lkSteps.onAdminListingPage().managerFlatFilters().queryFilter().sendKeys(NAME_TEXT);
        lkSteps.onAdminListingPage().managerFlatFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().managerFlatsItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        urlSteps.queryParam("query", NAME_TEXT).ignoreParam("status");
        lkSteps.onAdminListingPage().managerFlatsItem().forEach(flat ->
                flat.should(hasText(containsString(NAME_TEXT))));
    }

    @Test
    @DisplayName("Поиск по ФИО 2")
    public void shouldSeeByAuthor2() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        lkSteps.onAdminListingPage().managerFlatFilters().queryFilter().sendKeys(NAME_TEXT2);
        lkSteps.onAdminListingPage().managerFlatFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().managerFlatsItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        lkSteps.onAdminListingPage().managerFlatsItem().forEach(flat ->
                flat.should(hasText(containsString(NAME_TEXT2))));
    }

    @Test
    @DisplayName("Поиск по имени")
    public void shouldSeeByAuthorName() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        lkSteps.onAdminListingPage().managerFlatFilters().queryFilter().sendKeys(NAME_PARTIAL);
        lkSteps.onAdminListingPage().managerFlatFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().managerFlatsItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        lkSteps.onAdminListingPage().managerFlatsItem().forEach(flat ->
                assertThat(flat.getText()).containsIgnoringCase(NAME_PARTIAL));
    }

    @Test
    @DisplayName("Поиск по обратному имени не работает")
    public void shouldSeeByOppositeAuthorName() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        lkSteps.onAdminListingPage().managerFlatFilters().queryFilter().sendKeys(NAME_OPPOSITE);
        lkSteps.onAdminListingPage().managerFlatFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().managerFlatsItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        urlSteps.queryParam("query", NAME_OPPOSITE).ignoreParam("status");
        lkSteps.onAdminListingPage().flatNotFoundListing().should(isDisplayed());
        lkSteps.onAdminListingPage().managerFlatsItem().should(hasSize(0));
    }
}
