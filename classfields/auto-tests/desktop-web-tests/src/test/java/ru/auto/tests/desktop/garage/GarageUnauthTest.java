package ru.auto.tests.desktop.garage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.PRIMER_OTCHETA;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.page.GaragePage.ADD_CAR;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;

@DisplayName("Гараж")
@Story("Промо страница под незарегом")
@Feature(AutoruFeatures.GARAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GarageUnauthTest {

    long pageOffset;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth").post();

        urlSteps.testing().path(GARAGE).open();
        pageOffset = basePageSteps.getPageYOffset();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("клик на кнопку «Добавить автомобиль»")
    public void shouldClickAddButton() {
        basePageSteps.onGaragePage().addBlock().button(ADD_CAR).click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN).addParam("r", encode(format("%s/garage/add/",
                urlSteps.getConfig().getTestingURI()))).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Войдите, чтобы увидеть свои автомобили»")
    public void shouldClickAuthUrl() {
        basePageSteps.onGaragePage().button("Войдите, чтобы увидеть свои автомобили").click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN).addParam("r", encode(format("%s/garage/",
                urlSteps.getConfig().getTestingURI()))).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Проверить автомобиль»")
    public void shouldClickCheckAutoButton() {
        basePageSteps.onGaragePage().block("Отзывные кампании").button("Проверить автомобиль").click();

        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", format("%s/garage/add-conditional/", urlSteps.getConfig().getDesktopURI()))
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Оценить автомобиль»")
    public void shouldClickEvaluateAutoButton() {
        basePageSteps.onGaragePage().block("Оценка автомобиля").button("Оценить автомобиль").click();

        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", format("%s/garage/add-conditional/", urlSteps.getConfig().getDesktopURI()))
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Добавить автомобиль в гараж»")
    public void shouldClickAddToGarageButton() {
        basePageSteps.onGaragePage().block("Полная история вашего автомобиля")
                .button("Добавить автомобиль в гараж").click();

        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", format("%s/garage/add-conditional/", urlSteps.getConfig().getDesktopURI()))
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Пример отчёта»")
    public void shouldClickExampleUrl() {
        mockRule.with("desktop/CarfaxReportRawExample").update();

        basePageSteps.onGaragePage().block("Полная история вашего автомобиля").button("Пример отчёта").click();
        urlSteps.testing().path(PRIMER_OTCHETA).shouldNotSeeDiff();
    }
}
