package ru.auto.tests.publicapi.card;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
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
import ru.auto.tests.publicapi.model.AutoApiComplaintRequest;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.COMMERCIAL;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.DUPLICATE;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.IS_SPARE_PART;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.NOT_PART;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.NO_ANSWER;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.PRICE_ERROR;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.RESELLER;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.SOLD;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.SPAM_CALL;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.WRONG_ADDRESS;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.WRONG_AD_PARAMETERS;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.WRONG_COMPATIBILITY;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.WRONG_MODEL;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.WRONG_NUMBER_CALL;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.WRONG_OFFER_CATEGORY;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.WRONG_PHONE;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.WRONG_PHOTO;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.WRONG_PLACE;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.WRONG_PROPERTY_TYPE;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.WRONG_WORK_TYPES;
import static ru.auto.tests.publicapi.model.AutoApiComplaintRequest.ReasonEnum.WRONG_YEAR;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("POST /offer/{category}/{offerID}/complaints")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CompaintsReasonsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Parameter("Причина")
    @Parameterized.Parameter(0)
    public AutoApiComplaintRequest.ReasonEnum reason;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(provideReasons());
    }

    private static Object[] provideReasons() {
        return new AutoApiComplaintRequest.ReasonEnum[]{
                WRONG_PHOTO,
                DUPLICATE,
                SOLD,
                PRICE_ERROR,
                WRONG_AD_PARAMETERS,
                WRONG_ADDRESS,
                WRONG_PROPERTY_TYPE,
                NO_ANSWER,
                COMMERCIAL,
                IS_SPARE_PART,
                WRONG_YEAR,
                WRONG_MODEL,
                RESELLER,
                WRONG_OFFER_CATEGORY,
                WRONG_COMPATIBILITY,
                NOT_PART,
                WRONG_PLACE,
                WRONG_PHONE,
                WRONG_WORK_TYPES,
                SPAM_CALL,
                WRONG_NUMBER_CALL
        };
    }

    @Test
    public void shouldSetComplaint() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();

        api.offerCard().createComplaintOfferCard().categoryPath(CARS).offerIDPath(offerId).reqSpec(defaultSpec())
                .body(new AutoApiComplaintRequest().reason(reason)).execute(validatedWith(shouldBeSuccess()));
    }
}
