package ru.yandex.general.search;

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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.ExternalLinks.REALTY_SEARCH_APARTMENT_LINK;
import static ru.yandex.general.consts.ExternalLinks.REALTY_SEARCH_LINK;
import static ru.yandex.general.consts.GeneralFeatures.SEARCH_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.SHOULD_REQUEST_VERTIS_WIZARDS;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.element.Button.FALSE;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.element.SearchBar.FIND;
import static ru.yandex.general.page.BasePage.MAIN_PAGE_MOSCOW_H1;
import static ru.yandex.general.page.ListingPage.MORE_OFFERS_REALTY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(SEARCH_FEATURE)
@Feature("Выдача недвижимости по текстовому поиску")
@DisplayName("Выдача недвижимости по текстовому поиску")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class TextSearchRealtyTest {

    private static final String TEXT = "квартира";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текстовый поиск для получения карусели «Яндекс.Недвижимость»")
    public void shouldSeeRealtyChips() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().searchBar().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(TEXT_PARAM, TEXT).shouldNotDiffWithWebDriverUrl();
        basePageSteps.onListingPage().wizardCarouselHeader().should(hasText("Яндекс.Недвижимость"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем текстовый поиск с выдачей по недвижимости")
    public void shouldSeeTextSearchReset() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();
        basePageSteps.onListingPage().searchBar().clearInput().click();
        basePageSteps.onListingPage().searchBar().button(FIND).click();

        basePageSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        basePageSteps.onListingPage().chips().should(not(isDisplayed()));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка в кнопке «Больше объявлений на Я.Недвижимости»")
    public void shouldSeeMoreRealtyOffersLink() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();

        basePageSteps.onListingPage().link(MORE_OFFERS_REALTY).should(hasAttribute(HREF, REALTY_SEARCH_APARTMENT_LINK));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("В каждом сниппете с выдачей по недвижимости ссылка на Я.Недвижимости")
    public void shouldSeeRealtyLinkEverySnippet() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();

        basePageSteps.onListingPage().wizardSnippets().stream().forEach(snippet -> snippet.link().should
                (hasAttribute(HREF, containsString(REALTY_SEARCH_LINK))));
    }

}
