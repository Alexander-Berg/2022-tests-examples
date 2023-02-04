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
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
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
@DisplayName("Нет бейджа ВАС'а на сниппете без подключенной услуги")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class VasSnippetNoVasIconTest {

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

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();

        mockRule.graphqlStub(mockResponse()
                .setSearch(listingCategoryResponse().offers(asList(mockSnippet(BASIC_SNIPPET).getMockSnippet())).build())
                .setRegionsTemplate()
                .setCategoriesTemplate()
                .build()).withDefaults().create();

        urlSteps.testing().path(ELEKTRONIKA);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет бейджа ВАС'а на сниппете без подключенной услуги, листинг плиткой")
    public void shouldNotSeeVasIconGridListing() {
        urlSteps.open();

        basePageSteps.onListingPage().snippetFirst().vasBadge().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет бейджа ВАС'а на сниппете без подключенной услуги, листинг списком")
    public void shouldNotSeeVasIconListListing() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);
        urlSteps.open();

        basePageSteps.onListingPage().snippetFirst().vasBadge().should(not(isDisplayed()));
    }

}
