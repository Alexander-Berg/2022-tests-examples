package ru.auto.tests.desktop.lk.sales.reseller;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Кнопка «Поделиться»")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_RESELLER)
@GuiceModules(DesktopTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ShareTest {

    private final static String FACEBOOK_SHARE_URL_TEMPLATE = "https://www.facebook.com/sharer.php?src=sp&u=%s&title=%s&utm_source=share2";
    private final static String TWITTER_SHARE_URL_TEMPLATE = "https://twitter.com/intent/tweet?text=%s&url=%s&utm_source=share2";
    private final static String VKONTAKTE_SHARE_URL_TEMPLATE = "https://vk.com/share.php?url=%s&title=%s&utm_source=share2";
    private final static String ODNOKLASSNIKI_SHARE_URL_TEMPLATE = "https://connect.ok.ru/offer?url=%s&title=%s&utm_source=share2";
    private final static String HREF = "href";

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String salesMock;

    @Parameterized.Parameter(2)
    public String shareUrlPath;

    @Parameterized.Parameter(3)
    public String shareText;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop-lk/UserOffersCarsActive", "/cars/used/sale/vaz/2121/1076842087-f1e84/", "%D0%9F%D1%80%D0%BE%D0%B4%D0%B0%D1%8E%20LADA%20(%D0%92%D0%90%D0%97)%202121%20(4x4)%201977-2019%20Bronto%202018%20%D0%B3%D0%BE%D0%B4%D0%B0%20%D0%B7%D0%B0%20795%C2%A0000%20%D1%80%D1%83%D0%B1%D0%BB%D0%B5%D0%B9%20%D0%BD%D0%B0%20%D0%90%D0%B2%D1%82%D0%BE.%D1%80%D1%83!"},
                {MOTO, "desktop-lk/UserOffersMotoActive", "/motorcycle/used/sale/ducati/monster_s4/1076842087-f1e84/", "%D0%9F%D1%80%D0%BE%D0%B4%D0%B0%D1%8E%20Ducati%20Monster%20S4%202000%20%D0%B3%D0%BE%D0%B4%D0%B0%20%D0%B7%D0%B0%20500%C2%A0000%20%D1%80%D1%83%D0%B1%D0%BB%D0%B5%D0%B9%20%D0%BD%D0%B0%20%D0%90%D0%B2%D1%82%D0%BE.%D1%80%D1%83!"},
                {TRUCKS, "desktop-lk/UserOffersTrucksActive", "/lcv/used/sale/hyundai/porter/1076842087-f1e84/", "%D0%9F%D1%80%D0%BE%D0%B4%D0%B0%D1%8E%20Hyundai%20Porter%202008%20%D0%B3%D0%BE%D0%B4%D0%B0%20%D0%B7%D0%B0%20450%C2%A0000%20%D1%80%D1%83%D0%B1%D0%BB%D0%B5%D0%B9%20%D0%BD%D0%B0%20%D0%90%D0%B2%D1%82%D0%BE.%D1%80%D1%83!"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserFavoriteReseller"),
                stub(salesMock)
        ).create();

        basePageSteps.setWideWindowSize();

        urlSteps.testing().path(MY).path(RESELLER).path(category).open();
    }

    @Test
    @Ignore
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны увидеть кнопку Facebook с правильным путем")
    public void shouldSeeFacebookShareButtonWithCorrectHref() {
        String shareLink = format(FACEBOOK_SHARE_URL_TEMPLATE,
                encode(urlSteps.getConfig().getDesktopURI() + shareUrlPath), shareText);

        basePageSteps.mouseOver(basePageSteps.onLkResellerSalesPage().getSale(0));
        basePageSteps.onLkResellerSalesPage().getSale(0).controlsColumn().moreIcon().should(isDisplayed()).click();
        basePageSteps.onLkResellerSalesPage().moreMenu().share().waitUntil(isDisplayed()).click();

        basePageSteps.onLkResellerSalesPage().shareDropdown().fb().should(isDisplayed())
                .should(hasAttribute(HREF, shareLink));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны увидеть кнопку Вконтакте с правильным путем")
    public void shouldSeeVkontakteShareButtonWithCorrectHref() {
        String shareLink = format(VKONTAKTE_SHARE_URL_TEMPLATE,
                encode(urlSteps.getConfig().getDesktopURI() + shareUrlPath), shareText);

        basePageSteps.mouseOver(basePageSteps.onLkResellerSalesPage().getSale(0));
        basePageSteps.onLkResellerSalesPage().getSale(0).controlsColumn().moreIcon().should(isDisplayed()).click();
        basePageSteps.onLkResellerSalesPage().moreMenu().share().waitUntil(isDisplayed()).click();

        basePageSteps.onLkResellerSalesPage().shareDropdown().vk().should(isDisplayed())
                .should(hasAttribute(HREF, shareLink));
    }

    @Test
    @Ignore
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны увидеть кнопку Twitter с правильным путем")
    public void shouldSeeTwitterShareButtonWithCorrectHref() {
        String shareLink = format(TWITTER_SHARE_URL_TEMPLATE,
                shareText, encode(urlSteps.getConfig().getDesktopURI() + shareUrlPath));

        basePageSteps.mouseOver(basePageSteps.onLkResellerSalesPage().getSale(0));
        basePageSteps.onLkResellerSalesPage().getSale(0).controlsColumn().moreIcon().should(isDisplayed()).click();
        basePageSteps.onLkResellerSalesPage().moreMenu().share().waitUntil(isDisplayed()).click();

        basePageSteps.onLkResellerSalesPage().shareDropdown().twitter().should(isDisplayed())
                .should(hasAttribute(HREF, shareLink));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны увидеть кнопку Одноклассники с правильным путем")
    public void shouldSeeOdnoklassnikiShareButtonWithCorrectHref() {
        String shareLink = format(ODNOKLASSNIKI_SHARE_URL_TEMPLATE,
                encode(urlSteps.getConfig().getDesktopURI() + shareUrlPath), shareText);

        basePageSteps.mouseOver(basePageSteps.onLkResellerSalesPage().getSale(0));
        basePageSteps.onLkResellerSalesPage().getSale(0).controlsColumn().moreIcon().should(isDisplayed()).click();
        basePageSteps.onLkResellerSalesPage().moreMenu().share().waitUntil(isDisplayed()).click();

        basePageSteps.onLkResellerSalesPage().shareDropdown().ok().should(isDisplayed())
                .should(hasAttribute(HREF, shareLink));
    }

}
