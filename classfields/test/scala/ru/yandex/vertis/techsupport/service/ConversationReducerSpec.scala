package ru.yandex.vertis.vsquality.techsupport.service

import org.scalacheck.{Arbitrary, Prop}
import org.scalatestplus.scalacheck.Checkers
import ru.yandex.vertis.vsquality.techsupport.model.{ClientRequestContext, Conversation}
import ru.yandex.vertis.vsquality.techsupport.service.impl.ConversationReducerImpl
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase

/**
  * @author devreggs
  */
class ConversationReducerSpec extends SpecBase {

  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
  import ru.yandex.vertis.vsquality.techsupport.CoreArbitraries._

  val conversationReducer = ConversationReducerImpl
  implicit private val rc: ClientRequestContext = generate()

  "ConversationReducer" should {

    "reduce conversation assigned to bot" in {
      Checkers.check(Prop.forAll(implicitly[Arbitrary[Conversation]].arbitrary) { conversation =>
        conversationReducer
          .makeMessage(conversation, conversation.messages.head.author)
          .payload
          .map(_.text)
          .getOrElse("")
          .linesIterator
          .length >= conversation.messages.length
      })
    }
  }
}
