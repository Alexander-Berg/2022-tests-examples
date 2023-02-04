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
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.OfferAddSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_DRAFT_CLEAR;
import static ru.yandex.general.consts.Goals.FORM_OFFER_DRAFT_SAVE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_RESUME_CLEAR;
import static ru.yandex.general.consts.Goals.FORM_OFFER_RESUME_PROCEED;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.mock.MockCurrentDraft.FINAL_SCREEN;
import static ru.yandex.general.mock.MockCurrentDraft.mockCurrentDraft;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.FormPage.CONTINUE;
import static ru.yandex.general.page.FormPage.START_NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на черновике")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class FormDraftGoalsTest {

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

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        passportSteps.accountWithOffersLogin();
        mockRule.graphqlStub(mockResponse()
                .setCurrentDraft(mockCurrentDraft(FINAL_SCREEN).build())
                .setCategoriesTemplate()
                .setCategoryTemplate()
                .setCurrentUserExample()
                .build()).withDefaults().create();
        offerAddSteps.setMoscowCookie();
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_RESUME_PROCEED)
    @DisplayName("Цель «FORM_OFFER_RESUME_PROCEED»")
    public void shouldSeeFormOfferResumeProceedGoal() {
        offerAddSteps.onFormPage().button(CONTINUE).click();

        goalsSteps.withGoalType(FORM_OFFER_RESUME_PROCEED)
                .withPageRef(urlSteps.testing().path(FORM).toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_RESUME_CLEAR)
    @DisplayName("Цель «FORM_OFFER_RESUME_CLEAR»")
    public void shouldSeeFormOfferResumeClearGoal() {
        offerAddSteps.onFormPage().spanLink(START_NEW).click();

        goalsSteps.withGoalType(FORM_OFFER_RESUME_CLEAR)
                .withPageRef(urlSteps.testing().path(FORM).toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_DRAFT_CLEAR)
    @DisplayName("Цель «FORM_OFFER_DRAFT_CLEAR»")
    public void shouldSeeFormOfferDraftClearGoal() {
        offerAddSteps.onFormPage().back().click();
        offerAddSteps.onFormPage().modal().button("Сбросить").waitUntil(isDisplayed()).click();

        goalsSteps.withGoalType(FORM_OFFER_DRAFT_CLEAR)
                .withPageRef(urlSteps.testing().path(FORM).toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_DRAFT_SAVE)
    @DisplayName("Цель «FORM_OFFER_DRAFT_SAVE»")
    public void shouldSeeFormOfferDraftSaveGoal() {
        offerAddSteps.onFormPage().back().click();
        offerAddSteps.onFormPage().modal().button("Сохранить черновик").waitUntil(isDisplayed()).click();

        goalsSteps.withGoalType(FORM_OFFER_DRAFT_SAVE)
                .withPageRef(urlSteps.testing().path(FORM).toString())
                .withCount(1)
                .shouldExist();
    }

}
