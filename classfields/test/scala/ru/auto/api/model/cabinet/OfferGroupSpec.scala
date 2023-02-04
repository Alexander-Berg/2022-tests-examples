package ru.auto.api.model.cabinet

import ru.auto.api.ApiOfferModel.DiscountStatistic
import ru.auto.api.BaseSpec
import ru.auto.api.PriceReportModel.PriceReportGroupsResponse
import ru.auto.api.PriceReportModel.PriceReportGroupsResponse.{Preset, OfferGroup => ProtoOfferGroup, OfferGroupInfo => ProtoOfferGroupInfo}

class OfferGroupSpec extends BaseSpec {

  val group = OfferGroup(
    id = OfferGroup.OfferGroupId(
      markId = "mark",
      modelId = "model",
      superGenId = 1,
      complectationId = 1,
      configurationId = 1,
      techParamId = 1,
      year = 1
    ),
    info = OfferGroup.OfferGroupInfo(
      mark = "mark",
      model = "model",
      superGen = "superGen",
      complectation = "complectation",
      configuration = "configuration",
      techParam = "techParam",
      bodyType = "bodyType",
      horsePower = 100,
      engineType = "engineType",
      transmission = "transmission",
      drive = "drive",
      year = 1
    ),
    counter = OfferGroup.OfferGroupCounter(total = 10, inStock = 5, onOrder = 5),
    priceRange = OfferGroup.OfferGroupPriceRange(minPrice = 10, minDiscountPrice = 5, maxPrice = 20),
    discountStatistic = DiscountStatistic.newBuilder().build(),
    isDealer = true
  )

  val protoGroupInfoTemplate = ProtoOfferGroupInfo
    .newBuilder()
    .setCounter(
      PriceReportGroupsResponse.Counter
        .newBuilder()
        .setInStock(group.counter.inStock)
        .setOnOrder(group.counter.onOrder)
        .setTotal(group.counter.total)
        .build()
    )
    .setPriceRange(
      PriceReportGroupsResponse.PriceRange
        .newBuilder()
        .setMaxPrice(group.priceRange.maxPrice)
        .setMinPrice(group.priceRange.minPrice)
        .setMinPriceDiscount(group.priceRange.minDiscountPrice)
        .build()
    )
    .build()

  val protoOfferGroupBuilderTemplate = ProtoOfferGroup
    .newBuilder()
    .setModel(group.info.model)
    .setModelId(group.id.modelId)
    .setMark(group.info.mark)
    .setMarkId(group.id.markId)
    .setSuperGen(group.info.superGen)
    .setSuperGenId(group.id.superGenId)
    .setComplectation(group.info.complectation)
    .setComplectationId(group.id.complectationId)
    .setConfiguration(group.info.configuration)
    .setConfigurationId(group.id.configurationId)
    .setTechParam(group.info.techParam)
    .setTechParamId(group.id.techParamId)
    .setBodyType(group.info.bodyType)
    .setEngineType(group.info.engineType)
    .setHorsePower(group.info.horsePower)
    .setTransmission(group.info.transmission)
    .setDrive(group.info.drive)
    .setYear(group.id.year)
    .setDealer(protoGroupInfoTemplate)

