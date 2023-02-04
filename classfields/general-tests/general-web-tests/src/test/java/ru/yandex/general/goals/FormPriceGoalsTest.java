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
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.FormConstants.Categories.REZUME_IN_SELLING;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_PRICE_FREE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_PRICE_READY;
import static ru.yandex.general.consts.Goals.FORM_OFFER_PRICE_SKIP;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.page.FormPage.PUBLISH;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на цену на форме")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class FormPriceGoalsTest {

    private static final String PRICE = "2499";
    private static final String SALLARY = "45000";

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
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PRICE_READY)
    @DisplayName("Цель «FORM_OFFER_PRICE_READY»")
    public void shouldSeeFormOfferPriceReadyGoal() {
        offerAddSteps.withPrice(PRICE).fillToAddressStep();

        goalsSteps.withGoalType(FORM_OFFER_PRICE_READY)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PRICE_READY)
    @DisplayName("Цель «FORM_OFFER_PRICE_READY» для цены «Даром»")
    public void shouldSeeFormOfferPriceReadyFreePriceGoal() {
        offerAddSteps.withFreePrice().fillToAddressStep();

        goalsSteps.withGoalType(FORM_OFFER_PRICE_READY)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PRICE_FREE)
    @DisplayName("Цель «FORM_OFFER_PRICE_FREE» для цены «Даром»")
    public void shouldSeeFormOfferPriceFreeGoal() {
        offerAddSteps.withFreePrice().fillToAddressStep();

        goalsSteps.withGoalType(FORM_OFFER_PRICE_FREE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PRICE_READY)
    @DisplayName("Цель «FORM_OFFER_PRICE_READY» для зарплаты")
    public void shouldSeeFormOfferPriceReadySallaryGoal() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle())
                .withPrice(SALLARY).fillToAddressStep();

        goalsSteps.withGoalType(FORM_OFFER_PRICE_READY)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PRICE_READY)
    @DisplayName("Нет цели «FORM_OFFER_PRICE_READY» без цены")
    public void shouldNotSeeFormOfferPriceReadyGoalWithoutPrice() {
        offerAddSteps.fillToAddressStep();

        goalsSteps.withGoalType(FORM_OFFER_PRICE_READY)
                .withCurrentPageRef()
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PRICE_READY)
    @DisplayName("Нет цели «FORM_OFFER_PRICE_READY» без зарплаты")
    public void shouldNotSeeFormOfferPriceReadyGoalWithoutSallary() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle())
                .fillToAddressStep();

        goalsSteps.withGoalType(FORM_OFFER_PRICE_READY)
                .withCurrentPageRef()
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PRICE_SKIP)
    @DisplayName("Цель «FORM_OFFER_PRICE_SKIP»")
    public void shouldSeeFormOfferPriceSkipGoal() {
        offerAddSteps.fillToAddressStep();
        offerAddSteps.scrollingToElement(offerAddSteps.onFormPage().button(PUBLISH));

        goalsSteps.withGoalType(FORM_OFFER_PRICE_SKIP)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PRICE_SKIP)
    @DisplayName("Цель «FORM_OFFER_PRICE_SKIP» для зарплаты")
    public void shouldSeeFormOfferPriceSkipSallaryGoal() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle())
                .fillToAddressStep();
        offerAddSteps.scrollingToElement(offerAddSteps.onFormPage().button(PUBLISH));

        goalsSteps.withGoalType(FORM_OFFER_PRICE_SKIP)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PRICE_SKIP)
    @DisplayName("Нет цели «FORM_OFFER_PRICE_SKIP» для цены «Даром»")
    public void shouldNotSeeFormOfferPriceSkipFreePriceGoal() {
        offerAddSteps.withFreePrice().fillToAddressStep();
        offerAddSteps.scrollingToElement(offerAddSteps.onFormPage().button(PUBLISH));

        goalsSteps.withGoalType(FORM_OFFER_PRICE_SKIP)
                .withCurrentPageRef()
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PRICE_SKIP)
    @DisplayName("Нет цели «FORM_OFFER_PRICE_SKIP» для заполненной цены")
    public void shouldNotSeeFormOfferPriceSkipGoal() {
        offerAddSteps.withPrice(PRICE).fillToAddressStep();
        offerAddSteps.scrollingToElement(offerAddSteps.onFormPage().button(PUBLISH));

        goalsSteps.withGoalType(FORM_OFFER_PRICE_SKIP)
                .withCurrentPageRef()
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PRICE_SKIP)
    @DisplayName("Нет цели «FORM_OFFER_PRICE_SKIP» для заполненной зарплаты")
    public void shouldNotSeeFormOfferPriceSkipWithSallaryGoal() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle())
                .fillToDescriptionStep();
        offerAddSteps.withPrice(SALLARY).fillPrice().nextClick();
        offerAddSteps.scrollingToElement(offerAddSteps.onFormPage().button(PUBLISH));

        goalsSteps.withGoalType(FORM_OFFER_PRICE_SKIP)
                .withCurrentPageRef()
                .withCount(0)
                .shouldExist();
    }

}
