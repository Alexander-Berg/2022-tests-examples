package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import com.yandex.mail.R
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findMany
import com.yandex.mail.testopithecus.steps.has
import com.yandex.xplat.common.Log
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.testopithecus.AttachmentView
import com.yandex.xplat.testopithecus.Message
import com.yandex.xplat.testopithecus.MessageAttach
import com.yandex.xplat.testopithecus.MessageListDisplay
import com.yandex.xplat.testopithecus.MessageView
import io.qameta.allure.kotlin.Allure
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.util.Arrays
import java.util.Locale

class MessageListDisplayImpl(private val device: UiDevice) : MessageListDisplay {
    override fun getMessageList(limit: Int): YSArray<MessageView> {
        val items = device.findMany("content")

        val messages = mutableListOf<MessageView>().apply {
            addAll(
                items.map {

                    val threadCounter = try {
                        it.find("thread_counter")!!.text.toInt()
                    } catch (e: Exception) {
                        null
                    }

                    var firstLine = try {
                        it.find("first_line")!!.text
                    } catch (e: Exception) {
                        device.find("email_list_recycler").scroll(Direction.DOWN, 1F)
                        it.find("first_line")!!.text
                    }

                    val from = it.find("sender")!!.text
                    val subject = it.find("subject")!!.text
                    val important = it.find("important_icon") != null
                    val read = it.find("sender_unread_status") == null
                    val timestamp = getTimestamp(it)

                    val attachments: List<AttachmentView> = it.findMany("item_attach_container").map { attach ->
                        val name = attach.contentDescription.removePrefix("Attachment ")
                        val ext = attach.find("item_attach_icon")!!.text.toLowerCase()
                        val title = if (ext == "…") name else "$name.$ext"
                        MessageAttach(title)
                    }

                    Message(
                        from = from,
                        subject = subject,
                        timestamp = timestamp.toLong(),
                        firstLine = firstLine.toString(),
                        threadCounter = threadCounter,
                        read = read,
                        important = important,
                        attachments = attachments.toMutableList()
                    )
                }
            )
        }

        return messages
    }

    override fun refreshMessageList() {
        Allure.step("Рефрешим список писем") {
            onView(withId(R.id.email_list_swiperefresh)).perform(swipeDown())
            Thread.sleep(3000)
        }
    }

    override fun swipeDownMessageList() {
        Allure.step("Подгружаем список писем") {
            device.find("email_list_recycler").swipe(Direction.UP, 0.8f, 7500)
            Thread.sleep(3000)
        }
    }

    override fun unreadCounter(): Int {
        if (device.has("counter")) {
            return device.find("counter").text?.toIntOrNull() ?: 0
        }
        return 0
    }

    private fun getTimestamp(message: UiObject2): Long {
        val formatters: List<DateTimeFormatter> = Arrays.asList(
            DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .toFormatter(Locale.ENGLISH),
            DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ofPattern("hh:mm"))
                .parseDefaulting(ChronoField.DAY_OF_YEAR, LocalDateTime.now().dayOfYear.toLong())
                .parseDefaulting(ChronoField.MONTH_OF_YEAR, LocalDateTime.now().monthValue.toLong())
                .parseDefaulting(ChronoField.YEAR, LocalDateTime.now().year.toLong())
                .toFormatter(Locale.ENGLISH),
        )
        val date = message.find("date_time")!!.text
        for (formatter in formatters) {
            try {
                return LocalDateTime.parse(
                    date,
                    formatter
                ).toEpochSecond(OffsetDateTime.now().offset) * 1000
            } catch (e: DateTimeParseException) {
                Log.info("${e.parsedString}\n ${e.stackTrace}")
            }
        }
        return 0
    }
}
