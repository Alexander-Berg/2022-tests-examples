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

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.JOURNAL;
import static ru.yandex.realty.consts.Pages.POST_PAGE;
import static ru.yandex.realty.consts.RealtyFeatures.JOURNAL_FEATURE;

@Link("https://st.yandex-team.ru/VERTISTEST-1621")
@Feature(JOURNAL_FEATURE)
@DisplayName("Журнал. Тач")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PostSwipeGalleryTest {

    private static final String STYLE_ATTRIBUTE = "style";
    private static final String TRANSFORM_100 = "transform: translateX(-100%);";
    private static final String TRANSFORM_0 = "transform: translateX(0%);";
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
        basePageSteps.onJournalPage().picturesBlock().waitUntil(hasAttribute(STYLE_ATTRIBUTE, TRANSFORM_0));
        basePageSteps.onJournalPage().pictureSwipeButtonRight().click();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Свайп картинок вправо")
    public void shouldSeeSwipePicturesRight() {
        basePageSteps.onJournalPage().picturesBlock().should(hasAttribute(STYLE_ATTRIBUTE, TRANSFORM_100));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Свайп картинок влево")
    public void shouldSeeSwipePicturesLeft() {
        basePageSteps.onJournalPage().picturesBlock().waitUntil(hasAttribute(STYLE_ATTRIBUTE, TRANSFORM_100));
        basePageSteps.onJournalPage().pictureSwipeButtonLeft().click();
        basePageSteps.onJournalPage().picturesBlock().waitUntil(hasAttribute(STYLE_ATTRIBUTE, TRANSFORM_0));
    }
}
