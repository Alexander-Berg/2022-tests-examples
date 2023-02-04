package ru.auto.tests.desktop.notes;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.NOTES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.step.CookieSteps.COOKIE_NAME_NOTE_FAV_INFO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Заметки на карточке объявления")
@Feature(NOTES)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class NotesSaleAddTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private final static String CARD_DROPDOWN_DESC_NOTE = "Добавить заметку к объявлению";
    private final static String CARD_DROPDOWN_DESC_NOTE_FAVORITE_TIP = "Объявления с заметками добавляются в Избранное " +
            "автоматически, чтобы вы могли легко их найти";
    private static final String NOTE_TEXT = "Note text";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String saleMock;

    @Parameterized.Parameter(2)
    public String notePutMock;

    @Parameterized.Parameter(3)
    public String favoritePostMock;

    @Parameterized.Parameter(4)
    public String saleWithNoteMock;

    @Parameterized.Parameter(5)
    public int favoriteIndex;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedUser", "desktop/UserNotesCarsPut",
                        "desktop/UserFavoritesCarsPost", "desktop/OfferCarsUsedUserWithNote", 0},
                {TRUCK, "desktop/OfferTrucksUsedUser", "desktop/UserNotesTrucksPut",
                        "desktop/UserFavoritesTrucksPost", "desktop/OfferTrucksUsedUserWithNote", 1},
                {MOTORCYCLE, "desktop/OfferMotoUsedUser", "desktop/UserNotesMotoPut",
                        "desktop/UserFavoritesMotoPost", "desktop/OfferMotoUsedUserWithNote", 2}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                saleMock,
                notePutMock,
                favoritePostMock,
                "desktop/UserFavoritesAllWithNote").post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();

        mockRule.overwriteStub(1, saleWithNoteMock);
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Добавляем заметку")
    @Category({Regression.class, Testing.class})
    public void shouldAddNotesFromHeaderWithoutLogin() {
        basePageSteps.onCardPage().cardHeader().toolBar().noteButton().click();
        basePageSteps.onCardPage().cardHeader().noteBar().input().sendKeys(NOTE_TEXT);
        basePageSteps.onCardPage().cardHeader().noteBar().saveButton().click();
        basePageSteps.onCardPage().cardHeader().noteBar().note().should(hasText(NOTE_TEXT));
        basePageSteps.refresh();
        basePageSteps.onCardPage().cardHeader().noteBar().note().should(hasText(NOTE_TEXT));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Показываем подсказку о добавлении в избранное после добавления заметки")
    @Category({Regression.class, Testing.class})
    public void shouldSeeAddFavoriteHint() {
        basePageSteps.onCardPage().cardHeader().toolBar().noteButton().click();
        basePageSteps.onCardPage().cardHeader().noteBar().input().sendKeys(NOTE_TEXT);
        basePageSteps.onCardPage().cardHeader().noteBar().saveButton().click();
        basePageSteps.onBasePage().activePopup().waitUntil(isDisplayed())
                .waitUntil(hasText(CARD_DROPDOWN_DESC_NOTE_FAVORITE_TIP));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Создаём куки после добавления заметки")
    @Category({Regression.class, Testing.class})
    public void shouldCreateNoteCookie() {
        cookieSteps.shouldNotSeeCookie(COOKIE_NAME_NOTE_FAV_INFO);
        basePageSteps.onCardPage().cardHeader().toolBar().noteButton().click();
        basePageSteps.onCardPage().cardHeader().noteBar().input().sendKeys(NOTE_TEXT);
        basePageSteps.onCardPage().cardHeader().noteBar().saveButton().click();
        cookieSteps.shouldSeeCookieWithValue(COOKIE_NAME_NOTE_FAV_INFO, "1");
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Показываем хинт на кнопке заметок")
    @Category({Regression.class, Testing.class})
    public void shouldSeeAddNoteButtonHint() {
        basePageSteps.onCardPage().cardHeader().toolBar().noteButton().should(isDisplayed()).hover();
        basePageSteps.onBasePage().activePopup().should(isDisplayed()).should(hasText(CARD_DROPDOWN_DESC_NOTE));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны добавить в избранное после добавления заметки")
    public void shouldAddToFavoriteAfterNote() {
        String saleUrl = urlSteps.fromUri(urlSteps.getCurrentUrl()).toString();
        basePageSteps.onCardPage().cardHeader().toolBar().noteButton().click();
        basePageSteps.onCardPage().cardHeader().noteBar().input().sendKeys(NOTE_TEXT);
        basePageSteps.onCardPage().cardHeader().noteBar().saveButton().click();
        basePageSteps.onCardPage().cardHeader().noteBar().note().should(hasText(NOTE_TEXT));
        basePageSteps.onCardPage().cardHeader().toolBar().favoriteDeleteButton().should(isDisplayed());
        basePageSteps.onCardPage().header().favoritesButton().click();
        basePageSteps.onCardPage().favoritesPopup().favoritesList().should(hasSize(3)).get(favoriteIndex)
                .link().should(hasAttribute("href", saleUrl));
    }
}
