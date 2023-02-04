package ru.yandex.realty.promo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
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
import ru.yandex.realty.step.PromocodesSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.defaultPromo;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.promoConstrains;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.promoFeature;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.PROMOCODES;
import static ru.yandex.realty.page.PromocodePage.EXPIRE_MESSAGE;
import static ru.yandex.realty.page.PromocodePage.FAILED_MESSAGE;
import static ru.yandex.realty.page.PromocodePage.PREMIUM;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by kopitsa on 12.07.17.
 */

@DisplayName("Используем нерабочие промокоды")
@Feature(PROMOCODES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class FailingPromoTest {

    private static final String ONLY_FOR_PREMIUM = "premium";
    private static final String OUTDATED_DEADLINE = "2017-01-15T00:00:00+03:00";
    private static final String DEFAULT_PROMO_BONUS = "0";
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
    @Owner(KOPITSA)
    @DisplayName("Создаем и используем истекший промокод")
    public void shouldMakeExpiredPromo() {
        apiSteps.createPromocode(promoBody.withFeatures(asList(promoFeature()
                .withTag(ONLY_FOR_PREMIUM)))
                .withConstraints(promoConstrains().withDeadline(OUTDATED_DEADLINE)));
        promoSteps.usePromoCode(promoName);
        promoSteps.onPromoPage().alert(EXPIRE_MESSAGE).should(isDisplayed());
        promoSteps.onPromoPage().promocodeItem(PREMIUM).amount()
                .should(hasText(containsString(valueOf(DEFAULT_PROMO_BONUS))));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Создаем и используем промокод с ограничением по черному списку")
    public void shouldMakeBlacklistPromo() {
        apiSteps.createPromocode(promoBody.withFeatures(asList(promoFeature()
                .withTag(ONLY_FOR_PREMIUM)))
                .withConstraints(promoConstrains().withBlacklist(asList(account.getId()))));
        promoSteps.usePromoCode(promoName);
        promoSteps.onPromoPage().alert(FAILED_MESSAGE).should(isDisplayed());
        promoSteps.onPromoPage().promocodeItem(PREMIUM).amount()
                .should(hasText(containsString(valueOf(DEFAULT_PROMO_BONUS))));
    }
}
