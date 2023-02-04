package ru.yandex.general.goals;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.OfferAddSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.beans.Attribute.input;
import static ru.yandex.general.beans.Attribute.multiselect;
import static ru.yandex.general.beans.Attribute.select;
import static ru.yandex.general.beans.Attribute.switcher;
import static ru.yandex.general.consts.FormConstants.Categories.UMNIE_KOLONKI;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_PROPS_SKIP;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.page.FormPage.PUBLISH;

@Epic(GOALS_FEATURE)
@DisplayName("Цели «FORM_OFFER_PROPS_SKIP» на форме")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class FormPropsSkipGoalsTest {

    private static final String MANUFACTURER = "Производитель";
    private static final String WORK_TIME = "Время работы, час";
    private static final String WORK_TIME_VALUE = "22";
    private static final String BLUETOOTH = "Bluetooth";
    private static final String TIP_PITANIYA = "Тип питания";
    private static final String APPLE = "Apple";
    private static final String FROM_AKKUM = "от аккумулятора";
    private static final String FROM_NETWORK = "от сети";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.accountForOfferCreationLogin();
        urlSteps.testing().path(FORM).open();
        offerAddSteps.withCategory(UMNIE_KOLONKI);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_SKIP)
    @DisplayName("5 целей «FORM_OFFER_PROPS_SKIP» при незаполнении всех атрибутов")
    public void shouldSee5FormOfferPropsSkipGoal() {
        offerAddSteps.fillToAddressStep();
        offerAddSteps.scrollingToElement(offerAddSteps.onFormPage().button(PUBLISH));

        goalsSteps.withGoalType(FORM_OFFER_PROPS_SKIP)
                .withCurrentPageRef()
                .withCount(5)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_SKIP)
    @DisplayName("Нет целей «FORM_OFFER_PROPS_SKIP» при заполнении всех атрибутов")
    public void shouldNotSeeFormOfferPropsSkipGoalAllFilled() {
        offerAddSteps.withAttributes(
                select(MANUFACTURER).setValue(APPLE),
                select("Голосовой помощник").setValue("Apple Siri"),
                multiselect(TIP_PITANIYA).setValues(FROM_AKKUM, FROM_NETWORK),
                input(WORK_TIME).setValue(WORK_TIME_VALUE),
                switcher(BLUETOOTH))
                .fillToAddressStep();
        offerAddSteps.scrollingToElement(offerAddSteps.onFormPage().button(PUBLISH));

        goalsSteps.withGoalType(FORM_OFFER_PROPS_SKIP)
                .withCurrentPageRef()
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_SKIP)
    @DisplayName("3 цели «FORM_OFFER_PROPS_SKIP» при частичном заполнении атрибутов")
    public void shouldSee3FormOfferPropsSkipGoalPartialyFilled() {
        offerAddSteps.withAttributes(
                select(MANUFACTURER).setValue(APPLE),
                input(WORK_TIME).setValue(WORK_TIME_VALUE),
                switcher(BLUETOOTH))
                .fillToAddressStep();
        offerAddSteps.scrollingToElement(offerAddSteps.onFormPage().button(PUBLISH));

        goalsSteps.withGoalType(FORM_OFFER_PROPS_SKIP)
                .withCurrentPageRef()
                .withCount(2)
                .shouldExist();
    }

}
