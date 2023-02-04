package auto.common.clients.carfax.testkit

import auto.common.clients.carfax.Carfax
import auto.common.clients.carfax.Carfax.Carfax
import ru.auto.api.vin.orders.orders_api_model.PublicOrderModel
import ru.auto.api.vin.orders.request_model.GetOrdersListRequest
import ru.auto.api.vin.vin_report_model.RawVinEssentialsReport
import zio.test.mock
import zio.test.mock.Mock
import zio.{Has, IO, URLayer, ZLayer}

object CarfaxMock extends Mock[Carfax] {

  object GetEssentialsRawReport extends Effect[String, Carfax.CarfaxError, RawVinEssentialsReport]

  object GetOrders extends Effect[GetOrdersListRequest, Carfax.CarfaxError, List[PublicOrderModel]]

  override val compose: URLayer[Has[mock.Proxy], Carfax] = ZLayer.fromService { proxy =>
    new Carfax.Service {
      override def getEssentialsRawReport(vin: String): IO[Carfax.CarfaxError, RawVinEssentialsReport] =
        proxy(GetEssentialsRawReport, vin)

      override def getOrders(request: GetOrdersListRequest): IO[Carfax.CarfaxError, List[PublicOrderModel]] =
        proxy(GetOrders, request)
    }
  }
}
