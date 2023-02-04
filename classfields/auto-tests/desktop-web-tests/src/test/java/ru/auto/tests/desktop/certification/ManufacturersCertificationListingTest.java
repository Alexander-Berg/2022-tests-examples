package ru.auto.tests.desktop.certification;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CERTIFICATION;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сертификация - сертификация производителей в листинге")
@Feature(CERTIFICATION)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ManufacturersCertificationListingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    //@Parameter("Тег")
    @Parameterized.Parameter
    public String searchTag;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"certificate_manufacturer"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsCertificateManufacturer").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam("search_tag", searchTag).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onListingPage().getSale(0), 0, 0);
        basePageSteps.onListingPage().getSale(0).certificationIcon().waitUntil(isDisplayed()).hover();
        basePageSteps.onListingPage().activePopup().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поп-ап с описанием программы сертификации производителя")
    public void shouldSeeCertificateHint() {
        basePageSteps.onListingPage().activePopup().should(hasText("Land Rover Approved — это программа продаж " +
                "сертифицированных автомобилей Land Rover с пробегом, которые предлагаются клиентам на самых выгодных " +
                "условиях. К участию в программе допускаются только тщательно проверенные автомобили с пробегом " +
                "до 150 000 км и возрастом до 7 лет, доставленные на территорию РФ официальным импортером.\n\n" +
                "Каждый автомобиль прошел внимательный отбор, обязательную техническую проверку по 165 пунктам и полную " +
                "предпродажную подготовку с устранением всех выявленных недостатков квалифицированными механиками " +
                "с использованием оригинальных запчастей. История каждого из них прозрачна. Их надежность " +
                "доказана на деле!\nПодробнее о программе\nРеклама"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке в поп-апе")
    public void shouldClickPopupUrl() {
        basePageSteps.onListingPage().activePopupLink()
                .should(hasAttribute("href",
                        "http://www.landrover.ru/approved-used/index.html/?utm_source=manuf_cert"));
    }
}