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

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.LISTING_OFFERS_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.KOMPUTERNAYA_TEHNIKA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;

@Epic(GOALS_FEATURE)
@Feature(LISTING_OFFERS_SHOW)
@DisplayName("Цель «LISTING_OFFERS_SHOW»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class ListingOffersShowGoalTest {

    private static final String TEXT = "ноутбук";

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
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «LISTING_OFFERS_SHOW» при открытии родительской категории")
    public void shouldSeeListingOffersShowGoalOnParentCategory() {
        urlSteps.testing().path(ELEKTRONIKA).open();

        goalsSteps.withGoalType(LISTING_OFFERS_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «LISTING_OFFERS_SHOW» при открытии конечной категории")
    public void shouldSeeListingOffersShowGoalOnFinalCategory() {
        urlSteps.testing().path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI).open();

        goalsSteps.withGoalType(LISTING_OFFERS_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет цели «LISTING_OFFERS_SHOW» при отображении главной")
    public void shouldNotSeeListingOffersShowGoalOnOpenMainPage() {
        urlSteps.testing().open();

        goalsSteps.withGoalType(LISTING_OFFERS_SHOW)
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет цели «LISTING_OFFERS_SHOW» при текстовом поиске по категории")
    public void shouldNotSeeListingOffersShowGoalOnOpenTextSearh() {
        urlSteps.testing().path(KOMPUTERNAYA_TEHNIKA).queryParam(TEXT_PARAM, TEXT).open();

        goalsSteps.withGoalType(LISTING_OFFERS_SHOW)
                .withCount(0)
                .shouldExist();
    }

}
