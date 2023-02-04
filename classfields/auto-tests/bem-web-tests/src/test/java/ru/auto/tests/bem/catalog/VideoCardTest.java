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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.VIDEO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.VIDEO)
@DisplayName("Блок видео")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VideoCardTest {

    private static final String MARK = "/vaz/";
    private static final String MODEL = "/vesta/";
    private static final String GENERATION = "/20417749/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with(
                "desktop/SearchCarsBreadcrumbsRid213",
                "desktop/SessionUnauth",
                "desktop/VideoSearchCarsVazVesta",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();

        urlSteps.testing().path(CATALOG).path(MARK).path(MODEL).path(GENERATION).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Видео в блоке «Популярные видео»")
    public void shouldSeePopularVideos() {
        basePageSteps.onCatalogPage().videos().title().should(hasText("Популярные видео с LADA (ВАЗ) Vesta I"));
        basePageSteps.onCatalogPage().videos().videosList().should(hasSize(greaterThan(0))).forEach(item ->
                item.should(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Все видео»")
    public void shouldClickAllVideosUrl() {
        basePageSteps.onCatalogPage().videos().button("Все видео").waitUntil(isDisplayed()).click();
        urlSteps.testing().path(VIDEO).path(CARS).path(MARK).path(MODEL).path(GENERATION).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по видео")
    public void shouldClickVideo() {
        basePageSteps.onCatalogPage().videos().getVideo(0).waitUntil(isDisplayed()).click();
        basePageSteps.onCatalogPage().activePopup().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрытие видео")
    public void shouldCloseVideo() {
        basePageSteps.onCatalogPage().videos().getVideo(0).waitUntil(isDisplayed()).click();
        basePageSteps.onCatalogPage().activePopup().waitUntil(isDisplayed());
        basePageSteps.onCatalogPage().activePopupCloser().waitUntil(isDisplayed()).click();
        basePageSteps.onCatalogPage().activePopup().waitUntil(not(isDisplayed()));
    }
}