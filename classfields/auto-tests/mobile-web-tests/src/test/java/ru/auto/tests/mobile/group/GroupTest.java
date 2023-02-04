package ru.auto.tests.mobile.group;

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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ABOUT;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Групповая карточка")
@Feature(AutoruFeatures.GROUP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GroupTest {

    private static final String PATH = "/kia/optima/21342050-21342121/";
    private static final String SALE_PATH = "/kia/optima/21342125/21342381/1076842087-f1e84/";
    private static final int PAGE_SIZE = 10;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("mobile/SearchCarsBreadcrumbsMarkModelGroup"),
                stub("mobile/SearchCarsGroupContextGroup"),
                stub("mobile/SearchCarsGroupContextGroupInStock"),
                stub("mobile/SearchCarsGroupContextListing"),
                stub("desktop/ReferenceCatalogCarsConfigurationsSubtree"),
                stub("desktop/OfferCarsPhones")
        ).create();

        basePageSteps.setWindowHeight(1000);
        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение группы")
    public void shouldSeeGroup() {
        basePageSteps.onGroupPage().groupHeader().should(hasText("Новые Kia Optima IV Рестайлинг\nот 1 169 400 ₽\n" +
                "Получить лучшую цену\nПодбор дилеров с лучшими предложениями\nПоделиться\nО модели"));
        basePageSteps.onGroupPage().footer().should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @DisplayName("Отображение сниппета")
    public void shouldSeeSnippet() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onGroupPage().getSale(0));

        urlSteps.onCurrentUrl().setProduction().open();
        basePageSteps.hideElement(basePageSteps.onFavoritesPage().devToolsBranch());
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onGroupPage().getSale(0));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по сниппету")
    public void shouldClickSnippet() {
        mockRule.setStubs(stub("desktop/OfferCarsNewDealer")).update();

        basePageSteps.onGroupPage().getSale(0).header().click();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(SALE_PATH).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по комплектации на сниппете")
    public void shouldClickSnippetComplectation() {
        mockRule.setStubs(stub("desktop/OfferCarsNewDealer")).update();

        basePageSteps.onGroupPage().getSale(0).title().click();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(SALE_PATH).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Контакты»")
    public void shouldClickContactsButton() {
        basePageSteps.onGroupPage().getSale(0).button("Контакты").click();
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onGroupPage().popup().waitUntil(isDisplayed())
                .should(hasText(anyOf(matchesPattern("АвтоГЕРМЕС KIA Рябиновая\n" +
                                "Официальный дилер\n• На Авто.ру \\d+ (год|года|лет)\nМосква, Рябиновая улица, 43Б. На карте\n" +
                                "Подписаться на объявления\nДоехать с Яндекс.Такси\n62 авто в наличии\n" +
                                "Заказать обратный звонок\nПозвонить\nАвтоГЕРМЕС KIA Рябиновая · Официальный дилер · " +
                                "c 9:00 до 21:00"),
                        matchesPattern("АвтоГЕРМЕС KIA Рябиновая\nОфициальный дилер\n• " +
                                "На Авто.ру \\d+ (год|года|лет)\nМосква, Рябиновая улица, 43Б. На карте\nПодписаться на объявления\n" +
                                "Доехать с Яндекс.Такси\n62 авто в наличии\nЗаказать обратный звонок\nПозвонить\n" +
                                "АвтоГЕРМЕС KIA Рябиновая · Официальный дилер · c 9:00 до 21:00 · АвтоГЕРМЕС KIA Рябиновая · " +
                                "Официальный дилер · c 9:00 до 21:00"))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Позвонить»")
    public void shouldClickCallButton() {
        basePageSteps.onGroupPage().getSale(0).callButton().click();
        basePageSteps.onGroupPage().popup().waitUntil(isDisplayed()).should(hasText("Телефон\n+7 916 039-84-27\n" +
                "с 10:00 до 23:00\n+7 916 039-84-28\nс 12:00 до 20:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Позвонить» в контактах")
    public void shouldClickContactsCallButton() {
        basePageSteps.onGroupPage().getSale(0).button("Контакты").click();
        basePageSteps.onGroupPage().popup().button("Позвонить").click();
        basePageSteps.onGroupPage().popup().waitUntil(isDisplayed()).should(hasText("Телефон\n+7 916 039-84-27\n" +
                "с 10:00 до 23:00\n+7 916 039-84-28\nс 12:00 до 20:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Написать» в контактах")
    public void shouldClickContactsSendMessageButton() {
        String groupUrl = urlSteps.getCurrentUrl();

        basePageSteps.onGroupPage().getSale(0).button("Контакты").click();
        basePageSteps.onGroupPage().popup().sendMessageButton().click();

        basePageSteps.onCardPage().authPopup().should(isDisplayed());
        basePageSteps.onCardPage().authPopup().iframe()
                .should(hasAttribute("src", containsString(
                        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                                .addParam("r", encode(groupUrl))
                                .addParam("inModal", "true")
                                .addParam("autoLogin", "true")
                                .addParam("welcomeTitle", "")
                                .toString()
                )));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подгрузка предложений")
    public void shouldLoadMoreOffers() {
        mockRule.setStubs(stub("mobile/SearchCarsGroupContextGroupPage2")).update();

        basePageSteps.onGroupPage().salesList().should(hasSize(PAGE_SIZE));
        basePageSteps.onGroupPage().getSale(PAGE_SIZE - 1).hover();
        basePageSteps.onGroupPage().salesList().waitUntil(hasSize(PAGE_SIZE * 2));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «О модели»")
    public void shouldClickAboutModelButton() {
        basePageSteps.onGroupPage().aboutModelButton().click();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).path(ABOUT).shouldNotSeeDiff();
    }
}
