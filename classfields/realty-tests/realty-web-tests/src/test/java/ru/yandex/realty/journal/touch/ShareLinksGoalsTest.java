package ru.yandex.realty.journal.touch;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.JOURNAL;
import static ru.yandex.realty.consts.Pages.POST_PAGE;
import static ru.yandex.realty.consts.RealtyFeatures.JOURNAL_FEATURE;

// TODO: 22.01.2021 Перенести когда-то в цели
@Link("https://st.yandex-team.ru/VERTISTEST-1621")
@Feature(JOURNAL_FEATURE)
@DisplayName("Журнал. Тач")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithProxyModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ShareLinksGoalsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ProxySteps proxy;

    @Inject
    private GoalsSteps goalsSteps;

    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String service;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"ВКонтакте", "VK"},
                {"Одноклассники", "OK"},
                {"Facebook", "FACEBOOK"},
                {"Телеграм", "TELEGRAM"},
                {"WhatsApp", "WHATSAPP"},
                {"Twitter", "TWITTER"},
                {"Скопировать ссылку", "COPY_LINK"},
        });
    }

    @Before
    public void before() {
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        urlSteps.testing().path(JOURNAL).path(POST_PAGE).path("/v3/").open();
        proxy.clearHarUntilThereAreNoHarEntries();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим цели поделяшек")
    public void shouldSeeShareLinksGoals() {
        basePageSteps.onJournalPage().shareService(label).click();
        goalsSteps.urlMatcher(containsString("share.click_out"))
                .withGoalParams(goal().setService(service)).shouldExist();
    }
}