  "OfferGroup.toProto" should {
    "return group only for dealer" in {
      val expected = protoOfferGroupBuilderTemplate
        .setPreset(Preset.UNIQUE_OFFERS)
        .build()

      OfferGroup.toProto(dealerGroup = Some(group.copy(isDealer = true)), competitorGroup = None) shouldBe Some(
        expected
      )
    }

    "return group only for competitor" in {
      val expected = protoOfferGroupBuilderTemplate
        .clearDealer()
        .setCompetitor(protoGroupInfoTemplate)
        .setPreset(Preset.ABSENT_OFFERS)
        .setDiscountStatistic(DiscountStatistic.newBuilder().build())
        .build()

      OfferGroup.toProto(dealerGroup = None, competitorGroup = Some(group.copy(isDealer = false))) shouldBe Some(
        expected
      )
    }

    "return group for dealer and competitor with price too high" in {
      val dealerGroup =
        group.copy(priceRange = OfferGroup.OfferGroupPriceRange(minPrice = 15, minDiscountPrice = 120, maxPrice = 150))
      val competitorGroup =
        group.copy(priceRange = OfferGroup.OfferGroupPriceRange(minPrice = 15, minDiscountPrice = 100, maxPrice = 150))

      val expected = protoOfferGroupBuilderTemplate
        .setDealer(
          protoGroupInfoTemplate.toBuilder.setPriceRange(
            PriceReportGroupsResponse.PriceRange
              .newBuilder()
              .setMaxPrice(dealerGroup.priceRange.maxPrice)
              .setMinPrice(dealerGroup.priceRange.minPrice)
              .setMinPriceDiscount(dealerGroup.priceRange.minDiscountPrice)
              .build()
          )
        )
        .setCompetitor(
          protoGroupInfoTemplate.toBuilder.setPriceRange(
            PriceReportGroupsResponse.PriceRange
              .newBuilder()
              .setMaxPrice(competitorGroup.priceRange.maxPrice)
              .setMinPrice(competitorGroup.priceRange.minPrice)
              .setMinPriceDiscount(competitorGroup.priceRange.minDiscountPrice)
              .build()
          )
        )
        .setPreset(Preset.PRICE_TOO_HIGH)
        .setDiscountStatistic(DiscountStatistic.newBuilder().build())
        .build()

      OfferGroup.toProto(
        dealerGroup = Some(dealerGroup.copy(isDealer = true)),
        competitorGroup = Some(competitorGroup.copy(isDealer = false))
      ) shouldBe Some(
        expected
      )
    }

    "return group for dealer and competitor with price too low" in {
      val dealerGroup =
        group.copy(priceRange = OfferGroup.OfferGroupPriceRange(minPrice = 5, minDiscountPrice = 8, maxPrice = 100))
      val competitorGroup =
        group.copy(priceRange = OfferGroup.OfferGroupPriceRange(minPrice = 5, minDiscountPrice = 10, maxPrice = 100))

      val expected = protoOfferGroupBuilderTemplate
        .setDealer(
          protoGroupInfoTemplate.toBuilder.setPriceRange(
            PriceReportGroupsResponse.PriceRange
              .newBuilder()
              .setMaxPrice(dealerGroup.priceRange.maxPrice)
              .setMinPrice(dealerGroup.priceRange.minPrice)
              .setMinPriceDiscount(dealerGroup.priceRange.minDiscountPrice)
              .build()
          )
        )
        .setCompetitor(
          protoGroupInfoTemplate.toBuilder.setPriceRange(
            PriceReportGroupsResponse.PriceRange
              .newBuilder()
              .setMaxPrice(competitorGroup.priceRange.maxPrice)
              .setMinPrice(competitorGroup.priceRange.minPrice)
              .setMinPriceDiscount(competitorGroup.priceRange.minDiscountPrice)
              .build()
          )
        )
        .setPreset(Preset.PRICE_TOO_LOW)
        .setDiscountStatistic(DiscountStatistic.newBuilder().build())
        .build()

      OfferGroup.toProto(
        dealerGroup = Some(dealerGroup.copy(isDealer = true)),
        competitorGroup = Some(competitorGroup.copy(isDealer = false))
      ) shouldBe Some(
        expected
      )
    }

    "return group for dealer and competitor with NONE preset" in {
      val dealerGroup =
        group.copy(priceRange = OfferGroup.OfferGroupPriceRange(minPrice = 15, minDiscountPrice = 10, maxPrice = 100))
      val competitorGroup =
        group.copy(priceRange = OfferGroup.OfferGroupPriceRange(minPrice = 15, minDiscountPrice = 10, maxPrice = 100))

      val expected = protoOfferGroupBuilderTemplate
        .setDealer(
          protoGroupInfoTemplate.toBuilder.setPriceRange(
            PriceReportGroupsResponse.PriceRange
              .newBuilder()
              .setMaxPrice(dealerGroup.priceRange.maxPrice)
              .setMinPrice(dealerGroup.priceRange.minPrice)
              .setMinPriceDiscount(dealerGroup.priceRange.minDiscountPrice)
              .build()
          )
        )
        .setCompetitor(
          protoGroupInfoTemplate.toBuilder.setPriceRange(
            PriceReportGroupsResponse.PriceRange
              .newBuilder()
              .setMaxPrice(competitorGroup.priceRange.maxPrice)
              .setMinPrice(competitorGroup.priceRange.minPrice)
              .setMinPriceDiscount(competitorGroup.priceRange.minDiscountPrice)
              .build()
          )
        )
        .setPreset(Preset.NONE)
        .setDiscountStatistic(DiscountStatistic.newBuilder().build())
        .build()

      OfferGroup.toProto(
        dealerGroup = Some(dealerGroup.copy(isDealer = true)),
        competitorGroup = Some(competitorGroup.copy(isDealer = false))
      ) shouldBe Some(
        expected
      )
    }
  }
}
