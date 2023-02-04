package ru.yandex.vertis.chat.service.impl.distributed

/**
  * Runnable specs on [[RemoteHttpChatService]]
  * along single HTTP Chat API server.
  *
  * @author dimas
  */
class RemoteHttp1ChatServiceSpec extends RemoteHttpChatServiceSpecBase {

  def numberOfInstances: Int = 1
}
