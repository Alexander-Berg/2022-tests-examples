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

import static ru.yandex.general.consts.FormConstants.Categories.PERENOSKA;
import static ru.yandex.general.consts.FormConstants.Categories.REZUME_IN_SELLING;
import static ru.yandex.general.consts.FormConstants.Categories.UMNIE_KOLONKI;
import static ru.yandex.general.consts.FormConstants.Conditions.NEW;
import static ru.yandex.general.consts.FormConstants.Conditions.USED;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CATEGORY_CLICK;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CATEGORY_MANUAL_SELECT_CLICK;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CATEGORY_MANUAL_SELECT_SHOW;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CATEGORY_MORE_CLICK;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CATEGORY_MORE_SHOW;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CATEGORY_NOT_FOUND_CLICK;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CATEGORY_SHOW;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CONDITION_CLICK;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CONDITION_SHOW;
import static ru.yandex.general.consts.Goals.FORM_OFFER_DESCRIPTION_SHOW;
import static ru.yandex.general.consts.Goals.FORM_OFFER_LOGIN_CLICK;
import static ru.yandex.general.consts.Goals.FORM_OFFER_LOGIN_REGISTRATION_CLICK;
import static ru.yandex.general.consts.Goals.FORM_OFFER_LOGIN_SHOW;
import static ru.yandex.general.consts.Goals.FORM_OFFER_PHOTO_READY;
import static ru.yandex.general.consts.Goals.FORM_OFFER_PRICE_SHOW;
import static ru.yandex.general.consts.Goals.FORM_OFFER_ROOT_CATEGORY_CLICK;
import static ru.yandex.general.consts.Goals.FORM_OFFER_SELECT_PHOTO_SHOW;
import static ru.yandex.general.consts.Goals.FORM_OFFER_TITLE_READY;
import static ru.yandex.general.consts.Goals.FORM_OFFER_TITLE_SHOW;
import static ru.yandex.general.consts.Goals.FORM_RULES_CLICK;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ADD;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.page.FormPage.HOW_TO;
import static ru.yandex.general.page.FormPage.LOGIN;
import static ru.yandex.general.page.FormPage.NO_SUITABLE;
import static ru.yandex.general.page.FormPage.REGISTRATION;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на форме")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class FormGoalsTest {

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
    @Feature(FORM_OFFER_ROOT_CATEGORY_CLICK)
    @DisplayName("Цель «FORM_OFFER_ROOT_CATEGORY_CLICK»")
    public void shouldSeeFormOfferRootCategoryClickGoal() {
        offerAddSteps.fillSection();

        goalsSteps.withGoalType(FORM_OFFER_ROOT_CATEGORY_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_ROOT_CATEGORY_CLICK)
    @DisplayName("Цель «FORM_OFFER_ROOT_CATEGORY_CLICK» по клику на «Загрузить фид»")
    public void shouldSeeFormOffeRootCategoryClickGoalFromFeed() {
        offerAddSteps.onFormPage().link("Загрузить фид").click();

        goalsSteps.withGoalType(FORM_OFFER_ROOT_CATEGORY_CLICK)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_RULES_CLICK)
    @DisplayName("Цель «FORM_RULES_CLICK»")
    public void shouldSeeFormRulesClickGoal() {
        offerAddSteps.onFormPage().link(HOW_TO).click();

        goalsSteps.withGoalType(FORM_RULES_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_TITLE_SHOW)
    @DisplayName("Цели «FORM_OFFER_TITLE_SHOW»")
    public void shouldSeeFormOfferTitleShowGoal() {
        offerAddSteps.fillToNameStep();

        goalsSteps.withGoalType(FORM_OFFER_TITLE_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_SELECT_PHOTO_SHOW)
    @DisplayName("Цель «FORM_OFFER_SELECT_PHOTO_SHOW»")
    public void shouldSeeFormOfferSelectPhotoShowGoal() {
        offerAddSteps.fillSection();

        goalsSteps.withGoalType(FORM_OFFER_SELECT_PHOTO_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_TITLE_READY)
    @DisplayName("Цель «FORM_OFFER_TITLE_READY»")
    public void shouldSeeFormOfferTitleReadyGoal() {
        offerAddSteps.fillToDescriptionStep();

        goalsSteps.withGoalType(FORM_OFFER_TITLE_READY)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PHOTO_READY)
    @DisplayName("Цель «FORM_OFFER_PHOTO_READY»")
    public void shouldSeeFormOfferPhotoReadyGoal() {
        offerAddSteps.fillToNameStep();

        goalsSteps.withGoalType(FORM_OFFER_PHOTO_READY)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CATEGORY_SHOW)
    @DisplayName("Цель «FORM_OFFER_CATEGORY_SHOW»")
    public void shouldSeeFormOfferCategoryShowGoal() {
        offerAddSteps.fillToCategoryStep();

        goalsSteps.withGoalType(FORM_OFFER_CATEGORY_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CATEGORY_CLICK)
    @DisplayName("Цель «FORM_OFFER_CATEGORY_CLICK»")
    public void shouldSeeFormOfferCategoryClickGoal() {
        offerAddSteps.fillToDescriptionStep();

        goalsSteps.withGoalType(FORM_OFFER_CATEGORY_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CATEGORY_MORE_SHOW)
    @DisplayName("Цель «FORM_OFFER_CATEGORY_MORE_SHOW»")
    public void shouldSeeFormOfferCategoryMoreShowGoal() {
        offerAddSteps.fillToCategoryStep();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();

        goalsSteps.withGoalType(FORM_OFFER_CATEGORY_MORE_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CATEGORY_MORE_CLICK)
    @DisplayName("Цель «FORM_OFFER_CATEGORY_MORE_CLICK»")
    public void shouldSeeFormOfferCategoryMoreClickGoal() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToCategoryStep();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().categories().get(3).click();

        goalsSteps.withGoalType(FORM_OFFER_CATEGORY_MORE_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CATEGORY_NOT_FOUND_CLICK)
    @DisplayName("Цель «FORM_OFFER_CATEGORY_NOT_FOUND_CLICK»")
    public void shouldSeeFormOfferCategoryNotFoundClickGoal() {
        openCategoryManualSelectPopup();

        goalsSteps.withGoalType(FORM_OFFER_CATEGORY_NOT_FOUND_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CATEGORY_MANUAL_SELECT_SHOW)
    @DisplayName("Цель «FORM_OFFER_CATEGORY_MANUAL_SELECT_SHOW»")
    public void shouldSeeFormOfferCategoryManualSelectShowGoal() {
        openCategoryManualSelectPopup();

        goalsSteps.withGoalType(FORM_OFFER_CATEGORY_MANUAL_SELECT_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CATEGORY_MANUAL_SELECT_CLICK)
    @DisplayName("Цель «FORM_OFFER_CATEGORY_MANUAL_SELECT_CLICK»")
    public void shouldSeeFormOfferCategoryManualSelectClickGoal() {
        openCategoryManualSelectPopup();
        offerAddSteps.onFormPage().modal().link("Электроника").waitUntil(isDisplayed()).click();

        goalsSteps.withGoalType(FORM_OFFER_CATEGORY_MANUAL_SELECT_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_DESCRIPTION_SHOW)
    @DisplayName("Цель «FORM_OFFER_DESCRIPTION_SHOW»")
    public void shouldSeeFormOfferDescriptionShowGoal() {
        offerAddSteps.fillToDescriptionStep();

        goalsSteps.withGoalType(FORM_OFFER_DESCRIPTION_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CONDITION_SHOW)
    @DisplayName("Цель «FORM_OFFER_CONDITION_SHOW»")
    public void shouldSeeFormOfferConditionShowGoal() {
        offerAddSteps.fillToDescriptionStep();

        goalsSteps.withGoalType(FORM_OFFER_CONDITION_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PRICE_SHOW)
    @DisplayName("Цель «FORM_OFFER_PRICE_SHOW»")
    public void shouldSeeFormOfferPriceShowGoal() {
        offerAddSteps.fillToDescriptionStep();

        goalsSteps.withGoalType(FORM_OFFER_PRICE_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_PRICE_SHOW)
    @DisplayName("Цель «FORM_OFFER_PRICE_SHOW» для зарплаты")
    public void shouldSeeFormOfferPriceShowSallaryGoal() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle())
                .fillToDescriptionStep();

        goalsSteps.withGoalType(FORM_OFFER_PRICE_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_LOGIN_SHOW)
    @DisplayName("Цель «FORM_OFFER_LOGIN_SHOW»")
    public void shouldSeeFormOfferLoginShowGoal() {
        offerAddSteps.fillToDescriptionStep();

        goalsSteps.withGoalType(FORM_OFFER_LOGIN_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_LOGIN_CLICK)
    @DisplayName("Цель «FORM_OFFER_LOGIN_CLICK»")
    public void shouldSeeFormOfferLoginClickGoal() {
        offerAddSteps.withCategory(PERENOSKA).fillToDescriptionStep();
        offerAddSteps.onFormPage().passportAuthorization().link(LOGIN).click();

        goalsSteps.withGoalType(FORM_OFFER_LOGIN_CLICK)
                .withPageRef(urlSteps.testing().path(ADD).path(PERENOSKA.getCategoryPath()).toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_LOGIN_REGISTRATION_CLICK)
    @DisplayName("Цель «FORM_OFFER_LOGIN_REGISTRATION_CLICK»")
    public void shouldSeeFormOfferLoginRegistrationClickGoal() {
        offerAddSteps.withCategory(PERENOSKA).fillToDescriptionStep();
        offerAddSteps.onFormPage().passportAuthorization().link(REGISTRATION).click();

        goalsSteps.withGoalType(FORM_OFFER_LOGIN_REGISTRATION_CLICK)
                .withPageRef(urlSteps.testing().path(ADD).path(PERENOSKA.getCategoryPath()).toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CONDITION_CLICK)
    @DisplayName("Цель «FORM_OFFER_CONDITION_CLICK» - новый товар")
    public void shouldSeeFormOfferConditionClickNewGoal() {
        offerAddSteps.withCondition(NEW).fillToAttributesStep();

        goalsSteps.withGoalType(FORM_OFFER_CONDITION_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CONDITION_CLICK)
    @DisplayName("Цель «FORM_OFFER_CONDITION_CLICK» - б/у товар")
    public void shouldSeeFormOfferConditionClickUsedGoal() {
        offerAddSteps.withCondition(USED).fillToAttributesStep();

        goalsSteps.withGoalType(FORM_OFFER_CONDITION_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    private void openCategoryManualSelectPopup() {
        offerAddSteps.fillToCategoryStep();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
    }

}
