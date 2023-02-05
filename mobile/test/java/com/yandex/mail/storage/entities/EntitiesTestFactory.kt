package com.yandex.mail.storage.entities

import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.yandex.mail.LoginData
import com.yandex.mail.account.MailProvider.YANDEX
import com.yandex.mail.entity.AccountEntity
import com.yandex.mail.entity.AccountType.LOGIN
import com.yandex.mail.entity.Attach
import com.yandex.mail.entity.DraftAttachEntry
import com.yandex.mail.entity.Folder
import com.yandex.mail.entity.Label
import com.yandex.mail.network.response.Ava2Response.Ava
import com.yandex.mail.network.response.Ava2Response.AvaType.AVATAR
import com.yandex.mail.network.response.Ava2Response.ProfileInfo
import com.yandex.mail.network.response.SearchSuggestResponse
import com.yandex.mail.network.response.SearchSuggestResponse.Target
import com.yandex.mail.provider.Constants.NO_FOLDER_ID
import com.yandex.mail.react.entity.Avatar
import com.yandex.mail.ui.entities.MessageContent
import com.yandex.mail.ui.entities.PromoTip
import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.PassportUid
import com.yandex.xplat.xmail.Story
import java.util.concurrent.atomic.AtomicInteger

object EntitiesTestFactory {

    private val MESSAGE_ID_GENERATOR = AtomicInteger()

    private val LABEL_ID_GENERATOR = AtomicInteger()

    private val COMMON_ID_GENERATOR = AtomicInteger()

    @JvmStatic
    fun buildAccountEntity(uid: Long, isSelected: Boolean): AccountEntity {
        return AccountEntity(
            uid,
            "name$uid",
            "type$uid",
            isSelected,
            true,
            0L,
            0L,
            LOGIN.stringType,
            YANDEX.stringRepresentation,
            "xtoken",
            hasToken = true,
            isYandexoid = false,
            isPdd = false
        )
    }

    fun buildPassportAccount(account: LoginData): PassportAccount {
        return mock {
            on { androidAccount }.doReturn(account.toAccount())
            on { toString() }.doReturn(account.toAccount().toString())
            on { isAuthorized }.doReturn(!TextUtils.isEmpty(account.token)) // TODO not actual state
            on { isMailish }.doReturn(false)
            on { uid }.doReturn(PassportUid.Factory.from(account.uid))
        }
    }

    @JvmStatic
    fun buildNanoFolder(): Folder {
        return Folder(
            COMMON_ID_GENERATOR.incrementAndGet().toLong(),
            -1,
            "",
            -1,
            null,
            0,
            0,
        )
    }

    @JvmStatic
    fun buildNanoMailLabel(): Label {
        val labelId = LABEL_ID_GENERATOR.getAndIncrement().toString()
        return Label(
            labelId,
            USER,
            "label$labelId",
            -1,
            -1,
            -1,
            null,
        )
    }

    @JvmStatic
    fun mapEntityLabeltoUi(label: Label) = com.yandex.mail.ui.entities.Label(label.lid, label.type, label.name, label.color)

    @JvmStatic
    fun buildMessageContent(): MessageContent {
        val threadId = MESSAGE_ID_GENERATOR.getAndIncrement().toLong()
        return MessageContent(
            id = threadId,
            folderId = NO_FOLDER_ID,
            timestampMillis = 1L,
            subject = "subject",
            subjIsEmpty = false,
            firstLine = "firstLine",
            addressLine = "addressLine",
            unreadCount = 0,
            messageCount = 2,
            avatar = buildAvatar(),
            hasAttach = true,
            labelIds = emptyList(),
            labels = emptyList(),
            attachments = listOf(buildAttachment()),
            isThread = true,
            type = 0,
            addresses = listOf("address@yandex.ru")
        )
    }

