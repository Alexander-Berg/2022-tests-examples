package ru.auto.tests.mobile.listing;

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
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - предложения дня")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class DailyOffersCarsTest {

    private static final int DAILY_OFFERS_COUNT = 5;

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
                "mobile/SearchCarsNew",
                "mobile/SearchCarsNewPageSize5",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/OfferCarsPhones").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение предложений дня")
    public void shouldSeeDailyOffers() {
        basePageSteps.onListingPage().dailyOffers().dailyOffersList().should(hasSize(DAILY_OFFERS_COUNT))
                .forEach(item -> item.waitUntil(isDisplayed()));

        basePageSteps.onListingPage().dailyOffers().title().waitUntil(hasText("Предложения дня"));
        basePageSteps.onListingPage().dailyOffers().getDailyOffer(0).waitUntil(hasText("BMW X1\n3 090 000 ₽\nНовый\n" +
                "18d xDrive 2.0d AT (150 л.с.) 4WD\nxDrive18d M Sport\nБорисХоф BMW Восток\nКонтакты"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по предложению")
    public void shouldClickOffer() {
        mockRule.with("desktop/OfferCarsNewDealer").update();

        basePageSteps.onListingPage().dailyOffers().getDailyOffer(0).click();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path("/kia/optima/21342125/21342344/1076842087-f1e84/")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Контакты»")
    public void shouldClickContactsButton() {
        basePageSteps.onListingPage().dailyOffers().getDailyOffer(0).button("Контакты").click();
        basePageSteps.onListingPage().popup().waitUntil(hasText(matchesPattern("БорисХоф BMW Восток\n" +
                "Официальный дилер\n• На Авто.ру \\d+ (год|года|лет)\nБалашиха, г. Москва мкрн ЦОВБ 21. На карте\n" +
                "Подписаться на объявления\nДоехать с Яндекс.Такси\n539 авто в наличии\nЗаказать обратный звонок\n" +
                "Позвонить\nБорисХоф BMW Восток · Официальный дилер · c 9:00 до 21:00")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по иконке телефона")
    public void shouldClickPhoneIcon() {
        basePageSteps.onListingPage().dailyOffers().getDailyOffer(0).phoneIcon().click();
        basePageSteps.onListingPage().popup().waitUntil(hasText("Телефон\n+7 916 039-84-27\nс 10:00 до 23:00\n" +
                "+7 916 039-84-28\nс 12:00 до 20:00"));
    }
}
