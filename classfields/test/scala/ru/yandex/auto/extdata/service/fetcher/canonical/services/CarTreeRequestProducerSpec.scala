package ru.yandex.auto.extdata.service.fetcher.canonical.services

import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalacheck.Gen
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.auto.core.AutoSchemaVersions
import ru.yandex.auto.core.model.enums.State.Search
import ru.yandex.auto.core.region.{Region, RegionTree, RegionType}
import ru.yandex.auto.eds.service.RegionService
import ru.yandex.auto.extdata.service.canonical.router.model.CanonicalUrlRequestType.DealersListing
import ru.yandex.auto.extdata.service.canonical.router.model.Params._
import ru.yandex.auto.extdata.service.canonical.router.model.{
  CanonicalUrlRequest,
  CanonicalUrlRequestType,
  CustomTag,
  RequestParam
}
import ru.yandex.auto.extdata.service.canonical.services.impl.CarsTreeRequestProducer
import ru.yandex.auto.extdata.service.util.url.CarAdExtractorsUtils
import ru.yandex.auto.message.AutoUtilsSchema.NamePlateMessage
import ru.yandex.auto.message.CarAdSchema.CarAdMessage
import ru.yandex.auto.traffic.utils.ColorUtils
import ru.yandex.auto.util.proto.RichCarAdMessage
import ru.yandex.auto.util.proto.RichCarAdMessage.SearchTags
import ru.yandex.vertis.mockito.MockitoSupport

import java.util
import java.util.Optional

@RunWith(classOf[JUnitRunner])
class CarTreeRequestProducerSpec extends WordSpec with Matchers with MockitoSupport with ScalaCheckPropertyChecks {
  private val regionService: RegionService = mock[RegionService]

  when(regionService.getRegionById(?)).thenAnswer(new Answer[Optional[Region]] {

    override def answer(invocation: InvocationOnMock): Optional[Region] =
      Optional.of(new Region(invocation.getArgument[Int](0), "", RegionType.SUBJECT_FEDERATION, null, 0, 0))
  })

  private val listingRequest = CanonicalUrlRequest(CanonicalUrlRequestType.ListingType)
  private val specificationRequest = CanonicalUrlRequest(CanonicalUrlRequestType.CardSpecifications)

  private val Mark = "mark"
  private val Price = 1000 * 1000
  private val Model = "model"

  private val ColorToColorTranslit = {
    val code = ColorUtils.getKnownColorCodes.head

    code -> ColorUtils.getColorTranslation(code).get
  }
  private val State = Search.NEW
  private val GeoId = 213
  private val NotMskGeoId = RegionTree.SPB
  private val NamePlate = "super"
  private val Year = 2020
  private val Body = "sedan"
  private val SuperGenId = 1L
  private val GearType = "forward_control"
  private val EngineType = "DIESEL"

  private val markParam = new MarkParam(Mark)
  private val modelParam = new ModelParam(Model)
  private val sectionAll = new SectionParam("all")
  private val sectionNew = SectionParam(Search.NEW)
  private val sectionUsed = SectionParam(Search.USED)
  private val sectionParam = SectionParam(State)
  private val colorParam = new ColorParam(ColorToColorTranslit._2)
  private val geoParam = new GeoIdParam(GeoId)
  private val namePlateParam = new NamePlateParam(NamePlate)
  private val yearParam = new YearParam(Year)
  private val bodyParam = new BodyTypeParam(Body)
  private val gearParam = new GearTypeParam("drive-forward_wheel")
  private val categoryParam = CategoryParam.Cars
  private val engineParam = new EngineTypeParam("dizel")
  private val priceParam = new PriceDoParam(1500000)
  private val superGenIdParam = new SuperGenParam(SuperGenId)

  private val DealersEmptyRequest = CanonicalUrlRequest(DealersListing)

