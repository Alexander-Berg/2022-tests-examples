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
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - подменный номер под незарегом")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ContactsRealPhonesUnregMotoTest {

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
                "desktop/OfferMotoPhonesRedirectFalse").post();

        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение контактов объявления")
    public void shouldSeeContacts() {
        basePageSteps.onCardPage().contacts().showPhoneButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().activePopupCloser().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().contacts().should(hasText("Частное лицо\nМоскваПоселок Остров\n" +
                "Написать\n+7 916 356-52-48\nКруглосуточно\nЕщё 1 телефон"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа контактов")
    public void shouldSeeContactsPopup() {
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().time().click();
        basePageSteps.onCardPage().contactsPopup().waitUntil(isDisplayed())
                .should(hasText("Частное лицо\nЧастное лицо\n+7 916 356-52-48\nКруглосуточно\n+7 916 356-52-50\n" +
                        "Круглосуточно\nТолько частникам\nВладелец просит автосалоны и перепродавцов машин не " +
                        "беспокоить.\nВнимание\nПриобретая ТС, никогда не отправляйте предоплату.Подробнее\nМосква" +
                        "Поселок Остров\nHarley-Davidson Dyna Super Glide • 530 000 ₽\n1 584 см³ / 75 л.с. / 4 такта\n" +
                        "2 цилиндра / v-образное\n6 передач\nремень\nчёрный\nЗаметка об этом мотоцикле " +
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