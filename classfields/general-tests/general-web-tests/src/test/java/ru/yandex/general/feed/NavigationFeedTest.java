package ru.yandex.general.feed;

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

import static ru.yandex.general.consts.GeneralFeatures.FEEDS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(FEEDS_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с раздела «Автозагрузка» в ЛК")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class NavigationFeedTest {

    private static final String MY_OFFERS_TITLE = "Мои объявления";
    private static final String FAVORITES_TITLE = "Избранное";

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
        passportSteps.accountWithOffersLogin();
        urlSteps.testing().path(MY).path(FEED).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с «Автозагрузка» в «Избранное» по клику в хедере")
    public void shouldSeeFeedToFavoritesFromHeader() {
        basePageSteps.onFeedPage().header().linkWithTitle(FAVORITES_TITLE).click();

        basePageSteps.onFavoritesPage().textH1().should(hasText(FAVORITES_TITLE));
        urlSteps.testing().path(MY).path(FAVORITES).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с «Автозагрузка» в «Избранное» по клику в прилипшем хедере")
    public void shouldSeeFeedToFavoritesFromFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onFeedPage().floatedHeader().linkWithTitle(FAVORITES_TITLE).click();

        basePageSteps.onFavoritesPage().textH1().should(hasText(FAVORITES_TITLE));
        urlSteps.testing().path(MY).path(FAVORITES).shouldNotDiffWithWebDriverUrl();
    }

}
