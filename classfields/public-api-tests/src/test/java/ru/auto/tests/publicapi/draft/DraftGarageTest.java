package ru.auto.tests.publicapi.draft;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.*;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("POST /user/draft/{category}/garage/{id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DraftGarageTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldCreatedDraftByGarage() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);
        String sessionId = loginResult.getSession().getId();

        AutoApiVinGarageCreateCardResponse createdGarageResponse =
                adaptor.createGarageCard(getResourceAsString("garage/create_card_request.json"), sessionId);

        AutoApiVinGarageCard garage = createdGarageResponse.getCard();

        AutoApiDraftResponse createdDraftResponse = api.draft().createFromGarageCard()
                .categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .idPath(garage.getId())
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        AutoApiOffer draft = createdDraftResponse.getOffer();

        AutoApiCarInfo draftCarInfo = draft.getCarInfo();
        AutoApiDocuments draftDocument = draft.getDocuments();

        AutoApiCarInfo garageCarInfo = garage.getVehicleInfo().getCarInfo();
        AutoApiDocuments garageDocument = garage.getVehicleInfo().getDocuments();

        Assertions.assertThat(draftCarInfo.getMark()).isEqualTo(garageCarInfo.getMark());
        Assertions.assertThat(draftCarInfo.getModel()).isEqualTo(garageCarInfo.getModel());
        Assertions.assertThat(draftCarInfo.getSuperGenId()).isEqualTo(garageCarInfo.getSuperGenId());
        Assertions.assertThat(draftCarInfo.getComplectationId()).isEqualTo(garageCarInfo.getComplectationId());

        Assertions.assertThat(draftCarInfo.getBodyType()).isEqualTo(garageCarInfo.getBodyType());
        Assertions.assertThat(draftCarInfo.getEngineType()).isEqualTo(garageCarInfo.getEngineType());
        Assertions.assertThat(draftCarInfo.getTransmission()).isEqualTo(garageCarInfo.getTransmission());
        Assertions.assertThat(draftCarInfo.getDrive()).isEqualTo(garageCarInfo.getDrive());
        Assertions.assertThat(draftCarInfo.getSteeringWheel()).isEqualTo(garageCarInfo.getSteeringWheel());

        Assertions.assertThat(draftDocument.getOwnersNumber()).isEqualTo(garageDocument.getOwnersNumber());
        Assertions.assertThat(draftDocument.getYear()).isEqualTo(garageDocument.getYear());
        Assertions.assertThat(draftDocument.getVin()).isEqualTo(garageDocument.getVin());
    }
}
