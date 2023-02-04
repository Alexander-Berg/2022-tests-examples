package ru.auto.tests.desktop.header;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_OFFERS;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Шапка под незарегом")
@Feature(HEADER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class HeaderUnauthTest {

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
        mockRule.setStubs(stub("desktop/SearchCarsBreadcrumbsEmpty")).create();

        basePageSteps.setWideWindowSize(768);
        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение шапки")
    public void shouldSeeHeader() {
        basePageSteps.onMainPage().header().should(hasText("Авто.ру\nПоиск по объявлениям\nИзбранное\nПоиски\nСравнения" +
                "\nСообщения\nЯ продаю\nВойти\nРазместить бесплатно\nЛегковые\nКоммерческие\nМото\nЭлектромобили\nПроАвто" +
                "\nКредиты\nОСАГО\nГараж\nВыкуп\nЖурнал\nДилерам"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по лого auto.ru")
    public void shouldClickLogo() {
        basePageSteps.onMainPage().header().logo().click();
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Избранное»")
    public void shouldClickFavoritesButton() {
        basePageSteps.onMainPage().header().favoritesButton().click();
        basePageSteps.onMainPage().favoritesPopup().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Поиски»")
    public void shouldClickSearchesButton() {
        basePageSteps.onMainPage().header().savedSearchesButton().click();
        basePageSteps.onMainPage().headerSavedSearchesPopup().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Сравнения»")
    public void shouldClickCompareButton() {
        basePageSteps.onMainPage().header().compareButton().click();
        urlSteps.testing().path(COMPARE_OFFERS).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Сообщения»")
    public void shouldClickChatButton() {
        basePageSteps.onMainPage().header().chatButton().click();
        basePageSteps.onMainPage().chatAuthPopup().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Я продаю»")
    public void shouldClickISellButton() {
        basePageSteps.onMainPage().header().iSellButton().click();
        urlSteps.testing().path(MY).path(ALL).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Войти»")
    public void shouldClickAuthButton() {
        String currentUrl = urlSteps.getCurrentUrl();
        basePageSteps.onMainPage().header().button("Войти").click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN).addParam("r", encode(currentUrl)).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Продать»")
    public void shouldClickSellButton() {
        basePageSteps.onMainPage().header().button("Разместить бесплатно").click();
        urlSteps.testing().path(CARS).path(USED).path(ADD).shouldNotSeeDiff();
    }
}
