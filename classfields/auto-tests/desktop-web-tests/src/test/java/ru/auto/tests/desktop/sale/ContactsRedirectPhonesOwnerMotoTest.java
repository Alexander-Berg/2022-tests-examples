package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления легковых - подменный номер")
@Feature(AutoruFeatures.SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ContactsRedirectPhonesOwnerMotoTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    private static final String DESCRIPTION = "Защитный номер\nВаш номер\n+7 222 222-22-22\n+7 111 111-11-11\n" +
            "Блокируются\nРекламные звонки,\nсмс-спам\nПроходят\nЗвонки покупателей\nОтключить защиту";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferMotoUsedUserOwner",
                "desktop/UserOffersMotoCallHistory").post();

        cookieSteps.setCookieForBaseDomain("calls-promote-closed", "true");
        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение истории звонков")
    public void shouldSeeCallHistory() {
        basePageSteps.onCardPage().cardHeader().stats().callHistoryButton().click();
        basePageSteps.onCardPage().callHistoryPopup().waitUntil(isDisplayed()).should(hasText("История звонков\n" +
                "+7 495 132-17-26\n8 февраля 2019, 14:45\n16 c\n+7 495 132-17-27\n8 января 2019, 15:45\n50 c"
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа «Почему номер не мой?»")
    public void shouldSeePopup() {
        basePageSteps.onCardPage().contacts().phonesIsVirtualDescription().hover();
        basePageSteps.onCardPage().activePopup().waitUntil(isDisplayed()).waitUntil(hasText(DESCRIPTION));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Отключить защиту»")
    public void shouldClickDisableUrl() {
        basePageSteps.onCardPage().contacts().phonesIsVirtualDescription().hover();
        basePageSteps.onCardPage().activePopup().should(isDisplayed());
        basePageSteps.onCardPage().activePopupLink("Отключить защиту").hover().click();
        urlSteps.testing().path(MOTO).path(EDIT).path(SALE_ID).shouldNotSeeDiff();
    }
}