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

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_DESCRIPTION_READY;
import static ru.yandex.general.consts.Goals.FORM_OFFER_DESCRIPTION_SKIP;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.page.FormPage.PUBLISH;

@Epic(GOALS_FEATURE)
@DisplayName("Цели «FORM_OFFER_DESCRIPTION_READY» и «FORM_OFFER_DESCRIPTION_SKIP» на форме")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class FormDescriptionGoalsTest {

    private static final String DESCRIPTION = "Описание, несколько слов + символы";

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
    @Feature(FORM_OFFER_DESCRIPTION_READY)
    @DisplayName("Цель «FORM_OFFER_DESCRIPTION_READY»")
    public void shouldSeeFormOfferDescriptionReadyGoal() {
        offerAddSteps.withDescription(DESCRIPTION).fillToAddressStep();

        goalsSteps.withGoalType(FORM_OFFER_DESCRIPTION_READY)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_DESCRIPTION_READY)
    @DisplayName("Нет цели «FORM_OFFER_DESCRIPTION_READY» без описания")
    public void shouldNotSeeFormOfferDescriptionReadyGoalWithoutDescription() {
        offerAddSteps.fillToAddressStep();

        goalsSteps.withGoalType(FORM_OFFER_DESCRIPTION_READY)
                .withCurrentPageRef()
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_DESCRIPTION_SKIP)
    @DisplayName("Цель «FORM_OFFER_DESCRIPTION_SKIP»")
    public void shouldSeeFormOfferDescriptionSkipGoal() {
        offerAddSteps.fillToAddressStep();
        offerAddSteps.scrollingToElement(offerAddSteps.onFormPage().button(PUBLISH));

        goalsSteps.withGoalType(FORM_OFFER_DESCRIPTION_SKIP)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_DESCRIPTION_SKIP)
    @DisplayName("Нет цели «FORM_OFFER_DESCRIPTION_SKIP» с описанием")
    public void shouldNotSeeFormOfferDescriptionSkipGoalWithDescription() {
        offerAddSteps.withDescription(DESCRIPTION).fillToAddressStep();
        offerAddSteps.scrollingToElement(offerAddSteps.onFormPage().button(PUBLISH));

        goalsSteps.withGoalType(FORM_OFFER_DESCRIPTION_SKIP)
                .withCurrentPageRef()
                .withCount(0)
                .shouldExist();
    }

}
