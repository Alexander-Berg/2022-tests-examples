package ru.yandex.general.vas;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.VAS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(VAS_FEATURE)
@DisplayName("Отображение иконки VAS")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class VasSnippetIconTest {

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

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        mockRule.graphqlStub(mockResponse()
                .setSearch(listingCategoryResponse().offers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setVas())).build())
                .setRegionsTemplate()
                .setCategoriesTemplate()
                .build()).withDefaults().create();

        urlSteps.testing().path(ELEKTRONIKA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Есть бейдж ВАС'а на сниппете")
    public void shouldSeeVasIcon() {
        basePageSteps.onListingPage().snippetFirst().vasBadge().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот сниппета с бейджом ВАС'а")
    public void shouldSeeSnippetWithVasScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().snippetFirst());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().snippetFirst());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот ховера по бейджу ВАС'а")
    public void shouldSeeSnippetWithVasHover() {
        basePageSteps.onListingPage().snippetFirst().vasBadge().hover();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().popup());

        urlSteps.setProductionHost().open();
        basePageSteps.onListingPage().snippetFirst().vasBadge().hover();

        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
