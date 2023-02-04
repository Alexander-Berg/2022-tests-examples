package ru.yandex.realty.promo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.test.api.realty.promocode.CreatePromoBody;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.PromocodesSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.defaultPromo;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.promoConstrains;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.promoFeature;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.PROMOCODES;
import static ru.yandex.realty.page.PromocodePage.FAILED_MESSAGE;
import static ru.yandex.realty.page.PromocodePage.MONEY;
import static ru.yandex.realty.page.PromocodePage.PREMIUM;
import static ru.yandex.realty.page.PromocodePage.RISING;
import static ru.yandex.realty.page.PromocodePage.SUCCESS_MESSAGE;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kurau (Yuri Kalinin)
 */
@DisplayName("Используем промокоды")
@Feature(PROMOCODES)
@Story("Использование")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class UsePromocodesTest {

    private static final long DEFAULT_PROMO_BONUS = 3;
    private static final long TWO_ACTIVATIONS = 2;
    private static final long ONE_ACTIVATION = 1;
    private static final int LIFETIME = 20;
    private static final String ONLY_FOR_RAISING = "raising";
    private static final String ONLY_FOR_PREMIUM = "premium";
    private static final String ONLY_FOR_MONEY = "money";

    private String promoName;
    private CreatePromoBody promoBody;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PromocodesSteps promoSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private Account account;

    @Before
    public void openWallet() {
        promoName = getRandomString();
        promoBody = defaultPromo().withCode(promoName);

        apiSteps.createVos2Account(account, OWNER);
        urlSteps.testing().path(Pages.PROMOCODES).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Используем промокод для «Поднятия»")
    public void shouldUseForRaising() {
        apiSteps.createPromocode(promoBody.withFeatures(asList(promoFeature()
                .withCount(DEFAULT_PROMO_BONUS)
                .withTag(ONLY_FOR_RAISING))));
        promoSteps.usePromoCode(promoName);
        promoSteps.onPromoPage().alert(SUCCESS_MESSAGE).should(isDisplayed());
        promoSteps.refresh();

        promoSteps.onPromoPage().promocodeItem(RISING).amount()
                .should(hasText(containsString(valueOf(DEFAULT_PROMO_BONUS))));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Используем промокод для «Премиума»")
    public void shouldUseForPremium() {
        apiSteps.createPromocode(promoBody.withFeatures(asList(promoFeature()
                .withCount(DEFAULT_PROMO_BONUS)
                .withTag(ONLY_FOR_PREMIUM))));
        promoSteps.usePromoCode(promoName);
        promoSteps.onPromoPage().alert(SUCCESS_MESSAGE).should(isDisplayed());
        promoSteps.refresh();

        promoSteps.onPromoPage().promocodeItem(PREMIUM).amount()
                .should(hasText(containsString(valueOf(DEFAULT_PROMO_BONUS))));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Используем промокод для «Денег»")
    public void shouldUseForMoney() {
        apiSteps.createPromocode(promoBody.withFeatures(asList(promoFeature()
                .withCount(DEFAULT_PROMO_BONUS)
                .withTag(ONLY_FOR_MONEY))));
        promoSteps.usePromoCode(promoName);
        promoSteps.onPromoPage().alert(SUCCESS_MESSAGE).should(isDisplayed());
        promoSteps.refresh();

        promoSteps.onPromoPage().promocodeItem(MONEY).amount()
                .should(hasText(containsString(valueOf(DEFAULT_PROMO_BONUS))));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Одноразовый код можем применить только один раз")
    public void shouldUseOnlyOne() {
        apiSteps.createPromocode(promoBody.withFeatures(asList(promoFeature()
                .withCount(DEFAULT_PROMO_BONUS)
                .withTag(ONLY_FOR_MONEY)))
                .withConstraints(promoConstrains().withUserActivations(ONE_ACTIVATION)));
        promoSteps.usePromoCode(promoName);

        urlSteps.open();
        promoSteps.usePromoCode(promoName);
        promoSteps.onPromoPage().alert(FAILED_MESSAGE).should(isDisplayed());

        urlSteps.open();
        promoSteps.onPromoPage().promocodeItem(MONEY).amount()
                .should(hasText(containsString(valueOf(DEFAULT_PROMO_BONUS))));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Многоразовый код применяем дважды")
    public void shouldUseTwice() {
        apiSteps.createPromocode(promoBody.withFeatures(asList(promoFeature()
                .withCount(DEFAULT_PROMO_BONUS)
                .withTag(ONLY_FOR_MONEY)))
                .withConstraints(promoConstrains().withUserActivations(TWO_ACTIVATIONS)));
        promoSteps.usePromoCode(promoName);
        promoSteps.onPromoPage().alert(SUCCESS_MESSAGE).should(isDisplayed());
        promoSteps.refresh();
        promoSteps.onPromoPage().promocodeItem(MONEY).amount()
                .waitUntil(hasText(containsString(valueOf(DEFAULT_PROMO_BONUS))));
        urlSteps.open();
        promoSteps.usePromoCode(promoName);
        promoSteps.onPromoPage().alert(SUCCESS_MESSAGE).should(isDisplayed());
        promoSteps.refresh();
        promoSteps.onPromoPage().promocodeItem(MONEY).amount()
                .should(hasText(containsString(valueOf(DEFAULT_PROMO_BONUS * TWO_ACTIVATIONS))));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Создаем протухающий через 20 секунд промокод")
    public void shouldMakeExpiredPromo() {
        apiSteps.createPromocode(promoBody.withFeatures(asList(promoFeature()
                .withCount(DEFAULT_PROMO_BONUS)
                .withTag(ONLY_FOR_PREMIUM))));
        promoSteps.usePromoCode(promoName);
        promoSteps.onPromoPage().alert(SUCCESS_MESSAGE).should(isDisplayed());
        basePageSteps.refresh();
        promoSteps.onPromoPage().promocodeItem(PREMIUM).amount()
                .waitUntil(hasText(containsString(valueOf(DEFAULT_PROMO_BONUS))));

        basePageSteps.refresh();
        promoName = getRandomString();
        promoBody = defaultPromo().withCode(promoName);
        apiSteps.createPromocode(promoBody.withFeatures(asList(promoFeature()
                .withTag(ONLY_FOR_PREMIUM).withLifetime(String.format("%d seconds", LIFETIME)))));
        promoSteps.usePromoCode(promoName);
        promoSteps.onPromoPage().alert(SUCCESS_MESSAGE).should(isDisplayed());
        promoSteps.refresh();
        promoSteps.onPromoPage().promocodeItem(PREMIUM).amount()
                .waitUntil(hasText(containsString(valueOf(DEFAULT_PROMO_BONUS * 2))));

        basePageSteps.refresh();
        waitSomething(LIFETIME + 1, TimeUnit.SECONDS);
        basePageSteps.refresh();
        assertThat("Количество промокодов должно упасть до 3 из-за протухания промокодов",
                promoSteps.onPromoPage().promocodeItem(PREMIUM).amount().getText(),
                containsString(valueOf(DEFAULT_PROMO_BONUS)));
    }
}
