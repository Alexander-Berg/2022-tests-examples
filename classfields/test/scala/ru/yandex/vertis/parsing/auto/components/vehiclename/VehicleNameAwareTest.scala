package ru.yandex.vertis.parsing.auto.components.vehiclename

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.clients.searchline._
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.tracing.Traced

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class VehicleNameAwareTest extends FunSuite with ScalaFutures with MockitoSupport {
  private val vehicleNameAware = new NopVehicleNameAware {}

  private val mockedParsedRow = mock[ParsedRow]

  implicit private val trace: Traced = TracedUtils.empty

  private val names = MarkModelWithNames(
    Mark("HONDA"),
    MarkName("Honda"),
    RuMarkName("Хонда"),
    Model("PARTNER"),
    ModelName("Partner"),
    RuModelName("Партнер")
  )

  test("getVehicleName") {
    when(mockedParsedRow.markModelWithNames).thenReturn(Some(names))

    val result = vehicleNameAware.getVehicleName(mockedParsedRow).futureValue
    assert(result.nonEmpty)
    assert(result.get.name == "Honda Partner")
    assert(result.get.ruName == "Хонда Партнер")
  }

  test("getVehicleName: no names") {
    when(mockedParsedRow.markModelWithNames).thenReturn(None)

    val result = vehicleNameAware.getVehicleName(mockedParsedRow).futureValue
    assert(result.isEmpty)
  }
}
