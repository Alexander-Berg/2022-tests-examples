package ru.auto.tests.mobile.listing;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.mobile.page.ListingPage.TOP_SALES_COUNT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - галерея сниппета")
@Feature(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(MobileTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SnippetGalleryContactsTrucksTest {

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {TRUCK}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchTrucksBreadcrumbsEmpty",
                "mobile/SearchTrucksAll",
                "desktop/OfferTrucksPhones").post();

        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();
        basePageSteps.onListingPage().getSale(TOP_SALES_COUNT).hover();
        basePageSteps.onListingPage().getSale(TOP_SALES_COUNT).gallery().contacts()
                .should(hasText("GROSS AUTO\nАвтосалон")).click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Контакты")
    public void shouldSeeContacts() {
        basePageSteps.onListingPage().popup().waitUntil(hasText(matchesPattern("GROSS AUTO\nАвтосалон • " +
                "На Авто.ру \\d+ лет\nМосква, Россия, Московская область, Ленинский район, Видное, Южная промзона. " +
                "На карте\n ДомодедовскаяОрехово\nДоехать с Яндекс.Такси\nЗаказать обратный звонок\nПозвонить\n" +
                "GROSS AUTO · Автосалон · c 8:00 до 23:00")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Позвонить»")
    public void shouldClickCallButton() {
        basePageSteps.onListingPage().popup().button("Позвонить").click();
        basePageSteps.onListingPage().popup().waitUntil(hasText("Телефон\n+7 916 039-84-27\nс 10:00 до 23:00\n" +
                "+7 916 039-84-28\nс 12:00 до 20:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Доехать с Яндекс.Такси»")
    public void shouldClickTaxiButton() {
        basePageSteps.onListingPage().popup().button("Доехать\u00a0с\u00a0Яндекс.Такси").click();
        basePageSteps.switchToNextTab();
        urlSteps.shouldUrl(containsString("redirect.appmetrica.yandex.com/route?end-lat"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «на карте»")
    public void shouldClickMapButton() {
        basePageSteps.onListingPage().popup().button("На\u00a0карте").click();
        basePageSteps.onListingPage().yandexMap().waitUntil(isDisplayed());
    }
}
