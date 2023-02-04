package ru.yandex.vertis.telepony.tools

import ru.yandex.vertis.telepony.dao.OperatorNumberDaoV2
import ru.yandex.vertis.telepony.model.Status.Ready
import ru.yandex.vertis.telepony.model.{OperatorNumber, Status}
import ru.yandex.vertis.telepony.service.OperatorNumberServiceV2.UpdateRequestV2
import ru.yandex.vertis.telepony.util.{AutomatedContext, Page, RequestContext}

import scala.concurrent.Future

/**
  * Force new to ready migration.
  *
  * @author evans
  */
object PrepareNewTool extends Tool {

  implicit private val rc: RequestContext = AutomatedContext(id = "tool")
  val component = environment.serviceComponents.find(_.domain == "billing_realty").get

  import component._

  import scala.concurrent.ExecutionContext.Implicits.global

  def new2Ready(opn: OperatorNumber): Future[OperatorNumber] = {
    val request = UpdateRequestV2(geoId = None, None, status = Some(Ready(None)))
    operatorNumberServiceV2.update(opn.number, request)
  }

  //todo:
  def processAll(): Future[Unit] =
    for {
      phones <- operatorNumberServiceV2.list(OperatorNumberDaoV2.Filter.Actual, Page(0, Int.MaxValue))
      toPrepare = phones
        .filter(opn => opn.status.value == Status.New.statusValue)
      _ <- Future.sequence(toPrepare.map(new2Ready))
    } yield ()

  processAll().onComplete { res =>
    log.info(res.toString)
    System.exit(0)
  }
}
