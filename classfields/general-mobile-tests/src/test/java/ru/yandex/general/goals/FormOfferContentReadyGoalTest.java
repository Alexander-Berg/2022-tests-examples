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
import ru.yandex.general.consts.FormConstants.Categories;
import ru.yandex.general.mobile.step.OfferAddSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.FormConstants.Categories.UMNIE_KOLONKI;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CONTENT_READY;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ADD;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.beans.Attribute.multiselect;
import static ru.yandex.general.beans.Attribute.select;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(GOALS_FEATURE)
@DisplayName("Цель «FORM_OFFER_CONTENT_READY», при успешной публикации оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormOfferContentReadyGoalTest {

    private Categories category = UMNIE_KOLONKI;

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

        offerAddSteps.setCookie(CLASSIFIED_REGION_ID, "65");
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CONTENT_READY)
    @DisplayName("Цель «FORM_OFFER_CONTENT_READY», при успешной публикации оффера")
    public void shouldSeeFormOfferContentReadyGoal() {
        urlSteps.testing().path(FORM).open();
        offerAddSteps.withCategory(category).withAttributes(
                multiselect("Тип питания").setValues("от сети"),
                select("Голосовой помощник").setValue("Apple Siri")).addOffer();

        goalsSteps.withGoalType(FORM_OFFER_CONTENT_READY)
                .withPageRef(urlSteps.testing().path(ADD).path(category.getCategoryPath()).toString())
                .withCount(1)
                .shouldExist();
    }

}
