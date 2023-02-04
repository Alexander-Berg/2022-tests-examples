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

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SHARE;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Поделяшки на карточке объявления")
@Feature(SHARE)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ShareSaleCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String SHARE_TEXT = "%D0%A1%D0%BC%D0%BE%D1%82%D1%80%D0%B8%D1%82%D0%B5%2C%20%D0%BA%D0%B0%D0%BA%D0%B0%D1%8F%20%D0%BC%D0%B0%D1%88%D0%B8%D0%BD%D0%B0%3A%20Land%20Rover%20Discovery%20III%202008%20%D0%B3%D0%BE%D0%B4%D0%B0%20%D0%B7%D0%B0%20700%C2%A0000%20%D1%80%D1%83%D0%B1%D0%BB%D0%B5%D0%B9%20%D0%BD%D0%B0%20%D0%90%D0%B2%D1%82%D0%BE.%D1%80%D1%83!";

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
        mockRule.newMock().with("desktop/OfferCarsUsedUser").post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().cardActions().shareButton());
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("ВКонтакте")
    @Category({Regression.class, Testing.class})
    public void shouldSeeVkButton() {
        basePageSteps.onCardPage().popup().button("ВКонтакте").should(isDisplayed())
                .should(hasAttribute("href", "https://vk.com/share.php?url=https%3A%2F%2Ftest.avto.ru%2Fcars%2Fused%2Fsale%2Fland_rover%2Fdiscovery%2F1076842087-f1e84%2F&title=%D0%A1%D0%BC%D0%BE%D1%82%D1%80%D0%B8%D1%82%D0%B5%2C%20%D0%BA%D0%B0%D0%BA%D0%B0%D1%8F%20%D0%BC%D0%B0%D1%88%D0%B8%D0%BD%D0%B0%3A%20Land%20Rover%20Discovery%20III%202008%20%D0%B3%D0%BE%D0%B4%D0%B0%20%D0%B7%D0%B0%20700%C2%A0000%20%D1%80%D1%83%D0%B1%D0%BB%D0%B5%D0%B9%20%D0%BD%D0%B0%20%D0%90%D0%B2%D1%82%D0%BE.%D1%80%D1%83!&description=%D0%92%D0%BD%D0%B5%D0%B4%D0%BE%D1%80%D0%BE%D0%B6%D0%BD%D0%B8%D0%BA%20Land%20Rover%20Discovery%20III%202008%20%D0%B3%D0%BE%D0%B4%D0%B0%2C%20%D0%BF%D1%80%D0%BE%D0%B1%D0%B5%D0%B3%20210%20000%20%D0%BA%D0%BC%2C%20%D0%B4%D0%B2%D0%B8%D0%B3%D0%B0%D1%82%D0%B5%D0%BB%D1%8C%202.7d%20AT%20(190%20%D0%BB.%D1%81.)%204WD%2C%20%D1%86%D0%B2%D0%B5%D1%82%20%D1%81%D0%B5%D1%80%D0%B5%D0%B1%D1%80%D0%BE%20%D0%B7%D0%B0%20700%C2%A0000%20%D1%80%D1%83%D0%B1%D0%BB%D0%B5%D0%B9.&image=https%3A%2F%2Fimages.mds-proxy.test.avto.ru%2Fget-autoru-vos%2F2171969%2F023fb9f39f18cb4818f4627eac2e9db9%2F1200x900&utm_source=share2"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Одноклассники")
    @Category({Regression.class, Testing.class})
    public void shouldSeeOkButton() {
        basePageSteps.onCardPage().popup().button("Одноклассники").should(isDisplayed())
                .should(hasAttribute("href", "https://connect.ok.ru/offer?url=https%3A%2F%2Ftest.avto.ru%2Fcars%2Fused%2Fsale%2Fland_rover%2Fdiscovery%2F1076842087-f1e84%2F&title=%D0%A1%D0%BC%D0%BE%D1%82%D1%80%D0%B8%D1%82%D0%B5%2C%20%D0%BA%D0%B0%D0%BA%D0%B0%D1%8F%20%D0%BC%D0%B0%D1%88%D0%B8%D0%BD%D0%B0%3A%20Land%20Rover%20Discovery%20III%202008%20%D0%B3%D0%BE%D0%B4%D0%B0%20%D0%B7%D0%B0%20700%C2%A0000%20%D1%80%D1%83%D0%B1%D0%BB%D0%B5%D0%B9%20%D0%BD%D0%B0%20%D0%90%D0%B2%D1%82%D0%BE.%D1%80%D1%83!&description=%D0%92%D0%BD%D0%B5%D0%B4%D0%BE%D1%80%D0%BE%D0%B6%D0%BD%D0%B8%D0%BA%20Land%20Rover%20Discovery%20III%202008%20%D0%B3%D0%BE%D0%B4%D0%B0%2C%20%D0%BF%D1%80%D0%BE%D0%B1%D0%B5%D0%B3%20210%20000%20%D0%BA%D0%BC%2C%20%D0%B4%D0%B2%D0%B8%D0%B3%D0%B0%D1%82%D0%B5%D0%BB%D1%8C%202.7d%20AT%20(190%20%D0%BB.%D1%81.)%204WD%2C%20%D1%86%D0%B2%D0%B5%D1%82%20%D1%81%D0%B5%D1%80%D0%B5%D0%B1%D1%80%D0%BE%20%D0%B7%D0%B0%20700%C2%A0000%20%D1%80%D1%83%D0%B1%D0%BB%D0%B5%D0%B9.&imageUrl=https%3A%2F%2Fimages.mds-proxy.test.avto.ru%2Fget-autoru-vos%2F2171969%2F023fb9f39f18cb4818f4627eac2e9db9%2F1200x900&utm_source=share2"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("WhatsApp")
    @Category({Regression.class, Testing.class})
    public void shouldSeeWhatsAppButton() {
        basePageSteps.onCardPage().popup().button("WhatsApp").should(isDisplayed())
                .should(hasAttribute("href", "https://api.whatsapp.com/send?text=%D0%A1%D0%BC%D0%BE%D1%82%D1%80%D0%B8%D1%82%D0%B5%2C%20%D0%BA%D0%B0%D0%BA%D0%B0%D1%8F%20%D0%BC%D0%B0%D1%88%D0%B8%D0%BD%D0%B0%3A%20Land%20Rover%20Discovery%20III%202008%20%D0%B3%D0%BE%D0%B4%D0%B0%20%D0%B7%D0%B0%20700%C2%A0000%20%D1%80%D1%83%D0%B1%D0%BB%D0%B5%D0%B9%20%D0%BD%D0%B0%20%D0%90%D0%B2%D1%82%D0%BE.%D1%80%D1%83!%20https%3A%2F%2Ftest.avto.ru%2Fcars%2Fused%2Fsale%2Fland_rover%2Fdiscovery%2F1076842087-f1e84%2F&utm_source=share2"));
    }
}
