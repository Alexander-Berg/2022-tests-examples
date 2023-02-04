package ru.yandex.general.passportRequiredModal;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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

import static ru.yandex.general.consts.GeneralFeatures.PASSPORT_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.element.Filters.SAVE_SEARCH;
import static ru.yandex.general.element.FloatedHeader.SAVE;
import static ru.yandex.general.element.Header.CHATS;
import static ru.yandex.general.element.Header.FAVORITE;
import static ru.yandex.general.element.Header.MY_OFFERS;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(PASSPORT_FEATURE)
@DisplayName("Отображение модалки паспортного логина")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class PassportRequiredModalViewTest {

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
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().path(ELEKTRONIKA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение модалки паспортного логина по тапу на «Добавить в избранное»")
    public void shouldSeePassportModalFromOfferLike() {
        basePageSteps.onListingPage().snippetFirst().hover();
        basePageSteps.onListingPage().snippetFirst().addToFavorite().click();

        basePageSteps.onListingPage().passportRequiredModal().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение модалки паспортного логина по тапу на «Сохранить поиск»")
    public void shouldSeePassportModalFromSaveSearch() {
        basePageSteps.onListingPage().filters().spanLink(SAVE_SEARCH).click();

        basePageSteps.onListingPage().passportRequiredModal().should(isDisplayed());
    }

}
