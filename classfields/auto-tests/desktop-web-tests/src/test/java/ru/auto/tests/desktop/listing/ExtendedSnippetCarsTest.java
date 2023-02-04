package ru.auto.tests.desktop.listing;

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
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - расширенный сниппет дилера в легковых")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ExtendedSnippetCarsTest {

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsExtended",
                "desktop/OfferCarsPhones").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam("autoru_billing_service_type", "extended").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение сниппета")
    public void shouldSeeSnippet() {
        basePageSteps.onListingPage().getSale(0).should(hasText(matchesPattern("Mercedes-Benz E-Класс 200 V " +
                "\\(W213, S213, C238\\)\n2.0 л / 197 л.с. / Бензин\nавтомат\nкупе\nполный\n" +
                "E 200 4MATIC Sport\n65 опций\n4 498 200 ₽\n2020\\nНовый\\nВ наличии\\nАвтофорум Mercedes-Benz\n " +
                "МедведковоБабушкинская\nМосква\nНа Авто.ру \\d+ (года|лет)\n429 авто в наличии\nПоказать телефон")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        basePageSteps.onListingPage().getSale(0).nameLink().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP)
                .path("/mercedes/e_klasse/21662665/21228969/1076842087-f1e84/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефонов")
    public void shouldSeePhones() {
        basePageSteps.onListingPage().getSale(0).showPhonesButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().contactsPopup().waitUntil(hasText("Автофорум Mercedes-Benz\nОфициальный дилер " +
                "Mercedes-Benz\nПодписаться\n+7 916 039-84-27\nРоман\nc 10:00 до 23:00\n+7 916 039-84-28\nДмитрий\n" +
                "c 12:00 до 20:00\n МедведковоБабушкинскаяМоскваМКАД, 92-й километр\nMercedes-Benz E-Класс 200 V " +
                "(W213, S213, C238) • 4 498 200 ₽\n2.0 л / 197 л.с. / Бензин\nавтомат\nкупе\nполный\nE 200 4MATIC Sport\n" +
                "65 опций\nЗаметка об этом автомобиле (её увидите только вы)"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «На Авто.ру с N года»")
    public void shouldClickAutoruYearUrl() {
        basePageSteps.onListingPage().getSale(0).autoruYearUrl().should(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path("/avtoforum_moskva_mercedes/")
                .addParam("from", "auto-snippet").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «В наличии N т.с.»")
    public void shouldClickInStockUrl() {
        basePageSteps.onListingPage().getSale(0).inStockUrl().should(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path("/avtoforum_moskva_mercedes/")
                .addParam("from", "auto-snippet").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по названию дилера")
    public void shouldClickDealerUrl() {
        basePageSteps.onListingPage().getSale(0).dealerUrl().should(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path("/avtoforum_moskva_mercedes/")
                .addParam("from", "auto-snippet").shouldNotSeeDiff();
    }
}