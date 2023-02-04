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
import static ru.yandex.general.consts.Goals.FORM_OFFER_CONTENT_READY;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ADD;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.TRANSPORTIROVKA_PERENOSKI;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(GOALS_FEATURE)
@DisplayName("Цель «FORM_OFFER_CONTENT_READY», при успешной публикации оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class FormOfferContentReadyGoalTest {

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
        passportSteps.createAccountAndLogin();
        offerAddSteps.setCookie(CLASSIFIED_REGION_ID, "65");
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CONTENT_READY)
    @DisplayName("Цель «FORM_OFFER_CONTENT_READY», при успешной публикации оффера")
    public void shouldSeeFormOfferContentReadyGoal() {
        offerAddSteps.addOffer();

        goalsSteps.withGoalType(FORM_OFFER_CONTENT_READY)
                .withPageRef(urlSteps.testing().path(ADD).path(TRANSPORTIROVKA_PERENOSKI).toString())
                .withCount(1)
                .shouldExist();
    }

}
