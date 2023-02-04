package ru.auto.tests.mobile.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Notifications.DELETED_FROM_FAV;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(VIN)
@Story("Карточка отчёта")
@DisplayName("Добавление в избранное")
@GuiceModules(MobileEmulationTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryReportFavoritesTest {

    private static final String SALE_ID = "1076842087-f1e84";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(HISTORY).path(SALE_ID).path(SLASH);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем в избранное")
    public void shouldAddToFavorite() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/CarfaxOfferCarsRawNotPaid"),
                stub("desktop/OfferCarsUsedUser"),
                stub("desktop/UserFavoritesCarsPost")
        ).create();
        urlSteps.open();

        basePageSteps.onHistoryPage().vinReport().favoriteButton().click();

        basePageSteps.onHistoryPage().notifier("В избранном 1 предложениеСмотреть").should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем из избранного")
    public void shouldDeleteFromFavorite() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/CarfaxOfferCarsRawNotPaid"),
                stub("desktop/OfferCarsUsedUserIsFavoriteTrue"),
                stub("desktop/UserFavoritesCarsDelete")
        ).create();
        urlSteps.open();

        basePageSteps.onHistoryPage().vinReport().favoriteButton().click();

        basePageSteps.onHistoryPage().notifier(DELETED_FROM_FAV).should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается страница авторизации по клику на кнопку избранного")
    public void shouldSeeAuthorizationPageByFavoriteButton() {
        mockRule.setStubs(
                stub("desktop/CarfaxOfferCarsRawNotPaid"),
                stub("desktop/OfferCarsUsedUser"),
                stub("desktop/OfferCarsPhones")
        ).create();
        urlSteps.open();

        String currentUrl = urlSteps.getCurrentUrl();

        basePageSteps.onHistoryPage().vinReport().favoriteButton().click();

        basePageSteps.onCardPage().authPopup().should(isDisplayed());
        basePageSteps.onCardPage().authPopup().iframe()
                .should(hasAttribute("src", containsString(
                        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                                .addParam("r", encode(currentUrl))
                                .addParam("inModal", "true")
                                .addParam("autoLogin", "true")
                                .addParam("welcomeTitle", "")
                                .toString()
                )));
    }

}
