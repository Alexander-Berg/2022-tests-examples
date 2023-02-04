package ru.yandex.realty.journal.desktop;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.JOURNAL;
import static ru.yandex.realty.consts.RealtyFeatures.JOURNAL_FEATURE;
import static ru.yandex.realty.page.BasePage.FOR_PROFESSIONAL;
import static ru.yandex.realty.page.BasePage.JOURNAL_LINK;
import static ru.yandex.realty.step.UrlSteps.FROM_PARAM;
import static ru.yandex.realty.step.UrlSteps.MAIN_MENU_VALUE;

@Link("https://st.yandex-team.ru/VERTISTEST-1621")
@Feature(JOURNAL_FEATURE)
@DisplayName("Журнал")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class FromHeaderTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход в журнал из хедера")
    public void shouldSeePassToJournal() {
        basePageSteps.chooseFromHeader(FOR_PROFESSIONAL, JOURNAL_LINK);
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(JOURNAL).path("category/analitika/").queryParam(FROM_PARAM, MAIN_MENU_VALUE).shouldNotDiffWithWebDriverUrl();
    }
}
