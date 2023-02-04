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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.NOTES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Удаление заметки в поп-апе контактов")
@Feature(NOTES)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class NotesContactsPopupDeleteTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
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
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String saleWithNoteMock;

    @Parameterized.Parameter(2)
    public String phonesMock;

    @Parameterized.Parameter(3)
    public String noteDeleteMock;

    @Parameterized.Parameter(4)
    public String saleMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedUserWithNote", "desktop/OfferCarsPhones",
                        "desktop/UserNotesCarsDelete", "desktop/OfferCarsUsedUser"},
                {TRUCK, "desktop/OfferTrucksUsedUserWithNote", "desktop/OfferTrucksPhones",
                        "desktop/UserNotesTrucksDelete", "desktop/OfferTrucksUsedUser"},
                {MOTORCYCLE, "desktop/OfferMotoUsedUserWithNote", "desktop/OfferMotoPhones",
                        "desktop/UserNotesMotoDelete", "desktop/OfferMotoUsedUser"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                saleWithNoteMock,
                phonesMock,
                noteDeleteMock,
                saleMock).post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().waitUntil(isDisplayed());

        mockRule.overwriteStub(1, saleMock);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаляем заметку")
    public void shouldDeleteNote() {
        basePageSteps.onCardPage().contactsPopup().time().click();
        basePageSteps.moveCursor(basePageSteps.onCardPage().contactsPopup().noteBar().note());
        basePageSteps.onCardPage().contactsPopup().noteBar().deleteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().contactsPopup().noteBar().note().should(not(hasText(NOTE_TEXT)));
        basePageSteps.onCardPage().contactsPopup().closer().click();
        basePageSteps.onCardPage().cardHeader().noteBar().note().should(not(isDisplayed()));
    }
}