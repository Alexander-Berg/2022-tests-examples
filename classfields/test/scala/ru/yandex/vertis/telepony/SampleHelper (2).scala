package ru.yandex.vertis.telepony

import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.OperatorNumberServiceV2
import ru.yandex.vertis.telepony.service.OperatorNumberServiceV2.CreateRequest
import ru.yandex.vertis.telepony.util.{AutomatedContext, RequestContext}

/**
  * @author evans
  */
object SampleHelper {

  implicit val rc: RequestContext = AutomatedContext(id = "1")

  def createNumberRequest(
      phone: Phone,
      account: OperatorAccount = OperatorAccounts.MtsShared,
      originOperator: Operator = Operators.Mts): CreateRequest =
    OperatorNumberServiceV2.CreateRequest(
      phone,
      account,
      originOperator,
      None,
      None
    )
}
