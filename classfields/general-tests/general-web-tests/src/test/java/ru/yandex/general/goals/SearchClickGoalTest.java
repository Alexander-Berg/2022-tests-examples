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
import org.openqa.selenium.Keys;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.SEARCH_TEXT;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.SLASH;
import static ru.yandex.general.element.SearchBar.FIND;

@Epic(GOALS_FEATURE)
@DisplayName("Цель «SEARCH_TEXT» при поиске")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SearchClickGoalTest {

    private static final String TEXT = "телевизор";

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
    public String path;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Отправка события с главной", SLASH},
                {"Отправка события с листинга категории", ELEKTRONIKA}
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MOSKVA).path(path).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_TEXT)
    @DisplayName("Цель «SEARCH_TEXT» при тапе на «Показать объявления»")
    public void shouldSeeSearchTestGoalOnShowOffers() {
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().button(FIND).click();

        goalsSteps.withGoalType(SEARCH_TEXT)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_TEXT)
    @DisplayName("Цель «SEARCH_TEXT» при вводе текста и тапе на Enter")
    public void shouldSeeSearchTestGoalOnEnterHit() {
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().searchBar().input().sendKeys(Keys.ENTER);

        goalsSteps.withGoalType(SEARCH_TEXT)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_TEXT)
    @DisplayName("Цель «SEARCH_TEXT» при тапе на элемент саджеста")
    public void shouldSeeSearchTestGoalOnSuggestItemClick() {
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().searchBar().suggestItem(TEXT).click();

        goalsSteps.withGoalType(SEARCH_TEXT)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

}
