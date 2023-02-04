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
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.UrlSteps;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.arenda.constants.UriPath.FLATS;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.element.lk.admin.ManagerFlatFilters.CONFIRMED_STATUS;
import static ru.yandex.arenda.element.lk.admin.ManagerFlatFilters.DENIED_STATUS;
import static ru.yandex.arenda.element.lk.admin.ManagerFlatFilters.DRAFT;
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
public class FlatFilterStatusTest {


    private static final String STATUS = "status";
    private static final String QUERY = "query";
    private static final String PAGE = "page";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

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
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersPopup().divWithLabel(DRAFT).should(not(isChecked()));
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersPopup().divWithLabel("Все").should(not(isChecked()));
    }

    @Test
    @DisplayName("При переходе по урлу видим статус")
    public void shouldSeeStatusByUrl() {
        List<String> checkedStatuses = asList(WAITING_FOR_CONFIRMATION_STATUS, CONFIRMED_STATUS,
                WORK_IN_PROGRESS_STATUS, DRAFT, DENIED_STATUS, LOOKING_FOR_TENANT_STATUS);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).queryParam(STATUS, "RENTED").open();
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersButton().click();
        checkedStatuses.forEach(status ->
                lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersPopup().divWithLabel(status)
                        .should(not(isChecked())));
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersPopup().divWithLabel(RENTED_STATUS).should(isChecked());
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersPopup().divWithLabel("Все").should(not(isChecked()));
    }

    @Test
    @DisplayName("Выбираем один статус -> все офферы в данном статусе")
    public void shouldSeeStatus() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).queryParam(STATUS, "RENTED").open();
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersButton().click();
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersPopup().divWithLabel(RENTED_STATUS).click();
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersPopup().divWithLabel(CONFIRMED_STATUS).click();
        lkSteps.onAdminListingPage().managerFlatFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().managerFlatsItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).queryParam(STATUS, "CONFIRMED").ignoreParam("sort")
                .ignoreParam("direction").ignoreParam(QUERY).ignoreParam(PAGE).shouldNotDiffWithWebDriverUrl();
        lkSteps.onAdminListingPage().managerFlatsItem().forEach(flat ->
                flat.should(hasText(containsString(CONFIRMED_STATUS))));

    }

    @Test
    @DisplayName("Выбираем два статуса -> все офферы в данном статусе")
    public void shouldSeeTwoStatuses() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).queryParam(STATUS, "DRAFT").open();
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersButton().click();
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersPopup().divWithLabel(DRAFT).click();
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersPopup().divWithLabel(WORK_IN_PROGRESS_STATUS).click();
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersPopup().divWithLabel(DENIED_STATUS).click();
        lkSteps.onAdminListingPage().managerFlatFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().managerFlatsItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).queryParam(STATUS, "WORK_IN_PROGRESS")
                .queryParam(STATUS, "DENIED").ignoreParam(QUERY).ignoreParam(PAGE).ignoreParam("sort")
                .ignoreParam("direction").shouldNotDiffWithWebDriverUrl();
        lkSteps.onAdminListingPage().managerFlatsItem().forEach(flat ->
                flat.should(anyOf(hasText(containsString(WORK_IN_PROGRESS_STATUS)),
                        hasText(containsString(DENIED_STATUS)))));
    }

    @Test
    @DisplayName("Выбираем «Все» статусы -> видим в урле")
    public void shouldSeeAllStatuses() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersButton().click();
        lkSteps.onAdminListingPage().managerFlatFilters().flatFiltersPopup().divWithLabel("Все").click();
        lkSteps.onAdminListingPage().managerFlatFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().managerFlatsItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).queryParam(STATUS, "DRAFT",
                "WAITING_FOR_CONFIRMATION", "CONFIRMED", "WORK_IN_PROGRESS", "RENTED", "DENIED", "LOOKING_FOR_TENANT",
                "CANCELLED_WITHOUT_SIGNING", "AFTER_RENT")
                .queryParam(PAGE, "1").queryParam(QUERY, "").ignoreParam("sort").ignoreParam("direction")
                .shouldNotDiffWithWebDriverUrl();
    }
}
