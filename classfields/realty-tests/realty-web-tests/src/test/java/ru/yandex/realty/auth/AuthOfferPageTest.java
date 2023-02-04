package ru.yandex.realty.auth;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.config.RealtyWebConfig;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.page.OfferAddPage.LOGIN;
import static ru.yandex.realty.page.OfferAddPage.REGISTER;

/**
 * Created by ivanvan on 31.07.17.
 */
@DisplayName("Форма добавления объявления. Блок авторизации.")
@Feature(OFFERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class AuthOfferPageTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private RealtyWebConfig config;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Before
    public void openManagementAddPage() {
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
    }

    @Test
    @Owner(IVANVAN)
    @Category({Regression.class, Production.class})
    @DisplayName("Нажимаем верхнюю кнопку «Войти» со страницы оффера")
    @Description("Проверяем, что переходим на страницу паспорта")
    public void shouldSeePassportPageViaTopAuthButton() {
        offerAddSteps.onOfferAddPage().noAuthTopBlock().link(LOGIN).waitUntil(isDisplayed());
        offerAddSteps.onOfferAddPage().noAuthTopBlock().link(LOGIN)
                .should(hasHref(containsString(config.getPassportTestURL().toString() + "auth")));
    }

    @Test
    @Owner(IVANVAN)
    @Category({Regression.class, Production.class})
    @DisplayName("Нажимаем нижнюю кнопку «Войти» со страницы оффера")
    @Description("Проверяем, что переходим на страницу паспорта")
    public void shouldSeePassportPageViaLowerAuthButton() {
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        offerAddSteps.onOfferAddPage().noAuthLowerBlock().link(LOGIN).waitUntil(isDisplayed());
        offerAddSteps.onOfferAddPage().noAuthLowerBlock().link(LOGIN)
                .should(hasHref(containsString(config.getPassportTestURL().toString() + "auth")));
    }

    @Test
    @Owner(IVANVAN)
    @Category({Regression.class, Production.class})
    @DisplayName("Нажимаем нижнюю кнопку «Регистрация» со страницы оффера")
    @Description("Проверяем, что переходим на страницу регистрации")
    public void shouldSeeRegistrationPageViaLowerAuthButton() {
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        offerAddSteps.onOfferAddPage().noAuthLowerBlock().link(REGISTER).waitUntil(isDisplayed());
        offerAddSteps.onOfferAddPage().noAuthLowerBlock().link(REGISTER)
                .should(hasHref(containsString(config.getPassportTestURL().toString() + "registration")));
    }
}
