package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;

@DisplayName("Главная. Смс на приложение")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class SendSmsToAppTest {

    String PHONE = "9811355809";
    String PHONE_EVENT = format("{\"phone\":\"+7%s\"}", PHONE);


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private ProxySteps proxy;

    @Inject
    private GoalsSteps goalsSteps;

    @Before
    public void before() {
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        urlSteps.setMoscowCookie();
        basePageSteps.resize(1500, 1600);
        urlSteps.testing().open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим что отпрвлен запрос и успешную форму")
    public void shouldSeeSmsRequest() {
        basePageSteps.onMainPage().sendAppSmsForm().input().sendKeys(PHONE);
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onMainPage().sendAppSmsForm().button("Получить").waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().sendAppSmsForm().should(hasText(containsString("СМС отправлено")));
        goalsSteps.urlMatcher(StringContains.containsString("/gate/applinksms/getForFree/"))
                .withParams(PHONE_EVENT)
                .shouldExist();
    }
}
