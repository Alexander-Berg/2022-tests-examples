package ru.yandex.general.goals;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;

@Epic(GOALS_FEATURE)
@Feature(FORM_OFFER_SHOW)
@DisplayName("Цель «FORM_OFFER_SHOW», при открытии формы подачи оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormOfferShowGoalTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «FORM_OFFER_SHOW», при открытии формы подачи оффера")
    public void shouldSeeFormOfferShowGoal() {
        urlSteps.testing().path(FORM).open();

        goalsSteps.withGoalType(FORM_OFFER_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
