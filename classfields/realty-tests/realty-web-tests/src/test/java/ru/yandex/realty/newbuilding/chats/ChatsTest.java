package ru.yandex.realty.newbuilding.chats;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.NewBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.element.newbuildingsite.ChatMessage.BIND_PHONE;
import static ru.yandex.realty.element.newbuildingsite.SiteCardAbout.CHAT_WITH_DEVELOPER;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Чаты с застройщиком")
@Link("https://st.yandex-team.ru/VERTISTEST-1754")
@Feature(NEWBUILDING_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ChatsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Account account;

    @Inject
    private NewBuildingSteps newBuildingSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Открываем чат, видим первое сообщение")
    public void shouldSeeChat() {
        apiSteps.createVos2Account(account, OWNER);
        urlSteps.testing().newbuildingSiteWithChats().open();
        newBuildingSteps.refreshUntil(() -> {
            newBuildingSteps.refresh();
            newBuildingSteps.onNewBuildingSitePage().siteCardAbout().button(CHAT_WITH_DEVELOPER).click();
            return newBuildingSteps.onNewBuildingSitePage().developerChat().firstMessage();
        }, isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот модуля телефона")
    public void shouldSeePhoneBindModalScreenshot() {
        apiSteps.createVos2Account(account, OWNER);
        urlSteps.testing().newbuildingSiteWithChats().open();
        newBuildingSteps.refreshUntil(() -> {
            newBuildingSteps.refresh();
            newBuildingSteps.onNewBuildingSitePage().siteCardAbout().button(CHAT_WITH_DEVELOPER).click();
            return newBuildingSteps.onNewBuildingSitePage().developerChat().firstMessage();
        }, isDisplayed());
        newBuildingSteps.onNewBuildingSitePage().developerChat().firstMessage().wantToBuy().click();
        newBuildingSteps.onNewBuildingSitePage().developerChat().messages().waitUntil(hasSize(3));
        newBuildingSteps.onNewBuildingSitePage().developerChat().textarea().sendKeys("тест" + Keys.ENTER);
        newBuildingSteps.onNewBuildingSitePage().developerChat().phoneBindModal().click();
        Screenshot testing = compareSteps.takeScreenshot(
                newBuildingSteps.onNewBuildingSitePage().developerChat().phoneBindModal());
        urlSteps.setProductionHost().open();
        newBuildingSteps.onNewBuildingSitePage().siteCardAbout().button(CHAT_WITH_DEVELOPER).click();
        newBuildingSteps.onNewBuildingSitePage().developerChat().messages().waitUntil(hasSize(5)).get(4)
                .chatMessageButton(BIND_PHONE).click();
        newBuildingSteps.onNewBuildingSitePage().developerChat().phoneBindModal().click();
        Screenshot production = compareSteps.takeScreenshot(
                newBuildingSteps.onNewBuildingSitePage().developerChat().phoneBindModal());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот чата")
    public void shouldSeeChatScreenshot() {
        apiSteps.createVos2Account(account, OWNER);
        compareSteps.resize(1600, 2000);
        urlSteps.testing().newbuildingSiteWithChats().open();
        newBuildingSteps.refreshUntil(() -> {
            newBuildingSteps.refresh();
            newBuildingSteps.onNewBuildingSitePage().siteCardAbout().button(CHAT_WITH_DEVELOPER).click();
            return newBuildingSteps.onNewBuildingSitePage().developerChat().firstMessage();
        }, isDisplayed());
        newBuildingSteps.refresh();
        newBuildingSteps.onNewBuildingSitePage().siteCardAbout().button(CHAT_WITH_DEVELOPER).click();
        newBuildingSteps.onNewBuildingSitePage().developerChat().click();
        Screenshot testing = compareSteps.takeScreenshot(newBuildingSteps.onNewBuildingSitePage().developerChat());
        urlSteps.setProductionHost().open();
        newBuildingSteps.onNewBuildingSitePage().siteCardAbout().button(CHAT_WITH_DEVELOPER).click();
        newBuildingSteps.onNewBuildingSitePage().developerChat().click();
        Screenshot production = compareSteps.takeScreenshot(newBuildingSteps.onNewBuildingSitePage().developerChat());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Закрываем чат")
    public void shouldSeeClosedChat() {
        apiSteps.createVos2Account(account, OWNER);
        urlSteps.testing().newbuildingSiteWithChats().open();
        newBuildingSteps.refreshUntil(() -> {
            newBuildingSteps.refresh();
            newBuildingSteps.onNewBuildingSitePage().siteCardAbout().button(CHAT_WITH_DEVELOPER).click();
            return newBuildingSteps.onNewBuildingSitePage().developerChat().firstMessage();
        }, isDisplayed());
        newBuildingSteps.onNewBuildingSitePage().developerChat().closeCross().click();
        newBuildingSteps.onNewBuildingSitePage().developerChat().should(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Под незалогином ведет на паспорт, после залогина открывается чат")
    public void shouldSeeAuthPage() {
        apiSteps.createVos2AccountWithoutLogin(account, OWNER);
        newBuildingSteps.refreshUntil(() -> {
            newBuildingSteps.getDriver().manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS);
            urlSteps.testing().newbuildingSiteWithChats().open();
            newBuildingSteps.onNewBuildingSitePage().siteCardAbout().button(CHAT_WITH_DEVELOPER).click();
            return basePageSteps.onPassportLoginPage().login();
        }, isDisplayed(), 120);
        basePageSteps.onPassportLoginPage().loginInPassport(account);
        newBuildingSteps.onNewBuildingSitePage().developerChat().should(isDisplayed());
    }

}
