package com.yandex.mail.network.response

import com.yandex.mail.runners.IntegrationTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(IntegrationTestRunner::class)
class MessageAttachmentsMetaJsonTest: GsonTest() {

    @Test
    fun fromJson() {
        val messageAttachmentsMeta = gson.fromJson(MESSAGE_ATTACHMENTS_JSON, MessageAttachmentsMeta::class.java)
        assertThat(messageAttachmentsMeta.count).isEqualTo(4)
        assertThat(messageAttachmentsMeta.fullSize).isEqualTo(4830410L)
        assertThat(messageAttachmentsMeta.attachments!!.size).isEqualTo(4)

        val messageAttachment1 = messageAttachmentsMeta.attachments!![0]
        assertThat(messageAttachment1.hid).isEqualTo("1.2")
        assertThat(messageAttachment1.displayName).isEqualTo("IMG_20180719_170545.jpg")
        assertThat(messageAttachment1.attachClass).isEqualTo("image")
        assertThat(messageAttachment1.size).isEqualTo(117527L)
        assertThat(messageAttachment1.mimeType).isEqualTo("image/jpeg")
        assertThat(messageAttachment1.downloadUrl).isEqualTo("download_url")
        assertThat(messageAttachment1.isDisk).isEqualTo(false)

        val messageAttachment2 = messageAttachmentsMeta.attachments!![1]
        assertThat(messageAttachment2.hid).isEqualTo("1.1.1")
        assertThat(messageAttachment2.displayName).isEqualTo("Горы.jpg")
        assertThat(messageAttachment2.attachClass).isEqualTo("image")
        assertThat(messageAttachment2.size).isEqualTo(1762478L)
        assertThat(messageAttachment2.mimeType).isEqualTo("application/octet-stream")
        assertThat(messageAttachment2.downloadUrl).isEqualTo("https://yadi.sk/i/dKx7nw-3myvaaQ")
        assertThat(messageAttachment2.isDisk).isEqualTo(true)

        val messageAttachment3 = messageAttachmentsMeta.attachments!![2]
        assertThat(messageAttachment3.hid).isEqualTo("1.1.2")
        assertThat(messageAttachment3.displayName).isEqualTo("Зима.jpg")
        assertThat(messageAttachment3.attachClass).isEqualTo("image")
        assertThat(messageAttachment3.size).isEqualTo(1394575L)
        assertThat(messageAttachment3.mimeType).isEqualTo("application/octet-stream")
        assertThat(messageAttachment3.downloadUrl).isEqualTo("https://yadi.sk/i/i1FS9aELuGOhZA")
        assertThat(messageAttachment3.isDisk).isEqualTo(true)

        val messageAttachment4 = messageAttachmentsMeta.attachments!![3]
        assertThat(messageAttachment4.hid).isEqualTo("1.1.3")
        assertThat(messageAttachment4.displayName).isEqualTo("Мишки.jpg")
        assertThat(messageAttachment4.attachClass).isEqualTo("image")
        assertThat(messageAttachment4.size).isEqualTo(1555830L)
        assertThat(messageAttachment4.mimeType).isEqualTo("application/octet-stream")
        assertThat(messageAttachment4.downloadUrl).isEqualTo("https://yadi.sk/i/nXhviy6girPRAw")
        assertThat(messageAttachment4.isDisk).isEqualTo(true)
    }

    companion object {
        const val MESSAGE_ATTACHMENTS_JSON =
                """{
                "count": 4,
                "fullSize": 4830410,
                "attachments": [
                    {
                        "hid": "1.2",
                        "display_name": "IMG_20180719_170545.jpg",
                        "class": "image",
                        "size": 117527,
                        "mime_type": "image/jpeg",
                        "download_url": "download_url",
                        "preview_supported": true,
                        "preview_url": "previewUrl"
                    },
                    {
                        "hid": "1.1.1",
                        "display_name": "Горы.jpg",
                        "narod": true,
                        "mime_type": "application/octet-stream",
                        "download_url": "https://yadi.sk/i/dKx7nw-3myvaaQ",
                        "preview_url": "",
                        "size": 1762478,
                        "class": "image"
                    },
                    {
                        "hid": "1.1.2",
                        "display_name": "Зима.jpg",
                        "narod": true,
                        "mime_type": "application/octet-stream",
                        "download_url": "https://yadi.sk/i/i1FS9aELuGOhZA",
                        "preview_url": "",
                        "size": 1394575,
                        "class": "image"
                    },
                    {
                        "hid": "1.1.3",
                        "display_name": "Мишки.jpg",
                        "narod": true,
                        "mime_type": "application/octet-stream",
                        "download_url": "https://yadi.sk/i/nXhviy6girPRAw",
                        "preview_url": "",
                        "size": 1555830,
                        "class": "image"
                    }
                ]
            }"""
    }
}
