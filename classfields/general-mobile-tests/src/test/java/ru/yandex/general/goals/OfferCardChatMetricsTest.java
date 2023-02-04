package ru.yandex.general.goals;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.beans.metrics.EcommerceRequestBody;
import ru.yandex.general.mobile.step.OfferAddSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.yandex.general.beans.metrics.EcommerceEvent.ecommerceEvent;
import static ru.yandex.general.beans.metrics.EcommerceRequestBody.ecommerceRequestBody;
import static ru.yandex.general.beans.metrics.EventAction.eventAction;
import static ru.yandex.general.beans.metrics.Product.product;
import static ru.yandex.general.beans.metrics.Ym.ym;
import static ru.yandex.general.consts.FormConstants.Categories.PERENOSKA;
import static ru.yandex.general.consts.GeneralFeatures.CHAT_INIT_PURCHASE;
import static ru.yandex.general.consts.GeneralFeatures.ECOMMERCE_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.CHAT_INIT;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.mobile.page.OfferCardPage.WRITE;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.mobile.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Метрики чата на карточке оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class OfferCardChatMetricsTest {

    private static final String PRICE = "32000";
    private static final String TITLE = "Переноска";
    private static final String PARENT_CATEGORY = "Животные и товары для них";
    private static final String CATEGORY = "Транспортировка, переноски";

    private static final String JSONPATHS_TO_IGNORE = "__ym.ecommerce[0].purchase.actionField";

    private EcommerceRequestBody ecommerceRequestBody;
    private String offerId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.createAccountAndLogin();
        urlSteps.testing().path(FORM).open();
        offerAddSteps.withCategory(PERENOSKA)
                .withPrice(PRICE).addOffer();
        offerAddSteps.onOfferCardPage().successPublishMessage().waitUntil(isDisplayed());
        offerId = urlSteps.getOfferId();

        offerAddSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        passportSteps.createAccountAndLogin();
        urlSteps.testing().path(CARD).path(offerId).open();
        goalsSteps.clearHar();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);
        offerAddSteps.onOfferCardPage().button(WRITE).click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Epic(ECOMMERCE_FEATURE)
    @Ignore("CLASSBACK-1609")
    @Feature(CHAT_INIT_PURCHASE)
    @DisplayName("Событие «PURCHASE» при инициализации чата на карточке")
    public void shouldSeePurchaseEcommerceEventOnChatInit() {
        ecommerceRequestBody = ecommerceRequestBody().setYm(ym().setEcommerce(asList(
                ecommerceEvent().setPurchase(eventAction().setProducts(asList(
                        product().setId(offerId)
                                .setPrice(PRICE)
                                .setName(TITLE).setCategory(format("%s/%s", PARENT_CATEGORY, CATEGORY))))))));

        goalsSteps.withPageUrl(urlSteps.getCurrentUrl())
                .withCount(1)
                .withBody(ecommerceRequestBody)
                .withEcommercePurchase()
                .withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Epic(GOALS_FEATURE)
    @Feature(CHAT_INIT)
    @Ignore("CLASSBACK-1609")
    @DisplayName("Цель «CHAT_INIT» при инициализации чата на карточке")
    public void shouldSeeChatInitGoal() {
        goalsSteps.withGoalType(CHAT_INIT)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Epic(ECOMMERCE_FEATURE)
    @Feature(CHAT_INIT_PURCHASE)
    @Ignore("CLASSBACK-1609")
    @DisplayName("Нет события «PURCHASE» для старого чата на карточке")
    public void shouldNotSeePurchaseEcommerceEventOnChatInit() {
        goalsSteps.withPageUrl(urlSteps.getCurrentUrl())
                .withEcommercePurchase()
                .withCount(1)
                .shouldExist();

        offerAddSteps.refresh();
        goalsSteps.clearHar();
        offerAddSteps.onOfferCardPage().button(WRITE).click();

        goalsSteps.withPageUrl(urlSteps.getCurrentUrl())
                .withEcommercePurchase()
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Epic(GOALS_FEATURE)
    @Feature(CHAT_INIT)
    @Ignore("CLASSBACK-1609")
    @DisplayName("Нет цели «CHAT_INIT» для старого чата на карточке")
    public void shouldNotSeeChatInitGoal() {
        goalsSteps.withGoalType(CHAT_INIT)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();

        offerAddSteps.refresh();
        goalsSteps.clearHar();
        offerAddSteps.onOfferCardPage().button(WRITE).click();

        goalsSteps.withGoalType(CHAT_INIT)
                .withCurrentPageRef()
                .withCount(0)
                .shouldExist();
    }

}
