package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - подменный номер под незарегом")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ContactsRedirectPhonesUnregCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/OfferCarsUsedUserWithAllowedSafeDeal"),
                stub("desktop/OfferCarsPhones")).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение контактов объявления")
    public void shouldSeeContacts() {
        basePageSteps.onCardPage().contacts().showPhoneButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().activePopupCloser().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().contacts().should(hasText("Федор\nМоскваметро Марьино\nНачать сделку\nНаписать\n" +
                "+7 916 039-84-27\nc 10:00 до 23:00\nЕщё 1 телефон"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа контактов")
    public void shouldSeeContactsPopup() {
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().time().click();
        basePageSteps.onCardPage().contactsPopup().waitUntil(isDisplayed()).should(hasText("Федор\nЧастное лицо" +
                "\n+7 916 039-84-27\nc 10:00 до 23:00\n+7 916 039-84-28\nc 12:00 до 20:00\nПредложите Безопасную сделку" +
                "\nМы пересмотрели сделку купли-продажи автомобиля, разложили её по полочкам и перенесли в онлайн " +
                "для вашего удобства и безопасности. Подробнее\nНомер защищён от спама\nSMS и сообщения в мессенджерах " +
                "доставлены не будут, звоните.\nТолько частникам\nВладелец просит автосалоны и перепродавцов машин " +
                "не беспокоить.\nВнимание\nПриобретая ТС, никогда не отправляйте предоплату.Подробнее\nМоскваметро " +
                "Марьино\nLand Rover Discovery III • 700 000 ₽\n2.7 л / 190 л.с. / Дизель\nавтомат" +
                "\nвнедорожник 5 дв.\nполный\nсеребристый\nЗаметка об этом автомобиле (её увидите только вы)"));
    }
}