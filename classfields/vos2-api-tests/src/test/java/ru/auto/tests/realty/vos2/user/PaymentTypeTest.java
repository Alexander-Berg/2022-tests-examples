package ru.auto.tests.realty.vos2.user;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.runners.GuiceDataProviderRunner;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.anno.Vos;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;
import ru.auto.tests.realty.vos2.objects.GetUserResp;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.PaymentTypeEnum;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.PaymentTypeEnum.JURIDICAL_PERSON;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithUnableToParseParam;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.testdata.TestData.defaultPaymentTypes;

@DisplayName("PUT /api/realty/user/{userID}/payment_type")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class PaymentTypeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient vos2;

    @Inject
    @Vos
    private Account account;

    @Test
    public void shouldSee404WithInvalidVosID() {
        String randomVosId = getRandomString();
        vos2.user().changeRoute().userIDPath(randomVosId).valueQuery(JURIDICAL_PERSON)
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }

    @Test
    public void shouldSee400WithInvalidPaymentType() {
        String randomPaymentType = getRandomString();
        vos2.user().changeRoute().userIDPath(account.getId()).valueQuery(randomPaymentType)
                .execute(validatedWith(shouldBe400WithUnableToParseParam("value", randomPaymentType)));
    }

    @DataProvider
    public static Object[] paymentTypes() {
        return defaultPaymentTypes();
    }

    @Test
    @UseDataProvider("paymentTypes")
    public void shouldSuccessChangeUserPaymentType(PaymentTypeEnum paymentType) {
        vos2.user().changeRoute().userIDPath(account.getId()).valueQuery(paymentType)
                .execute(validatedWith(shouldBeStatusOk()));

        GetUserResp resp = vos2.user().getUserRoute().userIDPath(account.getId())
                .execute(validatedWith(shouldBeStatusOk())).as(GetUserResp.class, GSON);

        Assertions.assertThat(resp.getUser()).hasPaymentType(paymentType.getValue());
    }
}
