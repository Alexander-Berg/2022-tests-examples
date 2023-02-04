package ru.yandex.general.adult;

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
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FOR_ADULTS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.INTIM_TOVARI;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.SLASH;
import static ru.yandex.general.mobile.step.BasePageSteps.ADULT_CONFIRMED;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.adultListingResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FOR_ADULTS_FEATURE)
@Feature("Попап «Только для взрослых»")
@DisplayName("Попап «Только для взрослых»")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AdultPopupTest {

    private static final String CARD_ID = "12345";
    private static final String YES = "Да";
    private static final String NO = "Пока нет";
    private static final String TRUE = "true";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameter(2)
    public MockResponse mockResponse;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Попап «Товары для взрослых» на листинге", INTIM_TOVARI,
                        mockResponse().setSearch(adultListingResponse().build())
                },
                {"Попап «Товары для взрослых» на карточке оффера", CARD + CARD_ID + SLASH,
                        mockResponse().setCard(mockCard(BASIC_CARD).setCategoryForAdults(true).setId(CARD_ID).build())
                }
        });
    }

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();

        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(path);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проставляется кука «classified_adult_confirmed» при тапе на «Да», на листинге и карточке")
    public void shouldSeeCookieTapYes() {
        urlSteps.open();
        basePageSteps.onBasePage().popup().button(YES).waitUntil(isDisplayed()).click();

        basePageSteps.shouldSeeCookie(ADULT_CONFIRMED, TRUE);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редирект на главную по тапу на «Пока нет», кука не проставляется, на листинге и карточке")
    public void shouldSeeHomepageRedirect() {
        urlSteps.open();
        basePageSteps.onBasePage().popup().button(NO).waitUntil(isDisplayed()).click();

        basePageSteps.shouldNotSeeCookie(ADULT_CONFIRMED);
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет попапа с кукой «classified_adult_confirmed», на листинге и карточке")
    public void shouldNotSeePopupWithCookie() {
        basePageSteps.setCookie(ADULT_CONFIRMED, TRUE);
        urlSteps.open();

        basePageSteps.onBasePage().popup().should(not(isDisplayed()));
    }

}
