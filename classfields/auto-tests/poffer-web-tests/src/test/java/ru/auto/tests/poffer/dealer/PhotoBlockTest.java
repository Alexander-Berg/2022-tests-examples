package ru.auto.tests.poffer.dealer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import pazone.ashot.Screenshot;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.PofferSteps;
import ru.auto.tests.passport.account.Account;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.desktop.consts.AutoruFeatures.POFFER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Дилер, блок фото")
@Feature(POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class PhotoBlockTest {

    private static final String IMAGE_FILE = new File("src/main/resources/images/lifan_solano.jpg").getAbsolutePath();
    private Account account;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PofferSteps pofferSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        account = pofferSteps.linkUserToDealer();
        loginSteps.loginAs(account);
        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
        pofferSteps.onPofferPage().firstStepStsVinBlock().button("Пропустить").click();
        pofferSteps.fillMark("Lifan");
        pofferSteps.fillModel("Solano");
        pofferSteps.fillYear("2020");
        pofferSteps.fillBody("Седан");
        pofferSteps.fillGeneration("II");
        pofferSteps.fillEngine("Бензин");
        pofferSteps.fillDrive("Передний");
        pofferSteps.fillGearbox("Механическая");
        pofferSteps.fillModification("100\u00a0л.с.");
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение фоторедактора")
    @Category({Regression.class, Screenshooter.class, Testing.class})
    public void shouldSeePhotoEditor() throws InterruptedException {
        screenshotSteps.setWindowSizeForScreenshot();

        pofferSteps.onPofferPage().photoBlock().photo().sendKeys(IMAGE_FILE);
        pofferSteps.onPofferPage().photoEditor().button("Готово").waitUntil(isDisplayed());
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(pofferSteps.onPofferPage().photoEditor());

        pofferSteps.onPofferPage().photoEditor().button("Готово").click();
        pofferSteps.onPofferPage().breadcrumbs().button("Очистить").click();
        TimeUnit.SECONDS.sleep(3);
        urlSteps.onCurrentUrl().setProduction().open();
        pofferSteps.onPofferPage().firstStepStsVinBlock().button("Пропустить").click();
        pofferSteps.fillMark("Lifan");
        pofferSteps.fillModel("Solano");
        pofferSteps.fillYear("2020");
        pofferSteps.fillBody("Седан");
        pofferSteps.fillGeneration("II");
        pofferSteps.fillEngine("Бензин");
        pofferSteps.fillDrive("Передний");
        pofferSteps.fillGearbox("Механическая");
        pofferSteps.fillModification("100\u00a0л.с.");
        pofferSteps.onPofferPage().photoBlock().photo().sendKeys(IMAGE_FILE);
        pofferSteps.onPofferPage().photoEditor().button("Готово").waitUntil(isDisplayed());
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(pofferSteps.onPofferPage().photoEditor());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @After
    public void after() {
        pofferSteps.unlinkUserFromDealer(account.getId());
    }
}
