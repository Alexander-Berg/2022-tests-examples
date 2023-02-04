package ru.yandex.vertis.telepony.tools

import ru.yandex.vertis.telepony.dao.OperatorNumberDaoV2
import ru.yandex.vertis.telepony.model.{OperatorNumber, Phone, StatusValues}
import ru.yandex.vertis.telepony.service.RedirectServiceV2
import ru.yandex.vertis.telepony.util.{AutomatedContext, Page, RequestContext}

import scala.concurrent.Future

/**
  * Delete phones that were deleted by MTS. (https://st.yandex-team.ru/VSINFR-1874)
  *
  * @author evans
  */
object DeleteOutdatedTool extends Tool {

  implicit private val rc: RequestContext = AutomatedContext(id = "tool")
  import component._

  import scala.concurrent.ExecutionContext.Implicits.global

  //set actual domain
  val component = environment.serviceComponents.find(_.domain == "auto-dealers").get

  //Insert actual numbers
  val obsoletePhones =
    "+79852015681,+79852017274,+79852018225,+79852019543,+79104473006"
      .split(",")
      .map(Phone.apply)
      .toSet

  def delete(opn: OperatorNumber): Future[OperatorNumber] = {
//    val request = UpdateRequestV2(geoId = None, None, status = Some(Deleted()))
    log.info(opn.number.toString)
//    operatorNumberServiceV2.update(opn.number, request)
    Future.successful(null)
  }

  def deleteAll(): Future[Unit] =
    for {
      phones <- operatorNumberServiceV2.list(OperatorNumberDaoV2.Filter.Actual, Page(0, Int.MaxValue))
      redirects <- redirectServiceV2.list(RedirectServiceV2.Filter.Empty, Page(0, Int.MaxValue))
      redirectingPhones = redirects.map(_.source.number).toSet
      toRemove = phones
        .filter(opn => obsoletePhones.contains(opn.number))
        .filter(_.status.value != StatusValues.Deleted)
        .filter(p => !redirectingPhones.contains(p.number))
      _ <- Future.sequence(toRemove.map(delete))
    } yield ()

  deleteAll().onComplete { res =>
    log.info(res.toString)
    System.exit(0)
  }
}
