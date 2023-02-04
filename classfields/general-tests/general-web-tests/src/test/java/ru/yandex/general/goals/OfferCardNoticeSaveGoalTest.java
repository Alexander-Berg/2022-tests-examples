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
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.CARD_OFFER_NOTICE_SAVE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.element.CardNotice.SAVE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.general.step.BasePageSteps.TRUE;

@Epic(GOALS_FEATURE)
@DisplayName("Цель «CARD_OFFER_NOTICE_SAVE» при сохранении заметки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class OfferCardNoticeSaveGoalTest {

    private static final String NOTICE_TEXT = "Заметка к офферу";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().open();
        basePageSteps.onListingPage().snippetFirst().click();
        basePageSteps.switchToNextTab();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_OFFER_NOTICE_SAVE)
    @DisplayName("Цель «CARD_OFFER_NOTICE_SAVE» при сохранении заметки")
    public void shouldSeeCardOfferNoticeSaveGoal() {
        basePageSteps.onOfferCardPage().notice().textarea().click();
        basePageSteps.onOfferCardPage().notice().textarea().sendKeys(NOTICE_TEXT);
        basePageSteps.onOfferCardPage().notice().button(SAVE).click();

        goalsSteps.withGoalType(CARD_OFFER_NOTICE_SAVE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
