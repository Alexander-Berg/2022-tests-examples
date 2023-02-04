package ru.auto.tests.mobile.adbanners;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BANNERS;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.page.AdsPage.GALLERY_MOBILE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Баннеры в полной галерее карточки")
@Feature(BANNERS)
@GuiceModules(MobileEmulationTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardFullGalleryBannersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {CARS},
                {MOTORCYCLE},
                {LCV}
        });
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Клик по баннеру в полной галерее")
    public void shouldClickBannerFullGallery() {
        cookieSteps.setCookie("card_prevnext_swipe_info", "1",
                format(".%s", urlSteps.getConfig().getBaseDomain()));

        urlSteps.testing().path(category).path(USED).addParam("seller_group", "PRIVATE").open();
        basePageSteps.onListingPage().getSale(3).title().click();
        basePageSteps.onCardPage().features().waitUntil(isDisplayed());
        basePageSteps.onCardPage().gallery().getItem(0).click();

        int galleryFullItems = basePageSteps.onCardPage().fullScreenGallery().itemsList().size() - 1;
        //чтоб убрать промку с кредитами

        basePageSteps.onCardPage().fullScreenGallery().getItem(galleryFullItems).waitUntil(isDisplayed()).hover();
        basePageSteps.onAdsPage().ad(GALLERY_MOBILE).hover().click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}
