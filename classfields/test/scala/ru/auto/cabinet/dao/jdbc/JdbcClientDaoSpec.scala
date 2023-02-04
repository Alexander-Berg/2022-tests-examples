package ru.auto.cabinet.dao.jdbc

import com.google.protobuf.{BoolValue, StringValue, UInt64Value}
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.enablers.Emptiness.emptinessOfGenTraversable
import ru.auto.cabinet.model.{
  ClientModerationBuffer,
  ClientStatuses,
  DealershipInfo,
  File,
  PoiFilesCode,
  S3Config,
  S3Files,
  SalonInfo,
  StatusCounter
}
import ru.auto.cabinet.ApiModel.FindClientsRequest
import ru.auto.cabinet.test.JdbcSpecTemplate
import ru.auto.cabinet.test.model.SalonInfoData
import slick.jdbc.GetResult
import slick.jdbc.MySQLProfile.api._
import JdbcClientDaoSpec._
import auto.common.Pagination.RequestPagination
import ru.auto.cabinet.converter.DealerAutoruToModerationConverter
import ru.auto.cabinet.dao.entities.ClientsModerationBuffer.ClientModerationBufferGet
import ru.auto.cabinet.service.instr.EmptyInstr
import ru.auto.cabinet.service.{DealerService, S3FileUrlService}

import java.sql.Timestamp
import java.time.Instant
import scala.concurrent.Future

class JdbcClientDaoSpec extends AsyncWordSpec with JdbcSpecTemplate {

  implicit val instr = new EmptyInstr("test")

  private val clientDao = new JdbcClientDao(
    office7Database,
    office7Database,
    balanceHandle.databaseName,
    poiHandle.databaseName)

  private val bufferDao =
    new JdbcClientsChangedBufferDao(office7Database, office7Database)

  private val markDao = new MarkDao(office7Database, office7Database)
  private val companyDao = new CompanyDao(office7Database, office7Database)

  private val clientUserDao =
    new ClientUserDao(office7Database, office7Database)

  private val clientDealerDao =
    new ClientDealerDao(office7Database, office7Database)

  private val poiDao =
    new JdbcPoiDataDao(poiDatabase, poiDatabase)

  private val dealerDao = new DealerService(
    companyDao,
    clientDao,
    clientUserDao,
    clientDealerDao,
    markDao,
    poiDao)

