package ru.auto.tests.mobile.video;

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

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ABOUT;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.VIDEO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок видео на группе новых - о модели")
@Feature(AutoruFeatures.VIDEO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VideoGroupAboutModelTest {

    private static final String MARK = "kia";
    private static final String MODEL = "optima";
    private static final String GENERATION = "21342050";
    private static final String PATH = "/21342050-21342121/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("mobile/SearchCarsBreadcrumbsMarkModelGroup",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree",
                "desktop/ReferenceCatalogCarsConfigurationsGallery",
                "desktop/VideoSearchCarsKiaOptimaPageSize6").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL).path(PATH).path(ABOUT).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока «Все видео о модели»")
    public void shouldSeeModelVideos() {
        basePageSteps.onGroupAboutModelPage().videos().videosList().should(hasSize(6))
                .forEach(item -> item.should(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Все видео о модели»")
    public void shouldClickModelVideosUrl() {
        basePageSteps.onGroupAboutModelPage().videos().button("Видео о модели").click();
        urlSteps.testing().path(VIDEO).path(CARS).path(MARK.toLowerCase()).path(MODEL).path(GENERATION).path("/")
                .addParam("from", "card-group").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Все видео»")
    public void shouldClickAllVideosUrl() {
        basePageSteps.onGroupAboutModelPage().videos().button("Все видео").click();
        urlSteps.testing().path(VIDEO).path(CARS).path(MARK.toLowerCase()).path(MODEL).path(GENERATION).path("/")
                .addParam("from", "card-group").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по видео")
    @Category({Regression.class, Testing.class})
    public void shouldClickVideo() {
        basePageSteps.onGroupAboutModelPage().videos().getVideo(0)
                .should(hasText("12:46\nТест драйв KIA Optima 2018 - САМ БЫ НЕ Купил, НО Брать Можно. " +
                        "Замер разгона 0-100")).click();
        basePageSteps.onGroupAboutModelPage().videoFrame().waitUntil(isDisplayed());
    }
}
