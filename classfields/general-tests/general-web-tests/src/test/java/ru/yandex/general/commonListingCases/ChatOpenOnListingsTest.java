package ru.yandex.general.commonListingCases;

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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.CHAT_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.element.Header.CHATS;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic("Общие кейсы для страниц с листингами")
@Feature(CHAT_FEATURE)
@DisplayName("Открытие чата с главной/категории/страницы продавца")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ChatOpenOnListingsTest {

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
    public String name;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "Страница «{0}»")
    public static Collection<Object[]> links() {
        return asList(new Object[][]{
                {"Главная", ""},
                {"Листинг категории", ELEKTRONIKA},
                {"Профиль продавца", format("%s%s", PROFILE, SELLER_PATH).replace("//", "/")}
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(path).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие чатов по клику в хэдере с главной/категории/страницы продавца")
    public void shouldSeeChatOpenFromHeader() {
        basePageSteps.onBasePage().header().spanLinkWithTitle(CHATS).click();

        basePageSteps.onBasePage().chatPopup().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие чатов по клику в прилипшем хэдере с главной/категории/страницы продавца")
    public void shouldSeeChatOpenFromFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onBasePage().floatedHeader().spanLinkWithTitle(CHATS).click();

        basePageSteps.onBasePage().chatPopup().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие чатов по клику в попапе «Юзер инфо» с главной/категории/страницы продавца")
    public void shouldSeeChatOpenFromUserInfoPopup() {
        basePageSteps.onBasePage().header().avatar().click();
        basePageSteps.onBasePage().userInfoPopup().spanLink(CHATS).waitUntil(isDisplayed()).click();

        basePageSteps.onBasePage().chatPopup().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие чатов по клику в попапе «Юзер инфо» из прилипшего хэдера с главной/категории/страницы продавца")
    public void shouldSeeChatOpenFromUserInfoPopupFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onBasePage().floatedHeader().avatar().click();
        basePageSteps.onBasePage().userInfoPopup().spanLink(CHATS).waitUntil(isDisplayed()).click();

        basePageSteps.onBasePage().chatPopup().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие чатов по клику виджет чатов с главной/категории/страницы продавца")
    public void shouldSeeChatOpenFromWidget() {
        basePageSteps.onBasePage().chatWidgetButton().click();

        basePageSteps.onBasePage().chatPopup().should(isDisplayed());
    }

}
