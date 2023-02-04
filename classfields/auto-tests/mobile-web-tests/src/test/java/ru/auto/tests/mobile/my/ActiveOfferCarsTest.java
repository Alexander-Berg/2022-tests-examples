package ru.auto.tests.mobile.my;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.FROM_WEB_TO_APP;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("ЛК - активное объявление легковых")
@Epic(LK)
@Feature(SALES)
@Story("Отображение активного объявления")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ActiveOfferCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                "desktop/UserOffersCarsActive").post();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение объявления")
    @Owner(DSVICHIHIN)
    public void shouldSeeSale() {
        basePageSteps.onLkPage().getSale(0).should(hasText("Audi A3 III (8V) Рестайлинг\n700 000 ₽\nДобавьте панораму. " +
                "Вы получите до 2,5 раз больше звонков.\nДа, продаю\n?\nСнять с продажи\nРедактировать\nПоднять в " +
                "поиске за 69 ₽\n-40%\nТурбо-продажа\n20 просмотров · Включено 3 опции · 3 дня\n581 ₽\n349 ₽\n-40%" +
                "\nЭкспресс-продажа\n5 просмотров · Включено 2 опции · 6 дней\n411 ₽\n247 ₽\nПоказ в Историях" +
                "\nДействует 3 дня\n4 194 ₽\nПоднятие в ТОП\nДействует 3 дня\n199 ₽\nВыделение цветом\nДействует 3 дня" +
                "\n69 ₽\nСпецпредложение\nДействует 3 дня\n197 ₽"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        mockRule.with("desktop/OfferCarsUsedUser").update();

        basePageSteps.onLkPage().getSale(0).link().click();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/land_rover/discovery/").path(SALE_ID)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Редактировать»")
    public void shouldClickEditButton() {
        basePageSteps.onLkPage().getSale(0).button("Редактировать").click();
        urlSteps.testing().path(PROMO).path(FROM_WEB_TO_APP).addParam("action", "edit").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Добавить панораму»")
    public void shouldClickAddPanoramaButton() {
        basePageSteps.onLkPage().getSale(0).addPanoramaButton().click();
        basePageSteps.onLkPage().popup().waitUntil(isDisplayed()).should(hasText("С панорамой — больше звонков " +
                "до 2,5 раз. Добавить её можно в приложении Авто.ру\n360°\nУстановить приложение\nПозже"));
        basePageSteps.onLkPage().popup().button("Позже").click();
        basePageSteps.onLkPage().popup().waitUntil(not(isDisplayed()));
    }

}
