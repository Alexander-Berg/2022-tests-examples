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

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - подменный номер под незарегом")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ContactsRealPhonesUnregTrucksTest {

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
        mockRule.newMock().with("desktop/OfferTrucksUsedUser",
                "desktop/OfferTrucksPhonesRedirectFalse").post();

        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение контактов объявления")
    public void shouldSeeContacts() {
        basePageSteps.onCardPage().contacts().showPhoneButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().activePopupCloser().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().contacts().should(hasText("Юрий\n Улица ГорчаковаБульвар адмирала УшаковаМоскварайон" +
                " Южное Бутово\nНаписать\n+7 916 039-50-85\nКруглосуточно\nЕщё 1 телефон"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа контактов")
    public void shouldSeeContactsPopup() {
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().time().click();
        basePageSteps.onCardPage().contactsPopup().waitUntil(isDisplayed()).should(hasText("Юрий\nЧастное лицо\n" +
                "+7 916 039-50-85\nКруглосуточно\n+7 916 039-50-90\nКруглосуточно\nТолько частникам\nВладелец просит " +
                "автосалоны и перепродавцов машин не беспокоить.\nВнимание\nПриобретая ТС, никогда не отправляйте " +
                "предоплату.Подробнее\n Улица ГорчаковаБульвар адмирала УшаковаМоскварайон Южное Бутово\nЗИЛ " +
                "5301 \"Бычок\" • 250 000 ₽\nфургон\n4.5 л\nбелый\nЗаметка об этом грузовике (её увидите только вы)"));
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