package ru.auto.tests.mobile.sale;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - преимущества")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class BenefitsTest {

    private static final String MARK = "land_rover";
    private static final String MODEL = "discovery";
    private static final String GEN = "2307388";

    private static final String SALE_ID = "/1076842087-f1e84/";
    private String saleUrl;

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
                stub("desktop/OfferCarsUsedUser"),
                stub("desktop/CarfaxOfferCarsRawNotPaid")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        saleUrl = urlSteps.getCurrentUrl();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение преимуществ")
    public void shouldSeeBenefits() {
        basePageSteps.onCardPage().benefits().should(hasText("Только на Авто.ру\nЭксклюзивное предложение\nОнлайн-показ\n" +
                "По видеосвязи\nПродаёт собственник\nДокументы проверены\nДТП не найдены\nПо данным ГИБДД\n1 владелец\n" +
                "По информации из ПТС\nНа гарантии\nДо января 2030\nПочти как новый\nУчитывая возраст и пробег\n4,6/5\n" +
                "Рейтинг модели на Авто.ру"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("ДТП не найдены")
    public void shouldSeeNoAccidents() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().benefits().benefit("ДТП не\u00a0найдены"));
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("ДТП не найдены\nПо данным ГИБДД " +
                "автомобиль с этим VIN-номером официально не регистрировался в ДТП\nСмотреть полный отчёт"));
        basePageSteps.onCardPage().popup().button("Смотреть полный отчёт").click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN).addParam("r", encode(saleUrl)).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("1 владелец")
    public void shouldSeeOneOwner() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().benefits().benefit("1 владелец"));
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("1 владелец — физическое лицо\n" +
                "1 владелец\nС апреля 2006 г.\nАвтомобиль покупался и использовался одним владельцем. " +
                "Это значит, что продавец знает всю его историю, а при необходимости сможет прокомментировать " +
                "любой пункт.\nСмотреть полный отчёт"));
        basePageSteps.onCardPage().popup().button("Смотреть полный отчёт").click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN).addParam("r", encode(saleUrl)).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("На гарантии")
    public void shouldSeeWarranty() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().benefits().benefit("На гарантии"));
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("На гарантии до января 2030\n" +
                "Это значит, что вы сможете устранять возможные неисправности за счёт производителя"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Почти как новый")
    public void shouldSeeAlmostNew() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().benefits().benefit("Почти как новый"));
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Почти как новый\nЭтот автомобиль " +
                "произведён 14 лет назад, имеет пробег ниже среднего, а также по информации ГИБДД не участвовал " +
                "в серьёзных ДТП"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Рейтинг модели")
    public void shouldSeeRating() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().benefits().benefit("Рейтинг модели"));
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed())
                .should(hasText("4,6\n/ 5\nРейтинг по 102 отзывам\nСмотреть все отзывы"));
        basePageSteps.onCardPage().popup().button("Смотреть все отзывы").should(hasAttribute
                ("href", urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).path(GEN).path("/")
                        .addParam("from", "card").toString()));
        basePageSteps.onCardPage().popup().button("Смотреть все отзывы").click();
        urlSteps.testing().path(REVIEWS).addParam("from", "card").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Онлайн-показ")
    public void shouldSeeOnline() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().benefits().benefit("Онлайн-показ"));
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed())
                .should(hasText("Онлайн-показ\nПродавец готов показать автомобиль онлайн. Свяжитесь с ним и вместе " +
                        "выберите удобный сервис для видеозвонка.\nНаписать в чат\nПозвонить"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Продаёт собственник")
    public void shouldSeeProvenOwner() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().benefits().benefit("Продаёт собственник"));
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Продаёт собственник\nМы получили " +
                "копии СТС и водительского удостоверения от продавца автомобиля\nКак пройти проверку"));
        basePageSteps.onCardPage().popup().button("Как пройти проверку").click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(format("%s?action=send_proven_message", saleUrl))).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Написать в чат» в онлайн-показе")
    public void shouldClickChatButton() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().benefits().benefit("Онлайн-показ"));
        basePageSteps.onCardPage().popup().button("Написать в чат").click();

        basePageSteps.onCardPage().authPopup().should(isDisplayed());
        basePageSteps.onCardPage().authPopup().iframe()
                .should(hasAttribute("src", containsString(
                        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                                .addParam("r", encode(saleUrl))
                                .addParam("inModal", "true")
                                .addParam("autoLogin", "true")
                                .addParam("welcomeTitle", "")
                                .toString()
                )));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Только на Авто.ру")
    public void shouldSeeAutoruOnly() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().benefits().benefit("Только на\u00a0Авто.ру"));
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed())
                .should(hasText("Только на Авто.ру\nНе пропустите — продавец указал, что объявление размещается " +
                        "только на Авто.ру\n\nПодробнее"));
    }
}
