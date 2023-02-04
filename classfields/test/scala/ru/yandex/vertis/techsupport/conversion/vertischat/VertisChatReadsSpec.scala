package ru.yandex.vertis.vsquality.techsupport.conversion.vertischat

import cats.data.Validated
import cats.syntax.validated._
import org.scalacheck.{Gen, Prop}
import org.scalatestplus.scalacheck.Checkers
import ru.yandex.vertis.vsquality.techsupport.model.{
  ClientInfo,
  ClientRequestContext,
  Domain,
  Message,
  MessageType,
  Request,
  UserId
}
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
import ru.yandex.vertis.vsquality.techsupport.model.api.RequestMeta
import ru.yandex.vertis.chat.model.api.api_model.{Message => ChatMessage}
import VertisChatConversionInstances._
import ru.yandex.vertis.vsquality.techsupport.config.MdsConfig

/**
  * @author devreggs
  */
class VertisChatReadsSpec extends SpecBase {

  private val mdsConfig = MdsConfig("http://avatars.mdst.yandex.net")

  private val chatMessageGen: Gen[ChatMessage] =
    (for {
      client     <- gen[UserId.Client.Autoru]
      clientInfo <- gen[ClientInfo].map(_.copy(clientId = client))
      rc         <- gen[ClientRequestContext].map(_.copy(clientInfo = clientInfo))
      message    <- gen[Message].suchThat(_.`type` != MessageType.BotCommand)
      msg        <- gen[Request.TechsupportAppeal.ProcessMessage].map(_.copy(context = rc, message = message))
    } yield msg)
      .suchThat { msg =>
        msg.message.payload.exists(
          _.text.nonEmpty
        ) && (msg.message.`type` != MessageType.Feedback || msg.message.payload
          .exists(_.pollRating.nonEmpty))
      }
      .map { request =>
        VertisChatConversionInstances.VertisChatWrites.serialize(request) match {
          case Validated.Valid(a)   => a.clearAttachments
          case Validated.Invalid(e) => throw new IllegalArgumentException(s"Failed serialization $e")
        }
      }

  "VertisChatReads" should {
    "reads all clients messages" in {
      Checkers.check(Prop.forAll(chatMessageGen, gen[RequestMeta]) { (chatMessage, rm) =>
        vertisChatReads(mdsConfig).deserialize(chatMessage, Domain.Autoru)(rm) match {
          case Validated.Valid(Some(request)) =>
            val msg = VertisChatWrites.serialize(request)
            msg == chatMessage.valid
          case other =>
            throw new IllegalArgumentException(s"Failed deserialization $other")
        }
      })

    }
  }
}
