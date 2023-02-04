package ru.yandex.vertis.chat.service.impl.distributed

/**
  * Runnable specs on [[RemoteHttpChatService]]
  * along two HTTP Chat API servers.
  *
  * @author dimas
  */
class RemoteHttp2ChatServiceSpec extends RemoteHttpChatServiceSpecBase {

  def numberOfInstances: Int = 2
}
