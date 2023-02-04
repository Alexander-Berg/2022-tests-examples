package ru.yandex.general.sellerProfile;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.HOMEPAGE_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.PROFILE_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(PROFILE_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Переход с профиля продавца через ссылки из попапа «Юзер инфо» на странички ЛК")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class NavigationFromSellerProfileToLkUserInfoPopupTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Parameterized.Parameter
    public String linkName;

    @Parameterized.Parameter(1)
    public String h1;

    @Parameterized.Parameter(2)
    public String path;

    @Parameterized.Parameters(name = "Ссылка «{0}»")
    public static Collection<Object[]> links() {
        return asList(new Object[][]{
                {"Мои объявления", "Мои объявления", OFFERS},
                {"Статистика", "Статистика", STATS},
                {"Избранное", "Избранное", FAVORITES},
                {"Автозагрузка", "Автозагрузка", FEED},
                {"Настройки", "Настройки профиля", CONTACTS}
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(PROFILE).path(SELLER_PATH).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с профиля продавца через ссылки из попапа «Юзер инфо» на странички ЛК")
    public void shouldSeeSellerProfileToLkPagesFromUserInfoPopup() {
        basePageSteps.onProfilePage().header().avatar().click();
        basePageSteps.onProfilePage().userInfoPopup().link(linkName).click();

        basePageSteps.onBasePage().textH1().should(hasText(h1));
        urlSteps.testing().path(MY).path(path).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с профиля продавца через ссылки из попапа «Юзер инфо» из прилипшего хэдера на странички ЛК")
    public void shouldSeeSellerProfileToLkPagesFromUserInfoPopupFloated() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onProfilePage().floatedHeader().avatar().click();
        basePageSteps.onProfilePage().userInfoPopup().link(linkName).click();

        basePageSteps.onBasePage().textH1().should(hasText(h1));
        urlSteps.testing().path(MY).path(path).shouldNotDiffWithWebDriverUrl();
    }

}
