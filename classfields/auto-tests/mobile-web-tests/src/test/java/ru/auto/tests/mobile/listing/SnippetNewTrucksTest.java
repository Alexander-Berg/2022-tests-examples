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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Листинг - телефон")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SnippetNewTrucksTest {

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
        mockRule.newMock().with("desktop/SearchTrucksBreadcrumbsEmpty",
                "mobile/SearchTrucksNew",
                "desktop/OfferTrucksPhones").post();

        urlSteps.testing().path(MOSKVA).path(TRUCK).path(NEW).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Контакты»")
    public void shouldClickContactsButton() {
        basePageSteps.onListingPage().getSale(0).button("Контакты").click();
        basePageSteps.onListingPage().popup().waitUntil(hasText(matchesPattern("Авто-М Hyundai Подольск\n" +
                "Официальный дилер\n• На Авто.ру \\d+ (год|года|лет)\nПодольск, Домодедовское шоссе, 5. " +
                "На карте\n станция Подольск\nДоехать с Яндекс.Такси\nЗаказать обратный звонок\nПозвонить\n" +
                "Авто-М Hyundai Подольск · Официальный дилер · c 9:00 до 20:00")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Позвонить»")
    public void shouldClickCallButton() {
        basePageSteps.onListingPage().getSale(0).callButton().hover().click();
        basePageSteps.onListingPage().popup().waitUntil(hasText("Телефон\n+7 916 039-84-27\nс 10:00 до 23:00\n" +
                "+7 916 039-84-28\nс 12:00 до 20:00"));
    }
}
