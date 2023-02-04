package ru.auto.tests.publicapi.draft;


import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiDraftResponse;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import static javax.ws.rs.core.UriBuilder.fromUri;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("POST /user/draft/{category}/{offerId}/publish")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class PublishUrlTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldSeeUrlAndMobileUrlForCars() {
        AutoApiDraftResponse response = createDraft(CARS);

        String url = fromUri("https://test.avto.ru/cars/used/sale/")
                .path(response.getOffer().getCarInfo().getMark().toLowerCase())
                .path(response.getOffer().getCarInfo().getModel().toLowerCase())
                .path(response.getOfferId())
                .build().toString() + "/";
        String mobileUrl = fromUri("http://m.test.avto.ru/cars/used/sale/")
                .path(response.getOffer().getCarInfo().getMark().toLowerCase())
                .path(response.getOffer().getCarInfo().getModel().toLowerCase())
                .path(response.getOfferId())
                .build().toString() + "/";
        assertThat(response.getOffer()).hasUrl(url).hasMobileUrl(mobileUrl);
    }

    @Test
    public void shouldSeeUrlAndMobileUrlForMoto() {
        AutoApiDraftResponse response = createDraft(MOTO);

        String url = fromUri("https://test.avto.ru/atv/used/sale/")
                .path(response.getOffer().getMotoInfo().getMark().toLowerCase())
                .path(response.getOffer().getMotoInfo().getModel().toLowerCase())
                .path(response.getOfferId())
                .build().toString() + "/";
        String mobileUrl = fromUri("http://m.test.avto.ru/atv/used/sale/")
                .path(response.getOffer().getMotoInfo().getMark().toLowerCase())
                .path(response.getOffer().getMotoInfo().getModel().toLowerCase())
                .path(response.getOfferId())
                .build().toString() + "/";
        assertThat(response.getOffer()).hasUrl(url).hasMobileUrl(mobileUrl);
    }

    @Test
    public void shouldSeeUrlAndMobileUrlForTrucks() {
        AutoApiDraftResponse response = createDraft(TRUCKS);

        String url = fromUri("https://test.avto.ru/lcv/used/sale/")
                .path(response.getOffer().getTruckInfo().getMark().toLowerCase())
                .path(response.getOffer().getTruckInfo().getModel().toLowerCase())
                .path(response.getOfferId())
                .build().toString() + "/";
        String mobileUrl = fromUri("http://m.test.avto.ru/lcv/used/sale/")
                .path(response.getOffer().getTruckInfo().getMark().toLowerCase())
                .path(response.getOffer().getTruckInfo().getModel().toLowerCase())
                .path(response.getOfferId())
                .build().toString() + "/";
        assertThat(response.getOffer()).hasUrl(url).hasMobileUrl(mobileUrl);
    }

    private AutoApiDraftResponse createDraft(AutoApiOffer.CategoryEnum category) {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDraft(account.getLogin(), sessionId, category).getOfferId();
        AutoApiDraftResponse response = api.draft().publishDraft().categoryPath(category.name()).offerIdPath(offerId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess())).as(AutoApiDraftResponse.class);

        return response;
    }
}
