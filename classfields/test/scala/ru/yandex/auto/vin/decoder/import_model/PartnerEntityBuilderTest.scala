package ru.yandex.auto.vin.decoder.import_model

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.import_model.ImportModel.AutoSaleEntityImpl
import ru.yandex.auto.vin.decoder.manager.vin.VinData
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Sale, VinInfoHistory}
import ru.yandex.auto.vin.decoder.report.processors.entities.HistoryEntity
import auto.carfax.common.utils.tracing.Traced
import ru.yandex.auto.vin.decoder.manager.offer.VosOffers
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared

class PartnerEntityBuilderTest extends AnyFunSuite {

  private val vin = VinCode("SALWA2FF8FA505566")
  implicit private val trace: Traced = Traced.empty

  test("buildEntities builds entities correctly") {

    checkEntity(EventType.EURO_ALLIANCE_SALES, _.addSales(Sale.newBuilder)) {
      case Some(
            AutoSaleEntityImpl(
              _,
              _,
              "Евроальянс Официальный дилер Volkswagen",
              Some("https://vw-novgorod.ru/"),
              List(22337),
              _
            )
          ) =>
        true
    }

    checkEntity(EventType.CM_EXPERT_SALES, _.setDealerExternalId("cme000068").addSales(Sale.newBuilder)) {
      case Some(AutoSaleEntityImpl(_, _, "ААА МОТОРС", Some("https://aaa-motors.ru/"), List(40904), _)) => true
    }

    checkEntity(EventType.CM_EXPERT_SALES, _.setDealerExternalId("cme000880").addSales(Sale.newBuilder)) {
      case Some(AutoSaleEntityImpl(_, _, "", None, List(40089), _)) => true
    }

    checkEntity(
      EventType.CM_EXPERT_SALES,
      _.setDealerExternalId("cme000890").addSales(Sale.newBuilder.setSellerId("2391"))
    ) { case Some(AutoSaleEntityImpl(_, _, "", None, List(16167), _)) =>
      true
    }
  }

  private def checkEntity(
      et: EventType,
      vihB: VinInfoHistory.Builder => VinInfoHistory.Builder
    )(isValid: PartialFunction[Option[HistoryEntity], Boolean]) = {
    val entities = PartnerEntityBuilder.buildEntities(getVinData(et, vihB), Map.empty)
    assert {
      isValid.isDefinedAt(entities.headOption) && isValid(entities.headOption)
    }
  }

  private def getVinData(et: EventType, vihB: VinInfoHistory.Builder => VinInfoHistory.Builder): VinData = {
    VinData(
      vin,
      Map.empty,
      Map(et -> List(Prepared(0, 0, 0, vihB(VinInfoHistory.newBuilder.setEventType(et)).build, ""))),
      VosOffers.Empty
    )
  }
}
