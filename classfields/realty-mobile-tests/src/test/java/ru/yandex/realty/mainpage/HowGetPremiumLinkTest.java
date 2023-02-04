package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;

@Issue("VERTISTEST-1352")
@Feature(MAIN)
@DisplayName("Ссылка «Как попасть в премиум блок?»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class HowGetPremiumLinkTest {

    private static final String HOW_TO_TEXT = "Как попасть в премиум блок?";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openMainPage() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка «Как попасть в премиум блок?»")
    public void shouldSeeHowGetPremiumUrl() {
        basePageSteps.scrollUntilExists(() -> basePageSteps.onMobileMainPage().link(HOW_TO_TEXT));
        basePageSteps.onMobileMainPage().link(HOW_TO_TEXT).should(hasHref(equalTo(
                urlSteps.testing().path("/promotion/").fragment("vas").toString())));
    }

}
