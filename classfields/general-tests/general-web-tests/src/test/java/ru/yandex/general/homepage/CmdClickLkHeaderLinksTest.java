package ru.yandex.general.homepage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.general.consts.GeneralFeatures.HOMEPAGE_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.element.Header.FAVORITE;
import static ru.yandex.general.element.Header.MY_OFFERS;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(HOMEPAGE_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с главной. Открытие ссылок по CMD + Click в новом окне")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class CmdClickLkHeaderLinksTest {

    private static final String FAVORITES_TITLE = "Избранное";
    private static final String MY_OFFERS_TITLE = "Мои объявления";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход в «Избранное» по клику в хэдере")
    public void shouldSeeGoToFavoritesFromHeaderCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onListingPage().header().linkWithTitle(FAVORITE));
        basePageSteps.switchToNextTab();

        basePageSteps.onMyOffersPage().textH1().should(hasText(FAVORITES_TITLE));
        urlSteps.testing().path(MY).path(FAVORITES).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход с главной на статистику из попапа «Юзер инфо» на странички ЛК")
    public void shouldSeeHomePageToStatisticsFromUserInfoPopupCmdClick() {
        basePageSteps.onListingPage().header().avatar().click();
        basePageSteps.cmdClick(basePageSteps.onListingPage().userInfoPopup().link("Статистика"));
        basePageSteps.switchToNextTab();

        basePageSteps.onBasePage().textH1().should(hasText("Статистика"));
        urlSteps.testing().path(MY).path(STATS).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход с главной на автозагрузку из попапа «Юзер инфо» из прилипшего хэдера")
    public void shouldSeeHomePageToLkPagesFromUserInfoPopupFloated() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onListingPage().floatedHeader().avatar().click();
        basePageSteps.cmdClick(basePageSteps.onListingPage().userInfoPopup().link("Автозагрузка"));
        basePageSteps.switchToNextTab();

        basePageSteps.onBasePage().textH1().should(hasText("Автозагрузка"));
        urlSteps.testing().path(MY).path(FEED).shouldNotDiffWithWebDriverUrl();
    }

}
