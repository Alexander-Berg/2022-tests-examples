package ru.yandex.realty.mainpage;

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
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.net.URLEncoder;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Owners.VICDEV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.element.base.HeaderMain.LOGIN;
import static ru.yandex.realty.element.base.HeaderMain.NEW_OFFER;
import static ru.yandex.realty.page.BasePage.COMPARISON;
import static ru.yandex.realty.page.BasePage.FAVORITES;
import static ru.yandex.realty.page.BasePage.SUBSCRIPTIONS;


@DisplayName("Главная. Ссылки в хедере")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class HeaderMainTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps user;

    @Before
    public void openMainPage() {
        urlSteps.testing().open();
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(VICDEV)
    @DisplayName("Кнопка «Войти»")
    @Description("Проверка ссылки на пасспорт. Клик и отображения попапа в тесте auth")
    public void shouldSeeLoginButton() {
        user.onBasePage().headerMain().link(LOGIN).should(hasAttribute("href",
                allOf(
                        containsString(urlSteps.getConfig().getPassportTestURL().toString()),
                        containsString(format("retpath=%s",
                                URLEncoder.encode("https://" + urlSteps.getConfig().getTestingURI().getHost())))
                )));
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(VICDEV)
    @DisplayName("Кнопка «Новое объявление»")
    public void shouldSeeManagementButton() {
        user.onBasePage().headerMain().link(NEW_OFFER).should(hasAttribute("href",
                containsString(Pages.MANAGEMENT_NEW_ADD)));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Ссылка: «Избранное»")
    public void shouldSeeLinkComparisonIHeader() {
        user.onBasePage().headerMain().favoritesButton().should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Сердечко «Избранное» в хедере")
    public void shouldSeeLinkFavorites() {
        user.onBasePage().headerMain().favoritesButton().should(isDisplayed());
    }
}
