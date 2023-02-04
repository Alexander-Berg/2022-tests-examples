package ru.yandex.realty.journal.desktop;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.JOURNAL;
import static ru.yandex.realty.consts.Pages.POST_PAGE;
import static ru.yandex.realty.consts.RealtyFeatures.JOURNAL_FEATURE;

@Link("https://st.yandex-team.ru/VERTISTEST-1621")
@Feature(JOURNAL_FEATURE)
@DisplayName("Журнал")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PassToBeforeAfterTest {

    private static final String V2_PATH = "/v2/";
    private static final String V3_PATH = "/v3/";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(JOURNAL).path(POST_PAGE);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("«Следующая» статья")
    public void shouldSeeAfterPost() {
        urlSteps.path(V2_PATH).open();
        basePageSteps.onJournalPage().beforeAfterButton("Следующая").click();
        urlSteps.testing().path(JOURNAL).path(POST_PAGE).path(V3_PATH).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("«Предыдущая» статья")
    public void shouldSeeBeforePost() {
        urlSteps.path(V3_PATH).open();
        basePageSteps.onJournalPage().beforeAfterButton("Предыдущая").click();
        urlSteps.testing().path(JOURNAL).path(POST_PAGE).path(V2_PATH).shouldNotDiffWithWebDriverUrl();
    }
}
