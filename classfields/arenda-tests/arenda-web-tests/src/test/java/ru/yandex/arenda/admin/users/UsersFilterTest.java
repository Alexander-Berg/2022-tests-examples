package ru.yandex.arenda.admin.users;

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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.constants.UriPath.USERS;
import static ru.yandex.arenda.element.lk.admin.ManagerFlatFilters.SEARCH_BUTTON;
import static ru.yandex.arenda.steps.MainSteps.FIRST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1729")
@DisplayName("[Админка] Листинг пользователей")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class UsersFilterTest {

    private static final String WRONG_TEXT = "полнаячушьвполезапроса";
    private static final String NAME_TEXT = "Николаев Владимир Петрович";
    private static final String NAME_PARTIAL = "Владимир";
    private static final String NAME_OPPOSITE = "Владимир Петрович Николаев";

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
    @DisplayName("Поиск по ФИО")
    public void shouldSeeUserByAuthor() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USERS).open();
        lkSteps.onAdminListingPage().managerUserFilters().queryFilter().sendKeys(NAME_TEXT);
        lkSteps.onAdminListingPage().managerUserFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().managerUsersItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        urlSteps.queryParam("query", NAME_TEXT).ignoreParam("page");
        lkSteps.onAdminListingPage().managerUsersItem().forEach(flat ->
                flat.should(hasText(containsString(NAME_TEXT))));
    }

    @Test
    @DisplayName("Поиск по обратному имени не работает")
    public void shouldNotSeeByOppositeAuthorName() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USERS).open();
        lkSteps.onAdminListingPage().managerUserFilters().queryFilter().sendKeys(NAME_OPPOSITE);
        lkSteps.onAdminListingPage().managerUserFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().managerUsersItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        urlSteps.queryParam("query", NAME_OPPOSITE).ignoreParam("page");
        lkSteps.onAdminListingPage().userNotFoundListing().should(isDisplayed());
        lkSteps.onAdminListingPage().managerUsersItem().should(hasSize(0));
    }

    @Test
    @DisplayName("В поле с поиском вводим любую чушь -> скриншот")
    public void shouldSeeUserEmptyScreenshot() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USERS).open();
        lkSteps.onAdminListingPage().managerUserFilters().queryFilter().sendKeys(WRONG_TEXT);
        lkSteps.onAdminListingPage().managerUserFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().userNotFoundListing().waitUntil(isDisplayed());
        Screenshot testing = compareSteps.takeScreenshot(lkSteps.onAdminListingPage().root());
        urlSteps.setProductionHost().open();
        lkSteps.onAdminListingPage().managerUserFilters().queryFilter().sendKeys(WRONG_TEXT);
        lkSteps.onAdminListingPage().managerUserFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().userNotFoundListing().waitUntil(isDisplayed());
        Screenshot production = compareSteps.takeScreenshot(lkSteps.onAdminListingPage().root());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @DisplayName("Поиск по имени")
    public void shouldSeeUserByAuthorOnlyName() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USERS).open();
        lkSteps.onAdminListingPage().managerUserFilters().queryFilter().sendKeys(NAME_PARTIAL);
        lkSteps.onAdminListingPage().managerUserFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().managerUsersItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        urlSteps.queryParam("query", NAME_PARTIAL).ignoreParam("page");
        lkSteps.onAdminListingPage().managerUsersItem().forEach(flat ->
                flat.should(hasText(containsString(NAME_PARTIAL))));
    }
}
