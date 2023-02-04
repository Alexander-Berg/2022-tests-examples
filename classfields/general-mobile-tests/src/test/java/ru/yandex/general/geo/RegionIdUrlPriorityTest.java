package ru.yandex.general.geo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.SANKT_PETERBURG;
import static ru.yandex.general.consts.Pages.SLASH;
import static ru.yandex.general.consts.QueryParams.DNO_ID_VALUE;
import static ru.yandex.general.consts.QueryParams.NIZHNIJ_NOVGOROD_ID_VALUE;
import static ru.yandex.general.consts.QueryParams.REGION_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.mobile.element.Wrapper.FIND;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.general.step.BasePageSteps.YANDEX_GID;
import static ru.yandex.qatools.htmlelements.matchers.common.HasAttributeMatcher.hasAttribute;

@Feature(GEO_FEATURE)
@DisplayName("Приоритет region_id в URL над гео кукой, при формировании URL")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class RegionIdUrlPriorityTest {

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

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, DNO_ID_VALUE);
        basePageSteps.setCookie(YANDEX_GID, NIZHNIJ_NOVGOROD_ID_VALUE);
        urlSteps.testing().queryParam(REGION_ID_PARAM, EXPECTED_ID).open();
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
    @DisplayName("Приоритет region_id в URL над гео кукой, при формировании URL категории в футере")
    public void shouldSeeCategoryFooterURL() {
        basePageSteps.onListingPage().footer().category(CATEGORY_NAME).should(
                hasAttribute(HREF, urlSteps.testing().path(EXPECTED_REGION_PATH).path(ELEKTRONIKA).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отсутствует гео при формировании URL в кнопке добавления оффера")
    public void shouldSeeURLWithRegionIdAddOfferButton() {
        basePageSteps.onListingPage().tabBar().addOffer().should(
                hasAttribute(HREF, urlSteps.testing().path(FORM).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Приоритет region_id в URL над гео кукой, при поиске")
    public void shouldSeeRegionIdInUrl() {
        basePageSteps.onListingPage().searchBar().openSearch().click();
        basePageSteps.onListingPage().wrapper().input().sendKeys(TEXT);
        basePageSteps.onListingPage().wrapper().button(FIND).click();

        urlSteps.testing().path(EXPECTED_REGION_PATH).queryParam(TEXT_PARAM, TEXT).shouldNotDiffWithWebDriverUrl();
    }

}
