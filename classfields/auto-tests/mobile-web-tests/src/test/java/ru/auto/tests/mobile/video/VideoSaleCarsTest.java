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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Pages.VIDEO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.VIDEO)
@DisplayName("Блок видео на карточке объявления")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VideoSaleCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String MARK = "land_rover";
    private static final String MODEL = "discovery";
    private static final String GENERATION = "2307388";

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
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "mobile/VideoSearchCarsLandRover").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().footer().hover();

    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока «Популярные видео»")
    public void shouldSeePopularVideos() {
        basePageSteps.onCardPage().videos().videosList().should(hasSize(2)).forEach(item -> item.should(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Все видео»")
    public void shouldClickAllVideosUrl() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().videos().button("Все видео"));
        urlSteps.testing().path(VIDEO).path(CARS).path(MARK.toLowerCase()).path(MODEL).path(GENERATION).path("/")
                .addParam("from", "card").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по видео")
    @Category({Regression.class, Testing.class})
    public void shouldClickVideo() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().videos().getVideo(0)
                .should(hasText("20:14\nЭтот внедорожник я хотел купить. land rover discovery 3. тест.")));
        basePageSteps.onCardPage().videoFrame().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Закрытие видео")
    @Category({Regression.class, Testing.class})
    public void shouldCloseVideo() {
        basePageSteps.onCardPage().videos().getVideo(0).waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().videoCloseIcon().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().videoFrame().waitUntil(not(isDisplayed()));
    }
}
