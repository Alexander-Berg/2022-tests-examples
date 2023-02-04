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
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.yandex.general.beans.ajaxRequests.AddToFavorites.addToFavorites;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.KOMPUTERNAYA_TEHNIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.Pages.TAG;
import static ru.yandex.general.step.AjaxProxySteps.ADD_TO_FAVORITES;
import static ru.yandex.general.step.AjaxProxySteps.DELETE_FROM_FAVORITES;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic("Общие кейсы для страниц с листингами")
@Feature("Добавление сниппета в избранное")
@DisplayName("Добавление/удаление сниппета в избранное с главной/категории/текстового поиска")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddToFavoritesFromListingsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private AjaxProxySteps ajaxProxySteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "Страница «{0}»")
    public static Collection<Object[]> links() {
        return asList(new Object[][]{
                {"Главная", MOSKVA},
                {"Листинг категории", format("%s%s%s", MOSKVA, KOMPUTERNAYA_TEHNIKA, NOUTBUKI).replace("//", "/")},
                {"Текстовый поиск", format("%s%s%s", MOSKVA, TAG, "/noutbuk-apple/").replace("//", "/")}
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        passportSteps.createAccountAndLogin();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем сниппет в избранное на главной/листинге категории/текстовом поиске")
    public void shouldSeeAddToFavoritesRequest() {
        urlSteps.testing().path(path).open();
        String offerId = basePageSteps.onListingPage().firstSnippet().getOfferId();
        basePageSteps.onListingPage().snippetFirst().hover();
        basePageSteps.onListingPage().firstSnippet().addToFavorite().waitUntil(isDisplayed()).click();

        ajaxProxySteps.setAjaxHandler(ADD_TO_FAVORITES).withRequestText(
                addToFavorites().setOfferIds(asList(offerId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем сниппет из избранного на главной/листинге категории/текстовом поиске")
    public void shouldSeeDeleteFromFavoritesRequest() {
        urlSteps.testing().path(path).open();
        String offerId = basePageSteps.onListingPage().firstSnippet().getOfferId();
        basePageSteps.onProfilePage().firstSnippet().hover();
        basePageSteps.onProfilePage().firstSnippet().addToFavorite().waitUntil(isDisplayed()).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onProfilePage().firstSnippet().addToFavorite().click();

        ajaxProxySteps.setAjaxHandler(DELETE_FROM_FAVORITES).withRequestText(
                addToFavorites().setOfferIds(asList(offerId))).shouldExist();
    }

}