  "ClientDao" should {
    "find files by clientId + poiId" in {
      for {
        files <- clientDao.getFiles(1, 1)
      } yield files shouldBe Seq(
        File("img1.jpg", PoiFilesCode.withName("photo")),
        File("img2.jpg", PoiFilesCode.withName("photo")))
    }

    "find dealership info by clientId" in {
      for {
        dealership <- clientDao.getClientDealerships(10000)
      } yield dealership.map(
        _.copy(date = Instant.ofEpochMilli(0))) shouldBe Seq(
        DealershipInfo(
          4,
          10000,
          10,
          "img5.jpg",
          date = Instant.ofEpochMilli(0)))
    }

    "find client by id" in {
      for {
        client <- clientDao.get(1L)
      } yield client.clientId shouldBe 1L
    }

    "find detailed client by id without agency and company" in {
      for {
        detailedClient <- clientDao.getDetailed(1L)
      } yield {
        detailedClient.id shouldBe 1L
        detailedClient.name shouldBe Some("НБСмотор")
        detailedClient.agencyId shouldBe None
        detailedClient.agencyName shouldBe None
        detailedClient.companyId shouldBe None
        detailedClient.companyName shouldBe None
        detailedClient.clientProperties.regionId shouldBe 11316
        detailedClient.clientProperties.originId shouldBe "msk0105"
        detailedClient.clientProperties.firstModerated shouldBe false
      }
    }

    "find detailed client by id with agency and company" in {
      for {
        detailedClient <- clientDao.getDetailed(105L)
      } yield {
        detailedClient.id shouldBe 105L
        detailedClient.name shouldBe Some("НБСмотор")
        detailedClient.agencyId shouldBe Some(106L)
        detailedClient.agencyName shouldBe Some("НБСмотор")
        detailedClient.companyId shouldBe Some(1L)
        detailedClient.companyName shouldBe Some("test1")
        detailedClient.clientProperties.regionId shouldBe 11316
        detailedClient.clientProperties.originId shouldBe "msk0105"
      }
    }

    "update loyalty status but not amo buffer table" in {
      for {
        //cleanup
        cleanupList <- bufferDao.get()
        _ <- Future.traverse(cleanupList)(res => bufferDao.delete(res.id))
        //mark for sync
        _ <- clientDao.updateLoyalty(1L, true)
        _ <- clientDao.updateLoyalty(1L, false)
        bufferList <- bufferDao.get()
      } yield {
        val filteredList = bufferList.filter(_.clientId == 1L)
        filteredList.size shouldBe 0
      }
    }

    "find company clients" in {
      for {
        clients <- clientDao.companyClients(1L)
      } yield {
        clients should not be empty
        clients.map(_.clientId) should contain(105L)
      }
    }

    "filter clients by origin using FindClientsRequest" in {
      val filter = findClientsRequest(
        filter = Some {
          _.setOrigin(StringValue.of("msk0106"))
        }
      )

      val expectedClientId = 107L

      for {
        clients <- clientDao.findClients(filter)
      } yield clients.map(_.clientId) shouldBe Seq(expectedClientId)
    }

    "filter clients by agency id using FindClientsRequest" in {
      val filter = findClientsRequest(
        filter = Some {
          _.setAgencyId(UInt64Value.of(106L))
        }
      )

      val expectedClientId = 105L

      for {
        clients <- clientDao.findClients(filter)
      } yield clients.map(_.clientId) shouldBe Seq(expectedClientId)
    }

    "count clients by company id using FindClientsRequest" in {
      val filter = findClientsRequest(
        filter = Some {
          _.setCompanyId(UInt64Value.of(777L))
        }
      )

      for {
        count <- clientDao.countClients(filter)
      } yield count shouldBe 1
    }

    "filter clients by company id using FindClientsRequest" in {
      val filter = findClientsRequest(
        filter = Some {
          _.setCompanyId(UInt64Value.of(777L))
        }
      )

      val expectedClientId = 333L

      for {
        clients <- clientDao.findClients(filter)
      } yield clients.map(_.clientId) shouldBe Seq(expectedClientId)
    }

    "filter clients by stopped preset using FindClientsRequest" in {
      val filter = findClientsRequest(
        filter = Some {
          _.setPreset(FindClientsRequest.Filter.Preset.STOPPED)
        }
      )

      for {
        clients <- clientDao.findClients(filter)
      } yield clients.size shouldBe 0
    }

    "filter clients by freezed preset using FindClientsRequest" in {
      val filter = findClientsRequest(
        filter = Some {
          _.setPreset(FindClientsRequest.Filter.Preset.STOPPED)
        }
      )

      for {
        clients <- clientDao.findClients(filter)
      } yield clients.size shouldBe 0
    }

    "filter clients by active preset using FindClientsRequest" in {
      val filter = findClientsRequest(
        filter = Some {
          _.setPreset(FindClientsRequest.Filter.Preset.ACTIVE)
        }
      )

      for {
        clients <- clientDao.findClients(filter)
      } yield clients.size shouldBe 10
    }

    "filter clients by all preset using FindClientsRequest" in {
      val filter = findClientsRequest(
        filter = Some {
          _.setPreset(FindClientsRequest.Filter.Preset.ALL)
        }
      )

      for {
        clients <- clientDao.findClients(filter)
      } yield clients.size shouldBe 11
    }

    "filter clients by multiposting status using FindClientsRequest" in {
      val filterAll = findClientsRequest(
        filter = Some(f => f)
      )

      val filterEnabledMultiposting = findClientsRequest(
        filter = Some {
          _.setMultipostingEnabled(BoolValue.of(true))
        }
      )

      val filterDisabledMultiposting = findClientsRequest(
        filter = Some {
          _.setMultipostingEnabled(BoolValue.of(false))
        }
      )

      for {
        all <- clientDao.findClients(filterAll)
        enabled <- clientDao.findClients(filterEnabledMultiposting)
        disabled <- clientDao.findClients(filterDisabledMultiposting)
      } yield {
        all.size shouldBe 11
        enabled.size shouldBe 1
        disabled.size shouldBe 10
        enabled.map(_.clientId) should contain(107L)
      }
    }

    "filter clients with combined conditions using FindClientsRequest" in {
      val filterA = findClientsRequest(
        filter = Some { builder =>
          builder
            .setOrigin(StringValue.of("msk0105"))
            .setCompanyId(UInt64Value.of(888L))
        }
      )

      val filterB = findClientsRequest(
        filter = Some { builder =>
          builder
            .setOrigin(StringValue.of("msk0105"))
            .setCompanyId(UInt64Value.of(888L))
            .setPreset(FindClientsRequest.Filter.Preset.ACTIVE)
        }
      )

      val expectedA = Seq(444L, 999L)
      val expectedB = Seq(444L)

      for {
        clientsA <- clientDao.findClients(filterA)
        clientsB <- clientDao.findClients(filterB)
      } yield {
        clientsA.map(_.clientId) should contain theSameElementsAs expectedA
        clientsB.map(_.clientId) shouldBe expectedB
      }
    }

    "filter clients with pagination" in {
      val pageA = findClientsRequest(
        paging = Some {
          _.setPage(1).setPageSize(4)
        }
      )

      val pageB = findClientsRequest(
        paging = Some {
          _.setPage(2).setPageSize(4)
        }
      )

      val pageC = findClientsRequest(
        paging = Some {
          _.setPage(3).setPageSize(4)
        }
      )

      val expectedA = Seq(999L, 444L, 555L, 333L)
      val expectedB = Seq(201L, 200L, 107L, 106L)
      val expectedC = Seq(105L, 104L, 102L)

      for {
        clientsA <- clientDao.findClients(pageA)
        clientsB <- clientDao.findClients(pageB)
        clientsC <- clientDao.findClients(pageC)
      } yield {
        clientsA.map(_.clientId) shouldBe expectedA
        clientsB.map(_.clientId) shouldBe expectedB
        clientsC.map(_.clientId) shouldBe expectedC
      }
    }

    "append salon info to moderation" in {
      val salonInfo = SalonInfoData.salonInfo
      val poiId = salonInfo.poi.id
      val clientId = 105L
      val userId = 100L
      val balanceClientId = 5L
      val updateAt = Timestamp.from(Instant.now)

      val imagesS3Config = S3Files(
        "http://s3_images",
        "images_prefix"
      )

      val filesS3Config = S3Files(
        "http://s3_files",
        "files_prefix"
      )

      val s3Config = S3Config(imagesS3Config, filesS3Config)

      val s3FileUrlService = new S3FileUrlService(s3Config)

      val dealerModerationConverter =
        new DealerAutoruToModerationConverter(s3FileUrlService)

      for {
        client <- clientDao.get(clientId)
        dealer <- dealerDao.getDealer(client)

        event = dealerModerationConverter.convert(
          dealer,
          managerOpt = None,
          salonInfo,
          userId
        )

        clientModerationBuffer = ClientModerationBuffer(
          id = -1,
          clientId,
          payload = event,
          updatedAt = updateAt,
          sent = false
        )

        _ <- clientDao.appendSalonInfoToLegacyModeration(
          clientId,
          balanceClientId,
          salonInfo,
          clientModerationBuffer,
          isNewClient = false)
        (onmoderate, onmoderateSalon) <- office7Database.run(
          sql"SELECT is_onmoderate, is_onmoderate_salons FROM clients WHERE id = $clientId"
            .as[(Boolean, Boolean)]
            .head
        )
        moderationData <- office7Database.run(
          sql"SELECT data FROM moderation WHERE entity_id = $poiId AND entity_type = 'salon'"
            .as[String]
            .head
        )
        moderationBuffer <- office7Database.run(
          sql"""select * from clients_moderation_buffer where client_id = $clientId"""
            .as[ClientModerationBuffer]
            .head
        )
        buf <- bufferDao.get()
      } yield {
        moderationBuffer.payload shouldBe clientModerationBuffer.payload
        moderationData shouldBe salonInfo.serializePHP
        onmoderate shouldBe true
        onmoderateSalon shouldBe true
        buf.map(rec => rec.clientId -> rec.dataSource) should contain(
          (clientId, "moderation"))
      }
    }

    "append salon info to moderation and store phones and poi for new clients" in {
      val salonInfo = SalonInfoData.salonInfo
      val poiId = salonInfo.poi.id
      val clientId = 999L
      val userId = 100L
      val balanceClientId = 9L
      val expectedBalanceRegion = SalonInfoData.salonInfo.poi.yaCityId
      val updateAt = Timestamp.from(Instant.now)
      val expectedPoiPhones = List(
        PoiPhone(
          phone = "74993947451",
          phoneMask = "1:3:7",
          callFrom = 9,
          callTill = 23,
          title = "Дилер",
          marksIds = "",
          marksNames = ""
        ),
        PoiPhone(
          phone = "79267178974",
          phoneMask = "1:3:7",
          callFrom = 9,
          callTill = 23,
          title = "Дилер",
          marksIds = "",
          marksNames = ""
        )
      )
      val expectedLocation = GeoLocation(
        salonInfo.poi.yaCountryId,
        salonInfo.poi.yaRegionId,
        salonInfo.poi.yaCityId,
        salonInfo.poi.countryId,
        salonInfo.poi.regionId,
        salonInfo.poi.cityId
      )

      val imagesS3Config = S3Files(
        "http://s3_images",
        "images_prefix"
      )

      val filesS3Config = S3Files(
        "http://s3_files",
        "files_prefix"
      )

      val s3Config = S3Config(imagesS3Config, filesS3Config)

      val s3FileUrlService = new S3FileUrlService(s3Config)

      val dealerModerationConverter =
        new DealerAutoruToModerationConverter(s3FileUrlService)

      for {
        client <- clientDao.get(clientId)
        dealer <- dealerDao.getDealer(client)

        event = dealerModerationConverter.convert(
          dealer,
          managerOpt = None,
          salonInfo,
          userId)

        clientModerationBuffer = ClientModerationBuffer(
          id = -1,
          clientId,
          payload = event,
          updatedAt = updateAt,
          sent = false
        )

        _ <- clientDao.appendSalonInfoToLegacyModeration(
          clientId,
          balanceClientId,
          salonInfo,
          clientModerationBuffer,
          isNewClient = true)
        poiData <- office7Database.run(
          sql"SELECT id,address,ya_city_id as geo_id,lat,lng,city_id,region_id,country_id,ya_city_id,ya_region_id,ya_country_id FROM #${poiHandle.databaseName}.poi WHERE id = $poiId"
            .as[SalonInfo.Poi]
            .head
        )
        poiPhones <- office7Database.run(
          sql"SELECT title, phone, phone_mask, call_from, call_till, marks_ids, marks_names FROM #${poiHandle.databaseName}.poi_phones WHERE poi_id = $poiId"
            .as[PoiPhone]
        )
        balanceData <- balanceDatabase.run(
          sql"SELECT region_id FROM clients WHERE id = $balanceClientId"
            .as[Long]
            .head
        )
        geoLocationData <- office7Database.run(
          sql"SELECT ya_country_id,ya_region_id,ya_city_id,country_id,region_id,city_id FROM clients WHERE id = $clientId"
            .as[GeoLocation]
            .head
        )
        (onmoderate, onmoderateSalon) <- office7Database.run(
          sql"SELECT is_onmoderate, is_onmoderate_salons FROM clients WHERE id = $clientId"
            .as[(Boolean, Boolean)]
            .head
        )
        moderationData <- office7Database.run(
          sql"SELECT data FROM moderation WHERE entity_id = $poiId AND entity_type = 'salon'"
            .as[String]
            .head
        )
        moderationBuffer <- office7Database.run(
          sql"""select * from clients_moderation_buffer where client_id = $clientId"""
            .as[ClientModerationBuffer]
            .head
        )
        buf <- bufferDao.get()
      } yield {
        moderationBuffer.payload shouldBe clientModerationBuffer.payload
        poiData shouldBe salonInfo.poi
        poiPhones shouldEqual expectedPoiPhones
        balanceData shouldBe expectedBalanceRegion
        geoLocationData shouldBe expectedLocation
        moderationData shouldBe salonInfo.serializePHP
        onmoderate shouldBe true
        onmoderateSalon shouldBe true
        buf.map(rec => rec.clientId -> rec.dataSource) should contain(
          (clientId, "moderation"))
      }
    }

    "list all customer clients counters" in {
      val req = FindClientsRequest
        .newBuilder()
        .setFilter(
          FindClientsRequest.Filter
            .newBuilder()
            .setCompanyId(UInt64Value.of(888)))
        .build()

      val counters = Seq(
        StatusCounter(ClientStatuses.New, 1),
        StatusCounter(ClientStatuses.Active, 1))

      for {
        presets <- clientDao.getCustomerPresets(req)
      } yield presets should contain theSameElementsAs counters

    }
  }
}

