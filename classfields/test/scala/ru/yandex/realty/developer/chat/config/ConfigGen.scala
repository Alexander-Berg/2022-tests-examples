package ru.yandex.realty.developer.chat.config

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader
import org.scalacheck.Gen
import ru.yandex.realty.developer.chat.model.DeveloperCrmTypeNamespace.DeveloperCrmType
import ru.yandex.realty.model.gen.RealtyGenerators

import java.time.Instant
import java.util.UUID

object ConfigGen extends RealtyGenerators {

  val WebhookConfigGen: Gen[WebhookConfig] = {
    for {
      hook <- readableString
      hookHeaderName <- readableString
      hookHeaderValue <- readableString
      needsProxy <- bool
    } yield WebhookConfig(
      hook,
      Gen.oneOf(Seq.empty[HttpHeader], Seq(RawHeader(hookHeaderName, hookHeaderValue))).next,
      needsProxy
    )
  }

  val DeveloperConfigGen: Gen[DeveloperConfig] = {
    for {
      developerId <- Gen.posNum[Long]
      chatsEnabled <- bool
      hookConf <- WebhookConfigGen
      crmType <- Gen.oneOf(DeveloperCrmType.JIVOSITE, DeveloperCrmType.YANDEX_CHAT, DeveloperCrmType.COMAGIC)
      createTime <- instantInPast.map(_.getMillis).map(Instant.ofEpochMilli)
      replyToQuestions <- readableString
    } yield {
      DeveloperConfig(
        developerId,
        chatsEnabled,
        hookConf,
        UUID.randomUUID().toString,
        crmType,
        createTime,
        replyToQuestions
      )
    }
  }
}
