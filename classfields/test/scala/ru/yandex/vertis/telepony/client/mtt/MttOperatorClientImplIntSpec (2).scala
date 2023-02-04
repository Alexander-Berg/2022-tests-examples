package ru.yandex.vertis.telepony.client.mtt

import org.scalatest.Ignore
import ru.yandex.vertis.telepony.IntegrationSpecTemplate
import ru.yandex.vertis.telepony.client.OperatorClientSpec
import ru.yandex.vertis.telepony.model.mtt.SipId
import ru.yandex.vertis.telepony.model.{Operator, Operators, Phone}
import ru.yandex.vertis.telepony.service.OperatorClient
import ru.yandex.vertis.telepony.service.impl.mtt.MttOperatorClient
import ru.yandex.vertis.telepony.util.Threads

/**
  * @author neron
  */
@Ignore
class MttOperatorClientImplIntSpec extends OperatorClientSpec with IntegrationSpecTemplate {

  override lazy val operatorClient: OperatorClient =
    new MttOperatorClient(
      MttClientImplIntSpec.createMttClient(materializer)
    )(Threads.lightWeightTasksEc)

  override def operator: Operator = Operators.Mtt

  override def existingPhone: Phone = SipId("74996481803").toPhone
}
