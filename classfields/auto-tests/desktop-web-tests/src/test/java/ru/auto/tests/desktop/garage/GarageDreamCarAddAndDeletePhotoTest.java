package ru.auto.tests.desktop.garage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.DREAMCAR;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.element.garage.CardForm.CHOOSE_MARK;
import static ru.auto.tests.desktop.element.garage.CardForm.CHOOSE_MODEL;
import static ru.auto.tests.desktop.element.garage.CardForm.GENERATION;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка машины мечты, добавляем/удаляем фото")
@Epic(AutoruFeatures.GARAGE)
@Feature(AutoruFeatures.DREAM_CAR)
@Story("Карточка машины мечты")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class GarageDreamCarAddAndDeletePhotoTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private AccountManager am;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Before
    public void before() throws IOException {
        Account account = am.create();
        loginSteps.loginAs(account);

        urlSteps.testing().path(GARAGE).path(ADD).path(DREAMCAR).open();

        basePageSteps.onGarageCardPage().form().block(CHOOSE_MARK).item("Audi").waitUntil(isDisplayed()).click();
        basePageSteps.onGarageCardPage().form().block(CHOOSE_MODEL).radioButton("A3").waitUntil(isDisplayed()).click();
        basePageSteps.onGarageCardPage().form().block(GENERATION).radioButton("2016 - 2020 III (8V) Рестайлинг")
                .waitUntil(isDisplayed()).click();
        basePageSteps.onGarageCardPage().submitButton().click();
        basePageSteps.onGarageCardPage().h1().waitUntil(hasText("Audi A3"));
        basePageSteps.onGarageCardPage().gallery().photoInput()
                .sendKeys(new File("src/test/resources/images/audi_a3.jpeg").getAbsolutePath());
        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фото, проверяем запрос /garageUpdateCard")
    public void shouldSeeGarageUpdateCardRequestAfterUploadPhoto() {
        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/desktop/garageUpdateCard/",
                hasJsonBody("request/GarageDreamCarCardWithImage.json")
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фото, затем удаляем, проверяем запрос /garageUpdateCard")
    public void shouldSeeGarageUpdateCardRequestAfterDeletePhoto() {
        basePageSteps.onGarageCardPage().gallery().hover();
        basePageSteps.onGarageCardPage().gallery().deletePhoto().click();
        basePageSteps.acceptAlert();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/desktop/garageUpdateCard/",
                hasJsonBody("request/GarageDreamCarCardWithoutImage.json")
        ));
    }

}
