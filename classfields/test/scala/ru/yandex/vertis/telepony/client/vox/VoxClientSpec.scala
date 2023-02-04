package ru.yandex.vertis.telepony.client.vox

import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.logging.SimpleLogging
import ru.yandex.vertis.telepony.model.CallbackGenerator.CallbackOrderSourceGen
import ru.yandex.vertis.telepony.model.CallbackOrder.{CallerIdModes, SourceInfo, TargetInfo}
import ru.yandex.vertis.telepony.model.{CallPeriod, CallbackOrder, ObjectId, Phone, Tag, TypedDomains}
import ru.yandex.vertis.telepony.service.impl.vox.Internals.VoxStartCallbackRequestView
import ru.yandex.vertis.telepony.service.impl.vox.{VoxClient, VoxJsonProtocol}
import ru.yandex.vertis.telepony.settings.CallbackSettings
import ru.yandex.vertis.telepony.util.{FutureUtil, Page, Range}

import scala.concurrent.Future
import scala.util.Success

/**
  * @author neron
  */
trait VoxClientSpec extends SpecBase with SimpleLogging {

  def client: VoxClient

  private val config = ConfigFactory.parseResources("service.conf").resolve()

  private val domain = TypedDomains.autoru_def

  private val callbackRuleId = "2568945"

  def loadAllNumbers(): Future[Iterable[Phone]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val pageSize = 1000
    for {
      result <- client.getPhoneNumbers(Range(0, 1))
      pagesCount = result.total / pageSize
      phones <- FutureUtil.traverseSequential(0.to(pagesCount)) { pageNumber =>
        client.getPhoneNumbers(Page(pageNumber, pageSize)).map(_.values).andThen {
          case Success(res) =>
            log.info(s"page $pageNumber. ${res.size}")
        }
      }
    } yield phones.foldLeft(Iterable.empty[Phone])((acc, cur) => acc ++ cur)
  }

  "VoxClient" should {
    "return numbers" in {
      val allPhones = loadAllNumbers().futureValue
      log.info(s"size = ${allPhones.size}. ${allPhones.mkString(" ")}")
//      val result = client.getPhoneNumbers(Range(10000000, 10000001)).futureValue
//      log.info(s"total = ${result.total}, slice = ${result.slice}")
//      log.info(s"${result.mkString(" ")}")
    }
    "bind phone number to application" in {
      val numbers = Seq("+79585821054", "+79585805401").map(Phone.apply)
      client
        .bindPhoneNumberToApplication(
          numbers = numbers,
          ruleName = "autoru_def_redirect",
          bind = true
        )
        .futureValue
    }
    "get call history" in {
      val from = new DateTime(2018, 4, 24, 15, 10)
      val result = client
        .getCallHistory(
          slice = Range(0, 1000),
          from = from,
          to = DateTime.now(),
          ruleName = "autoru_def_redirect"
        )
        .futureValue

      log.info(s"total = ${result.total}, slice = ${result.slice}")
      log.info(s"${result.mkString(" ")}")

      result.foreach { item =>
        println(item.toRawCall)
      }
    }
    "start scenarios" in {
      val order = CallbackOrder(
        id = CallbackOrder.Id("4"),
        objectId = ObjectId("neron-1"),
        tag = Tag.Empty,
        payload = PayloadGen.next,
        createTime = null,
        status = null,
        source = SourceInfo(Phone("+78004445555"), CallerIdModes.SystemNumber),
        target = TargetInfo(
          number = Phone("+79817757583"),
          callerIdMode = CallerIdModes.SystemNumber,
          callPeriods = Seq(CallPeriod(DateTime.now().minusDays(1), DateTime.now().plusDays(1))),
          notification = NotificationOptGen.next
        ),
        domain,
        CallbackOrderSourceGen.next
      )

      val callbackSettings = CallbackSettings(config.getConfig("telepony.domain.default.callback"))

      val callbackRequest = VoxStartCallbackRequestView.from(
        callbackOrder = order,
        callbackSettings = callbackSettings,
        constantCallerId = callbackSettings.defaultCallerId
      )

      val customDataJson = VoxJsonProtocol.StartScenariosFormat.jsonReqFormat(callbackRequest)

      client.startScenario(callbackRuleId, customDataJson).futureValue
    }
  }

}
