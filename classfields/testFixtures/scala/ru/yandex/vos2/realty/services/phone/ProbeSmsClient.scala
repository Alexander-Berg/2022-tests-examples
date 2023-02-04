package ru.yandex.vos2.realty.services.phone

import java.util.concurrent.atomic.AtomicInteger

import ru.yandex.vos2.services.phone.{SmsParams, SmsSenderClient, TypedSmsTemplate}

import scala.util.{Success, Try}

class ProbeSmsClient extends SmsSenderClient {

  val calls = new AtomicInteger()

  override def send(template: TypedSmsTemplate, phone: SmsParams): Try[String] = {
    Success(calls.incrementAndGet().toString)
  }
}