    @JvmStatic
    fun buildMessageContent(id: Long, addresses: List<String> = listOf("address@yandex.ru")): MessageContent =
        MessageContent(
            id = id,
            folderId = NO_FOLDER_ID,
            timestampMillis = 1L,
            subject = "subject",
            subjIsEmpty = false,
            firstLine = "firstLine",
            addressLine = "addressLine",
            unreadCount = 0,
            messageCount = 2,
            avatar = buildAvatar(),
            hasAttach = true,
            labelIds = emptyList(),
            labels = emptyList(),
            attachments = listOf(buildAttachment()),
            isThread = false,
            type = 0,
            addresses = addresses,
        )

    @JvmStatic
    fun buildMessageContentWithTimestamp(timeStampMillis: Long): MessageContent {
        val threadId = MESSAGE_ID_GENERATOR.getAndIncrement().toLong()
        return MessageContent(
            id = threadId,
            folderId = NO_FOLDER_ID,
            timestampMillis = timeStampMillis,
            subject = "subject",
            subjIsEmpty = false,
            firstLine = "firstLine",
            addressLine = "addressLine",
            unreadCount = 0,
            messageCount = 2,
            avatar = buildAvatar(),
            hasAttach = true,
            labelIds = emptyList(),
            labels = emptyList(),
            attachments = listOf(buildAttachment()),
            isThread = true,
            type = 0,
            addresses = listOf("address@yandex.ru")
        )
    }

    private fun buildAvatar(): AvatarMeta {
        return AvatarMeta("name", "email")
    }

    @JvmStatic
    fun buildAttachment(): Attach {
        return buildAttachment(1L)
    }

    @JvmStatic
    fun buildAttachment(mid: Long): Attach {
        return Attach(
            mid,
            "hid",
            "name",
            "image",
            1000L,
            "image/png",
            false,
            false,
            "http://melnikov.music/stas.mihaylov.mp3",
            1001L,
            false,
        )
    }

    fun buildAttachEntry(draftId: Long, attachId: Long): DraftAttachEntry {
        return DraftAttachEntry(
            attach_id = attachId,
            did = draftId,
            temp_mul_or_disk_url = null,
            file_uri = "file_uri",
            display_name = "display_name",
            size = 100L,
            mime_type = null,
            preview_support = false,
            is_disk = false,
            uploaded = DraftAttachEntry.NOT_UPLOADED,
            local_file_uri = null,
            is_folder = false,
            attach_order = 0
        )
    }

    @JvmStatic
    fun profileInfo(color: String? = "#FFFFFF"): ProfileInfo {
        val uniqueSuffix = COMMON_ID_GENERATOR.incrementAndGet().toString()
        val local = "noreply$uniqueSuffix"
        val domain = "github.com"
        return ProfileInfo(
            true,
            domain,
            "DN$uniqueSuffix",
            ava(),
            "Display Name$uniqueSuffix",
            "$local@$domain",
            color,
            local
        )
    }

    private fun ava() = Ava(AVATAR, "url" + COMMON_ID_GENERATOR.incrementAndGet())

    @JvmStatic
    fun avatar(): Avatar.Builder {
        val uniqueSuffix = COMMON_ID_GENERATOR.incrementAndGet().toString()
        return Avatar.Builder()
            .monogram("mono$uniqueSuffix")
            .color("color$uniqueSuffix")
            .imageUrl("url$uniqueSuffix")
            .type("type$uniqueSuffix")
    }

    fun suggestList(target: Target, size: Int): List<SearchSuggestResponse> {
        return List(size) {
            SearchSuggestResponse(
                tar = target,
                showText = "showText $it",
                searchText = "searchText $it",
                displayName = "displayName $it",
                id = "",
                highlights = SearchSuggestResponse.Highlights(
                    showText = emptyList(),
                    displayName = null,
                    emails = null
                ),
                email = if (target == Target.CONTACT) "$it@example.ru" else null
            )
        }
    }

    @JvmStatic
    fun promoTip() = PromoTip("name", ColorDrawable(), "title", null, "positive", "negative")

    fun story(id: String): Story {
        return Story(id, "title", "file:///android_asset/stories/mark_search.png", mutableListOf(), mutableListOf())
    }
}
