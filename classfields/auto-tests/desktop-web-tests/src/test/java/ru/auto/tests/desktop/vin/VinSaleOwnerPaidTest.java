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

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Объявление - блок «История размещения» под владельцем")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VinSaleOwnerPaidTest {

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
        mockRule.newMock().with("desktop/OfferCarsUsedUserOwner",
                "desktop/SessionAuthUser",
                "desktop/CarfaxOfferCarsRawPaid").post();

        cookieSteps.setCookieForBaseDomain("promo_popup_history_seller_closed", "true");
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class, Regression.class})
    @DisplayName("Отображение блока под владельцем (отчёт куплен)")
    public void shouldSeeCardReport() {
        basePageSteps.onCardPage().vinReport().should(hasText("Отчёт о проверке по VIN\nОбновлён 18 сентября 2019\n87\n" +
                "Оценка\nВы можете оставить комментарии к данным в отчёте.\nКомментарии увидят пользователи, " +
                "купившие полный отчёт.\nДанные расходятся с ПТС\nИнформация об участии в 1 ДТП\n" +
                "Юридические ограничения не найдены\n4 владельца в ПТС\n2 записи в истории пробегов\n" +
                "Отзывные кампании не найдены\n3 записи в истории эксплуатации\nЕщё 1 размещение на Авто.ру\n" +
                "Расчёт транспортного налога\nЕще 1 пункт проверки\nПоказать бесплатный отчёт\n" +
                "Посмотреть полный отчёт"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class, Regression.class})
    @DisplayName("Скролл к отчёту")
    public void shouldScrollToReport() {
        urlSteps.addParam("action", "showVinReport").open();
        waitSomething(2, TimeUnit.SECONDS);
        assertThat("Не произошел скролл к отчёту", basePageSteps.getPageYOffset() > 0);
    }
}