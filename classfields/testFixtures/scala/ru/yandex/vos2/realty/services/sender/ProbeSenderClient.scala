package ru.yandex.vos2.realty.services.sender

import ru.yandex.vos2.services.sender.{DeliveryParams, SenderClient, SenderTemplate}

import scala.util.Try

/**
  * @author Nataila Ratskevich (reimai@yandex-team.ru)
  */
class ProbeSenderClient extends SenderClient {

  var allSent: List[SenderTemplate] = Nil

  /**
    * Send letter via sender.yandex-team.ru
    *
    * @param template letter template with payload
    * @param delivery letter recipient
    */
  override def sendLetter(template: SenderTemplate, delivery: DeliveryParams): Try[Unit] = {
    allSent = template :: allSent
    Try(())
  }

  def reset(): Unit = allSent = Nil
}
