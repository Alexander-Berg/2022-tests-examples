package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.publicapi.model.AutoApiDevice;
import ru.auto.tests.publicapi.model.AutoApiHelloRequest;
import ru.auto.tests.publicapi.model.VertisChatMessage;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("PUT /chat/tech-support/poll/{hash}")
@RunWith(Parameterized.class)
@GuiceModules(PublicApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PollFeedbackPropertiesTest {
    private static final int POLL_DELAY = 5;
    private static final int NULL_POLL_DELAY = 0;
    private static final int TIMEOUT = 60;
    private static final String SUPPORT_POLL_APP_VERSION = "9.1.0.9117";
    private static final int EXPECTED_2_MESSAGES = 2;
    private static final int EXPECTED_3_MESSAGES = 3;
    private static final int EXPECTED_4_MESSAGES = 4;
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

    @Parameter("Рейтинг")
    @Parameterized.Parameter(0)
    public String rating;

    @Parameter("Количество пресетов")
    @Parameterized.Parameter(SECOND_MESSAGE)
    public int presetsCount;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static List<Object[]> getParameters() {
        return Arrays.asList(pollVotes());
    }

    public static Object[][] pollVotes() {
        return new Object[][]{
                {"1", 4},
                {"2", 4},
                {"3", 3}
        };
    }

    @Test
    @Owner(DSKUZNETSOV)
    @Description("После оценки проверяем что в предыдущих сообщениях появились корректные свойства")
    public void shouldSeeFeedbackProperties() {
        Account account = am.create();
        preparePoll(account);
        ratePoll(rating);
        adaptor.waitUntilRoomHasCountOfMessages(EXPECTED_4_MESSAGES, NULL_POLL_DELAY, TIMEOUT, roomId, sessionId);

        List<VertisChatMessage> response = api.chat().getMessages().roomIdQuery(roomId).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess())).getMessages();

        assertThat(response.get(THIRD_MESSAGE).getProperties().getTechSupportPoll().getSelectedRating().toString()).isEqualTo(rating);
        assertThat(response.get(FOURTH_MESSAGE).getProperties().getTechSupportFeedback().getPresets().size()).isEqualTo(presetsCount);
    }

    @Step("Оцениваем в голосовалке со значением {rating}")
    private String ratePoll(String rating) {
        String pollHash = api.chat().getMessages().roomIdQuery(roomId).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess())).getMessages().get(THIRD_MESSAGE).getProperties().getTechSupportPoll().getHash();

        api.chat().techSupportPoll().hashPath(pollHash).ratingQuery(rating).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        return pollHash;
    }

    @Step("Регистрируем девайс в пушном для того, чтобы бек знал что этому клиенту можешь прислать голосовалку." +
            "Для получения голосовалка подготавливаем комнату: сообщение юзера + сообщение саппорта + ожидание")
    public void preparePoll(Account account) {
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
}
