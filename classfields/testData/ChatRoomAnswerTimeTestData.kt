package ru.auto.ara.core.testdata

import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.chat.GetChatRoomDispatcher

class ChatRoomAnswerTimeData(
    val dispatcher: GetChatRoomDispatcher,
    val replyTimeText: Int,
    val description: String
)

val CHAT_ROOM_ANSWER_TIME_DATA: Array<ChatRoomAnswerTimeData> = arrayOf(
    ChatRoomAnswerTimeData(GetChatRoomDispatcher("in_hour_reply_time"), R.string.reply_in_hour, "in_hour_reply_time"),
    ChatRoomAnswerTimeData(GetChatRoomDispatcher("in_several_hours_reply_time"), R.string.reply_in_several_hours, "in_several_hours_reply_time"),
    ChatRoomAnswerTimeData(GetChatRoomDispatcher("in_day_reply_time"), R.string.reply_in_day, "in_day_reply_time"),
    ChatRoomAnswerTimeData(GetChatRoomDispatcher("rarely_reply_chat_only"), R.string.reply_rarely_chat_only, "rarely_reply_chat_only")

)