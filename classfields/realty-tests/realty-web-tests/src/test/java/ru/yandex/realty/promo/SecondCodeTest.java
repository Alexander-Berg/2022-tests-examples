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
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.PromocodesSteps;
import ru.yandex.realty.step.UrlSteps;

import java.time.LocalDate;
import java.util.Locale;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.defaultPromo;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.promoConstrains;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.promoFeature;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.PROMOCODES;
import static ru.yandex.realty.page.PromocodePage.MONEY;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kurau (Yuri Kalinin)
 */
@DisplayName("Используем несколько промокодов")
@Feature(PROMOCODES)
@Story("Использование")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SecondCodeTest {

    private static final long DEFAULT_PROMO_BONUS = 3;
    private static final long DEFAULT_PERIOD = 3;
    private static final long SECOND_BONUS = 4;
    private static final long DURATION = 5;

    private static final String ONLY_FOR_MONEY = "money";

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
        apiSteps.createVos2Account(account, OWNER);

        String firstCode = getRandomString();
        apiSteps.createPromocode(defaultPromo().withCode(firstCode).withFeatures(asList(promoFeature()
                .withCount(DEFAULT_PROMO_BONUS)
                .withTag(ONLY_FOR_MONEY)))
                .withConstraints(promoConstrains()));
        apiSteps.applyPromocode(firstCode, account.getId());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Применяем код, ждём пока он закончится и смотрим, что он отменился")
    public void shouldRemoveOldCode() throws InterruptedException {
        String secondCode = getRandomString();
        apiSteps.createPromocode(defaultPromo().withCode(secondCode).withFeatures(asList(promoFeature()
                .withCount(DEFAULT_PROMO_BONUS).withLifetime(format("%d seconds", DURATION))
                .withTag(ONLY_FOR_MONEY)))
                .withConstraints(promoConstrains()));
        apiSteps.applyPromocode(secondCode, account.getId());

        Thread.sleep(SECONDS.toMillis(DURATION));

        urlSteps.testing().path(Pages.PROMOCODES).open();
        promoSteps.onPromoPage().promocodeItem(MONEY).amount()
                .should(hasText(containsString(String.valueOf(DEFAULT_PROMO_BONUS))));
        promoSteps.onPromoPage().promocodeItem(MONEY).deadline()
                .should(hasItem(hasText(toDate(DEFAULT_PERIOD))));

    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Добавляем два разных бонуса, смотрим, что отображается бонус и его меньшая длительность")
    public void shouldAddSeparateCodes() {
        String secondCode = getRandomString();
        apiSteps.createPromocode(defaultPromo().withCode(secondCode).withFeatures(asList(promoFeature()
                .withCount(SECOND_BONUS).withLifetime("10 days")
                .withTag(ONLY_FOR_MONEY)))
                .withConstraints(promoConstrains()));
        apiSteps.applyPromocode(secondCode, account.getId());

        urlSteps.testing().path(Pages.PROMOCODES).open();
        promoSteps.onPromoPage().promocodeItem(MONEY).amount()
                .should(hasText(containsString(String.valueOf(DEFAULT_PROMO_BONUS + SECOND_BONUS))));
        promoSteps.onPromoPage().promocodeItem(MONEY).deadline()
                .should(hasItem(hasText(toDate(DEFAULT_PERIOD))));
    }

    private String toDate(long plusDay) {
        return ofPattern("до d MMMM").withLocale(new Locale("ru")).format(LocalDate.now().plusDays(plusDay));
    }
}
