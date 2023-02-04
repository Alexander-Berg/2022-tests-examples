package ru.auto.tests.mobile.my;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.TestData.OWNER_USER_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("Личный кабинет - забаненное объявление")
@Epic(LK)
@Feature(SALES)
@Story("Забаненное объявление")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class LkBannedSaleTest {

    private static final String CARS_SALE_TEXT = "LADA (ВАЗ) 2106\n45 000 ₽\nЗаблокировано модератором\nПричины блокировки\nНеобходимо подтверждение\nМы полагаем, что вы пытались продать несуществующий автомобиль. Если это не так и вы можете подтвердить, что владеете им, напишите в службу поддержки.\nУдалить\nНаписать в поддержку";
    private static final String COMMERCIAL_SALE_TEXT = "Peugeot Boxer\n700 000 ₽\nЗаблокировано модератором\nПричины блокировки\nНомер телефона в описании\nУдалите номер телефона из Описания. Для него предусмотрено отдельное поле Номер телефона — там он защищен от спама, а покупателям будет удобно позвонить вам.\nРедактировать\nУдалить\nНаписать в поддержку";
    private static final String MOTO_SALE_TEXT = "ЗиД Сова\n25 000 ₽\nЗаблокировано модератором\nПричины блокировки\nПродажа ТС без документов\nВы пытались продать транспортное средство без документов, имеющее запрет на переоформление или утилизированное по данным ГИБДД. Это запрещено правилами Авто.ру.\nУдалить\nНаписать в поддержку";

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

    @Inject
    private LoginSteps loginSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String text;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, CARS_SALE_TEXT},
                {TRUCKS, COMMERCIAL_SALE_TEXT},
                {MOTO, MOTO_SALE_TEXT}
        });
    }

    @Before
    public void before() throws IOException {
        mockRule.newMock().with("mobile/UserOffersCarsBanned",
                "mobile/UserOffersTrucksBanned",
                "mobile/UserOffersMotoBanned",
                "desktop/UserOffersCarsDelete",
                "desktop/UserOffersTrucksDelete",
                "desktop/UserOffersMotoDelete",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(MY).path(category).open();
        loginSteps.loginAs(OWNER_USER_PROVIDER.get());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение плашки «Заблокировано модератором»")
    public void shouldSeeBannedMessage() {
        basePageSteps.onLkPage().getSale(0).should(hasText(text));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Написать в поддержку»")
    public void shouldClickSupportButton() {
        waitSomething(3, TimeUnit.SECONDS);
        basePageSteps.onLkPage().getSale(0).supportButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkPage().chat().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление оффера")
    public void shouldSeeRemovedOffer() {
        basePageSteps.onLkPage().getSale(0).button("Удалить").click();
        basePageSteps.acceptAlert();
        basePageSteps.onLkPage().notifier().waitUntil(isDisplayed()).should(hasText("Объявление удалено"));
    }
}
