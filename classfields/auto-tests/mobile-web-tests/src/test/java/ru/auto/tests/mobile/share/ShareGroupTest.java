package ru.auto.tests.mobile.share;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SHARE;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Поделяшки на группе")
@Feature(SHARE)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ShareGroupTest {

    private static final String PATH = "/kia/optima/21342050-21342121/";
    private static final String SHARE_TEXT = "%D0%9A%D1%83%D0%BF%D0%B8%D1%82%D1%8C%20%D0%BD%D0%BE%D0%B2%D1%8B%D0%B9%20Kia%20Optima%20IV%20%D0%A0%D0%B5%D1%81%D1%82%D0%B0%D0%B9%D0%BB%D0%B8%D0%BD%D0%B3%20%D0%A1%D0%B5%D0%B4%D0%B0%D0%BD%20%D0%90%D0%B2%D1%82%D0%BE.%D1%80%D1%83";

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("mobile/SearchCarsBreadcrumbsMarkModelGroup",
                "mobile/SearchCarsGroupContextGroup",
                "mobile/SearchCarsGroupContextListing",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
        basePageSteps.onGroupPage().shareButton().click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("ВКонтакте")
    @Category({Regression.class, Testing.class})
    public void shouldSeeVkButton() {
        basePageSteps.onGroupPage().popup().button("ВКонтакте").should(isDisplayed())
                .should(hasAttribute("href", "https://vk.com/share.php?url=https%3A%2F%2Ftest.avto.ru%2Fmoskva%2Fcars%2Fnew%2Fgroup%2Fkia%2Foptima%2F21342050-21342121%2F&title=%D0%9A%D1%83%D0%BF%D0%B8%D1%82%D1%8C%20%D0%BD%D0%BE%D0%B2%D1%8B%D0%B9%20Kia%20Optima%20IV%20%D0%A0%D0%B5%D1%81%D1%82%D0%B0%D0%B9%D0%BB%D0%B8%D0%BD%D0%B3%20%D0%A1%D0%B5%D0%B4%D0%B0%D0%BD%20%D0%90%D0%B2%D1%82%D0%BE.%D1%80%D1%83&image=https%3A%2F%2Fimages.mds-proxy.test.avto.ru%2Fget-autoru-vos%2F2154938%2Fd8fbdfbec250287a57b4a2108240774f%2F1200x900&utm_source=share2"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Одноклассники")
    @Category({Regression.class, Testing.class})
    public void shouldSeeOkButton() {
        basePageSteps.onGroupPage().popup().button("Одноклассники").should(isDisplayed())
                .should(hasAttribute("href", "https://connect.ok.ru/offer?url=https%3A%2F%2Ftest.avto.ru%2Fmoskva%2Fcars%2Fnew%2Fgroup%2Fkia%2Foptima%2F21342050-21342121%2F&title=%D0%9A%D1%83%D0%BF%D0%B8%D1%82%D1%8C%20%D0%BD%D0%BE%D0%B2%D1%8B%D0%B9%20Kia%20Optima%20IV%20%D0%A0%D0%B5%D1%81%D1%82%D0%B0%D0%B9%D0%BB%D0%B8%D0%BD%D0%B3%20%D0%A1%D0%B5%D0%B4%D0%B0%D0%BD%20%D0%90%D0%B2%D1%82%D0%BE.%D1%80%D1%83&imageUrl=https%3A%2F%2Fimages.mds-proxy.test.avto.ru%2Fget-autoru-vos%2F2154938%2Fd8fbdfbec250287a57b4a2108240774f%2F1200x900&utm_source=share2"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("WhatsApp")
    @Category({Regression.class, Testing.class})
    public void shouldSeeWhatsAppButton() {
        basePageSteps.onGroupPage().popup().button("WhatsApp").should(isDisplayed())
                .should(hasAttribute("href", "https://api.whatsapp.com/send?text=%D0%9A%D1%83%D0%BF%D0%B8%D1%82%D1%8C%20%D0%BD%D0%BE%D0%B2%D1%8B%D0%B9%20Kia%20Optima%20IV%20%D0%A0%D0%B5%D1%81%D1%82%D0%B0%D0%B9%D0%BB%D0%B8%D0%BD%D0%B3%20%D0%A1%D0%B5%D0%B4%D0%B0%D0%BD%20%D0%90%D0%B2%D1%82%D0%BE.%D1%80%D1%83%20https%3A%2F%2Ftest.avto.ru%2Fmoskva%2Fcars%2Fnew%2Fgroup%2Fkia%2Foptima%2F21342050-21342121%2F&utm_source=share2"));
    }
}
