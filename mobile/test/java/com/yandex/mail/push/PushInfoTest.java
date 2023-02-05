package com.yandex.mail.push;

import com.google.gson.Gson;
import com.yandex.mail.entity.Tab;
import com.yandex.mail.provider.Constants;
import com.yandex.mail.push.PushInfo.IntentKey;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.mail.util.InvalidPushInfo;
import com.yandex.mail.util.mailbox.Mailbox;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

import static com.yandex.mail.util.mailbox.MailboxEditor.Folder.createFolder;
import static com.yandex.mail.util.mailbox.MailboxEditor.Message.createMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(IntegrationTestRunner.class)
public class PushInfoTest extends BaseIntegrationTest {

    public static final String KEY = "KEY";

    @Test
    public void parseStringArray() throws InvalidPushInfo {
        Map<String, String> data = new HashMap<>();
        data.put(KEY, "[\"2060000006240607467\"]");
        String[] arr = PushInfo.parseStringArray(new Gson(), data, KEY);
        assertThat(arr[0]).isEqualTo("2060000006240607467");
    }

    @Test
    public void parseStringArray_shouldThrowException() {
        Map<String, String> data = new HashMap<>();
        data.put(KEY, "some shit");
        assertThatThrownBy(() -> {
            PushInfo.parseStringArray(new Gson(), data, KEY);
        }).isInstanceOf(InvalidPushInfo.class);
    }

    @Test
    public void parseTidReturnsNoTid_onEmptyString() {
        Map<String, String> data = new HashMap<>();
        data.put(IntentKey.TID, "");
        long parsedTid = PushInfo.parseTid(data);
        assertThat(parsedTid).isEqualTo(Constants.NO_THREAD_ID);
    }

    @Test
    public void parseLongArray() throws InvalidPushInfo {
        Map<String, String> data = new HashMap<>();
        data.put(KEY, "[\"2060000006240607467\"]");
        Long[] arr = PushInfo.parseLongArray(new Gson(), data, KEY);
        assertThat(arr[0]).isEqualTo(2_060_000_006_240_607_467L);
    }

    @Test
    public void parseLongArray_shouldThrowException() {
        Map<String, String> data = new HashMap<>();
        data.put(KEY, "some shit");
        assertThatThrownBy(() -> {
            PushInfo.parseLongArray(new Gson(), data, KEY);
        }).isInstanceOf(InvalidPushInfo.class);
    }

    @Test
    public void parseTwoDimensionalArray() throws InvalidPushInfo {
        Map<String, String> data = new HashMap<>();
        data.put(KEY, "[[\"2060000002483523844\",\"FAKE_FORWARDED_LBL\",\"FAKE_HAS_USER_LABELS_LBL\",\"FAKE_SEEN_LBL\"]]");
        String[][] arr = PushInfo.parseTwoDimensionalArray(new Gson(), data, KEY);
        assertThat(arr[0][0]).isEqualTo("2060000002483523844");
    }

    @Test
    public void parseTwoDimensionalArray_shouldThrowException() {
        Map<String, String> data = new HashMap<>();
        data.put(KEY, "some shit");
        assertThatThrownBy(() -> {
            PushInfo.parseTwoDimensionalArray(new Gson(), data, KEY);
        }).isInstanceOf(InvalidPushInfo.class);
    }

    @Test
    public void parseCounter() throws InvalidPushInfo, IOException {
        Map<Long, Integer> expectedResult = new HashMap<>();
        expectedResult.put(1L, 4);
        expectedResult.put(2L, 8);
        expectedResult.put(27L, 7);
        expectedResult.put(29L, 3);
        expectedResult.put(3L, 34);
        expectedResult.put(31L, 1);
        expectedResult.put(32L, 3);
        expectedResult.put(7L, 1);

        final String counters = "[" +
                                "1,4," +
                                "2,8," +
                                "27,7," +
                                "29,3," +
                                "3,34," +
                                "31,1," +
                                "32,3," +
                                "7,1" +
                                "]";

        Map<String, String> data = new HashMap<>();
        data.put(IntentKey.FOLDER_UNREAD_COUNTERS, counters);
        assertThat(PushInfo.parseUnreadCounters(data, new Gson())).isEqualTo(expectedResult);
    }

    @Test
    public void parseClearMessage_noXlistRequested() throws InvalidPushInfo {
        Mailbox.nonThreaded(this)
                .folder(createFolder()
                                .folderId(trashFid())
                                .addMessage(createMessage().messageId(1L)))
                .applyAndSync();

        Map<String, String> data = getBasePushIntent(PushInfo.Operation.MOVE);
        data.put(IntentKey.MIDS, "[\"1\"]");
        data.put(IntentKey.FID, "-1");

        PushInfo pushInfo = PushInfo.parseIntent(IntegrationTestRunner.app(), data);

        assertThat(pushInfo.needRequestXList).isFalse();
    }

    @Test
    public void parseDefaultTab() throws InvalidPushInfo {
        init(Accounts.testLoginData, true, true);

        Map<String, String> data = getBasePushIntent(PushInfo.Operation.INSERT);
        data.put(IntentKey.TAB, "default");
        PushInfo pushInfo = PushInfo.parseIntent(IntegrationTestRunner.app(), data);
        assertThat(pushInfo.folderId).isEqualTo(Tab.RELEVANT.getFakeFid());
    }

    @Test
    public void parseRelevantTab() throws InvalidPushInfo {
        init(Accounts.testLoginData, true, true);

        Map<String, String> data = getBasePushIntent(PushInfo.Operation.INSERT);
        data.put(IntentKey.TAB, "relevant");

        PushInfo pushInfo = PushInfo.parseIntent(IntegrationTestRunner.app(), data);
        assertThat(pushInfo.folderId).isEqualTo(Tab.RELEVANT.getFakeFid());
    }

    @Test
    public void parseNewsTab() throws InvalidPushInfo {
        init(Accounts.testLoginData, true, true);

        Map<String, String> data = getBasePushIntent(PushInfo.Operation.INSERT);
        data.put(IntentKey.TAB, "news");

        PushInfo pushInfo = PushInfo.parseIntent(IntegrationTestRunner.app(), data);
        assertThat(pushInfo.folderId).isEqualTo(Tab.NEWS.getFakeFid());
    }

    @Test
    public void parseSocialTab() throws InvalidPushInfo {
        init(Accounts.testLoginData, true, true);

        Map<String, String> data = getBasePushIntent(PushInfo.Operation.INSERT);
        data.put(IntentKey.TAB, "social");

        PushInfo pushInfo = PushInfo.parseIntent(IntegrationTestRunner.app(), data);
        assertThat(pushInfo.folderId).isEqualTo(Tab.SOCIAL.getFakeFid());
    }

    @NonNull
    private Map<String, String> getBasePushIntent(@NonNull PushInfo.Operation operation) {
        Map<String, String> data = new HashMap<>();
        data.put(IntentKey.UID, String.valueOf(account.loginData.uid));
        data.put(IntentKey.OPERATION, operation.getKey());
        data.put(IntentKey.LCN, "1");
        return data;
    }
}