  private val basicAdMessage: CarAdMessage =
    CarAdMessage
      .newBuilder()
      .setVersion(AutoSchemaVersions.CAR_AD_VERSION)
      .setMark(Mark)
      .setPriceRur(Price)
      .setModel(Model)
      .setColorFull(ColorToColorTranslit._1)
      .setSearchState(State.name())
      .setRegionCode(GeoId.toString)
      .addAllDealerIds(util.Arrays.asList(1L))
      .addVendor(1) // Vendor.RUSSIAN
      .addSuperGenerations(SuperGenId)
      .setNameplateFront(
        NamePlateMessage
          .newBuilder()
          .setVersion(1)
          .setSemanticUrl(NamePlate)
      )
      .setYear(Year)
      .setBodyType(Body)
      .setGearType(GearType)
      .setEngineType(EngineType)
      .build()

  private def pricesRequests =
    priceRequestWithMark(Mark)

  private def priceRequestWithMark(mark: String) =
    CarAdExtractorsUtils.priceUntilValues
      .filter(_ >= Price)
      .map { p =>
        listingRequest
          .withParam(CategoryParam.Cars)
          .withParam(new MarkParam(mark))
          .withParam(new PriceDoParam(p))
      }

  def generatedRequests(carAd: CarAdMessage): Set[CanonicalUrlRequest] = {
    val producer = new CarsTreeRequestProducer(regionService)
    producer.produce(carAd).map(produced => produced.request).toSet
  }

  implicit class RequestOps(r: CanonicalUrlRequest) {

    def withParams(params: Traversable[RequestParam[_]]): CanonicalUrlRequest =
      params.foldLeft(r)(_.withParam(_))

    def generateRequests(params: Traversable[Traversable[RequestParam[_]]]): Traversable[CanonicalUrlRequest] =
      params.map(r.withParams)
  }

  private def simpleSpec(
      msg: CarAdMessage,
      shouldContain: Traversable[CanonicalUrlRequest] = Seq(),
      shouldNotContain: Traversable[CanonicalUrlRequest] = Seq()
  ) = {
    val result = generatedRequests(msg)

    if (shouldContain.nonEmpty) {
      result should contain allElementsOf shouldContain
    }

    if (shouldNotContain.nonEmpty) {
      result should not contain allElementsOf(shouldNotContain)
    }
  }

  private def specForSearchStates(
      msg: CarAdMessage,
      params: Seq[Set[RequestParam[_]]],
      searchStates: Seq[SectionParam]
  ): Unit =
    for {
      searchState <- searchStates
      paramsWithState = params.map(_ + searchState)
    } yield simpleSpec(
      msg.toBuilder.setSearchState(searchState.value).build(),
      shouldContain = listingRequest.generateRequests(paramsWithState)
    )

  private def specForAllUsedStates(msg: CarAdMessage, params: Seq[Set[RequestParam[_]]]): Unit =
    specForSearchStates(msg, params, Seq(sectionUsed, sectionAll))

  private def specForAllKnownStates(msg: CarAdMessage, params: Seq[Set[RequestParam[_]]]): Unit =
    specForSearchStates(msg, params, Seq(sectionUsed, sectionAll, sectionNew))

