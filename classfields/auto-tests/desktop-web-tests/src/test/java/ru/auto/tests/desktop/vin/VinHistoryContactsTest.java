package ru.auto.tests.desktop.vin;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Notifications.DELETED_FROM_FAV;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(VIN)
@Story("Блок контактов")
@DisplayName("Страница истории автомобиля")
@GuiceModules(DesktopTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryContactsTest {

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
        mockRule.setStubs(
                stub("desktop/CarfaxOfferCarsRawNotPaid"),
                stub("desktop/OfferCarsUsedUser"),
                stub("desktop/OfferCarsPhones")
        ).create();

        urlSteps.testing().path(HISTORY).path("/1076842087-f1e84/").open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение блока контактов")
    public void shouldSeeContacts() {
        basePageSteps.onHistoryPage().contacts().mmm().should(hasText("Land Rover Discovery III"));
        basePageSteps.onHistoryPage().contacts().price().should(hasText("700 000 \u20BD"));
        basePageSteps.onHistoryPage().contacts().seller().should(hasText("Федор"));
        basePageSteps.onHistoryPage().contacts().address().should(hasText("Москва"));
        basePageSteps.onHistoryPage().vinReportPreview().button("Один отчёт за 499\u00a0₽")
                .waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Показать телефон»")
    public void shouldClickShowPhoneButton() {
        basePageSteps.onHistoryPage().contacts().showPhoneButton().click();
        basePageSteps.onHistoryPage().contactsPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().contactsPopup().phones().should(hasText("+7 916 039-84-27\nc 10:00 до 23:00\n" +
                "+7 916 039-84-28\nc 12:00 до 20:00"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Написать»")
    public void shouldClickSendMessageButton() {
        String currentUrl = urlSteps.getCurrentUrl();

        basePageSteps.onHistoryPage().contacts().sendMessageButton().waitUntil(isDisplayed()).click();

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

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем в избранное")
    public void shouldAddToFavorite() {
        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/CarfaxOfferCarsRawNotPaid"),
                stub("desktop/OfferCarsUsedUser"),
                stub("desktop/UserFavoritesCarsPost")
        ).create();
        basePageSteps.refresh();

        basePageSteps.onHistoryPage().contacts().favoriteButton().click();

        basePageSteps.onHistoryPage().notifier("В избранном 1 предложениеПерейти в избранное").should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем из избранного")
    public void shouldDeleteFromFavorite() {
        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/CarfaxOfferCarsRawNotPaid"),
                stub("desktop/OfferCarsUsedUserIsFavoriteTrue"),
                stub("desktop/UserFavoritesCarsDelete")
        ).create();
        basePageSteps.refresh();

        basePageSteps.onHistoryPage().contacts().favoriteButton().click();

        basePageSteps.onHistoryPage().notifier(DELETED_FROM_FAV).should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается попап авторизации по клику на кнопку избранного")
    public void shouldSeeAuthorizationPopupByFavoriteButton() {
        basePageSteps.onHistoryPage().contacts().favoriteButton().click();

        basePageSteps.onHistoryPage().authPopup().should(isDisplayed());
    }

}