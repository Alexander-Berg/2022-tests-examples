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
import static ru.auto.tests.desktop.consts.AutoruFeatures.NOTES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Добавление заметки в поп-апе контактов")
@Feature(NOTES)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class NotesContactsPopupAddTest {

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
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String saleMock;

    @Parameterized.Parameter(2)
    public String phonesMock;

    @Parameterized.Parameter(3)
    public String notePutMock;

    @Parameterized.Parameter(4)
    public String saleWithNoteMock;

    @Parameterized.Parameter(5)
    public String placeholderText;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedUser", "desktop/OfferCarsPhones", "desktop/UserNotesCarsPut",
                        "desktop/OfferCarsUsedUserWithNote", "Заметка об этом автомобиле (её увидите только вы)"},
                {TRUCK, "desktop/OfferTrucksUsedUser", "desktop/OfferTrucksPhones", "desktop/UserNotesTrucksPut",
                        "desktop/OfferTrucksUsedUserWithNote", "Заметка об этом грузовике (её увидите только вы)"},
                {MOTORCYCLE, "desktop/OfferMotoUsedUser", "desktop/OfferMotoPhones", "desktop/UserNotesMotoPut",
                        "desktop/OfferMotoUsedUserWithNote", "Заметка об этом мотоцикле (её увидите только вы)"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                saleMock,
                phonesMock,
                notePutMock).post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().waitUntil(isDisplayed());

        mockRule.overwriteStub(1, saleWithNoteMock);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавляем заметку")
    public void shouldAddNote() {
        basePageSteps.onCardPage().contactsPopup().noteBar().input(placeholderText, NOTE);
        basePageSteps.onCardPage().contactsPopup().noteBar().saveButton().click();
        basePageSteps.onCardPage().contactsPopup().noteBar().note().should(hasText(NOTE));
        basePageSteps.refresh();
        basePageSteps.onCardPage().cardHeader().noteBar().note().should(hasText(NOTE));
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().noteBar().input(placeholderText).should(hasValue(NOTE));
    }
}