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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - подменный номер под незарегом")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ContactsRedirectPhonesUnregMotoTest {

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
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferMotoUsedUser",
                "desktop/OfferMotoPhones").post();

        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение контактов объявления")
    public void shouldSeeContacts() {
        basePageSteps.onCardPage().contacts().showPhoneButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().activePopupCloser().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().contacts().should(hasText("Частное лицо\nМоскваПоселок Остров\nНаписать\n" +
                "+7 916 039-84-27\nc 10:00 до 23:00\nЕщё 1 телефон"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа контактов")
    public void shouldSeeContactsPopup() {
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().waitUntil(isDisplayed())
                .should(hasText("Частное лицо\nЧастное лицо\n+7 916 039-84-27\nc 10:00 до 23:00\n+7 916 039-84-28\n" +
                        "c 12:00 до 20:00\nНомер защищён от спама\nSMS и сообщения в мессенджерах доставлены не будут, " +
                        "звоните.\nТолько частникам\nВладелец просит автосалоны и перепродавцов машин не беспокоить.\n" +
                        "Внимание\nПриобретая ТС, никогда не отправляйте предоплату.Подробнее\nМоскваПоселок Остров\n" +
                        "Harley-Davidson Dyna Super Glide • 530 000 ₽\n1 584 см³ / 75 л.с. / 4 такта\n2 цилиндра / " +
                        "v-образное\n6 передач\nремень\nчёрный\nЗаметка об этом мотоцикле (её увидите только вы)"));
    }
}