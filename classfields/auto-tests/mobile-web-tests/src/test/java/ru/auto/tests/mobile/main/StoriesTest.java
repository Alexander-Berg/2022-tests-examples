package ru.auto.tests.mobile.main;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - истории")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class StoriesTest {

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
                stub("mobile/StorySearch"),
                stub("mobile/Story")
        ).create();

        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Отображение блока с историями")
    public void shouldSeeStoriesBlock() {
        basePageSteps.onMainPage().stories().storiesList().should(hasSize(4));
        basePageSteps.onMainPage().stories().should(hasText("Золотая карта от Колесо.ру в " +
                "подарок\nВот это машина!\nМировые новинки недели\nРоссийские новинки недели"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Клик по истории")
    public void shouldClickInStory() {
        basePageSteps.onMainPage().stories().getStory(0).should(hasText("Золотая карта от Колесо.ру в подарок"));
        basePageSteps.onMainPage().stories().getStory(0).click();
        basePageSteps.onMainPage().storiesGallery().should(isDisplayed())
                .should(hasText("Золотая карта от Колесо.ру\nСкидка 25% на любые услуги и 3% на отдельные " +
                        "товары. Поставьте машину в Гараж, чтобы получить карту\nПолучить скидку"));
        basePageSteps.onMainPage().storiesGallery().progressBarItems().should(hasSize(5));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Клик по кнопке внутри истории")
    public void shouldClickOnStoryButton() {
        basePageSteps.onMainPage().stories().getStory(0).click();
        basePageSteps.onMainPage().storiesGallery().button("Получить скидку").click();

        urlSteps.mobileURI().path(GARAGE).addParam("from", "story_koleso")
                .addParam("promo", "koleso").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Закрытие галереи с историей")
    public void shouldCloseStoryGallery() {
        basePageSteps.onMainPage().stories().getStory(0).click();
        basePageSteps.onMainPage().storiesGallery().closeButton().click();

        basePageSteps.onMainPage().storiesGallery().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Отображение просмотренной истории")
    public void shouldSeeViewedStory() {
        basePageSteps.writeInLocalStorage("viewed-stories", "2448fded-66ef-4076-9beb-15fe755ba059");
        basePageSteps.refresh();

        basePageSteps.onMainPage().stories().viewedStory().should(isDisplayed())
                .should(hasText("Золотая карта от Колесо.ру в подарок"));
    }
}
