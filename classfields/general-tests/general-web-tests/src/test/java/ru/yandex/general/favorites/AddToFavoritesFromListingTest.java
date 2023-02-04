package ru.yandex.general.favorites;

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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITE_OFFERS;
import static ru.yandex.general.consts.Owners.ILUHA;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@Epic(FAVORITES_FEATURE)
@Feature(FAVORITE_OFFERS)
@DisplayName("Добавление в избранное")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddToFavoritesFromListingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Главная", ""},
                {"Электроника", "/elektronika/"},
        });
    }

    @Before
    public void before() {
        passportSteps.createAccountAndLogin();
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().path(path).open();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Добавление в избранное с листинга")
    public void shouldAddFromListing() {
        basePageSteps.onListingPage().snippetFirst().hover();
        String link = basePageSteps.onListingPage().snippetFirst().link().getAttribute("href");
        basePageSteps.onListingPage().snippetFirst().addToFavorite().click();
        basePageSteps.onListingPage().snippetFirst().addToFavorite().waitUntilChecked();
        basePageSteps.wait500MS();
        urlSteps.testing().path(MY).path(FAVORITES).open();
        basePageSteps.onFavoritesPage().firstFavCard().link().should(hasAttribute("href", equalTo(link)));
    }
}