  "CarsTreeRequestProducer" should {

    "correctly generate cars/mark/price" in {
      val result = generatedRequests(basicAdMessage)

      pricesRequests.foreach {
        result should contain(_)
      }
    }

    "correctly generate dealer listing" when {
      def dealerListingSpec(msg: CarAdMessage, withColors: Boolean): Unit = {
        val geo = new GeoIdParam(msg.getRegionCode.toInt)

        val isNew = msg.getSearchState.equalsIgnoreCase("new")
        val isUsed = msg.getSearchState.equalsIgnoreCase("used")

        val baseParams = Seq(
          Set(geo, categoryParam, sectionAll)
        )
        val paramsWithColors = Seq(
          Set(geo, categoryParam, markParam, sectionNew, colorParam)
        )
        val paramsWithNew = Seq(
          Set(geo, categoryParam, sectionNew),
          Set(geo, categoryParam, markParam, sectionNew)
        )
        val paramsWithUsed = Seq(
          Set(geo, categoryParam, sectionUsed)
        )

        val params = baseParams ++
          (if (withColors) paramsWithColors else Seq.empty) ++
          (if (isNew) paramsWithNew else Seq.empty) ++
          (if (isUsed) paramsWithUsed else Seq.empty)

        val requests = DealersEmptyRequest.generateRequests(params)

        val result = generatedRequests(msg)

        result should contain allElementsOf requests

        if (!withColors) {
          result should not contain allElementsOf(DealersEmptyRequest.generateRequests(paramsWithColors))
        }
        if (!isUsed) {
          result should not contain allElementsOf(DealersEmptyRequest.generateRequests(paramsWithUsed))
        }
        if (!isNew) {
          result should not contain allElementsOf(DealersEmptyRequest.generateRequests(paramsWithNew))
        }
      }

      "car from msk" in {
        dealerListingSpec(basicAdMessage, withColors = true)
      }

      "car not from msk" in {
        val msg = basicAdMessage.toBuilder.setRegionCode(NotMskGeoId.toString).build()
        dealerListingSpec(msg, withColors = false)
      }

      "used car from msk" in {
        val msg = basicAdMessage.toBuilder.setSearchState("used").build()
        dealerListingSpec(msg, withColors = false)
      }
    }

    "don't generate dealers listing" when {
      "empty dealer" in {
        val msg = basicAdMessage.toBuilder.clearDealerIds().build()
        generatedRequests(msg).forall(_.`type` != DealersListing) shouldBe true
      }
    }

    "correctly generate prices in region part" in {
      val expected = pricesRequests.map(_.withParam(geoParam).withParam(sectionNew))

      generatedRequests(basicAdMessage) should contain allElementsOf expected
    }

    "correctly generate /region/cars/mark/state(/color)" when {
      def spec(msg: CarAdMessage, withColors: Boolean): Unit = {
        val geo = new GeoIdParam(msg.getRegionCode.toInt)

        val withoutColors = Seq(
          Set(geo, categoryParam, markParam, sectionAll),
          Set(geo, categoryParam, markParam, sectionNew)
        )

        val withColor = withoutColors.map(_ ++ Set(colorParam))
        val params = if (withColors) {
          withColor ++ withoutColors
        } else {
          withoutColors
        }

        val expected = listingRequest.generateRequests(params)
        val result = generatedRequests(msg)

        result should contain allElementsOf expected

        if (!withColors) {
          result should not contain allElementsOf(listingRequest.generateRequests(withColor))
        }
      }

      "comes msk car" in {
        spec(basicAdMessage, withColors = true)
      }

      "comes non msk car" in {
        spec(basicAdMessage.toBuilder.setRegionCode(NotMskGeoId.toString).build(), withColors = true)
      }
    }

    "correctly generate with model" when {
      val modelRequests: Seq[Set[RequestParam[_]]] = Seq(
        Set(categoryParam, markParam, modelParam, sectionAll, yearParam),
        Set(categoryParam, markParam, modelParam, sectionAll, priceParam),
        Set(categoryParam, markParam, modelParam, sectionAll, namePlateParam),
        Set(categoryParam, markParam, modelParam, sectionAll, superGenIdParam),
        Set(categoryParam, markParam, modelParam, sectionAll, engineParam),
        Set(categoryParam, markParam, modelParam, sectionAll, gearParam),
        Set(categoryParam, markParam, modelParam, sectionAll, colorParam),
        Set(categoryParam, markParam, modelParam, sectionAll, bodyParam),
        Set(categoryParam, markParam, modelParam, sectionNew, priceParam),
        Set(categoryParam, markParam, modelParam, sectionAll, priceParam, superGenIdParam),
        Set(categoryParam, markParam, modelParam, sectionNew, priceParam, superGenIdParam),
        Set(geoParam, categoryParam, markParam, modelParam, sectionAll),
        Set(geoParam, categoryParam, markParam, modelParam, sectionNew),
        Set(geoParam, categoryParam, markParam, modelParam, sectionAll, colorParam),
        Set(geoParam, categoryParam, markParam, modelParam, sectionAll, yearParam),
        Set(geoParam, categoryParam, markParam, modelParam, sectionAll, priceParam),
        Set(geoParam, categoryParam, markParam, modelParam, sectionNew, priceParam),
        Set(geoParam, categoryParam, markParam, modelParam, sectionAll, priceParam, superGenIdParam),
        Set(geoParam, categoryParam, markParam, modelParam, sectionNew, priceParam, superGenIdParam),
        Set(geoParam, categoryParam, markParam, modelParam, sectionAll, bodyParam),
        Set(geoParam, categoryParam, markParam, modelParam, engineParam, sectionAll),
        Set(geoParam, categoryParam, markParam, modelParam, namePlateParam, sectionAll),
        Set(geoParam, categoryParam, markParam, modelParam, namePlateParam, sectionNew),
        Set(geoParam, categoryParam, markParam, modelParam, namePlateParam, sectionAll, bodyParam),
        Set(geoParam, categoryParam, markParam, modelParam, namePlateParam, sectionNew, bodyParam),
        Set(geoParam, categoryParam, markParam, modelParam, namePlateParam, yearParam, sectionAll),
        Set(geoParam, categoryParam, markParam, modelParam, namePlateParam, yearParam, sectionNew),
        Set(geoParam, categoryParam, markParam, modelParam, namePlateParam, engineParam, sectionAll)
      )

      val invalidModelRequests: Seq[Set[RequestParam[_]]] = Seq.empty

      def specExcept(withoutParam: RequestParam[_], clear: CarAdMessage.Builder => CarAdMessage.Builder): Unit = {
        val (contain, notContain) = modelRequests.partition(_.contains(withoutParam))

        val msg = clear(basicAdMessage.toBuilder).build()

        simpleSpec(
          msg,
          shouldContain = listingRequest.generateRequests(notContain),
          shouldNotContain = listingRequest.generateRequests(contain)
        )
      }

      "model defined" in {
        simpleSpec(
          basicAdMessage,
          shouldContain = listingRequest.generateRequests(modelRequests),
          shouldNotContain = listingRequest.generateRequests(invalidModelRequests)
        )
      }

      "model not set" in {
        simpleSpec(
          basicAdMessage.toBuilder.clearModel().build(),
          shouldNotContain = listingRequest.generateRequests(modelRequests)
        )
      }

      "year not set" in {
        specExcept(yearParam, _.clearYear())
      }

      "body not set" in {
        specExcept(bodyParam, _.clearBodyType())
      }

      "engine not set" in {
        specExcept(engineParam, _.clearEngineType())
      }

      "nameplate not set" in {
        specExcept(namePlateParam, _.clearNameplateFront())
      }
    }

    "correctly generate /region/cars/mark/state(/body)" in {
      val params = Seq(
        Set(geoParam, categoryParam, markParam, sectionNew, bodyParam),
        Set(geoParam, categoryParam, markParam, sectionAll, bodyParam)
      )

      simpleSpec(basicAdMessage, shouldContain = listingRequest.generateRequests(params))
    }

    "correctly generate /region/cars/mark/engine/all" in {
      val params = Seq(
        Set(geoParam, categoryParam, markParam, engineParam, sectionAll)
      )

      simpleSpec(basicAdMessage, shouldContain = listingRequest.generateRequests(params))
    }

    "correctly generate /region/cars/engine/all" in {
      val params = Seq(
        Set(geoParam, categoryParam, engineParam, sectionAll)
      )

      simpleSpec(basicAdMessage, shouldContain = listingRequest.generateRequests(params))
    }

    "correctly generate /region/cars/mark/model(-nameplate)/generation/(all|new|used)" in {
      val commonParams = Set[RequestParam[_]](geoParam, categoryParam, markParam, modelParam, superGenIdParam)

      val params = for {
        paramsWithMaybeNameplate <- Seq(commonParams, commonParams + namePlateParam)
      } yield paramsWithMaybeNameplate

      specForAllKnownStates(basicAdMessage, params)
    }

    "correctly generate /region/cars/mark/model(-nameplate)/year/generation/(all|new|used)" in {
      val commonParams =
        Set[RequestParam[_]](geoParam, categoryParam, markParam, modelParam, superGenIdParam, yearParam)

      val params = for {
        paramsWithMaybeNameplate <- Seq(commonParams, commonParams + namePlateParam)
      } yield paramsWithMaybeNameplate

      specForAllKnownStates(basicAdMessage, params)
    }

    "correctly generate /region/cars/mark/model(-nameplate)/generation/(all|new|used)/body" in {
      val commonParams: Set[RequestParam[_]] =
        Set(geoParam, categoryParam, markParam, modelParam, superGenIdParam, bodyParam)

      val params = for {
        paramsWithMaybeNameplate <- Seq(commonParams, commonParams + namePlateParam)
      } yield paramsWithMaybeNameplate

      specForAllKnownStates(basicAdMessage, params)
    }

    "correctly generate /region/cars/mark/model-nameplate/(all|new|used)/engine" in {
      val params = Seq(
        Set[RequestParam[_]](geoParam, categoryParam, markParam, modelParam, namePlateParam, engineParam)
      )

      specForAllKnownStates(basicAdMessage, params)
    }

    "correctly generate /region/cars/mark/model(-nameplate)/generation/(all|new|used)/engine" in {
      val commonParams =
        Set[RequestParam[_]](geoParam, categoryParam, markParam, modelParam, superGenIdParam, engineParam)

      val params = for {
        paramsWithMaybeNameplate <- Seq(commonParams, commonParams + namePlateParam)
      } yield paramsWithMaybeNameplate

      specForAllKnownStates(basicAdMessage, params)
    }

    "correctly generate /region/cars/mark/model-nameplate/(all|new|used)/color" in {
      val params = Seq(Set[RequestParam[_]](geoParam, categoryParam, markParam, modelParam, namePlateParam, colorParam))

      specForAllKnownStates(basicAdMessage, params)
    }

    "correctly generate /region/cars/mark/model(-nameplate)/generation/(all|new|used)/color" in {
      val commonParams =
        Set[RequestParam[_]](geoParam, categoryParam, markParam, modelParam, superGenIdParam, colorParam)

      val params = for {
        paramsWithMaybeNameplate <- Seq(commonParams, commonParams + namePlateParam)
      } yield paramsWithMaybeNameplate

      specForAllKnownStates(basicAdMessage, params)
    }

    "correctly generate /region/cars/(all|new|used)/gear-type" in {
      val params = Seq(Set[RequestParam[_]](geoParam, categoryParam, gearParam))

      specForAllKnownStates(basicAdMessage, params)
    }

    "correctly generate /region/cars/mark/model-nameplate/(all|new|used)/gear-type" in {
      val params =
        Seq(Set[RequestParam[_]](geoParam, categoryParam, markParam, modelParam, namePlateParam, gearParam))

      specForAllKnownStates(basicAdMessage, params)
    }

    "correctly generate /region/cars/mark/model(-nameplate)/generation/(all|new|used)/gear-type" in {
      val commonParams =
        Set[RequestParam[_]](geoParam, categoryParam, markParam, modelParam, superGenIdParam, gearParam)

      val params = for {
        paramsWithMaybeNameplate <- Seq(commonParams, commonParams + namePlateParam)
      } yield paramsWithMaybeNameplate

      specForAllKnownStates(basicAdMessage, params)
    }

    "correctly generate /region/cars/mark/model(-nameplate)(/generation)/(all|new|used)/price-limit" in {
      val commonParams = Set[RequestParam[_]](geoParam, categoryParam, markParam, modelParam, priceParam)

      val params = for {
        paramsWithMaybeNameplateAndGeneration <- Seq(
          commonParams,
          commonParams + superGenIdParam,
          commonParams + namePlateParam,
          commonParams ++ Set(superGenIdParam, namePlateParam)
        )
      } yield paramsWithMaybeNameplateAndGeneration

      specForAllKnownStates(basicAdMessage, params)
    }

    "correctly generate credit urls".which(afterWord("look like") {
      val creditAdMessage =
        basicAdMessage.toBuilder.addSearchTags(SearchTags.AllowedForCreditSearchTag.toString).build()
      val onCreditParam = CreditParam.OnCredit

      val commonParams = Set[RequestParam[_]](
        geoParam,
        categoryParam,
        onCreditParam
      )

      "/region/cars/(all|used)/on-credit" in {
        specForAllUsedStates(creditAdMessage, Seq(commonParams))
      }

      "/region/cars/mark(/model(-nameplate))/(all|used)/on-credit" in {
        val params = for {
          paramsCombination <- Seq(
            commonParams + markParam,
            commonParams + markParam + modelParam,
            commonParams + markParam + modelParam + namePlateParam
          )
        } yield paramsCombination

        specForAllUsedStates(creditAdMessage, params)
      }

      "/region/cars/(all|used)/(body|engine|gear|color)/on-credit" in {
        val params = for {
          paramsCombination <- Seq(
            commonParams + bodyParam,
            commonParams + engineParam,
            commonParams + gearParam,
            commonParams + colorParam
          )
        } yield paramsCombination

        specForAllUsedStates(creditAdMessage, params)
      }
    })

    def carAdMessageGen(reqParamGen: Gen[RequestParam[_]]): Gen[(Set[RequestParam[_]], CarAdMessage)] =
      for {
        reqParam <- reqParamGen
        reqParams = Set[RequestParam[_]](reqParam, geoParam, categoryParam)
        carAdMessage = reqParams
          .foldLeft(basicAdMessage.toBuilder) {
            case (builder, colorParam: ColorParam) =>
              ColorUtils.getColorCode(colorParam.color).map(builder.setColorFull).getOrElse(builder)
            case (builder, priceDoParam: PriceDoParam) =>
              builder.setPriceRur(priceDoParam.priceDo.toInt)
            case (builder, gearTypeParam: GearTypeParam) =>
              CarAdExtractorsUtils.GearTypeMap
                .collectFirst { case (key, gear) if gear == gearTypeParam.gear => builder.setGearType(key) }
                .getOrElse(builder)
            case (builder, seoTagNameParam: SeoTagNameParam) => builder.addSearchTags(seoTagNameParam.value)
            case (builder, _)                                => builder
          }
          .build()
      } yield (reqParams, carAdMessage)

    "correctly generate urls each of".which(afterWord("contains filter without mark and looks like") {

      val colorParamGen: Gen[ColorParam] = Gen
        .oneOf(ColorUtils.getKnownColorCodes.toList)
        .map(code => ColorUtils.getColorTranslation(code).get)
        .map(color => new ColorParam(color))

      "/region/cars/(all|new|used)/color" in {
        forAll(carAdMessageGen(colorParamGen)) {
          case (reqParams, carAdMessage) => specForAllKnownStates(carAdMessage, Seq(reqParams))
        }
      }

      val gearTypeParamGen: Gen[GearTypeParam] =
        Gen.oneOf(CarAdExtractorsUtils.GearTypeMap.values.toList).map(new GearTypeParam(_))

      "/region/cars/(all|new|used)/gear-type" in {
        forAll(carAdMessageGen(gearTypeParamGen)) {
          case (reqParams, carAdMessage) => specForAllKnownStates(carAdMessage, Seq(reqParams))
        }
      }

      val priceDoParamGen: Gen[PriceDoParam] =
        Gen.oneOf(CarAdExtractorsUtils.priceUntilValues).map(new PriceDoParam(_))

      "/region/cars/(all|new|used)/do-<price>" in {
        forAll(carAdMessageGen(priceDoParamGen)) {
          case (reqParams, carAdMessage) => specForAllKnownStates(carAdMessage, Seq(reqParams))
        }
      }
    })

    "correctly generate urls each of".which(afterWord("contains SEO tag and looks like") {

      val seoTagNameParamGen: Gen[SeoTagNameParam] =
        Gen
          .oneOf(RichCarAdMessage.SearchTags.values.toList)
          .suchThat(searchTag => SeoTagNameParam(searchTag.toString).nonEmpty)
          .map(searchTag => SeoTagNameParam(searchTag.toString).get)

      "/region/cars/(all|new|used)/tag/<seo-tag>" in {
        forAll(carAdMessageGen(seoTagNameParamGen)) {
          case (reqParams, carAdMessage) => specForAllKnownStates(carAdMessage, Seq(reqParams))
        }
      }
    })

    "correctly generate urls each of".which(afterWord("contains custom tag and looks like") {

      val customTagNameParamGen: Gen[CustomTagNameParam] =
        Gen
          .oneOf(CustomTag.values.toList)
          .map(tag => new CustomTagNameParam(tag))

      "/region/cars/(all|new|used)/tag/<custom-tag>" in {
        forAll(carAdMessageGen(customTagNameParamGen)) {
          case (reqParams, carAdMessage) => specForAllKnownStates(carAdMessage, Seq(reqParams))
        }
      }
    })
  }
}
