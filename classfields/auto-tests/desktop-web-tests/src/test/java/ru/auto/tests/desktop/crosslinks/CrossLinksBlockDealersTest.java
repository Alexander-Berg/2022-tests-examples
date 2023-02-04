package ru.auto.tests.desktop.crosslinks;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Дилеры - листинг - блок перелинковки")
@Feature(AutoruFeatures.DEALERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CrossLinksBlockDealersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String mark;

    @Parameterized.Parameter(1)
    public String title;

    @Parameterized.Parameter(2)
    public String rating;

    @Parameterized.Parameter(3)
    public String catalogLinkName;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"toyota", "Всё о Toyota", "Рейтинг марки — 4.3 / 5", "Каталог Toyota"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/AutoruBreadcrumbsToyota",
                "desktop/AutoruDealerToyota",
                "desktop/ReviewsAutoCarsRatingToyota").post();

        urlSteps.testing().path(DILERY).path(CARS).path(mark).path(NEW).open();
        basePageSteps.onDealerListingPage().crossLinksBlock().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заголовок блока перелинковки")
    public void shouldSeeBlockTitle() {
        basePageSteps.onDealerListingPage().crossLinksBlock().title().should(hasText(title));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Рейтинг марки / модели")
    public void shouldSeeRating() {
        basePageSteps.onDealerListingPage().crossLinksBlock().rating().should(hasText(rating));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке в блоке")
    public void shouldClickLink() {
        basePageSteps.onDealerListingPage().crossLinksBlock().getLink(0).should(hasText(catalogLinkName)).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(mark).path("/").shouldNotSeeDiff();
    }
}