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
import ru.yandex.general.mobile.step.OfferAddSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.yandex.general.beans.Attribute.input;
import static ru.yandex.general.beans.Attribute.multiselect;
import static ru.yandex.general.beans.Attribute.select;
import static ru.yandex.general.beans.Attribute.switcher;
import static ru.yandex.general.consts.FormConstants.Categories.DRUGOI_GOTOVIY_BIZNES;
import static ru.yandex.general.consts.FormConstants.Categories.REZUME_IN_SELLING;
import static ru.yandex.general.consts.FormConstants.Categories.UMNIE_KOLONKI;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_PROPS_CHANGE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_PROPS_SHOW;
import static ru.yandex.general.consts.Goals.FORM_OFFER_PROPS_SKIP;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на атрибуты на форме")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormPropsGoalsTest {

    private static final String WORK_TIME = "Время работы от 1 до 60 час";
    private static final String BLUETOOTH = "Bluetooth";
    private static final String TIP_PITANIYA = "Тип питания";
    private static final String APPLE = "Apple";
    private static final String FROM_AKKUM = "от аккумулятора";
    private static final String FROM_NETWORK = "от сети";
    private static final String MANUFACTURER = "Производитель";
    private static final String WORK_TIME_VALUE = "33";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(FORM).open();
        offerAddSteps.withCategory(UMNIE_KOLONKI);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_SHOW)
    @DisplayName("Цель «FORM_OFFER_PROPS_SHOW» в категории товара")
    public void shouldSeeFormOfferShowGoal() {
        offerAddSteps.fillToAttributesStep();

        goalsSteps.withGoalType(FORM_OFFER_PROPS_SHOW)
                .withCurrentPageRef()
                .withCount(5)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_SHOW)
    @DisplayName("Цель «FORM_OFFER_PROPS_SHOW» в категории работы")
    public void shouldSeeFormOfferShowGoalWorkCategory() {
        offerAddSteps.withCategory(REZUME_IN_SELLING);
        offerAddSteps.fillToAttributesStep();

        goalsSteps.withGoalType(FORM_OFFER_PROPS_SHOW)
                .withCurrentPageRef()
                .withCount(6)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_SHOW)
    @DisplayName("Нет цели «FORM_OFFER_PROPS_SHOW» в категории без атрибутов")
    public void shouldNotSeeFormOfferPropsShowGoal() {
        offerAddSteps.withCategory(DRUGOI_GOTOVIY_BIZNES);
        offerAddSteps.fillToAttributesStep();

        goalsSteps.withGoalType(FORM_OFFER_PROPS_SHOW)
                .withCurrentPageRef()
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_CHANGE)
    @DisplayName("Цель «FORM_OFFER_PROPS_CHANGE» при выборе атрибута типа «Селект»")
    public void shouldSeeFormOfferPropsChangeSelectGoal() {
        offerAddSteps.fillToAttributesStep();
        offerAddSteps.withAttributes(
                select(MANUFACTURER).setValue(APPLE))
                .fillAttributes();

        goalsSteps.withGoalType(FORM_OFFER_PROPS_CHANGE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_CHANGE)
    @DisplayName("Цель «FORM_OFFER_PROPS_CHANGE» при выборе 2 айтемов типа «Мультиселект»")
    public void shouldSeeFormOfferPropsChangeMultiselectGoal() {
        offerAddSteps.fillToAttributesStep();
        offerAddSteps.withAttributes(
                multiselect(TIP_PITANIYA).setValues(FROM_AKKUM, FROM_NETWORK))
                .fillAttributes();

        goalsSteps.withGoalType(FORM_OFFER_PROPS_CHANGE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_CHANGE)
    @DisplayName("Цель «FORM_OFFER_PROPS_CHANGE» при заполнении атрибута типа «Инпут»")
    public void shouldSeeFormOfferPropsChangeInputGoal() {
        offerAddSteps.fillToAttributesStep();
        offerAddSteps.withAttributes(
                input(WORK_TIME).setValue(WORK_TIME_VALUE))
                .fillAttributes();

        goalsSteps.withGoalType(FORM_OFFER_PROPS_CHANGE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_CHANGE)
    @DisplayName("Цель «FORM_OFFER_PROPS_CHANGE» при заполнении атрибута типа «Чекбокс»")
    public void shouldSeeFormOfferPropsChangeCheckboxGoal() {
        offerAddSteps.fillToAttributesStep();
        offerAddSteps.withAttributes(
                switcher(BLUETOOTH))
                .fillAttributes();

        goalsSteps.withGoalType(FORM_OFFER_PROPS_CHANGE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_CHANGE)
    @DisplayName("Цель «FORM_OFFER_PROPS_CHANGE» при выключении атрибута типа «Чекбокс»")
    public void shouldSeeFormOfferPropsChangeCheckboxOffGoal() {
        offerAddSteps.fillToAttributesStep();
        offerAddSteps.onFormPage().attribute(BLUETOOTH).checkboxWithLabel("").click();
        offerAddSteps.waitSomething(1, SECONDS);
        goalsSteps.clearHar();

        offerAddSteps.onFormPage().attribute(BLUETOOTH).checkboxWithLabel("").click();

        goalsSteps.withGoalType(FORM_OFFER_PROPS_CHANGE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_SKIP)
    @DisplayName("5 целей «FORM_OFFER_PROPS_SKIP» при незаполнении всех атрибутов")
    public void shouldSee5FormOfferPropsSkipGoal() {
        offerAddSteps.fillToAttributesStep();
        offerAddSteps.fillAttributes();

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
        offerAddSteps.fillToAttributesStep();
        offerAddSteps.withAttributes(
                select(MANUFACTURER).setValue(APPLE),
                select("Голосовой помощник").setValue("Apple Siri"),
                multiselect(TIP_PITANIYA).setValues(FROM_AKKUM, FROM_NETWORK),
                input(WORK_TIME).setValue(WORK_TIME_VALUE),
                switcher(BLUETOOTH))
                .fillAttributes();

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
        offerAddSteps.fillToAttributesStep();
        offerAddSteps.withAttributes(
                select(MANUFACTURER).setValue(APPLE),
                input(WORK_TIME).setValue(WORK_TIME_VALUE),
                switcher(BLUETOOTH))
                .fillAttributes();

        goalsSteps.withGoalType(FORM_OFFER_PROPS_SKIP)
                .withCurrentPageRef()
                .withCount(2)
                .shouldExist();
    }

}
