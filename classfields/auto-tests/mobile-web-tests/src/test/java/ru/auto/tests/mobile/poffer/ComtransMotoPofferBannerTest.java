package ru.auto.tests.mobile.poffer;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.Pages;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mobile.element.AddOfferNavigateModal.SAFARI;
import static ru.auto.tests.desktop.mobile.element.AddOfferNavigateModalItem.CONTINUE;
import static ru.auto.tests.desktop.mobile.page.PofferPage.KOMTRANS;
import static ru.auto.tests.desktop.mobile.page.PofferPage.MOTO;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_21494;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Баннер «Продолжайте в приложении» на вкладках комтранс/мото")
@Epic(BETA_POFFER)
@Feature("Баннер «Продолжайте в приложении» на вкладках комтранс/мото")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ComtransMotoPofferBannerTest {

    private static final String BANNER_TEMPLATE = "Продолжайте в приложении\nРазмещение %s пока доступно только в " +
            "приложении и на компьютере\nСкачать";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Parameterized.Parameter
    public String tabName;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameter(2)
    public String bannerVehicleType;

    @Parameterized.Parameter(3)
    public String campaign;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<String[]> getParameters() {
        return asList(new String[][]{
                {KOMTRANS, Pages.TRUCKS, "комтранса", "touch_addoffer_splash_comts"},
                {MOTO, Pages.MOTO, "мототранспорта", "touch_addoffer_splash_moto"}
        });
    }

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_21494);

        urlSteps.desktopURI().path(CARS).path(USED).path(ADD).open();
        basePageSteps.onPofferPage().addOfferNavigateModal().closeIcon().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Баннер «Продолжайте в приложении»")
    public void shouldSeeBanner() {
        basePageSteps.onPofferPage().radioButton(tabName).waitUntil(isDisplayed()).click();

        basePageSteps.onPofferPage().markBlock().should(not(isDisplayed()));
        basePageSteps.onPofferPage().banner(tabName.toLowerCase()).waitUntil(isDisplayed()).should(
                hasText(format(BANNER_TEMPLATE, bannerVehicleType)));
        urlSteps.desktopURI().path(path).path(USED).path(ADD).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка «Скачать» в баннере «Продолжайте в приложении»")
    public void shouldSeeLinkDownloadInBanner() {
        basePageSteps.onPofferPage().radioButton(tabName).waitUntil(isDisplayed()).click();

        basePageSteps.onPofferPage().banner(tabName.toLowerCase()).downloadLink().should(hasAttribute(
                "href", allOf(
                        containsString("https://sb76.adj.st/add"),
                        containsString(format("adjust_campaign=%s", campaign)),
                        containsString("adjust_creative=poffer_category_stub"),
                        containsString(format("adjust_fallback=%s",
                                encode(urlSteps.mobileURI().path(path).path(USED).path(ADD).toString()))),
                        containsString(format("adjust_deeplink=%s", encode("autoru://app/add")))
                )
        ));
    }

}
