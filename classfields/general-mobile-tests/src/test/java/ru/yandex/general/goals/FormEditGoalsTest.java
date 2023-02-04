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
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CATEGORY_MANUAL_SELECT_SHOW;
import static ru.yandex.general.consts.Goals.FORM_OFFER_EDIT_CATEGORY_CHANGE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_EDIT_SAVE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_EDIT_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.mobile.page.FormPage.CHANGE_CATEGORY;
import static ru.yandex.general.mobile.page.FormPage.SAVE;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на форме редактирования")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormEditGoalsTest {

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
        passportSteps.accountWithOffersLogin();
        offerAddSteps.setCookie(CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW, "1");
        urlSteps.testing().path(MY).path(OFFERS).open();
        offerAddSteps.onMyOffersPage().snippetFirst().offerAction().click();
        offerAddSteps.onMyOffersPage().popup().spanLink("Редактировать").click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_EDIT_SHOW)
    @DisplayName("Цель «FORM_OFFER_EDIT_SHOW»")
    public void shouldSeeFormOfferEditShowGoal() {
        goalsSteps.withGoalType(FORM_OFFER_EDIT_SHOW)
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_EDIT_CATEGORY_CHANGE)
    @DisplayName("Цель «FORM_OFFER_EDIT_CATEGORY_CHANGE»")
    public void shouldSeeFormOfferEditCategoryChangeGoal() {
        offerAddSteps.onFormPage().button(CHANGE_CATEGORY).waitUntil(isDisplayed()).click();

        goalsSteps.withGoalType(FORM_OFFER_EDIT_CATEGORY_CHANGE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CATEGORY_MANUAL_SELECT_SHOW)
    @DisplayName("Цель «FORM_OFFER_CATEGORY_MANUAL_SELECT_SHOW» по «Сменить категорию» на форме редактирования")
    public void shouldSeeFormOfferCategoryManualSelectShowGoal() {
        offerAddSteps.onFormPage().button(CHANGE_CATEGORY).waitUntil(isDisplayed()).click();

        goalsSteps.withGoalType(FORM_OFFER_CATEGORY_MANUAL_SELECT_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_EDIT_CATEGORY_CHANGE)
    @DisplayName("Цель «FORM_OFFER_EDIT_CATEGORY_CHANGE» по инпуту категории на форме редактирования")
    public void shouldSeeFormOfferEditCategoryChangeGoalFromCategoryInput() {
        offerAddSteps.onFormPage().draftSection("Категория").button().click();

        goalsSteps.withGoalType(FORM_OFFER_EDIT_CATEGORY_CHANGE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CATEGORY_MANUAL_SELECT_SHOW)
    @DisplayName("Цель «FORM_OFFER_CATEGORY_MANUAL_SELECT_SHOW» по инпуту категории на форме редактирования")
    public void shouldSeeFormOfferCategoryManualSelectShowGoalFromCategoryInput() {
        offerAddSteps.onFormPage().draftSection("Категория").button().click();

        goalsSteps.withGoalType(FORM_OFFER_CATEGORY_MANUAL_SELECT_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_EDIT_SAVE)
    @DisplayName("Цель «FORM_OFFER_EDIT_SAVE»")
    public void shouldSeeFormOfferEditSaveGoal() {
        offerAddSteps.onFormPage().button(SAVE).click();

        goalsSteps.withGoalType(FORM_OFFER_EDIT_SAVE)
                .withCount(1)
                .shouldExist();
    }

}
