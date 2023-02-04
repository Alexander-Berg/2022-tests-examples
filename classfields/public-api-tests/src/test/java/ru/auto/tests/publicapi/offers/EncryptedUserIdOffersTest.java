package ru.auto.tests.publicapi.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.*;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /user/{encryptedUserID}/offers/{category}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class EncryptedUserIdOffersTest {

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldReturnOtherUserOffers() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiOffersSaveSuccessResponse offer = adaptor.createOffer(account.getLogin(), sessionId, CARS);
        AutoApiOffersSaveSuccessResponse offer2 = adaptor.createOffer(account.getLogin(), sessionId, CARS);
        adaptor.waitUserOffersActiveCount(sessionId, CARS, 2);
        adaptor.setAllowOffersShow(sessionId);

        AutoApiUserResponse userResponse = adaptor.getUser(sessionId, account.getId());
        String encryptedUserId = userResponse.getEncryptedUserId();

        AutoApiOfferListingResponse response = api.userOffers()
                .otherUserOffers()
                .reqSpec(defaultSpec())
                .statusQuery(AutoApiOffer.StatusEnum.ACTIVE)
                .encryptedUserIdPath(encryptedUserId)
                .categoryPath(CARS)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

        List<String> actualOfferIds = response.getOffers().stream().map(o -> o.getId()).collect(Collectors.toList());
        List<String> expectedOfferIds = Arrays.asList(offer.getOffer().getId(), offer2.getOffer().getId());
        Assertions.assertThat(actualOfferIds).hasSameElementsAs(expectedOfferIds);
    }
}
