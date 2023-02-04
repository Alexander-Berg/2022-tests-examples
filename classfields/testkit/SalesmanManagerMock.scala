package auto.common.manager.salesman.testkit

import common.geobase.model.RegionIds.RegionId
import auto.common.clients.salesman.Campaign
import auto.common.manager.salesman.SalesmanManager
import auto.common.model.ClientId
import ru.auto.api.api_offer_model.{Category, Section}
import zio.{IO, ZLayer}
import zio.test.mock.mockable

@mockable[SalesmanManager]
object SalesmanManagerMock

object SalesmanManagerEmpty {

  val empty = ZLayer.succeed {
    new SalesmanManager {
      override def getCampaigns(
          clientId: ClientId,
          includeDisabled: Boolean): IO[SalesmanManager.SalesmanManagerError, List[Campaign]] = ???

      override def isPaidCallInRegion(
          clientId: ClientId,
          regionId: RegionId,
          category: Category,
          section: Section): IO[SalesmanManager.SalesmanManagerError, Boolean] = ???
    }
  }
}
