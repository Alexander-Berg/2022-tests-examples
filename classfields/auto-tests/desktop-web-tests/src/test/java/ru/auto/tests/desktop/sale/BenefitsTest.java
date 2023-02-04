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
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - преимущества")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class BenefitsTest {

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
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/SessionUnauth").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение преимуществ")
    public void shouldSeeBenefits() {
        basePageSteps.onCardPage().benefits().should(hasText("Только на Авто.ру\nЭксклюзивное предложение\nОнлайн-показ\n" +
                "По видеосвязи\nЕщё 6 преимуществ"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Онлайн-показ")
    public void shouldSeeOnline() {
        basePageSteps.onCardPage().benefits().benefit("Онлайн-показ").hover();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Продавец готов показать автомобиль " +
                "онлайн. Свяжитесь с ним и вместе выберите удобный сервис для видеозвонка."));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Продаёт собственник")
    public void shouldSeeProvenOwner() {
        basePageSteps.onCardPage().benefits().button("Ещё 6\u00a0преимуществ").click();
        basePageSteps.onCardPage().benefits().benefit("Продаёт собственник").hover();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Мы получили копии СТС " +
                "и водительского удостоверения от продавца автомобиля\n\nКак пройти проверку"));
        basePageSteps.onCardPage().popup().button("Как пройти проверку").click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Покупатели ценят автомобили " +
                "от собственников\nРасскажите всем, что вы один из них — получите бейджик «Продаёт собственник».\n\n" +
                "Скачайте приложение Авто.ру и ищите в сообщениях чат с поддержкой. Разбудите бота любым приветствием, " +
                "а потом выберите команду «Хочу стать проверенным собственником».\nAppStore\nGoogle Play\nAppGallery\n" +
                "Наведите камеру смартфона на QR код, чтобы скачать или открыть приложение"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("ДТП не найдены")
    public void shouldSeeNoAccidents() {
        basePageSteps.onCardPage().benefits().button("Ещё 6\u00a0преимуществ").click();
        basePageSteps.onCardPage().benefits().benefit("ДТП не\u00a0найдены").click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("По данным ГИБДД автомобиль с этим " +
                "VIN-номером официально не регистрировался в ДТП"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("1 владелец")
    public void shouldSeeOneOwner() {
        basePageSteps.onCardPage().benefits().button("Ещё 6\u00a0преимуществ").click();
        basePageSteps.onCardPage().benefits().benefit("1 владелец").click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("1 владелец\nАвтомобиль покупался " +
                "и использовался одним владельцем. Это значит, что продавец знает всю его историю, а при необходимости " +
                "сможет прокомментировать любой пункт отчёта по VIN."));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("На гарантии")
    public void shouldSeeWarranty() {
        basePageSteps.onCardPage().benefits().button("Ещё 6\u00a0преимуществ").click();
        basePageSteps.onCardPage().benefits().benefit("На гарантии").hover();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Это значит, что вы сможете устранять " +
                "возможные неисправности за счёт производителя"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Почти как новый")
    public void shouldSeeAlmostNew() {
        basePageSteps.onCardPage().benefits().button("Ещё 6\u00a0преимуществ").click();
        basePageSteps.onCardPage().benefits().benefit("Почти как новый").hover();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Этот автомобиль произведён 14 лет " +
                "назад, имеет пробег ниже среднего, а также по информации ГИБДД не участвовал в серьёзных ДТП"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Только на Auto.ru")
    public void shouldSeeAutoruOnly() {
        basePageSteps.onCardPage().benefits().button("Ещё 6\u00a0преимуществ").click();
        basePageSteps.onCardPage().benefits().benefit("Только на\u00a0Авто.ру").hover();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Только на Авто.ру\n" +
                "Не пропустите — продавец указал, что объявление размещается только на Авто.ру\n\nПодробнее"));
    }
}
