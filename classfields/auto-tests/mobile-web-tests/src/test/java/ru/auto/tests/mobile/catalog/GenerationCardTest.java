package ru.auto.tests.mobile.catalog;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.FILTERS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - карточка поколения")
@Feature(AutoruFeatures.CATALOG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GenerationCardTest {

    private static final String MARK = "kia";
    private static final String MODEL = "ceed";
    private static final String GENERATION = "21212472";
    private static final Integer BODIES_CNT = 2;
    private static final String BODY = "универсал 5 дв.";
    private static final String BODY_ID = "21212618";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION).path("/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Галерея")
    public void shouldSeeGallery() {
        basePageSteps.onCatalogGenerationPage().gallery().should(isDisplayed());
        basePageSteps.onCatalogGenerationPage().gallery().img()
                .should(hasAttribute("src",
                        format("%s/%s", urlSteps.getConfig().getAvatarsURI(),
                                "get-verba/937147/2a00000161ffde06f655bce76d5c78e69e1b/cattouch")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение сниппета кузова")
    public void shouldSeeBodySnippet() {
        basePageSteps.onCatalogGenerationPage().getBody(0).should(hasText(startsWith("1/20\nУниверсал 5 дв.\n")));
        basePageSteps.onCatalogGenerationPage().getBody(0).image().should(hasAttribute("src",
                format("%s/%s", urlSteps.getConfig().getAvatarsURI(),
                        "get-verba/937147/2a00000161ffde06f655bce76d5c78e69e1b/cattouch")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор кузова в фильтре")
    @Category({Regression.class})
    public void shouldSelectBody() {
        basePageSteps.onCatalogGenerationPage().filter().select("Выбрать кузов").should(isDisplayed()).click();
        basePageSteps.onCatalogGenerationPage().dropdown().item(BODY).waitUntil(isDisplayed()).click();
        urlSteps.path(BODY_ID).path("/").ignoreParam("cookiesync").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Все параметры»")
    @Category({Regression.class})
    public void shouldClickAllParamsButton() {
        basePageSteps.onCatalogGenerationPage().filter().allParamsButton().should(isDisplayed()).click();
        urlSteps.path(FILTERS).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по поколению")
    @Category({Regression.class})
    public void shouldClickLastGeneration() {
        basePageSteps.onCatalogGenerationPage().gallery().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION)
                .path(BODY_ID).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кузову")
    @Category({Regression.class})
    public void shouldClickBody() {
        basePageSteps.onCatalogGenerationPage().bodiesList()
                .should(hasSize(BODIES_CNT)).get(0).url().click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION)
                .path(BODY_ID).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке на новые объявления кузова")
    @Category({Regression.class})
    public void shouldClickBodyNewSalesUrl() {
        basePageSteps.onCatalogGenerationPage().bodiesList().should(hasSize(BODIES_CNT)).get(0).newSalesUrl()
                .waitUntil(isDisplayed()).click();
        urlSteps.shouldUrl(anyOf(
                equalTo(urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL)
                        .path(format("/%s-%s/", GENERATION, BODY_ID))
                        .addParam("from", "single_group_snippet_listing").toString()),
                equalTo(urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(NEW).toString())));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке на б/у объявления кузова")
    @Category({Regression.class})
    public void shouldClickBodyUsedSalesUrl() {
        basePageSteps.onCatalogGenerationPage().bodiesList().should(hasSize(BODIES_CNT)).get(0).usedSalesUrl()
                .waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(GENERATION).path(BODY_ID).path(USED)
                .path("/").shouldNotSeeDiff();
    }
}
