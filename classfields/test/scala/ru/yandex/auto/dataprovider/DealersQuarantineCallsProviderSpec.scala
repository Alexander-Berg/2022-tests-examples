package ru.yandex.auto.dataprovider

import java.io.{ByteArrayInputStream, InputStream}

import common.TestController
import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.json.{JsArray, Json}
import ru.yandex.auto.extdata.{AutoDataTypes, ExtDataServiceProxy}
import ru.yandex.extdata.common.meta.DataType
import ru.yandex.extdata.core.Data.StreamingData
import ru.yandex.extdata.core.Instance

import scala.util.{Failure, Success, Try}

class DealersQuarantineCallsProviderSpec extends WordSpecLike with Matchers {

  val dt = new DataType(AutoDataTypes.dealersQuarantineCalls.name, AutoDataTypes.dealersQuarantineCalls.format)

  def eds(instance: Try[Instance]): ExtDataServiceProxy = {
    new ExtDataServiceProxy(new TestController(instance))
  }

  // https://st.yandex-team.ru/AUTO-11395
  "extDataService.readData should not fail if EDS client returns no data" in {
    val dqcp = new DealersQuarantineCallsProvider(dt, eds(Failure(new RuntimeException())))
    dqcp.quarantineCallHaters shouldEqual Set.empty
  }

  "extDataService.readData should work fine with empty list" in {
    val dqcp = createDqcp(Json.arr())
    dqcp.quarantineCallHaters shouldEqual Set.empty
  }

  "DealersQuarantineCallsProvider should fetch data from DEALER_QUARANTINE_CALLS" in {
    val dqcp = createDqcp(Json.arr(21020))
    dqcp.quarantineCallHaters shouldEqual Set("21020")
  }

  "DealersQuarantineCallsProvider should strictly follow after EDS changes" in {
    val dqcp = createDqcp(Json.arr(21020))
    dqcp.quarantineCallHaters shouldEqual Set("21020")

    dqcp.update(stream(Json.arr(21020, 1144))) shouldBe Success(())
    dqcp.quarantineCallHaters shouldEqual Set("21020", "1144")

    dqcp.update(stream(Json.arr(21020))) shouldBe Success(())
    dqcp.quarantineCallHaters shouldEqual Set("21020")
  }

  private def stream(arr: JsArray): InputStream = new ByteArrayInputStream(arr.toString().getBytes)

  private def createDqcp(arr: JsArray): DealersQuarantineCallsProvider = {
    val instance = Instance(null, StreamingData(stream(arr)))
    new DealersQuarantineCallsProvider(dt, eds(Success(instance)))
  }

}
