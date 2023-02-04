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
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.FAVOURITES_SEARCHES_ADD;
import static ru.yandex.general.consts.Goals.FAVOURITES_SEARCHES_SUBSCRIBE;
import static ru.yandex.general.consts.Goals.PROMO_SUBSCRIBE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;

@Epic(GOALS_FEATURE)
@DisplayName("Цели при сохранении поисков")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class AddSearchToFavouritesGoalTest {

    private static final String TEXT = "ноутбук";
    private static final String YES = "Да";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        passportSteps.createAccountAndLogin();
    }

    @Test
    @Feature(FAVOURITES_SEARCHES_ADD)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «FAVOURITES_SEARCHES_ADD» при сохранении поиска по категории")
    public void shouldSeeFavouritesSearchesAddGoalFromCategoryListing() {
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().saveSearch().click();
        basePageSteps.onListingPage().popup().button(YES).click();

        goalsSteps.withGoalType(FAVOURITES_SEARCHES_ADD)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Feature(FAVOURITES_SEARCHES_ADD)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «FAVOURITES_SEARCHES_ADD» при сохранении полнотекстового поиска")
    public void shouldSeeFavouritesSearchesAddGoalFromTextSearchListing() {
        urlSteps.testing().queryParam(TEXT_PARAM, TEXT).open();
        basePageSteps.onListingPage().saveSearch().click();
        basePageSteps.onListingPage().popup().button(YES).click();

        goalsSteps.withGoalType(FAVOURITES_SEARCHES_ADD)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Feature(PROMO_SUBSCRIBE)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «PROMO_SUBSCRIBE» при сохранении поиска по категории, подписавшись на рассылку")
    public void shouldSeePromoSubscribeAcceptMailing() {
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().saveSearch().click();
        basePageSteps.onListingPage().popup().button(YES).click();

        goalsSteps.withGoalType(PROMO_SUBSCRIBE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Feature(PROMO_SUBSCRIBE)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет цели «PROMO_SUBSCRIBE» при сохранении поиска, не подписавшись на рассылку")
    public void shouldNotSeePromoSubscribeNotAcceptMailing() {
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().saveSearch().click();
        basePageSteps.onListingPage().popup().checkboxWithLabel("новости").click();
        basePageSteps.onListingPage().popup().button(YES).click();

        goalsSteps.withGoalType(PROMO_SUBSCRIBE)
                .withCurrentPageRef()
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Feature(FAVOURITES_SEARCHES_SUBSCRIBE)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «FAVOURITES_SEARCHES_SUBSCRIBE» при сохранении поиска по категории, подписавшись на поиски")
    public void shouldSeeFavouritesSearchesSubscribe() {
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().saveSearch().click();
        basePageSteps.onListingPage().popup().button(YES).click();

        goalsSteps.withGoalType(FAVOURITES_SEARCHES_SUBSCRIBE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Feature(FAVOURITES_SEARCHES_SUBSCRIBE)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет цели «FAVOURITES_SEARCHES_SUBSCRIBE» при сохранении поиска по категории, не подписавшись на поиски")
    public void shouldNotSeeFavouritesSearchesSubscribe() {
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().saveSearch().click();
        basePageSteps.onListingPage().popup().button("Нет").click();

        goalsSteps.withGoalType(FAVOURITES_SEARCHES_SUBSCRIBE)
                .withCurrentPageRef()
                .withCount(0)
                .shouldExist();
    }

}
