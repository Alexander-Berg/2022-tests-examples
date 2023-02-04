package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
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
import ru.auto.tests.publicapi.model.AutoApiCreateRoomRequest;
import ru.auto.tests.publicapi.model.AutoApiLoginResponse;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiChatOfferSubjectSource;
import ru.auto.tests.publicapi.model.AutoApiChatSubjectSource;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("POST /chat/room")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CreateRoomCategoriesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(TestData.defaultCategories());
    }


    @Test
    @Issue("AUTORUAPI-3412")
    public void shouldCreateRoomWithOffer() {
        Account account = am.create();
        Account secondAccount = am.create();

        AutoApiLoginResponse loginResult = adaptor.login(account);
        String secondAccountSessionId = adaptor.login(secondAccount).getSession().getId();

        String offerId = adaptor.createOffer(secondAccount.getLogin(), secondAccountSessionId, category).getOfferId();

        JsonObject roomResponse = api.chat().createRoom().reqSpec(defaultSpec()).body(new AutoApiCreateRoomRequest().subject(new AutoApiChatSubjectSource()
                .offer(new AutoApiChatOfferSubjectSource().category(category.name()).id(offerId))))
                .xSessionIdHeader(loginResult.getSession().getId())
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        JsonObject roomResponseProd = prodApi.chat().createRoom().reqSpec(defaultSpec()).body(new AutoApiCreateRoomRequest().subject(new AutoApiChatSubjectSource()
                .offer(new AutoApiChatOfferSubjectSource().category(category.name()).id(offerId))))
                .xSessionIdHeader(loginResult.getSession().getId()).execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(roomResponse, jsonEquals(roomResponseProd).whenIgnoringPaths(CreateRoomTest.IGNORED_PATHS));
    }
}
