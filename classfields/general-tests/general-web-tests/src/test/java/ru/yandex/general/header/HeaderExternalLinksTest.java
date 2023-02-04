package ru.yandex.general.header;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.HEADER_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.element.Header.FOR_SHOPS;
import static ru.yandex.general.element.Header.HELP;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.qatools.htmlelements.matchers.common.HasAttributeMatcher.hasAttribute;

@Epic(HEADER_FEATURE)
@Feature("Ссылки")
@DisplayName("Ссылки хэдера на внешние странички")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class HeaderExternalLinksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на «Yandex» в хэдере")
    public void shouldSeeYandexLink() {
        basePageSteps.onListingPage().yLogo().should(hasAttribute(HREF, "https://yandex.ru/"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на «Помощь» в хэдере")
    public void shouldSeeHelpLink() {
        basePageSteps.onListingPage().header().link(HELP)
                .should(hasAttribute(HREF, "https://yandex.ru/support/o-desktop/"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на «Помощь» в прилипшем хэдере")
    public void shouldSeeHelpLinkFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);

        basePageSteps.onListingPage().floatedHeader().link(HELP)
                .should(hasAttribute(HREF, "https://yandex.ru/support/o-desktop/"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на «Магазинам» в хэдере")
    public void shouldSeeForShopsLink() {
        basePageSteps.onListingPage().header().link(FOR_SHOPS)
                .should(hasAttribute(HREF, "https://o.yandex.ru/b2b?from=main"));
    }

}
