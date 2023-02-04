package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiRoomResponse;
import ru.auto.tests.publicapi.model.AutoApiSuccessResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiSuccessResponse.StatusEnum.SUCCESS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("PUT /chat/room/{id}/typing")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class TypingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager accountManager;

    @Inject
    private ApiClient api;

    @Test
    @Owner(TIMONDL)
    public void shouldSeeSuccessTyping() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiRoomResponse room = adaptor.createRoom(sessionId, newArrayList(account));

        AutoApiSuccessResponse response = api.chat().typing()
                .reqSpec(defaultSpec())
                .idPath(room.getRoom().getId())
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        assertThat(response).hasStatus(SUCCESS);
    }
}
