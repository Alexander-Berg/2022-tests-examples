package com.yandex.mail.util;

import android.text.util.Rfc822Token;

import com.yandex.mail.compose.ComposeAction;
import com.yandex.mail.compose.DraftData;
import com.yandex.mail.entity.Folder;
import com.yandex.mail.entity.FolderType;
import com.yandex.mail.entity.Label;
import com.yandex.mail.entity.MessageMeta;
import com.yandex.mail.entity.ReplyType;
import com.yandex.mail.network.response.MessageAttachmentsMeta;
import com.yandex.mail.network.response.MessageBodyJson;
import com.yandex.mail.network.response.MessageMetaJson;
import com.yandex.mail.network.response.RecipientJson;
import com.yandex.mail.network.response.SearchSuggestResponse;
import com.yandex.mail.network.response.SearchSuggestResponse.Target;
import com.yandex.mail.storage.MessageStatus;
import com.yandex.mail.tools.Accounts;

import androidx.annotation.NonNull;

import static com.yandex.mail.provider.Constants.NO_MESSAGE_ID;
import static com.yandex.mail.storage.entities.LabelTypeKt.USER;
import static java.util.Collections.emptyList;
import static kotlin.collections.CollectionsKt.listOf;

public final class NanomailEntitiesTestUtils {

    private NanomailEntitiesTestUtils() {
        throw new NonInstantiableException();
    }

    @NonNull
    public static Folder createTestFolder(long folderId) {
        return createTestFolder(folderId, FolderType.INBOX);
    }

    @NonNull
    public static Folder createTestFolder(long folderId, FolderType type) {
        return new Folder(
                folderId,
                type.getServerType(),
                "Custom",
                0,
                null,
                0,
                0
        );
    }

    @NonNull
    public static MessageMeta createTestMessageMeta(long mid) {
        return new MessageMeta(
                mid,
                1,
                null,
                false,
                "",
                "Fake subject",
                "Fake first line",
                "Fake sender",
                true,
                false,
                1000,
                false,
                0
        );
    }

    @NonNull
    public static MessageBodyJson createTestMessageBodyJson(long mid) {
        final MessageBodyJson body = new MessageBodyJson();
        body.setInfo(new MessageBodyJson.Info(
                mid,
                emptyList(),
                listOf(new RecipientJson("email", "address", RecipientJson.Type.FROM)),
                "rfcId",
                "reffs"
        ));
        return body;
    }

    @NonNull
    public static MessageMetaJson createTestMessageMetaJson(long mid) {
        return createTestMessageMetaJson(mid, "1.2.1");
    }

    @NonNull
    public static MessageMetaJson createTestMessageMetaJson(long mid, MessageAttachmentsMeta messageAttachmentsMeta) {
        return createTestMessageMetaJson(mid, "1.2.1", messageAttachmentsMeta);
    }

    @NonNull
    public static MessageMetaJson createTestMessageMetaJson(long mid, String hid) {
        return createTestMessageMetaJson(mid, hid, createTestMessageAttachmentsMeta(hid));
    }

    @NonNull
    public static MessageMetaJson createTestMessageMetaJson(long mid, String hid, MessageAttachmentsMeta messageAttachmentsMeta) {
        return new MessageMetaJson(
                mid,
                1,
                emptyList(),
                listOf(MessageStatus.Status.READ.getId()),
                1L,
                false,
                false,
                new int[1],
                "Fake subjPrefix",
                "Fake subjText",
                new RecipientJson("email", "address", RecipientJson.Type.FROM),
                "First line",
                messageAttachmentsMeta,
                emptyList(),
                null
        );
    }

    @NonNull
    public static MessageAttachmentsMeta createTestMessageAttachmentsMeta() {
        return createTestMessageAttachmentsMeta("1.2.1");
    }

    @NonNull
    public static MessageAttachmentsMeta createTestMessageAttachmentsMeta(String hid) {
        return new MessageAttachmentsMeta(
                1,
                100L,
                true,
                listOf(createTestMessageAttachmentMeta(hid))
        );
    }

    @NonNull
    public static MessageBodyJson.Attach createTestMessageAttachmentMeta(String hid) {
        return new MessageBodyJson.Attach(
                hid,
                "Fake display name",
                100L,
                "Fake download url",
                true,
                0,
                false,
                false,
                "image",
                "image/jpeg",
                null
        );
    }

    @NonNull
    public static MessageBodyJson.Attach createTestMessageAttachmentMeta(String hid, boolean isDisk, String displayName, String mimeType) {
        return new MessageBodyJson.Attach(
                hid,
                displayName,
                100L,
                "Fake download url",
                isDisk,
                0,
                false,
                false,
                "image",
                mimeType,
                null
        );
    }

    @NonNull
    public static Label createTestLabel(@NonNull String labelId) {
        return new Label(
                labelId,
                USER,
                "label" + labelId,
                0,
                0,
                -1,
                -1
        );
    }

    @NonNull
    public static DraftData.Builder createTestDraftDataBuilder(long draftID, long baseMessageId) {
        return new DraftData.Builder()
                .accountId(Accounts.testLoginData.uid)
                .draftId(draftID)
                .action(ComposeAction.NEW_DRAFT)
                .from(new Rfc822Token("name", "email", null).toString())
                .to("to")
                .cc("cc")
                .bcc("bcc")
                .subject("subject")
                .body("body")
                .rfcId("rfcId")
                .references("references")
                .replyType(ReplyType.NONE)
                .replyMid(NO_MESSAGE_ID)
                .baseMessageId(baseMessageId);
    }

    @NonNull
    public static SearchSuggestResponse createSearchSuggestResponse(@NonNull Target target) {
        return new SearchSuggestResponse(
                target,
                "showText",
                "searchText",
                "displayName",
                "email",
                new SearchSuggestResponse.Highlights(emptyList(), emptyList(), emptyList()),
                ""
        );
    }

    @NonNull
    public static SearchSuggestResponse createSearchSuggestResponseWithSearchText(@NonNull Target target, @NonNull String searchText) {
        return new SearchSuggestResponse(
                target,
                "showText",
                searchText,
                "displayName",
                "email",
                new SearchSuggestResponse.Highlights(emptyList(), emptyList(), emptyList()),
                ""
        );
    }

    @NonNull
    public static SearchSuggestResponse createSearchSuggestEmailResponse(@NonNull String email, @NonNull String showText) {
        return new SearchSuggestResponse(
                Target.CONTACT,
                showText,
                "searchText",
                "displayName",
                email,
                new SearchSuggestResponse.Highlights(emptyList(), emptyList(), emptyList()),
                ""
        );
    }

    @NonNull
    public static DraftData createTestDraftData(long draftID, long baseMessageId) {
        return createTestDraftDataBuilder(draftID, baseMessageId).build();
    }
}
