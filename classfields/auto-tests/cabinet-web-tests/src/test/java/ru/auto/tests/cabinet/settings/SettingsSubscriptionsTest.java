package ru.auto.tests.cabinet.settings;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.SETTINGS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.SUBSCRIPTIONS;
import static ru.auto.tests.desktop.page.cabinet.CabinetSettingsSubscriptionsPage.DATA_SUCCESSFULLY_SAVED_POPUP;
import static ru.auto.tests.desktop.page.cabinet.CabinetSettingsSubscriptionsPage.PROJECT_NEWS_BLOCK;
import static ru.auto.tests.desktop.utils.Utils.getRandomEmail;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 3.09.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Настройки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class SettingsSubscriptionsTest {

    private static final String CHECKBOX_CHECKED = "Checkbox_checked";
    private static final String NOTIFIER_VISIBLE = "notifier_visible";
    private static final int TIMEOUT = 15;
    private static final String WAIT_MASSAGE = "Ждем пока поп-ап «Данные успешно сохранены» исчезнет со страницы";
    private static final int MAX_EMAILS_COUNT = 10;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/ApiSubscriptionsClient",
                "cabinet/ApiSubscriptionClientId1").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SETTINGS).path(SUBSCRIPTIONS).open();
        disableSubscriptionIfNecessary(PROJECT_NEWS_BLOCK);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Включение подписки")
    public void shouldSeeNewEmail() {
        steps.onSettingsSubscriptionsPage().mailingListBlock(PROJECT_NEWS_BLOCK).checkbox().click();
        steps.onSettingsSubscriptionsPage().serviceStatusPopup(DATA_SUCCESSFULLY_SAVED_POPUP).should(isDisplayed());
        shouldSeeAccountEmail();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Удаление единственного почтового адреса")
    public void shouldSeeRemovedSubscription() {
        mockRule.with("cabinet/ApiSubscriptionClientId1Delete").update();

        steps.onSettingsSubscriptionsPage().mailingListBlock(PROJECT_NEWS_BLOCK).checkbox().click();
        steps.onSettingsSubscriptionsPage().mailingListBlock(PROJECT_NEWS_BLOCK).lastEmailBlock().remove().click();

        steps.onSettingsSubscriptionsPage().serviceStatusPopup(DATA_SUCCESSFULLY_SAVED_POPUP).should(isDisplayed());
        steps.onSettingsSubscriptionsPage().mailingListBlock(PROJECT_NEWS_BLOCK).emailBlocks().should(not(isDisplayed()));
        steps.onSettingsSubscriptionsPage().mailingListBlock(PROJECT_NEWS_BLOCK).checkbox()
                .should(not(hasClass(containsString(CHECKBOX_CHECKED))));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Добавление 10 почтовых адресов")
    public void shouldSeeFiveAddedEmail() {
        steps.onSettingsSubscriptionsPage().mailingListBlock(PROJECT_NEWS_BLOCK).checkbox().click();

        addEmails(MAX_EMAILS_COUNT, PROJECT_NEWS_BLOCK);
        steps.onSettingsSubscriptionsPage().mailingListBlock(PROJECT_NEWS_BLOCK).lastEmailBlock().add()
                .should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Удаление почтового адреса")
    public void shouldRemoveOneEmail() {
        steps.onSettingsSubscriptionsPage().mailingListBlock(PROJECT_NEWS_BLOCK).checkbox().click();
        waitSomething(3, TimeUnit.SECONDS);

        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiSubscriptionClientId2",
                "cabinet/ApiSubscriptionClientId2Delete").post();

        addEmailToLastEmailBlock(PROJECT_NEWS_BLOCK);
        steps.onSettingsSubscriptionsPage().serviceStatusPopup(DATA_SUCCESSFULLY_SAVED_POPUP)
                .waitUntil(WAIT_MASSAGE, hasClass(not(containsString(NOTIFIER_VISIBLE))), TIMEOUT);
        steps.onSettingsSubscriptionsPage().mailingListBlock(PROJECT_NEWS_BLOCK).lastEmailBlock().remove().click();
        steps.onSettingsSubscriptionsPage().serviceStatusPopup(DATA_SUCCESSFULLY_SAVED_POPUP).should(isDisplayed());
        steps.onSettingsSubscriptionsPage().mailingListBlock(PROJECT_NEWS_BLOCK).emailBlocks().should(hasSize(1));
        steps.onSettingsSubscriptionsPage().mailingListBlock(PROJECT_NEWS_BLOCK).checkbox()
                .should(hasClass(containsString(CHECKBOX_CHECKED)));
    }

    @Step("«Отключаем подписку {subscription}» если она включена")
    private void disableSubscriptionIfNecessary(String subscription) {
        if (steps.onSettingsSubscriptionsPage().mailingListBlock(subscription).checkbox().getAttribute("class")
                .contains(CHECKBOX_CHECKED)) {
            steps.onSettingsSubscriptionsPage().mailingListBlock(subscription).checkbox().click();
            steps.onSettingsSubscriptionsPage().serviceStatusPopup(DATA_SUCCESSFULLY_SAVED_POPUP)
                    .waitUntil(WAIT_MASSAGE, hasClass(not(containsString(NOTIFIER_VISIBLE))), TIMEOUT);
        }
    }

    @Step("Проверяем, что на блоке отображается почта аккаунта")
    private void shouldSeeAccountEmail() {
        assertEquals(steps.onSettingsSubscriptionsPage().mailingListBlock(PROJECT_NEWS_BLOCK).lastEmailBlock()
                .getCurrentEmail(), "aristos@ma.ru");
    }

    @Step("Добавляем {count} рандомных почтовых адресов в подписку «{subscription}»")
    private void addEmails(int count, String subscription) {
        for (int curEmails = 1; curEmails < count; curEmails++) addEmailToLastEmailBlock(subscription);
    }

    @Step("Добавляем рандомную почту в последнюю подписку «{subscription}»")
    private void addEmailToLastEmailBlock(String subscription) {
        steps.onSettingsSubscriptionsPage().mailingListBlock(subscription).lastEmailBlock().add().click();
        steps.onSettingsSubscriptionsPage().mailingListBlock(subscription).lastEmailBlock().emailInput().click();
        steps.onSettingsSubscriptionsPage().mailingListBlock(subscription).lastEmailBlock().emailInput()
                .sendKeys(getRandomEmail());
        steps.onSettingsSubscriptionsPage().mailingListBlock(subscription).lastEmailBlock().emailInput().sendKeys(Keys.ENTER);
        steps.onSettingsSubscriptionsPage().serviceStatusPopup(DATA_SUCCESSFULLY_SAVED_POPUP).should(isDisplayed());
    }
}
