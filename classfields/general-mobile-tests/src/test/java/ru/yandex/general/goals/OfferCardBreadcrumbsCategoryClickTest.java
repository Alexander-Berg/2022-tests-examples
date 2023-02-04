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
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.BREADCRUMBS_CATEGORY_CLICK;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.SLASH;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.mobile.step.BasePageSteps.TRUE;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(GOALS_FEATURE)
@Feature(BREADCRUMBS_CATEGORY_CLICK)
@DisplayName("Цель «BREADCRUMBS_CATEGORY_CLICK» с карточки оффера")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardBreadcrumbsCategoryClickTest {

    private static final String ID = "123456";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String breadcrumbTitle;

    @Parameterized.Parameter(2)
    public int goalsCount;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Категория оффера в ХК. есть цель", "Мобильные телефоны", 1},
                {"Промежуточная категория в ХК. есть цель", "Телефоны и умные часы", 1},
                {"Родительская категория в ХК. есть цель", "Электроника", 1},
                {"Регион в ХК. нет цели", "Москва", 0},
                {"Ссылка «Все объявления» в ХК. нет цели", "Все объявления", 0}
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setId(ID).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);

        urlSteps.testing().path(CARD).path(ID).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «BREADCRUMBS_CATEGORY_CLICK» при переходе по ХК на карточке оффера")
    public void shouldSeeBreadcrumbsCategoryClickGoalFromOfferCard() {
        basePageSteps.onOfferCardPage().breadcrumbsItem(breadcrumbTitle).click();

        goalsSteps.withGoalType(BREADCRUMBS_CATEGORY_CLICK)
                .withPageRef(urlSteps.path(SLASH).toString())
                .withCount(goalsCount)
                .shouldExist();
    }

}
