package ru.yandex.realty.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.yandex.realty.page.PromocodePage;

import static java.lang.String.format;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.PROMOCODES;
import static ru.yandex.realty.page.PromocodePage.MONEY;

/**
 * @author kurau (Yuri Kalinin)
 */
public class PromocodesSteps extends WebDriverSteps {

    public static final String EASY_MONEY_2000 = "easy-money-2000";
    private static final String DEFAULT_PROMO_BONUS = "2 000";

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    public PromocodePage onPromoPage() {
        return on(PromocodePage.class);
    }

    @Step("Вводим промокод со страницы")
    public PromocodesSteps usePromoCode(String promoName) {
        onPromoPage().promoInput().waitUntil("Не появилось поле для ввода промокода", isDisplayed())
                .sendKeys(promoName);
        onPromoPage().addPromoButton().click();
        return this;
    }

    @Step("Добавляем 2000 на счет промокодом")
    public void use2000Promo() {
        urlSteps.testing().path(MANAGEMENT_NEW).path(PROMOCODES).open();
        usePromoCode(EASY_MONEY_2000);
        basePageSteps.refreshUntil(() -> onPromoPage().promocodeItem(MONEY), hasText(containsString(DEFAULT_PROMO_BONUS)));
    }

    @Step("Добавляем 2000 на счет промокодом, {times} раз(а)")
    public void use2000Promo(int times) {
        for (int i = 0; i < times; i++) {
            urlSteps.testing().path(MANAGEMENT_NEW).path(PROMOCODES).open();
            usePromoCode(EASY_MONEY_2000);
        }
        basePageSteps.refreshUntil(() -> onPromoPage().promocodeItem(MONEY),
                hasText(containsString(format("%s 000", times * 2))));
    }
}
