package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.restassured.ResponseSpecBuilders;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.*;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.VertisChatMessagePayload.ContentTypeEnum.PLAIN;
import static ru.auto.tests.publicapi.model.VertisChatMessageProperties.TypeEnum.TECH_SUPPORT_FEEDBACK_RESPONSE;
import static ru.auto.tests.publicapi.model.VertisChatMessageProperties.TypeEnum.TECH_SUPPORT_POLL;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by dskuznetsov on 27.12.18
 */

@DisplayName("PUT /chat/tech-support/poll/{hash}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class PollTest {
    private static final int POLL_DELAY = 5;
    private static final int NULL_POLL_DELAY = 0;
    private static final int TIMEOUT = 60;
    private static final String SUPPORT_POLL_APP_VERSION = "9.1.0.9117";
    private static final int EXPECTED_2_MESSAGES = 2;
    private static final int EXPECTED_3_MESSAGES = 3;
    private static final int EXPECTED_4_MESSAGES = 4;
    private static final int EXPECTED_5_MESSAGES = 5;
    private static final int EXPECTED_6_MESSAGES = 6;
    private static final int SECOND_MESSAGE = 1;
    private static final int THIRD_MESSAGE = 2;
    private static final int FOURTH_MESSAGE = 3;

    private String sessionId;
    private String roomId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Before
    @Description("Регистрируем девайс в пушном для того, чтобы бек знал что этому клиенту можешь прислать голосовалку." +
            "Для получения голосовалка подготавливаем комнату: сообщение юзера + сообщение саппорта + ожидание")
    public void preparePoll() {
        Account account = am.create();
        sessionId = adaptor.login(account).getSession().getId();
        api.device().hello().body(new AutoApiHelloRequest().appVersion(SUPPORT_POLL_APP_VERSION).device(new AutoApiDevice()))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
        roomId = api.chat().getTechSupportRoom().reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBeSuccess())).getRoom().getId();

        adaptor.createMessage(sessionId, roomId);
        // ждем отбивки от бота техподдержки, что переключит нас на человека
        adaptor.waitUntilRoomHasCountOfMessages(EXPECTED_2_MESSAGES, POLL_DELAY, TIMEOUT, roomId, sessionId);

        adaptor.initPollBySupportMessageWithTag(account.getId());

        adaptor.waitUntilRoomHasCountOfMessages(EXPECTED_3_MESSAGES, POLL_DELAY, TIMEOUT, roomId, sessionId);
    }

    @Test
    @Owner(DSKUZNETSOV)
    @Description("Дождались голосовалки и проверяем наличие непустого хеша (id голосовалки) + нужного типа сообщения")
    public void shouldSeePollType() {
        VertisChatMessageProperties response = api.chat().getMessages().roomIdQuery(roomId).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess())).getMessages().get(THIRD_MESSAGE).getProperties();

        assertThat(response.getTechSupportPoll().getHash()).isNotNull();
        assertThat(response.getType()).isEqualTo(TECH_SUPPORT_POLL);
    }

    @Test
    @Owner(DSKUZNETSOV)
    @Description("После ответа фидбеком ждем автоматического ответа от саппорта и проставления нужного свойства у предыдущего сообщения")
    public void shouldSeeAnswerAfterSendingPollFeedback() {
        String rating = getRandomRating();
        int presetNumber = getRandomPresetNumber();
        String pollHash = ratePoll(rating);
        String feedbackId = sendPollFeedback(presetNumber, pollHash).getId();

        adaptor.waitUntilRoomHasCountOfMessages(EXPECTED_6_MESSAGES, NULL_POLL_DELAY, TIMEOUT, roomId, sessionId);
        List<VertisChatMessage> response = api.chat().getMessages().roomIdQuery(roomId).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess())).getMessages();

        assertThat(response.get(FOURTH_MESSAGE).getProperties().getTechSupportFeedback().getSelectedPreset()).isEqualTo(feedbackId);
    }

    @Step("Оцениваем в голосовалке со значением {rating}")
    private String ratePoll(String rating) {
        AutoApiMessageListingResponse response = api.chat().getMessages().roomIdQuery(roomId).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));
        String pollHash = response.getMessages().get(THIRD_MESSAGE).getProperties().getTechSupportPoll().getHash();

        api.chat().techSupportPoll().hashPath(pollHash).ratingQuery(rating).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        return pollHash;
    }

    @Step("Отвечаем на фидбек пресетом {presetNumber}")
    private VertisChatTechSupportFeedbackPreset sendPollFeedback(int presetNumber, String pollHash) {
        adaptor.waitUntilRoomHasCountOfMessages(EXPECTED_4_MESSAGES, NULL_POLL_DELAY, TIMEOUT, roomId, sessionId);

        AutoApiMessageListingResponse response = api.chat().getMessages().roomIdQuery(roomId).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));
        VertisChatTechSupportFeedbackPreset feedbackPreset = response.getMessages().get(FOURTH_MESSAGE).getProperties().getTechSupportFeedback().getPresets().get(presetNumber);

        api.chat().sendMessage().reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .body(new AutoApiSendMessageRequest()
                        .payload(new VertisChatMessagePayload().contentType(PLAIN).value(feedbackPreset.getValue())).roomId(roomId)
                        .properties(new VertisChatMessageProperties()
                                .type(TECH_SUPPORT_FEEDBACK_RESPONSE)
                                .techSupportFeedback(new VertisChatTechSupportFeedback().hash(pollHash).selectedPreset(feedbackPreset.getId()))))
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBeSuccess()));

        return feedbackPreset;
    }

    @Step("Получаем рандомный рейтинг")
    private String getRandomRating() {
        return String.valueOf(RandomUtils.nextInt(1, 3));
    }

    @Step("Получаем рандомный номер сообщения-пресета")
    private int getRandomPresetNumber() {
        return RandomUtils.nextInt(0, 2);
    }
}
