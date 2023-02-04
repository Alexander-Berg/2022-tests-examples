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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.FILTERS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - карточка модели")
@Feature(AutoruFeatures.CATALOG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ModelCardTest {

    private static final String MARK = "vaz";
    private static final String MODEL = "granta";
    private static final String LAST_GENERATION = "21377296";
    private static final String OTHER_GENERATION = "I";
    private static final String OTHER_GENERATION_ID = "7684102";
    private static final String GENERATION_USED = "21377296";
    private static final String LAST_BODY = "21377430";
    private static final String BODY = "21377432";
    private static final String GENERATION_NEW = "21377296";
    private static final String BODY_NEW = "21575582";

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchVazGranta",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();

        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(SLASH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбор поколения в фильтре")
    public void shouldSelectGeneration() {
        basePageSteps.onCatalogModelPage().filter().select("Выбрать поколение").click();
        basePageSteps.onCatalogModelPage().dropdown().item(OTHER_GENERATION).waitUntil(isDisplayed()).click();
        urlSteps.path(OTHER_GENERATION_ID).ignoreParam("cookiesync").path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Все параметры»")
    public void shouldClickAllParamsButton() {
        basePageSteps.onCatalogModelPage().filter().allParamsButton().should(isDisplayed()).click();
        urlSteps.path(FILTERS).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Галерея")
    public void shouldSeeGallery() {
        basePageSteps.onCatalogModelPage().gallery().should(isDisplayed());
        basePageSteps.onCatalogModelPage().gallery().img()
                .should(hasAttribute("src",
                        format("%s/%s", urlSteps.getConfig().getAvatarsURI(),
                                "get-verba/997355/2a00000165898a82003ad7707972d4f9ad54/cattouchret")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по последнему поколению")
    public void shouldClickLastGeneration() {
        basePageSteps.onCatalogModelPage().gallery().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(LAST_GENERATION)
                .path(LAST_BODY).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение сниппета кузова")
    public void shouldSeeBodySnippet() {
        basePageSteps.onCatalogModelPage().getBody(0)
                .should(hasText(startsWith("1/12\nУниверсал 5 дв. Cross\n141 новая")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кузову")
    @Category({Regression.class, Testing.class})
    public void shouldClickBody() {
        basePageSteps.onCatalogModelPage().getBody(1).url().click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(LAST_GENERATION).path(BODY).path(SLASH)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке на новые объявления кузова")
    @Category({Regression.class, Testing.class})
    public void shouldClickBodyNewSalesUrl() {
        basePageSteps.onCatalogModelPage().getBody(0).newSalesUrl().hover().click();
        urlSteps.shouldUrl(anyOf(
                equalTo(urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL)
                        .path(format("/%s-%s/", GENERATION_NEW, BODY_NEW))
                        .addParam("from", "single_group_snippet_listing").toString()),
                equalTo(urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(NEW).toString())));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке на б/у объявления кузова")
    @Category({Regression.class, Testing.class})
    public void shouldClickBodyUsedSalesUrl() {
        basePageSteps.onCatalogModelPage().getBody(1).usedSalesUrl().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(GENERATION_USED).path(BODY)
                .path(USED).path(SLASH).shouldNotSeeDiff();
    }
}
