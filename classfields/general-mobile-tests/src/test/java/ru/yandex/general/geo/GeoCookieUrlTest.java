package ru.yandex.general.geo;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.GEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.SANKT_PETERBURG;
import static ru.yandex.general.consts.Pages.SLASH;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.mobile.element.Wrapper.FIND;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.general.step.BasePageSteps.YANDEX_GID;
import static ru.yandex.qatools.htmlelements.matchers.common.HasAttributeMatcher.hasAttribute;

@Feature(GEO_FEATURE)
@DisplayName("Формирование URL при наличии гео куки")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GeoCookieUrlTest {

    private static final String EXPECTED_ID = "2";
    private static final String EXPECTED_REGION_PATH = "/sankt-peterburg/";
    private static final String HREF = "href";
    private static final String CATEGORY_NAME = "Электроника";
    private static final String TEXT = "кот";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String cookieName;

    @Parameterized.Parameters(name = "Формирование URL с кукой «{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {CLASSIFIED_REGION_ID},
                {YANDEX_GID}
        });
    }

    @Before
    public void before() {
        basePageSteps.setCookie(cookieName, EXPECTED_ID);
        urlSteps.testing().open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет региона в URL, при формировании URL логотипа")
    public void shouldSeeLogoURL() {
        basePageSteps.onListingPage().header().oLogo().should(hasAttribute(HREF,
                urlSteps.testing().toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL категории с region_id в футере, при наличии гео куки")
    public void shouldSeeCategoryFooterURL() {
        basePageSteps.onListingPage().footer().category(CATEGORY_NAME).should(
                hasAttribute(HREF, urlSteps.path(EXPECTED_REGION_PATH).path(ELEKTRONIKA).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL в кнопке «Разместить» без гео")
    public void shouldSeeURLWithRegionIdPostButton() {
        basePageSteps.onListingPage().tabBar().addOffer().should(
                hasAttribute(HREF, urlSteps.path(FORM).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("При поиске прокидывается region_id, при наличии гео куки")
    public void shouldSeeRegionIdInUrl() {
        basePageSteps.onListingPage().searchBar().openSearch().click();
        basePageSteps.onListingPage().wrapper().input().sendKeys(TEXT);
        basePageSteps.onListingPage().wrapper().button(FIND).click();

        urlSteps.path(EXPECTED_REGION_PATH).queryParam(TEXT_PARAM, TEXT).shouldNotDiffWithWebDriverUrl();
    }

}
