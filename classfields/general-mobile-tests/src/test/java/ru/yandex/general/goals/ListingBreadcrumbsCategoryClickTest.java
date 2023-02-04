package ru.yandex.general.goals;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.BREADCRUMBS_CATEGORY_CLICK;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.*;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;

@Epic(GOALS_FEATURE)
@Feature(BREADCRUMBS_CATEGORY_CLICK)
@DisplayName("Цель «BREADCRUMBS_CATEGORY_CLICK» с листинга")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingBreadcrumbsCategoryClickTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String breadcrumbTitle;

    @Parameterized.Parameter(2)
    public int goalsCount;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Конечная категория в ХК. есть цель", "Компьютеры", 1},
                {"Родительская категория в ХК. есть цель", "Компьютерная техника", 1},
                {"Регион в ХК. нет цели", "Москва", 0},
                {"Ссылка «Все объявления» в ХК. нет цели", "Все объявления", 0}
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        basePageSteps.resize(650, 812);
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «BREADCRUMBS_CATEGORY_CLICK» при переходе по ХК на листинге")
    public void shouldSeeBreadcrumbsCategoryClickGoalFromListing() {
        basePageSteps.onListingPage().breadcrumbsItem(breadcrumbTitle).hover().click();

        goalsSteps.withGoalType(BREADCRUMBS_CATEGORY_CLICK)
                .withPageRef(urlSteps.toString())
                .withCount(goalsCount)
                .shouldExist();
    }

}
