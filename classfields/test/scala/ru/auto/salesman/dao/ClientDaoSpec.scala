package ru.auto.salesman.dao

import cats.data.NonEmptyList
import ru.auto.salesman.dao.ClientDao._
import ru.auto.salesman.model.{
  AdsRequestType,
  AdsRequestTypes,
  CityId,
  Client,
  ClientStatuses,
  Poi7Value,
  RegionId
}
import ru.auto.salesman.model.Poi7Property.AutoActivateCarsOffers
import ru.auto.salesman.test.BaseSpec

trait ClientDaoSpec extends BaseSpec {

  def clientDao: ClientDao

  "ClientDao" should {
    "get nothing for filter with empty result" in {
      clientDao.get(ForId(1L)).success.value shouldBe empty
    }
    "get one record for filter by id" in {
      val clientId = 16281L
      val List(client) = clientDao.get(ForId(clientId)).success.value
      client.clientId shouldBe clientId
      client.status shouldBe ClientStatuses.Active
      client.regionId shouldBe 1L
      client.singlePayment shouldBe Set(AdsRequestTypes.CarsUsed)
      client.paidCallsAvailable shouldBe true
    }
    "get one record for filter by id 2" in {
      val clientId = 16301L
      val List(client) = clientDao.get(ForId(clientId)).success.value
      client.clientId shouldBe clientId
      client.categorizedClientId shouldBe Some(15717L)
      client.status shouldBe ClientStatuses.Freeze
      client.regionId shouldBe 1L
      client.cityId shouldBe 213L
      client.singlePayment shouldBe Set.empty[AdsRequestType]
    }
    "get all active records" in {
      val status = ClientStatuses.Active
      clientDao.get(ForStatus(status)).success.value should contain only (
        Client(
          16281L,
          None,
          Some(15697L),
          None,
          RegionId(1L),
          CityId(21735L),
          status,
          Set(AdsRequestTypes.CarsUsed),
          firstModerated = false,
          paidCallsAvailable = true,
          priorityPlacement = true
        ),
        Client(
          16282L,
          None,
          Some(15697L),
          None,
          RegionId(1L),
          CityId(21735L),
          status,
          Set(AdsRequestTypes.CarsUsed),
          firstModerated = true,
          paidCallsAvailable = true,
          priorityPlacement = true
        ),
        Client(
          16284L,
          None,
          Some(15697L),
          None,
          RegionId(10174L),
          CityId(21735L),
          status,
          Set(AdsRequestTypes.CarsUsed),
          firstModerated = true,
          paidCallsAvailable = true,
          priorityPlacement = true
        )
      )
    }

    "get one record for categorized client id" in {
      val categorizedClientId = 15717L
      val clientId = 16301L
      val List(client) =
        clientDao.get(ForCategorizedClientId(categorizedClientId)).success.value
      client.clientId shouldBe clientId
      client.categorizedClientId shouldBe Some(categorizedClientId)
      client.status shouldBe ClientStatuses.Freeze
      client.regionId shouldBe 1L
      client.cityId shouldBe 213L
      client.singlePayment shouldBe Set.empty[AdsRequestType]
    }

    "get only active client with auto_activate_offers turned on" in {
      val result = clientDao
        .get(
          ForStatusWithPoi7(
            ClientStatuses.Active,
            AutoActivateCarsOffers -> Poi7Value("1")
          )
        )
        .success
        .value
      result should have size 1
      val expectedRecord = Client(
        16282L,
        None,
        Some(15697L),
        None,
        RegionId(1L),
        CityId(21735L),
        ClientStatuses.Active,
        Set(AdsRequestTypes.CarsUsed),
        firstModerated = true,
        paidCallsAvailable = true,
        priorityPlacement = true
      )
      result should contain only expectedRecord
    }

    "get client with auto_activate_cars_offers turned on" in {
      val result = clientDao
        .get(
          ForIdWithPoi7(16282, AutoActivateCarsOffers -> Poi7Value("1"))
        )
        .success
        .value
      result should have size 1
      val expectedRecord = Client(
        16282L,
        None,
        Some(15697L),
        None,
        RegionId(1L),
        CityId(21735L),
        ClientStatuses.Active,
        Set(AdsRequestTypes.CarsUsed),
        firstModerated = true,
        paidCallsAvailable = true,
        priorityPlacement = true
      )
      result should contain only expectedRecord
    }

    "not get client with auto_activate_cars_offers turned off" in {
      val result = clientDao
        .get(
          ForIdWithPoi7(16281, AutoActivateCarsOffers -> Poi7Value("1"))
        )
        .success
        .value
      result shouldBe empty
    }

    "find client by poi_id" in {
      val client = clientDao.getClientByPoiId(10601).success.value.get
      client.clientId shouldBe 16281

      clientDao.getClientByPoiId(999223344).success.value shouldBe empty
    }

    "find all clients in region" in {

      val EmptyClients = None
      val RegionIds = NonEmptyList.of(RegionId(10174))
      val ExpectedClientIds = Set(16284L)

      val regionFilter = ForCallsTurnedOn(RegionIds, EmptyClients)

      val clientsIds = clientDao.get(regionFilter).success.value.map(_.clientId)

      clientsIds should contain.theSameElementsAs(ExpectedClientIds)

    }

    "find selected clients in region" in {
      val ClientIds = Option(NonEmptyList.one(16282L))
      val RegionIds = NonEmptyList.of(RegionId(1), RegionId(10174))

      val clientsFilter = ForCallsTurnedOn(RegionIds, ClientIds)

      val clientsIds =
        clientDao.get(clientsFilter).success.value.map(_.clientId)

      clientsIds should contain.theSameElementsAs(ClientIds.get.toList)

    }

  }

}
