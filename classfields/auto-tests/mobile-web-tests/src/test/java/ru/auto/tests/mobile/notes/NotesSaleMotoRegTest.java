package ru.auto.tests.mobile.notes;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.NOTES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.step.CookieSteps.COOKIE_NAME_NOTE_FAV_INFO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Заметки на карточке объявления")
@Feature(NOTES)
@RunWith(value = GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class NotesSaleMotoRegTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String NOTE = "Note text";

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferMotoUsedUser",
                "desktop/UserNotesMotoPut",
                "desktop/UserNotesMotoDelete",
                "desktop/UserFavoritesMotoPost").post();

        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path(SALE_ID).open();

        mockRule.overwriteStub(1, "desktop/OfferMotoUsedUserWithNote");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавление заметки")
    public void shouldAddNote() {
        scrollToNote();
        basePageSteps.onCardPage().addNote(NOTE);
        basePageSteps.onCardPage().note().should(hasText(NOTE));
        basePageSteps.refresh();
        basePageSteps.onCardPage().note().should(hasText(NOTE));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Автоматическое добавление в избранное после добавления заметки")
    public void shouldAddToFavorites() {
        scrollToNote();
        basePageSteps.onCardPage().addNote(NOTE);
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Объявления с заметками автоматически добавляются в «Избранные объявления», " +
                        "чтобы вы могли легко их найти"));
        basePageSteps.onCardPage().cardActions().deleteFromFavoritesButton().waitUntil(isDisplayed());
        cookieSteps.shouldSeeCookieWithValue(COOKIE_NAME_NOTE_FAV_INFO, "1");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаление заметки")
    public void shouldDeleteNote() {
        scrollToNote();
        basePageSteps.onCardPage().addNote(NOTE);
        deleteNote();
        basePageSteps.onCardPage().note().waitUntil(not(isDisplayed()));
    }

    @Step("Скроллим к заметке")
    private void scrollToNote() {
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCardPage().cardActions().noteButton(), 0, 0);
        basePageSteps.scrollDown(basePageSteps.onCardPage().cardActions().noteButton().getSize().getHeight() * 3);
    }

    @Step("Удаляем заметку")
    private void deleteNote() {
        basePageSteps.onCardPage().note().should(isDisplayed()).click();
        basePageSteps.onCardPage().cardActions().notePopup().button("Удалить").waitUntil(isDisplayed()).click();
    }
}
