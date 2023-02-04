package ru.auto.tests.desktop.vin;

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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Объявление - блок «История размещения» под владельцем")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VinSaleOwnerNotPaidTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUserOwner",
                "desktop/CarfaxOfferCarsRawNotPaid").post();

        cookieSteps.setCookieForBaseDomain("promo_popup_history_seller_closed", "true");
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.scrollDown(1000);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class, Regression.class})
    @DisplayName("Отображение блока под владельцем (отчёт не куплен)")
    public void shouldSeeCardReport() {
        basePageSteps.onCardPage().vinReport().waitUntil(isDisplayed()).should(hasText("Отчёт о проверке по VIN\nОбновлён 1 февраля 2021\nВы " +
                "можете оставить комментарии к данным в отчёте.\nКомментарии увидят пользователи, купившие полный отчёт.\n" +
                "Характеристики совпадают с ПТС\nДанные о розыске и запрете на регистрацию появятся позже\n3 владельца в ПТС\n" +
                "Комментировать\n2 записи в истории пробегов\n2 записи в истории эксплуатации\nHD фотографии\n" +
                "Поиск данных о залоге\nПоиск оценок стоимости ремонта\nПроверка на работу в такси\nЕще 10 пунктов проверки\nПоказать " +
                "бесплатный отчёт\nКупить отчёт от 99 ₽"));
    }
}
