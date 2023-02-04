package ru.yandex.realty.journal.touch;

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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.JOURNAL;
import static ru.yandex.realty.consts.Pages.POST_PAGE;
import static ru.yandex.realty.consts.RealtyFeatures.JOURNAL_FEATURE;

@Link("https://st.yandex-team.ru/VERTISTEST-1621")
@Feature(JOURNAL_FEATURE)
@DisplayName("Журнал. Тач")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PostOpenGalleryTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(JOURNAL).path(POST_PAGE).path("/realty/").open();
        basePageSteps.onJournalPage().picturesBlock().click();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Галерея открывается")
    public void shouldSeeOpenGallery() {
        basePageSteps.onJournalPage().gallery().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Галерея закрывается")
    public void shouldSeeCloseGallery() {
        basePageSteps.onJournalPage().gallery().waitUntil(isDisplayed());
        basePageSteps.onJournalPage().closeGallery().click();
        basePageSteps.onJournalPage().gallery().waitUntil(not(isDisplayed()));
    }
}