object JdbcClientDaoSpec {

  case class PoiPhone(
      title: String,
      phone: String,
      phoneMask: String,
      callFrom: Int,
      callTill: Int,
      marksIds: String,
      marksNames: String)

  case class GeoLocation(
      yaCountryId: Long,
      yaRegionId: Long,
      yaCityId: Long,
      countryId: Option[Long],
      regionId: Option[Long],
      cityId: Option[Long])

  implicit
  val poiResult: GetResult[SalonInfo.Poi] = GetResult[SalonInfo.Poi] { r =>
    val id = r.nextLong()
    val address = r.nextString()
    val geoId = r.nextLong()
    val lat = r.nextDouble()
    val lng = r.nextDouble()
    val cityId = r.nextLong()
    val regionId = r.nextLong()
    val countryId = r.nextLong()
    val yaCityId = r.nextLong()
    val yaRegionId = r.nextLong()
    val yaCountryId = r.nextLong()

    SalonInfo.Poi(
      id,
      address,
      geoId,
      lat,
      lng,
      Some(cityId),
      Some(regionId),
      Some(countryId),
      yaCityId,
      yaRegionId,
      yaCountryId
    )
  }

  implicit val phoneResult: GetResult[PoiPhone] = GetResult[PoiPhone] { r =>
    val title = r.nextString()
    val phone = r.nextString()
    val phoneMask = r.nextString()
    val callFrom = r.nextInt()
    val callTill = r.nextInt()
    val marksIds = r.nextString()
    val marksNames = r.nextString()

    PoiPhone(
      title,
      phone,
      phoneMask,
      callFrom,
      callTill,
      marksIds,
      marksNames
    )
  }

