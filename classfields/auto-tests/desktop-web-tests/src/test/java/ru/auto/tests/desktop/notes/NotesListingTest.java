package ru.auto.tests.desktop.notes;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.NOTES;
import static ru.auto.tests.desktop.consts.Owners.KOPITSA;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.step.CookieSteps.COOKIE_NAME_NOTE_FAV_INFO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("Операции с заметками на странице выдачи объявлений")
@Feature(NOTES)
@Story("Страница выдачи")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class NotesListingTest {

    private final static String CARD_DROPDOWN_DESC_NOTE = "Добавить заметку к объявлению";
    private final static String CARD_DROPDOWN_DESC_NOTE_FAVORITE_TIP =
            "Объявления с заметками добавляются в Избранное автоматически, чтобы вы могли легко их найти";
    private final static String NOTE_TEXT = "Note text";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    //@Parameter("Категория ТС")
    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String searchMock;

    @Parameterized.Parameter(2)
    public String breadcrumbsMock;

    @Parameterized.Parameter(3)
    public String notesPutMock;

    @Parameterized.Parameter(4)
    public String notesDeleteMock;

    @Parameterized.Parameter(5)
    public String favoritesPostMock;

    @Parameterized.Parameter(6)
    public int favoriteIndex;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/SearchCarsAll", "desktop/SearchCarsBreadcrumbsEmpty", "desktop/UserNotesCarsPut",
                        "desktop/UserNotesCarsDelete", "desktop/UserFavoritesCarsPost", 0},
                {TRUCK, "desktop/SearchTrucksAll", "desktop/SearchTrucksBreadcrumbs", "desktop/UserNotesTrucksPut",
                        "desktop/UserFavoritesTrucksPost", "desktop/UserNotesTrucksDelete", 1},
                {MOTORCYCLE, "desktop/SearchMotoAll", "desktop/SearchMotoBreadcrumbs", "desktop/UserNotesMotoPut",
                        "desktop/UserNotesMotoDelete", "desktop/UserFavoritesMotoPost", 2},

        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/UserFavoritesAllWithNote",
                searchMock,
                breadcrumbsMock,
                notesPutMock,
                notesDeleteMock,
                favoritesPostMock).post();

        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Добавляем заметку")
    public void shouldAddNote() {
        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.onListingPage().getSale(0).toolBar().noteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().getSale(0).noteBar().input().waitUntil(isDisplayed())
                .sendKeys(NOTE_TEXT);
        basePageSteps.onListingPage().getSale(0).noteBar().saveButton().click();
        basePageSteps.onListingPage().getSale(0).noteBar().note().should(hasText(NOTE_TEXT));
        basePageSteps.onListingPage().getSale(0).toolBar().favoriteDeleteButton().waitUntil(isDisplayed());
        basePageSteps.onListingPage().header().favoritesButton().click();
        basePageSteps.onListingPage().favoritesPopup().waitUntil(isDisplayed());
        basePageSteps.onListingPage().favoritesPopup().getFavorite(favoriteIndex).note().should(hasText(NOTE_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Показываем подсказку о добавлении в избранное после добавления заметки")
    public void shouldSeeAddFavoriteHint() {
        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.onListingPage().getSale(0).toolBar().noteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().getSale(0).noteBar().input().waitUntil(isDisplayed())
                .sendKeys(NOTE_TEXT);
        basePageSteps.onListingPage().getSale(0).noteBar().saveButton().click();
        basePageSteps.onBasePage().activePopup()
                .should(isDisplayed())
                .should(hasText(CARD_DROPDOWN_DESC_NOTE_FAVORITE_TIP));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Удаляем заметку")
    public void shouldDeleteNotesFromHeaderWithoutLogin() {
        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.onListingPage().getSale(0).toolBar().noteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().getSale(0).noteBar().input().waitUntil(isDisplayed())
                .sendKeys(NOTE_TEXT);
        basePageSteps.onListingPage().getSale(0).noteBar().saveButton().click();
        basePageSteps.onListingPage().getSale(0).noteBar().hover();
        basePageSteps.onListingPage().getSale(0).noteBar().deleteButton().should(isDisplayed()).click();
        basePageSteps.onListingPage().getSale(0).noteBar().should(not(isDisplayed()));
        basePageSteps.refresh();
        basePageSteps.onListingPage().getSale(0).noteBar().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Создаём куки после добавления заметки")
    public void shouldCreateNoteCookie() {
        cookieSteps.shouldNotSeeCookie(COOKIE_NAME_NOTE_FAV_INFO);
        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.onListingPage().getSale(0).toolBar().noteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().getSale(0).noteBar().input().waitUntil(isDisplayed()).sendKeys(NOTE_TEXT);
        basePageSteps.onListingPage().getSale(0).noteBar().saveButton().click();
        cookieSteps.shouldSeeCookieWithValue(COOKIE_NAME_NOTE_FAV_INFO, "1");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Показываем хинт на кнопке заметок")
    public void shouldSeeAddNoteButtonHint() {
        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.onListingPage().getSale(0).toolBar().noteButton().should(isDisplayed()).hover();
        basePageSteps.onBasePage().activePopup().should(isDisplayed())
                .should(hasText(CARD_DROPDOWN_DESC_NOTE));
    }
}
