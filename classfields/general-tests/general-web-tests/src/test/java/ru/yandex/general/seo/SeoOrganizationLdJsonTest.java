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
import ru.yandex.general.step.JSoupSteps;

import static java.util.Arrays.asList;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static ru.yandex.general.beans.ldJson.Address.address;
import static ru.yandex.general.beans.ldJson.Organization.organization;
import static ru.yandex.general.consts.GeneralFeatures.ORGANIZATION_SEO_MARK;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.page.BasePage.ORGANIZATION;

@Epic(SEO_FEATURE)
@Feature(ORGANIZATION_SEO_MARK)
@DisplayName("Разметка Ld-Json «Organization» на страницах сервиса")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralRequestModule.class)
public class SeoOrganizationLdJsonTest {

    private static final String TEXT = "ноутбук macbook";

    private Organization expectedOrganization;

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Before
    public void before() {
        expectedOrganization = organization().setName("Яндекс.Объявления")
                .setUrl(jSoupSteps.realProduction().toString())
                .setSameAs(asList(
                        "https://twitter.com/o_yandex_",
                        "https://facebook.com/ЯндексОбъявления-104865174809187/",
                        "https://ok.ru/group/59403386421363",
                        "https://vk.com/o.yandex",
                        "https://www.instagram.com/o.yandex/",
                        "https://www.youtube.com/channel/UCpmFoojtSgPwR43zwli6GkQ"))
                .setAddress(address().setAddressLocality("Moscow, Russia")
                        .setPostalCode("115035")
                        .setStreetAddress("82 Sadovnicheskaya st., Building 2"))
                .setTelephone("+7(495)739-70-00");
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Разметка Ld-Json «Organization» на главной")
    public void shouldSeeOrganizationLdJsonHomepage() {
        jSoupSteps.testing().path(MOSKVA).setDesktopUserAgent().get();
        String actualOrganization = jSoupSteps.getLdJsonMark(ORGANIZATION);

        assertThatJson(actualOrganization).isEqualTo(expectedOrganization.toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Разметка Ld-Json «Organization» на листинге категории")
    public void shouldSeeOrganizationLdJsonListing() {
        jSoupSteps.testing().path(ELEKTRONIKA).setDesktopUserAgent().get();
        String actualOrganization = jSoupSteps.getLdJsonMark(ORGANIZATION);

        assertThatJson(actualOrganization).isEqualTo(expectedOrganization.toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Разметка Ld-Json «Organization» на странице полнотекстового поиска")
    public void shouldSeeOrganizationLdJsonTextSearch() {
        jSoupSteps.testing().queryParam(TEXT_PARAM, TEXT).setDesktopUserAgent().get();
        String actualOrganization = jSoupSteps.getLdJsonMark(ORGANIZATION);

        assertThatJson(actualOrganization).isEqualTo(expectedOrganization.toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Разметка Ld-Json «Organization» на карточке оффера")
    public void shouldSeeOrganizationLdJsonOfferCard() {
        jSoupSteps.testing().uri(jSoupSteps.getActualOfferCardUrl()).setDesktopUserAgent().get();
        String actualOrganization = jSoupSteps.getLdJsonMark(ORGANIZATION);

        assertThatJson(actualOrganization).isEqualTo(expectedOrganization.toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Разметка Ld-Json «Organization» на профиле продавца")
    public void shouldSeeOrganizationLdJsonSellerPage() {
        jSoupSteps.testing().path(PROFILE).path(SELLER_PATH).setDesktopUserAgent().get();
        String actualOrganization = jSoupSteps.getLdJsonMark(ORGANIZATION);

        assertThatJson(actualOrganization).isEqualTo(expectedOrganization.toString());
    }

}