  def findClientsRequest(
      filter: Option[
        FindClientsRequest.Filter.Builder => FindClientsRequest.Filter.Builder] =
        None,
      sorting: Option[
        FindClientsRequest.Sorting.Builder => FindClientsRequest.Sorting.Builder] =
        None,
      paging: Option[RequestPagination.Builder => RequestPagination.Builder] =
        None): FindClientsRequest = {
    val request = FindClientsRequest.newBuilder()

    filter.foreach { func =>
      request.setFilter {
        func(FindClientsRequest.Filter.newBuilder())
      }
    }

    sorting.foreach { func =>
      request.setSorting {
        func(FindClientsRequest.Sorting.newBuilder())
      }
    }

    paging.foreach { func =>
      request.setPagination {
        func(RequestPagination.newBuilder())
      }
    }

    request.build()
  }

  implicit val geoLocationResult: GetResult[GeoLocation] =
    GetResult[GeoLocation] { r =>
      val yaCountryId = r.nextLong()
      val yaRegionId = r.nextLong()
      val yaCityId = r.nextLong()
      val countryId = r.nextLong()
      val regionId = r.nextLong()
      val cityId = r.nextLong()

      GeoLocation(
        yaCountryId,
        yaRegionId,
        yaCityId,
        Some(countryId),
        Some(regionId),
        Some(cityId)
      )
    }

}
