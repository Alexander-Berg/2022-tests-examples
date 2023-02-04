package ru.yandex.vertis.general.wizard.api.services.impl

import common.geobase.model.RegionIds
import common.geobase.model.RegionIds.RegionId
import general.search.offer_count_model.{
  OfferAttributeCountByCategory,
  OfferAttributeCountSnapshot,
  OfferCountByAttribute,
  OfferCountByAttributeValue
}
import ru.yandex.vertis.general.wizard.api.services.TestCatalog
import ru.yandex.vertis.general.wizard.core.service.BonsaiService
import ru.yandex.vertis.general.wizard.core.service.impl.{LiveBonsaiService, LiveOfferStatsService}
import ru.yandex.vertis.general.wizard.model.{Attribute, CategoryListing, RegionListing}
import ru.yandex.vertis.general.wizard.model.AttributeValue.StringAttribute
import zio.test.Assertion._
import zio.test._

object LiveOfferStatsServiceTest extends DefaultRunnableSpec {

  private val msk = RegionIds.Moscow
  private val spb = RegionIds.SaintPetersburg

  private val dogs = TestCatalog.dogs.id
  private val cats = TestCatalog.cats.id

  private val color = "color"
  private val sex = "sex"

  private val sexFemale = "f"
  private val sexMale = "m"

  private val whiteColor = "white"
  private val blackColor = "black"

  private val white = Attribute(color, StringAttribute(whiteColor))
  private val black = Attribute(color, StringAttribute(blackColor))
  private val male = Attribute(sex, StringAttribute(sexMale))
  private val female = Attribute(sex, StringAttribute(sexFemale))

  private val offerAttributeCountSnapshot = OfferAttributeCountSnapshot(
    Map(
      msk.id -> OfferAttributeCountByCategory(
        Map(
          dogs -> OfferCountByAttribute(
            Map(
              color -> OfferCountByAttributeValue(
                Map(
                  whiteColor -> 1,
                  blackColor -> 2
                )
              ),
              sex -> OfferCountByAttributeValue(
                Map(
                  sexFemale -> 3,
                  sexMale -> 4
                )
              )
            )
          ),
          cats -> OfferCountByAttribute(
            Map(
              color -> OfferCountByAttributeValue(
                Map(
                  whiteColor -> 5,
                  blackColor -> 6
                )
              ),
              sex -> OfferCountByAttributeValue(
                Map(
                  sexFemale -> 7,
                  sexMale -> 8
                )
              )
            )
          )
        )
      ),
      spb.id -> OfferAttributeCountByCategory(
        Map(
          dogs -> OfferCountByAttribute(
            Map(
              color -> OfferCountByAttributeValue(
                Map(
                  whiteColor -> 9,
                  blackColor -> 10
                )
              ),
              sex -> OfferCountByAttributeValue(
                Map(
                  sexFemale -> 11,
                  sexMale -> 12
                )
              )
            )
          ),
          cats -> OfferCountByAttribute(
            Map(
              color -> OfferCountByAttributeValue(
                Map(
                  whiteColor -> 13,
                  blackColor -> 14
                )
              ),
              sex -> OfferCountByAttributeValue(
                Map(
                  sexFemale -> 15,
                  sexMale -> 16
                )
              )
            )
          )
        )
      )
    )
  )

  private val service =
    new LiveOfferStatsService(offerAttributeCountSnapshot, LiveBonsaiService.create(TestCatalog.bonsaiSnapshot))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("LiveOfferStatService")(
      testM("Should return 0 for non present values")(
        for {
          unknownRegionResult <- service.getCount(RegionListing(RegionId(0)))
          unknownCategoryResult <- service.getCount(CategoryListing(msk, "unknown", Seq.empty))
          unknownAttributeIdResult <- service.getCount(
            CategoryListing(msk, dogs, Seq(Attribute("unknown", StringAttribute(sexFemale))))
          )
          unknownAttributeValueResult <- service.getCount(
            CategoryListing(msk, dogs, Seq(Attribute(color, StringAttribute("unknown"))))
          )
        } yield assert(
          (
            unknownRegionResult,
            unknownCategoryResult,
            unknownAttributeIdResult,
            unknownAttributeValueResult
          )
        )(equalTo((0, 0, 0, 0)))
      ),
      testM("Should return correct value for region requests")(
        for {
          mskResult <- service.getCount(RegionListing(msk))
          spbResult <- service.getCount(RegionListing(spb))
        } yield assert((mskResult, spbResult))(equalTo(((1 to 8).sum, (9 to 16).sum)))
      ),
      testM("Should return correct value for category requests")(
        for {
          mskDogs <- service.getCount(CategoryListing(msk, dogs, Seq.empty))
          mskCats <- service.getCount(CategoryListing(msk, cats, Seq.empty))
          spbDogs <- service.getCount(CategoryListing(spb, dogs, Seq.empty))
          spbCats <- service.getCount(CategoryListing(spb, cats, Seq.empty))
        } yield assert(
          (
            mskDogs,
            mskCats,
            spbDogs,
            spbCats
          )
        )(
          equalTo(
            (
              (1 to 4).sum,
              (5 to 8).sum,
              (9 to 12).sum,
              (13 to 16).sum
            )
          )
        )
      ),
      testM("should return correct value for parent categories")(
        for {
          mskAnimals <- service.getCount(CategoryListing(msk, TestCatalog.animals.id, Seq.empty))
        } yield assert(mskAnimals)(equalTo((1 to 8).sum))
      ),
      testM("Should return correct value for attribute requests")(
        for {
          mskWhiteCats <- service.getCount(CategoryListing(msk, cats, Seq(white)))
          mskBlackCats <- service.getCount(CategoryListing(msk, cats, Seq(black)))
          mskFemaleCats <- service.getCount(CategoryListing(msk, cats, Seq(female)))
          mskMaleCats <- service.getCount(CategoryListing(msk, cats, Seq(male)))
        } yield assert(
          (
            mskWhiteCats,
            mskBlackCats,
            mskFemaleCats,
            mskMaleCats
          )
        )(
          equalTo(
            (
              5,
              6,
              7,
              8
            )
          )
        )
      )
    )
}
