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

import static ru.yandex.general.consts.FormConstants.Categories.REZUME_IN_SELLING;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_PRICE_FREE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_PRICE_READY;
import static ru.yandex.general.consts.Goals.FORM_OFFER_PRICE_SKIP;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на атрибуты на форме")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
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

    @Before
    public void before() {
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PRICE_READY)
    @DisplayName("Цель «FORM_OFFER_PRICE_READY»")
    public void shouldSeeFormOfferPriceReadyGoal() {
        offerAddSteps.fillToPriceStep();
        offerAddSteps.withPrice(PRICE).fillPrice();

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
        offerAddSteps.fillToPriceStep();
        offerAddSteps.withFreePrice().fillPrice();

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
        offerAddSteps.fillToPriceStep();
        offerAddSteps.withFreePrice().fillPrice();

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
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle());
        offerAddSteps.fillToPriceStep();
        offerAddSteps.withPrice(SALLARY).fillPrice();

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
        offerAddSteps.fillToPriceStep();
        offerAddSteps.fillPrice();

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
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle());
        offerAddSteps.fillToPriceStep();
        offerAddSteps.fillPrice();

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
        offerAddSteps.fillToPriceStep();
        offerAddSteps.fillPrice();

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
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle());
        offerAddSteps.fillToPriceStep();
        offerAddSteps.fillPrice();

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
        offerAddSteps.fillToPriceStep();
        offerAddSteps.withFreePrice().fillPrice();

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
        offerAddSteps.fillToPriceStep();
        offerAddSteps.withPrice(PRICE).fillPrice();

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
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle());
        offerAddSteps.fillToPriceStep();
        offerAddSteps.withPrice(SALLARY).fillPrice();

        goalsSteps.withGoalType(FORM_OFFER_PRICE_SKIP)
                .withCurrentPageRef()
                .withCount(0)
                .shouldExist();
    }

}
