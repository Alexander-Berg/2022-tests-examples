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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.DesktopConfig.LISTING_TOP_SALES_CNT;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.AUTO_SNIPPET;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.FROM;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - сниппет объявления дилера в легковых")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SnippetDealerCarsCarouselTest {

    private static final String DEALER_CODE = "borishof_balashiha_bmw";

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
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/SearchCarsAll")
        ).create();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam(OUTPUT_TYPE, CAROUSEL).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение авто сниппета диллера, тип листинга «Карусель»")
    public void shouldSeeSnippet() {
        basePageSteps.onListingPage().getCarouselSale(LISTING_TOP_SALES_CNT).should(hasText("BMW 5 серия 520d VII " +
                "(G30/G31)\n2.0 л\n190 л.с.\nдизель\nавтомат\nседан\nзадний\n520d\n55 опций\nНовый\n2019\n" +
                "2 109 877 ₽\nПоказать телефон\nПерезвоните мне\nБорисХоф BMW Восток\nБалашиха"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на страницу дилера, авто сниппет, тип листинга «Карусель»")
    public void shouldClickDealerUrl() {
        basePageSteps.onListingPage().getCarouselSale(LISTING_TOP_SALES_CNT).dealerUrl().waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path(DEALER_CODE).path(SLASH)
                .addParam(FROM, AUTO_SNIPPET).shouldNotSeeDiff();
    }

}
