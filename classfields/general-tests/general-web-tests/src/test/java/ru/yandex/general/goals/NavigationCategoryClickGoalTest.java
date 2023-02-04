package ru.yandex.general.goals;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.beans.GoalRequestBody.goalRequestBody;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.NAVIGATION_CATEGORY_CLICK;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.KOMPUTERNAYA_TEHNIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;

@Epic(GOALS_FEATURE)
@Feature(NAVIGATION_CATEGORY_CLICK)
@DisplayName("Цель «NAVIGATION_CATEGORY_CLICK»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class NavigationCategoryClickGoalTest {

    private static final String KOMPUTERI = "Компьютеры";
    private static final String KOMPUTERI_ID = "komputeri_kegUbf";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «NAVIGATION_CATEGORY_CLICK» с главной")
    public void shouldSeeNavigationCategoryClickGoalFromHomepage() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onListingPage().sidebarCategories().link("Компьютерная техника").click();

        goalsSteps.withGoalType(NAVIGATION_CATEGORY_CLICK)
                .withPageRef(urlSteps.toString())
                .withBody(goalRequestBody().setCategoryId("komputernaya-tehnika_kUhfu9"))
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «NAVIGATION_CATEGORY_CLICK» в дочернюю категорию с листинга")
    public void shouldSeeNavigationCategoryClickGoalToChildCategory() {
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).open();
        basePageSteps.onListingPage().sidebarCategories().link(KOMPUTERI).click();

        goalsSteps.withGoalType(NAVIGATION_CATEGORY_CLICK)
                .withPageRef(urlSteps.toString())
                .withBody(goalRequestBody().setCategoryId(KOMPUTERI_ID))
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «NAVIGATION_CATEGORY_CLICK» в родительскую категорию с листинга")
    public void shouldSeeNavigationCategoryClickGoalToParentCategory() {
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI).open();
        basePageSteps.onListingPage().sidebarCategories().link(KOMPUTERI).click();

        goalsSteps.withGoalType(NAVIGATION_CATEGORY_CLICK)
                .withPageRef(urlSteps.toString())
                .withBody(goalRequestBody().setCategoryId(KOMPUTERI_ID))
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет цели «NAVIGATION_CATEGORY_CLICK» по «Все объявления»")
    public void shouldNotSeeNavigationCategoryClickGoalFromAllCategories() {
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).open();
        basePageSteps.onListingPage().sidebarCategories().link("Все объявления").click();

        goalsSteps.withGoalType(NAVIGATION_CATEGORY_CLICK)
                .withPageRef(urlSteps.toString())
                .withCount(0)
                .shouldExist();
    }

}
