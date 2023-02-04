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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.VAS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.mobile.step.BasePageSteps.LIST;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(VAS_FEATURE)
@DisplayName("Отображение иконки VAS")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
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
        mockRule.graphqlStub(mockResponse()
                .setSearch(listingCategoryResponse().offers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setVas())).build())
                .setRegionsTemplate()
                .setCategoriesTemplate()
                .build()).withDefaults().create();

        urlSteps.testing().path(ELEKTRONIKA);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Есть бейдж ВАС'а на сниппете листинга плиткой")
    public void shouldSeeVasIconGridListing() {
        urlSteps.open();
        basePageSteps.onListingPage().snippetFirst().vasBadge().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Есть бейдж ВАС'а на сниппете листинга списком")
    public void shouldSeeVasIconListListing() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);

        urlSteps.open();
        basePageSteps.onListingPage().snippetFirst().vasBadge().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот сниппета с бейджом ВАС'а")
    public void shouldSeeSnippetWithVasScreenshot() {
        urlSteps.open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onListingPage().snippetFirst());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onListingPage().snippetFirst());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
