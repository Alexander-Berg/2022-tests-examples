package ru.auto.tests.publicapi.draft;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
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
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiOffersSaveSuccessResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.model.AutoApiOffer.StatusEnum.NEED_ACTIVATION;
import static ru.auto.tests.publicapi.model.AutoApiOffersSaveSuccessResponse.StatusEnum.SUCCESS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe404DraftNotFound;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("POST /user/draft/{category}/{offerId}/publish")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PublishCategoriesTest {

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

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(TestData.defaultCategories());
    }

    @Test
    public void shouldNotPublishForeignDraft() {
        Account account = am.create();
        Account foreignAccount = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createEmptyDraft(sessionId, category).getOfferId();
        String foreignSessionId = adaptor.login(foreignAccount).getSession().getId();

        api.draft().publishDraft().categoryPath(category.name()).offerIdPath(offerId)
                .xSessionIdHeader(foreignSessionId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe404DraftNotFound()));
    }

    @Test
    public void shouldNotPublishEmptyDraft() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createEmptyDraft(sessionId, category).getOfferId();
        JsonObject response = api.draft().publishDraft().categoryPath(category.name()).offerIdPath(offerId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_UNPROCESSABLE_ENTITY))).as(JsonObject.class, GSON);

        String offerIdProd = adaptor.createEmptyDraft(sessionId, category).getOfferId();
        JsonObject responseProd = prodApi.draft().publishDraft().categoryPath(category.name()).offerIdPath(offerIdProd)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_UNPROCESSABLE_ENTITY))).as(JsonObject.class, GSON);
        MatcherAssert.assertThat(response, jsonEquals(responseProd));
    }

    @Test
    @Description("Объявления создаются в статусе NEED_ACTIVATION")
    public void shouldSeeNeedActivationStatusForPublishDraft() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDraft(account.getLogin(), sessionId, category).getOfferId();
        AutoApiOffersSaveSuccessResponse response = api.draft().publishDraft().categoryPath(category.name()).offerIdPath(offerId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        assertThat(response.getOffer()).hasStatus(NEED_ACTIVATION);
    }

    @Test
    public void shouldPublishDraft() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDraft(account.getLogin(), sessionId, category).getOfferId();
        AutoApiOffersSaveSuccessResponse response = api.draft().publishDraft().categoryPath(category.name()).offerIdPath(offerId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));
        assertThat(response).hasStatus(SUCCESS);
        assertThat(response.getOffer()).hasUserRef("user:" + account.getId());
        MatcherAssert.assertThat("<private_seller> должен содержать один телефон", response.getOffer().getPrivateSeller().getPhones(), hasSize(1));
        MatcherAssert.assertThat("<seller> должен содержать один телефон", response.getOffer().getSeller().getPhones(), hasSize(1));
        MatcherAssert.assertThat("<discount_options.tradeid> должен сохраниться", response.getOffer().getDiscountOptions().getTradein(), is(10));


        assertThat(response.getOffer().getPrivateSeller().getPhones().get(0))
                .hasPhone(account.getLogin()).hasOriginal(account.getLogin());

        assertThat(response.getOffer().getSeller().getPhones().get(0))
                .hasPhone(account.getLogin()).hasOriginal(account.getLogin());
    }
}
