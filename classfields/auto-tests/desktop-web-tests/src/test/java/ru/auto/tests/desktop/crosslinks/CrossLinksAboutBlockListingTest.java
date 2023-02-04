package ru.auto.tests.desktop.crosslinks;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - блок перелинковки")
@Feature(LISTING)
@Story("Блок перелинковки «Все о Toyota Corolla»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CrossLinksAboutBlockListingTest {

    private static final String MARK = "Toyota";
    private static final String MODEL = "Corolla";
    private static final String TEXT = "Всё о Toyota Corolla\nРейтинг модели — 4.6 / 5\nКаталог Toyota Corolla\n" +
            "Отзывы о Toyota Corolla\nСтатистика цен Toyota Corolla\nПродать Toyota Corolla";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionUnauth"),
                stub("desktop/SearchCarsMarkModel"),
                stub("desktop/SearchCarsBreadcrumbsToyotaCorolla"),
                stub("desktop/ReviewsAutoCarsRatingToyotaCorolla")).create();

        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase()).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока")
    public void shouldSeeBlock() {
        basePageSteps.onListingPage().crossLinksBlock().waitUntil(isDisplayed()).should(hasText(TEXT));
        basePageSteps.onListingPage().crossLinksBlock().title().should(hasText(format("Всё о %s %s", MARK, MODEL)));
        basePageSteps.onListingPage().crossLinksBlock().rating().should(hasText("Рейтинг модели — 4.6 / 5"));
    }
}
