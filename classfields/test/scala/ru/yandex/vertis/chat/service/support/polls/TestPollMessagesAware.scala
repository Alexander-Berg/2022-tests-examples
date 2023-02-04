package ru.yandex.vertis.chat.service.support.polls

import ru.yandex.vertis.chat.components.dao.techsupport.polls.messages._
import ru.yandex.vertis.chat.util.DMap

/**
  * TODO
  *
  * @author aborunov
  */
class TestPollMessagesService(messages: => PollMessages) extends PollMessagesService {
  override def getPollMessages: PollMessages = messages
}

class TestPollMessagesAware(messages: => PollMessages) extends PollMessagesAware {

  override val pollMessagesService: DMap[PollMessagesService] =
    DMap.forAllDomains(new TestPollMessagesService(messages))
}
