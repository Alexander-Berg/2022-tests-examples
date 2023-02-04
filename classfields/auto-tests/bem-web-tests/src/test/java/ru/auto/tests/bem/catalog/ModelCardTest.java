package ru.auto.tests.bem.catalog;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.CatalogPageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@DisplayName("Каталог - карточка модели")
@Feature(AutoruFeatures.CATALOG)
public class ModelCardTest {

    private static final String MARK = "vaz";
    private static final String MODEL = "kalina";
    private static final String GENERATION_SEARCHER = "9389448";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public CatalogPageSteps catalogPageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор поколения")
    public void shouldSelectGeneration() {
        catalogPageSteps.onCatalogPage().filter().markModelGenBlock().breadcrumbsItem("Выбрать поколение ")
                .should(isDisplayed()).click();
        catalogPageSteps.selectFirstGen();
        urlSteps.path(GENERATION_SEARCHER).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Добавить отзыв»")
    @Category({Regression.class})
    public void shouldClickAddOpinionButton() {
        catalogPageSteps.onCatalogPage().opinionsBlock().addOpinion().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(CARS).path(REVIEWS).path(ADD).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Смотреть все» в отзывах")
    @Category({Regression.class})
    public void shouldClickShowAllOpinionsButton() {
        catalogPageSteps.onCatalogPage().opinionsBlock().showAllOpinions().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по отзыву")
    @Category({Regression.class})
    public void shouldClickOpinion() {
        String opinionTitle = catalogPageSteps.onCatalogPage().opinionsBlock().opinionsList()
                .should(hasSize(greaterThan(0)))
                .get(0).titleUrl().getText();
        catalogPageSteps.onCatalogPage().opinionsBlock().opinionsList().get(0).click();
        catalogPageSteps.switchToNextTab();
        catalogPageSteps.onBasePage().h2().should(hasText(opinionTitle));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по табу «Характеристики»")
    @Category({Regression.class})
    public void shouldClickSpecifications() {
        catalogPageSteps.onCatalogPage().tab("Характеристики").waitUntil(isDisplayed()).click();

        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(SPECIFICATIONS).shouldNotSeeDiff();
    }

}
