package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.ResponseSpecBuilders;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiChatAggregatorResponse;
import ru.auto.tests.publicapi.model.AutoApiMessageListingResponse;
import ru.auto.tests.publicapi.model.AutoApiSuccessResponse;
import ru.auto.tests.publicapi.model.VertisChatMessage;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.ABORUNOV;
import static ru.auto.tests.publicapi.consts.Owners.RAIV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("PUT /chat/message/user-spam")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class MessageMarkSpamTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Test
    @Owner(RAIV)
    public void shouldSeeSpamMessages() {
        Account account1 = am.create();
        Account account2 = am.create();
        String sessionId1 = adaptor.login(account1).getSession().getId();
        String sessionId2 = adaptor.login(account2).getSession().getId();
        String roomId = adaptor.createRoom(sessionId1, newArrayList(account2)).getRoom().getId();
        VertisChatMessage message = adaptor.createMessage(sessionId1, roomId).getMessage();
        AutoApiSuccessResponse resp = api.chat().markMessageAsSpamByUser().messageIdQuery(message.getId())
          .reqSpec(defaultSpec())
          .xSessionIdHeader(sessionId2)
          .executeAs(ResponseSpecBuilders.validatedWith(shouldBe200OkJSON()));

        assertThat(resp).hasStatus(AutoApiSuccessResponse.StatusEnum.SUCCESS);

        AutoApiMessageListingResponse messageListingResponse1 = api.chat().getSpamMessages().roomIdQuery(roomId)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId1)
                .executeAs(validatedWith(shouldBe200OkJSON()));
        message.setIsSpam(true);
        message.setPayload(null);
        message.setAttachments(null);
        message.setProperties(null);
        assertThat(messageListingResponse1).hasOnlyMessages(message);

        AutoApiMessageListingResponse messageListingResponse2 = api.chat().getSpamMessages().roomIdQuery(roomId)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId2)
                .executeAs(validatedWith(shouldBe200OkJSON()));
        assertThat(messageListingResponse2).hasOnlyMessages(message);
    }
}
