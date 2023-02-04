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

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления дилера - контакты")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ContactsDealerTrucksTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferTrucksUsedDealer",
                "desktop/OfferTrucksPhones").post();

        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по названию дилера")
    public void shouldClickDealerName() {
        basePageSteps.onCardPage().contacts().sellerName().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DILER).path(TRUCK).path(USED).path("/trak_platforma/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа контактов")
    public void shouldSeeContactsPopup() {
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().waitUntil(isDisplayed()).should(hasText("ТРАК ПЛАТФОРМА\nАвтосалон\n" +
                "+7 916 039-84-27\nРоман\nc 10:00 до 23:00\n+7 916 039-84-28\nДмитрий\nc 12:00 до 20:00\nЛюберцы" +
                "Транспортная улица, 16\nDAF XF 105 • 2 700 000 ₽\nбортовой грузовик\nг/п 16.3 т\n12.0 л / 462 л.с. / " +
                "Дизель\n2-х местная с 2 спальными\n6x2\nжёлтый\nмеханика\nЗаметка об этом грузовике " +
                "(её увидите только вы)"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по адресу в поп-апе")
    public void shouldClickShowAddress() {
        basePageSteps.onCardPage().contacts().showPhoneButton().should(isDisplayed()).click();
        basePageSteps.onCardPage().contactsPopup().address().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().yandexMap().waitUntil("Не подгрузились Яндекс.Карты", isDisplayed(), 10);
    }
}