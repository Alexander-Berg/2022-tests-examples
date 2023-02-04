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
import ru.yandex.general.beans.card.Address;
import ru.yandex.general.beans.ldJson.JobPosting;
import ru.yandex.general.mobile.page.BasePage;
import ru.yandex.general.mock.MockCard;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.rules.MockRuleWithoutWebdriver;
import ru.yandex.general.step.JSoupSteps;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static ru.yandex.general.beans.card.AddressText.addressText;
import static ru.yandex.general.beans.card.District.district;
import static ru.yandex.general.beans.card.GeoPoint.geoPoint;
import static ru.yandex.general.beans.ldJson.Address.address;
import static ru.yandex.general.beans.ldJson.JobPosting.jobPosting;
import static ru.yandex.general.consts.GeneralFeatures.JOB_POSTING_SEO_MARK;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.mock.MockCard.VACANCY_CARD;
import static ru.yandex.general.mock.MockCard.getCreateDateTime;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(SEO_FEATURE)
@Feature(JOB_POSTING_SEO_MARK)
@DisplayName("Разметка LD-JSON на карточке вакансии")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralRequestModule.class)
public class SeoJobPostingLdJsonCardMarkingTest {

    private static final String ADDRESS_NAME = "Павелецкая площадь, 2с1";
    private static final String DISTRICT_NAME = "Замоскворечье";
    private static final String SALLARY = "120000";
    private static final String SELLER_NAME = "Дон Валерий";
    private static final String TITLE = "Название вакансии";
    private static final String CANONICAL_LINK = "/rabota/offer/88918384664903681/";
    private static final String DESCRIPTION = "Описание вакансии";

    private static final Address ADDRESS = Address.address()
            .setAddress(addressText().setAddress(ADDRESS_NAME))
            .setGeoPoint(geoPoint().setLatitude("55.730423").setLongitude("37.634796"));
    private static final Address DISTRICT = Address.address()
            .setGeoPoint(geoPoint().setLatitude("55.734157").setLongitude("37.63429"))
            .setDistrict(district().setId("117067").setName(DISTRICT_NAME).setIsEnriched(false));

    private String createDateTime = getCreateDateTime();

    private MockCard mockCard;

    private JobPosting expectedJobPostingMark;

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Rule
    @Inject
    public MockRuleWithoutWebdriver mockRule;

    @Before
    public void before() {
        jSoupSteps.testing().path(CANONICAL_LINK);

        mockCard = mockCard(VACANCY_CARD).setId("88918384664903681")
                .setCreateDateTime(createDateTime)
                .setSellerName(SELLER_NAME)
                .setTitle(TITLE)
                .setCanonicalLink(CANONICAL_LINK);

        expectedJobPostingMark = jobPosting()
                .withOrganizationName(SELLER_NAME)
                .setTitle(TITLE)
                .setUrl(jSoupSteps.toString())
                .setDatePosted(createDateTime);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("LD-JSON разметка на карточке вакансии, есть зарплата и описание, указан адрес")
    public void shouldSeeCardLdJsonMarkingWithAllFields() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setSallaryPrice(SALLARY)
                        .setAddresses(ADDRESS)
                        .setDescription(DESCRIPTION).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        jSoupSteps.setMockritsaImposter(mockRule.getPort())
                .setMobileRobotUserAgent().get();

        expectedJobPostingMark.withSallary(SALLARY).withAddress(address().setStreetAddress(ADDRESS_NAME)).setDescription(DESCRIPTION);

        String actualJobPostingMark = jSoupSteps.getLdJsonMark(BasePage.JOB_POSTING);

        assertThatJson(actualJobPostingMark).isEqualTo(expectedJobPostingMark.toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("LD-JSON разметка на карточке вакансии, нет зарплаты и описания, указан район")
    public void shouldSeeCardLdJsonMarkingWithDistrictWithoutSallaryAndDescription() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setUnsetPrice()
                        .setAddresses(DISTRICT).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        jSoupSteps.setMockritsaImposter(mockRule.getPort())
                .setMobileRobotUserAgent().get();

        expectedJobPostingMark.withAddress(address().setAddressLocality(DISTRICT_NAME)).setDescription("");

        String actualJobPostingMark = jSoupSteps.getLdJsonMark(BasePage.JOB_POSTING);

        assertThatJson(actualJobPostingMark).isEqualTo(expectedJobPostingMark.toString());
    }

}
