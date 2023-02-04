package ru.yandex.general.commonLkCases;

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
import static ru.yandex.general.consts.GeneralFeatures.CHAT_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.element.Header.CHATS;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic("Общие кейсы для ЛК")
@Feature(CHAT_FEATURE)
@DisplayName("Открытие чата со всех страниц ЛК")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ChatOpenOnLkPagesTest {

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
                {"Статистика", STATS},
                {"Мои объявления", OFFERS},
                {"Избранное", FAVORITES},
                {"Автозагрузка", FEED},
                {"Настройки", CONTACTS}
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW, "1");
        passportSteps.accountWithOffersLogin();
        urlSteps.testing().path(MY).path(path).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие чатов по клику в хэдере со всех страниц ЛК")
    public void shouldSeeChatOpenFromHeader() {
        basePageSteps.onBasePage().header().spanLinkWithTitle(CHATS).click();

        basePageSteps.onBasePage().chatPopup().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие чатов по клику в прилипшем хэдере со всех страниц ЛК")
    public void shouldSeeChatOpenFromFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onBasePage().floatedHeader().spanLinkWithTitle(CHATS).click();

        basePageSteps.onBasePage().chatPopup().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие чатов по клику в попапе «Юзер инфо» со всех страниц ЛК")
    public void shouldSeeChatOpenFromUserInfoPopup() {
        basePageSteps.onBasePage().header().avatar().click();
        basePageSteps.onBasePage().userInfoPopup().spanLink(CHATS).waitUntil(isDisplayed()).click();

        basePageSteps.onBasePage().chatPopup().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие чатов по клику в попапе «Юзер инфо» из прилипшего хэдера со всех страниц ЛК")
    public void shouldSeeChatOpenFromUserInfoPopupFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onBasePage().floatedHeader().avatar().click();
        basePageSteps.onBasePage().userInfoPopup().spanLink(CHATS).waitUntil(isDisplayed()).click();

        basePageSteps.onBasePage().chatPopup().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие чатов по клику виджет чатов со всех страниц ЛК")
    public void shouldSeeChatOpenFromWidget() {
        basePageSteps.onBasePage().chatWidgetButton().click();

        basePageSteps.onBasePage().chatPopup().should(isDisplayed());
    }

}
