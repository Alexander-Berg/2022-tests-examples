package ru.auto.tests.mobile.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.LIKE;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.QueryParams.R;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Страница избранного - заглушка")
@Feature(AutoruFeatures.FAVORITES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class FavoritesPageEmptyTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(LIKE).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение заглушки")
    public void shouldSeeStub() {
        basePageSteps.onFavoritesPage().stub().should(hasText("Войдите, чтобы сохранять понравившиеся объявления " +
                "и узнавать об изменении цен на всех устройствах.\nВойти"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Войти»")
    public void shouldClickLoginButton() {
        basePageSteps.onFavoritesPage().loginButton().should(isDisplayed()).click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN).addParam(R, encode(format("%s/like/",
                urlSteps.getConfig().getTestingURI()))).shouldNotSeeDiff();
    }
}
