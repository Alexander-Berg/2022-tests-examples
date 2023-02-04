package ru.auto.tests.realtyapi.v1.money;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.environment.IntegrationTestEnvironment.MONEY_VALID_UID;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.GetDate.getNearFutureTime;

@Title("GET /money/spent/aggregated/offers/user/{uid}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetAggregatedOffersUserTest {

    private static final String LEVEL_DAY = "day";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(ARTEAMO)
    public void shouldHas403WithNoAuth() {
        api.money().userAggregatedWithOffersSpent()
                .uidPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithNoPartnerId() {
        api.money().userAggregatedWithOffersSpent()
                .reqSpec(authSpec())
                .uidPath(EMPTY)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));

    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithNoLevel() {
        api.money().userAggregatedWithOffersSpent()
                .reqSpec(authSpec())
                .uidPath(MONEY_VALID_UID)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));

    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithStartDateInFuture() {
        // Ответ от ручки содержит такое объяснение:
        // "Unexpected response: HTTP/1.1 400 Bad Request, body = The end instant must be greater than the start instant"
        // Так что, видимо, если поставить начлало интервала в будущее, то его конец все равно ставится в "сейчас" по умолчанию.

        // Есть, если что, и другие похожие тесты, если меняешь - найди и их тоже. Греп по "DateInFuture".

        api.money().userAggregatedWithOffersSpent()
                .reqSpec(authSpec())
                .uidPath(MONEY_VALID_UID)
                .levelQuery(LEVEL_DAY)
                .startTimeQuery(getNearFutureTime())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }
}
