package ru.yandex.vertis.telepony.client

import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.SampleHelper._
import ru.yandex.vertis.telepony.model.{Operator, OperatorRedirect, Phone}
import ru.yandex.vertis.telepony.service.OperatorClient
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._

/**
  * @author neron
  */
trait OperatorClientSpec extends SpecBase {

  def operatorClient: OperatorClient

  def operator: Operator

  def existingPhone: Phone

  s"${operator}OperatorClient" should {
    "delete" in {
      val source = existingPhone
      operatorClient.deleteRedirect(source).futureValue
      val actualOperatorRedirect = operatorClient.getRedirect(source).futureValue
      actualOperatorRedirect.source shouldEqual source
      actualOperatorRedirect.target shouldBe empty
    }

    "update" in {
      val source = existingPhone
      val target = PhoneGen.next
      val expectedOperatorRedirect = OperatorRedirect(source, Some(target))
      operatorClient.deleteRedirect(source).futureValue
      operatorClient.updateRedirect(expectedOperatorRedirect).futureValue
      val actualOperatorRedirect = operatorClient.getRedirect(source).futureValue
      actualOperatorRedirect shouldEqual expectedOperatorRedirect
    }

  }
}
