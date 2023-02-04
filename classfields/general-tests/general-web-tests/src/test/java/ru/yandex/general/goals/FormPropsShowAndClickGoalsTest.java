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
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.OfferAddSteps;
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
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;

@Epic(GOALS_FEATURE)
@DisplayName("Цели «FORM_OFFER_PROPS_SHOW» и «FORM_OFFER_PROPS_CLICK» на форме")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class FormPropsShowAndClickGoalsTest {

    private static final String WORK_TIME = "Время работы, час";
    private static final String BLUETOOTH = "Bluetooth";
    private static final String TIP_PITANIYA = "Тип питания";
    private static final String APPLE = "Apple";
    private static final String FROM_AKKUM = "от аккумулятора";
    private static final String FROM_NETWORK = "от сети";
    private static final String MANUFACTURER = "Производитель";

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
    public void shouldSeeFormOfferPropsShowGoal() {
        offerAddSteps.fillToDescriptionStep();
        offerAddSteps.onFormPage().spoilerOpen().click();

        goalsSteps.withGoalType(FORM_OFFER_PROPS_SHOW)
                .withCurrentPageRef()
                .withCount(5)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_SHOW)
    @DisplayName("Цель «FORM_OFFER_PROPS_SHOW» в категории работы")
    public void shouldSeeFormOfferPropsShowGoalWorkCategory() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).fillToDescriptionStep();
        offerAddSteps.onFormPage().spoilerOpen().click();

        goalsSteps.withGoalType(FORM_OFFER_PROPS_SHOW)
                .withCurrentPageRef()
                .withCount(6)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_SHOW)
    @DisplayName("Нет целей «FORM_OFFER_PROPS_SHOW» без открытия блока атрибутов")
    public void shouldNotSeeFormOfferPropsShowGoalWithoutOpenSpoiler() {
        offerAddSteps.fillToDescriptionStep();

        goalsSteps.withGoalType(FORM_OFFER_PROPS_SHOW)
                .withCurrentPageRef()
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_SHOW)
    @DisplayName("Нет цели «FORM_OFFER_PROPS_SHOW» в категории без атрибутов")
    public void shouldNotSeeFormOfferPropsShowGoal() {
        offerAddSteps.withCategory(DRUGOI_GOTOVIY_BIZNES).fillToDescriptionStep();

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
        offerAddSteps.withAttributes(
                select(MANUFACTURER).setValue(APPLE))
                .fillToPriceStep();

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
        offerAddSteps.withAttributes(
                multiselect(TIP_PITANIYA).setValues(FROM_AKKUM, FROM_NETWORK))
                .fillToPriceStep();

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
        offerAddSteps.withAttributes(
                input(WORK_TIME).setValue("34"))
                .fillToPriceStep();

        goalsSteps.withGoalType(FORM_OFFER_PROPS_CHANGE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_CHANGE)
    @DisplayName("Цель «FORM_OFFER_PROPS_CHANGE» при заполнении атрибута типа «Свитчер»")
    public void shouldSeeFormOfferPropsChangeSwitcherGoal() {
        offerAddSteps.withAttributes(
                switcher(BLUETOOTH))
                .fillToPriceStep();

        goalsSteps.withGoalType(FORM_OFFER_PROPS_CHANGE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PROPS_CHANGE)
    @DisplayName("Цель «FORM_OFFER_PROPS_CHANGE» при выключении атрибута типа «Свитчер»")
    public void shouldSeeFormOfferPropsChangeSwitcherOffGoal() {
        offerAddSteps.withAttributes(
                switcher(BLUETOOTH))
                .fillToPriceStep();
        offerAddSteps.waitSomething(1, SECONDS);
        goalsSteps.clearHar();

        offerAddSteps.onFormPage().attribute(BLUETOOTH).switcher().click();

        goalsSteps.withGoalType(FORM_OFFER_PROPS_CHANGE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
