package ru.yandex.realty.developer.chat.model.yachat

import org.scalacheck.Gen
import ru.yandex.realty.model.gen.RealtyGenerators

object YandexChatGen extends RealtyGenerators {

  val OperatorRequestSenderGen: Gen[OperatorRequest.Sender] = {
    for {
      name <- Gen.option(readableString)
      photo <- Gen.option(readableString)
    } yield OperatorRequest.Sender(name, photo)
  }

  val OperatorRequestMessageGen: Gen[OperatorRequest.Message] = {
    for {
      messageType <- Gen.oneOf(OperatorRequest.MessageType.Text, OperatorRequest.MessageType.Typein)
      textOpt <- messageType match {
        case OperatorRequest.MessageType.Text =>
          Gen.some(readableString(1, 4096))
        case OperatorRequest.MessageType.Typein =>
          Gen.const(None)
      }
      idOpt <- messageType match {
        case OperatorRequest.MessageType.Text =>
          Gen.some(Gen.posNum[Long])
        case OperatorRequest.MessageType.Typein =>
          Gen.const(None)
      }
    } yield OperatorRequest.Message(idOpt, messageType, textOpt)
  }

  val OperatorRequestRecipientGen: Gen[OperatorRequest.Recipient] = {
    for {
      id <- readableString
    } yield OperatorRequest.Recipient(id)
  }

  val OperatorRequestGen: Gen[OperatorRequest] = {
    for {
      sender <- OperatorRequestSenderGen
      recipient <- OperatorRequestRecipientGen
      message <- OperatorRequestMessageGen
    } yield {
      OperatorRequest(sender, recipient, message)

    }
  }
}
