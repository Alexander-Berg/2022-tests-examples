package ru.yandex.general.commonListingCases;

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
import ru.yandex.general.mock.MockListingSnippet;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.SLASH;
import static ru.yandex.general.mock.MockHomepage.homepageResponse;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic("Общие кейсы для страниц с листингами")
@Feature("Добавление сниппета в избранное")
@DisplayName("Добавление/удаление сниппета в избранное с главной/категории")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class NoFavoriteButtonOwnerSnippetListingsTest {

    private static final String CARD_ID = "1111111";

    private static MockListingSnippet snippet = mockSnippet(BASIC_SNIPPET).getMockSnippet().setIsOwner(true);

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

    @Parameterized.Parameters(name = "{index}. Страница «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Главная", SLASH,
                        mockResponse().setHomepage(homepageResponse().offers(asList(snippet)).build())
                },
                {"Листинг категории", ELEKTRONIKA,
                        mockResponse().setSearch(listingCategoryResponse().offers(asList(snippet)).build()),
                }
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(path);

        mockRule.graphqlStub(mockResponse
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();
        urlSteps.open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Иконка добавления в избранное не отображается на сниппете владельца оффера на главной/листинге категории")
    public void shouldNotSeeFavoriteButtonOwner() {
        basePageSteps.onListingPage().snippetFirst().waitUntil(isDisplayed()).hover();

        basePageSteps.onListingPage().firstSnippet().addToFavorite().should(not(isDisplayed()));
    }

}
