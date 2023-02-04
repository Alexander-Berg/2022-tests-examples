package ru.yandex.general.seo;

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
import org.junit.runner.RunWith;
import ru.yandex.general.beans.ldJson.Organization;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.JSoupSteps;
import ru.yandex.general.step.UrlSteps;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static ru.yandex.general.beans.ldJson.Organization.organization;
import static ru.yandex.general.consts.GeneralFeatures.LOGO_SEO_MARK;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;

@Epic(SEO_FEATURE)
@Feature(LOGO_SEO_MARK)
@DisplayName("Разметка Ld-Json «Logo» на страницах сервиса")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralRequestModule.class)
public class SeoLogoLdJsonTest {

    private static final String TEXT = "ноутбук macbook";

    private Organization expectedLogo;

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Before
    public void before() {
        expectedLogo = organization()
                .setUrl(jSoupSteps.realProduction().toString())
                .setLogo(jSoupSteps.realProduction().path("o-logo-576x576.png").toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Разметка Ld-Json «Logo» на главной")
    public void shouldSeeLogoLdJsonHomepage() {
        jSoupSteps.testing().path(MOSKVA).setDesktopUserAgent().get();
        String actualLogo = jSoupSteps.getLdJsonMarkLogo();

        assertThatJson(actualLogo).isEqualTo(expectedLogo.toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Разметка Ld-Json «Logo» на листинге категории")
    public void shouldSeeLogoLdJsonListing() {
        jSoupSteps.testing().path(ELEKTRONIKA).setDesktopUserAgent().get();
        String actualLogo = jSoupSteps.getLdJsonMarkLogo();

        assertThatJson(actualLogo).isEqualTo(expectedLogo.toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Разметка Ld-Json «Logo» на странице полнотекстового поиска")
    public void shouldSeeLogoLdJsonTextSearch() {
        jSoupSteps.testing().queryParam(TEXT_PARAM, TEXT).setDesktopUserAgent().get();
        String actualLogo = jSoupSteps.getLdJsonMarkLogo();

        assertThatJson(actualLogo).isEqualTo(expectedLogo.toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Разметка Ld-Json «Logo» на карточке оффера")
    public void shouldSeeLogoLdJsonOfferCard() {
        jSoupSteps.testing().uri(jSoupSteps.getActualOfferCardUrl()).setDesktopUserAgent().get();
        String actualLogo = jSoupSteps.getLdJsonMarkLogo();

        assertThatJson(actualLogo).isEqualTo(expectedLogo.toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Разметка Ld-Json «Logo» на профиле продавца")
    public void shouldSeeLogoLdJsonSellerPage() {
        jSoupSteps.testing().path(PROFILE).path(SELLER_PATH).setDesktopUserAgent().get();
        String actualLogo = jSoupSteps.getLdJsonMarkLogo();

        assertThatJson(actualLogo).isEqualTo(expectedLogo.toString());
    }

}
