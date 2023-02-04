package ru.yandex.general.listing;

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
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.Notifications.DONE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.QueryParams.SEARCHES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.mobile.step.BasePageSteps.GRID;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(LISTING_FEATURE)
@Feature("Сохранение поиска пустой выдачи")
@DisplayName("Сохранение поиска пустой выдачи")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class SaveSearchEmptyListing {

    private static final String ABRAKADABRA = "askfkasfasfsafsf";
    private static final String YES = "Да";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.createAccountAndLogin();
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().queryParam(TEXT_PARAM, ABRAKADABRA).open();
        basePageSteps.onListingPage().snippets().waitUntil(hasSize(0));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавление в избранное поиска с пустого листинга")
    public void shouldAddSearchFromEmptyListing() {
        basePageSteps.onListingPage().saveSearch().click();
        basePageSteps.onListingPage().popup().button(YES).click();
        basePageSteps.onListingPage().popupNotification(DONE).waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, SEARCHES_TAB_VALUE).open();
        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(1));
        basePageSteps.onFavoritesPage().favoritesCards().get(0).link()
                .should(hasAttribute(HREF, urlSteps.testing().path(MOSKVA)
                        .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                        .queryParam(TEXT_PARAM, ABRAKADABRA).toString()));
    }

}
