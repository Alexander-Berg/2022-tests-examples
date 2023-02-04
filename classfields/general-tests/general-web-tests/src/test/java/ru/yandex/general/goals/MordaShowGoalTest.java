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

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.MORDA_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;

@Epic(GOALS_FEATURE)
@Feature(MORDA_SHOW)
@DisplayName("Цель «MORDA_SHOW»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class MordaShowGoalTest {

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

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «MORDA_SHOW» при отображении главной")
    public void shouldSeeMordaShowGoalOnOpenMainPage() {
        basePageSteps.waitSomething(3, TimeUnit.SECONDS);
        goalsSteps.clearHar();
        urlSteps.testing().open();

        goalsSteps.withGoalType(MORDA_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет цели «MORDA_SHOW» на страничке категории")
    public void shouldNotSeeMordaShowGoalOnCategoryPage() {
        basePageSteps.waitSomething(3, TimeUnit.SECONDS);
        goalsSteps.clearHar();
        urlSteps.testing().path(ELEKTRONIKA).open();

        goalsSteps.withGoalType(MORDA_SHOW)
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет цели «MORDA_SHOW» на текстовом поиске")
    public void shouldNotSeeMordaShowGoalOnTextSearch() {
        basePageSteps.waitSomething(3, TimeUnit.SECONDS);
        goalsSteps.clearHar();
        urlSteps.testing().queryParam(TEXT_PARAM, TEXT).open();

        goalsSteps.withGoalType(MORDA_SHOW)
                .withCount(0)
                .shouldExist();
    }

}
