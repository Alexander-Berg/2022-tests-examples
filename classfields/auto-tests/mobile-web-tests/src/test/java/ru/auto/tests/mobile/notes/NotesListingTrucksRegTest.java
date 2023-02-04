package ru.auto.tests.mobile.notes;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.NOTES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Заметки в листинге")
@Feature(NOTES)
@RunWith(value = GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class NotesListingTrucksRegTest {

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
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/SearchTrucksBreadcrumbsEmpty",
                "mobile/SearchTrucksAll",
                "desktop/UserNotesTrucksPut",
                "desktop/UserNotesTrucksDelete").post();

        urlSteps.testing().path(MOSKVA).path(TRUCK).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Редактирование заметки")
    public void shouldEditNote() {
        basePageSteps.onListingPage().getSale(0).note().should(isDisplayed()).click();
        basePageSteps.clearInput(basePageSteps.onListingPage().notePopup().input());
        basePageSteps.onListingPage().notePopup().input().sendKeys("");
        basePageSteps.onListingPage().notePopup().button("Сохранить").should(isDisplayed()).click();
        basePageSteps.onListingPage().getSale(0).note().waitUntil(hasText(NOTE));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаление заметки")
    public void shouldDeleteNote() {
        basePageSteps.onListingPage().getSale(0).note().should(isDisplayed()).click();
        basePageSteps.onListingPage().notePopup().button("Удалить").waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().getSale(0).note().waitUntil(not(isDisplayed()));
    }
}
