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

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.CATEGORY;
import static ru.yandex.realty.consts.Pages.JOURNAL;
import static ru.yandex.realty.consts.Pages.POST_PAGE;
import static ru.yandex.realty.consts.RealtyFeatures.JOURNAL_FEATURE;

@Link("https://st.yandex-team.ru/VERTISTEST-1621")
@Feature(JOURNAL_FEATURE)
@DisplayName("Журнал")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ShowMoreTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("клик по «Загрузить ещё»")
    public void shouldSeeShowMore() {
        urlSteps.testing().path(JOURNAL).path(POST_PAGE)
                .path("/stil-khay-tek-v-interere-kak-sozdat-dom-iz-buduschego-v-obychnoy-kvartire/").open();
        basePageSteps.onJournalPage().link("Загрузить ещё").click();
        basePageSteps.onJournalPage().pageH1().should(hasText("Дизайн"));
        urlSteps.testing().path(JOURNAL).path(CATEGORY).path("/dizayn/").shouldNotDiffWithWebDriverUrl();
    }
}
