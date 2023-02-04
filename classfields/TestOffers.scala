package ru.yandex.vertis.subscriptions.plugin.auto
import ru.auto.api.ApiOfferModel.{Category, Offer, Section, State}
import ru.auto.api.CommonModel.Photo
import ru.auto.api.vin.VinResolutionEnums
import ru.auto.api.{ApiOfferModel, CarsModel, CatalogModel, CommonModel, MotoModel}

import scala.jdk.CollectionConverters._

/**
  * @author kusaeva
  */
object TestOffers {

  private val imageUrls = Photo
    .newBuilder()
    .setName("xxx")
    .putAllSizes(
      Map[String, String](
        "1200x900" -> "//avatars.mds.yandex.net/get-autoru-vos/2077743/b3fe326110b248d3de6e45a24c522bd5/1200x900",
        "1200x900n" -> "//avatars.mds.yandex.net/get-autoru-vos/2077743/b3fe326110b248d3de6e45a24c522bd5/1200x900n",
        "120x90" -> "//avatars.mds.yandex.net/get-autoru-vos/2077743/b3fe326110b248d3de6e45a24c522bd5/120x90",
        "320x240" -> "//avatars.mds.yandex.net/get-autoru-vos/2077743/b3fe326110b248d3de6e45a24c522bd5/320x240",
        "456x342" -> "//avatars.mds.yandex.net/get-autoru-vos/2077743/b3fe326110b248d3de6e45a24c522bd5/456x342",
        "832x624" -> "//avatars.mds.yandex.net/get-autoru-vos/2077743/b3fe326110b248d3de6e45a24c522bd5/832x624",
        "92x69" -> "//avatars.mds.yandex.net/get-autoru-vos/2077743/b3fe326110b248d3de6e45a24c522bd5/92x69",
        "full" -> "//avatars.mds.yandex.net/get-autoru-vos/2077743/b3fe326110b248d3de6e45a24c522bd5/full",
        "small" -> "//avatars.mds.yandex.net/get-autoru-vos/2077743/b3fe326110b248d3de6e45a24c522bd5/small",
        "thumb_m" -> "//avatars.mds.yandex.net/get-autoru-vos/2077743/b3fe326110b248d3de6e45a24c522bd5/thumb_m"
      ).asJava
    )

  val testMotoOffer: Offer = Offer
    .newBuilder()
    .setId("id")
    .setSection(Section.USED)
    .setMotoInfo(
      MotoModel.MotoInfo
        .newBuilder()
        .setMark("FORD")
        .setMarkInfo(
          CatalogModel.Mark
            .newBuilder()
            .setCode("FORD")
            .setName("Ford")
        )
        .setModel("Focus")
        .setModelInfo(
          CatalogModel.Model
            .newBuilder()
            .setCode("FOCUS")
            .setName("Focus")
        )
    )
    .setPriceInfo(CommonModel.PriceInfo.newBuilder().setDprice(10000).setRurDprice(10000))
    .addPriceHistory(CommonModel.PriceInfo.newBuilder().setDprice(10000).setRurDprice(10000))
    .addPriceHistory(CommonModel.PriceInfo.newBuilder().setDprice(90000).setRurDprice(90000))
    .setCategory(Category.MOTO)
    .setState(
      State
        .newBuilder()
        .setCondition(ApiOfferModel.Condition.CONDITION_OK)
        .addImageUrls(imageUrls)
    )
    .build()

  val testCarOffer: Offer = Offer
    .newBuilder()
    .setId("id")
    .setSection(Section.USED)
    .setDocuments(
      ApiOfferModel.Documents
        .newBuilder()
        .setVinResolution(VinResolutionEnums.Status.OK)
        .build()
    )
    .setCarInfo(
      CarsModel.CarInfo
        .newBuilder()
        .setMark("FORD")
        .setMarkInfo(
          CatalogModel.Mark
            .newBuilder()
            .setCode("FORD")
            .setName("Ford")
        )
        .setModel("Focus")
        .setModelInfo(
          CatalogModel.Model
            .newBuilder()
            .setCode("FOCUS")
            .setName("Focus")
        )
    )
    .setPriceInfo(CommonModel.PriceInfo.newBuilder().setDprice(10000).setRurDprice(10000))
    .addPriceHistory(CommonModel.PriceInfo.newBuilder().setDprice(10000).setRurDprice(10000))
    .addPriceHistory(CommonModel.PriceInfo.newBuilder().setDprice(90000).setRurDprice(90000))
    .setCategory(Category.CARS)
    .setState(
      State
        .newBuilder()
        .setCondition(ApiOfferModel.Condition.CONDITION_OK)
        .addImageUrls(imageUrls)
    )
    .build()
}
