package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - «Сейчас дилер не работает»")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ContactsDealerSleepTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String PHONE = "9111111111";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedDealerSleep",
                "desktop/OfferCarsPhones",
                "desktop/OfferCarsRegisterCallbackForNewCategory",
                "desktop/AuthLoginOrRegister",
                "desktop/UserConfirm").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Ignore // TODO будет новый дизайн, пока убрали
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение плашки «Сейчас дилер не работает»")
    public void shouldSeeDealerSleepTip() {
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().waitUntil(isDisplayed()).should(hasText("FAVORIT MOTORS Юг\n" +
                "Роман\n+7 916 039-84-27\nc 10:00 до 23:00\nДмитрий\n+7 916 039-84-28\nc 12:00 до 20:00\nВ избранное\n" +
                "Пожаловаться\nСейчас дилер не работает\nОставьте ваш телефон, и он перезвонит вам в рабочее время\n" +
                "Номер телефона\nПодтвердить номер\nЗаметка об этом автомобиле (её увидите только вы)\n " +
                "ПражскаяЮжнаяМосква\nПоказать адрес\nПодписаться на объявления"));
    }

    @Test
    @Ignore // TODO будет новый дизайн, пока убрали
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заказ обратного звонка")
    public void shouldSendDealerCallbackUnreg() {
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().dealerSleepBlock().input("Номер телефона").sendKeys(PHONE);
        basePageSteps.onCardPage().contactsPopup().dealerSleepBlock().submitButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().contactsPopup().dealerSleepBlock().input("Код из SMS")
                .sendKeys("1234");
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Заявка отправлена"));
        basePageSteps.onCardPage().contactsPopup().dealerSleepBlock().waitUntil(not(isDisplayed()));
    }
}