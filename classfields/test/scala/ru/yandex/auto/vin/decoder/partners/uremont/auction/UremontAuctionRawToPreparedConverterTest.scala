package ru.yandex.auto.vin.decoder.partners.uremont.auction

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

class UremontAuctionRawToPreparedConverterTest extends AnyFunSuite {
  implicit val t: Traced = Traced.empty
  val converter = new UremontAuctionRawToPreparedConverter

  test("success response") {
    val vin = VinCode("SJNFDAJ11U1084830")
    val raw = ResourceUtils.getStringFromResources(s"/uremont/auction/success.json")
    val model = UremontAuctionRawModel.apply(vin, raw, 200)
    val converted = converter.convert(model).await

    assert(model.data.migtorg.nonEmpty)
    assert(model.data.uremont.isEmpty)
    assert(model.data.insurance.isEmpty)

    assert(converted.getVin == vin.toString)
    assert(converted.getEventType == EventType.UREMONT_AUCTION)
    assert(converted.getGroupId == "")
    assert(converted.getStatus == VinInfoHistory.Status.OK)

    assert(converted.hasUremontAuction)
    val auction = converted.getUremontAuction

    assert(auction.getLotCount == 1)
    assert(auction.getLotCount == 1)
    assert(auction.getLot(0).getAuctionName == "Migtorg")
    assert(auction.getLot(0).getAuctionRegion == "Россия")
    assert(auction.getLot(0).getAuctionDate == 1529935200000L)
    assert(auction.getLot(0).getPhotosCount == 11)
    assert(auction.getLot(0).getPhotos(0).getExternalPhotoUrl == "https:/www.migtorg.com/media/photo/2553181.jpg")
    assert(!auction.getLot(0).getPhotos(0).hasMdsPhotoInfo)
  }

}
