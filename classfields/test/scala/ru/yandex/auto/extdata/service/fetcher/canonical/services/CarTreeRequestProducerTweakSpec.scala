package ru.yandex.auto.extdata.service.fetcher.canonical.services

import java.util
import java.util.Optional
import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.AutoSchemaVersions
import ru.yandex.auto.core.model.enums.State.Search
import ru.yandex.auto.core.region.{Region, RegionType}
import ru.yandex.auto.eds.service.RegionService
import ru.yandex.auto.extdata.service.canonical.services.impl.CarsTreeRequestProducer
import ru.yandex.auto.message.AutoUtilsSchema.NamePlateMessage
import ru.yandex.auto.message.CarAdSchema.CarAdMessage
import ru.yandex.auto.traffic.utils.ColorUtils
import ru.yandex.vertis.mockito.MockitoSupport

@RunWith(classOf[JUnitRunner])
class CarTreeRequestProducerTweakSpec extends WordSpec with Matchers with MockitoSupport {
  private val regionService: RegionService = mock[RegionService]

  when(regionService.getRegionById(?)).thenAnswer(new Answer[Optional[Region]] {
    override def answer(invocation: InvocationOnMock): Optional[Region] =
      Optional.of(new Region(invocation.getArgument[Int](0), "", RegionType.SUBJECT_FEDERATION, null, 0, 0))
  })

  private val Mark = "mark"
  private val ColorToColorTranslit = {
    val code = ColorUtils.getKnownColorCodes.head

    code -> ColorUtils.getColorTranslation(code).get
  }
  private val State = Search.NEW
  private val GeoId = 213
  private val NamePlate = "super"
  private val Year = 2020
  private val Body = "sedan"
  private val SuperGenId = 1L
  private val GearType = "forward_control"
  private val EngineType = "DIESEL"

  private val ads = for {
    mark <- Seq(Mark, "mark1", "mark2")
    model <- Seq(Mark, "mark1", "mark2")
    year <- Seq(Year, 2005, 2010)
    color <- Seq(ColorToColorTranslit._1, ColorToColorTranslit._2)
    superGen <- Seq(SuperGenId, 2L)
    body <- Seq(Body, "hatchback")
    price <- Seq(100000, 500000, 1000000)
  } yield CarAdMessage
    .newBuilder()
    .setVersion(AutoSchemaVersions.CAR_AD_VERSION)
    .setMark(mark)
    .setPriceRur(price)
    .setModel(model)
    .setColorFull(color)
    .setSearchState(State.name())
    .setRegionCode(GeoId.toString)
    .addAllDealerIds(util.Arrays.asList(1L))
    .addVendor(1) // Vendor.RUSSIAN
    .addSuperGenerations(superGen)
    .setNameplateFront(
      NamePlateMessage
        .newBuilder()
        .setVersion(1)
        .setSemanticUrl(NamePlate)
    )
    .setYear(year)
    .setBodyType(body)
    .setGearType(GearType)
    .setEngineType(EngineType)
    .build

  "CarsTreeRequestProducer" should {
    "works fast" in {
      for (_ <- 1 to 20) {
        val startNanos = System.nanoTime()
        val producer = new CarsTreeRequestProducer(regionService)
        ads.map { ad =>
          producer.produce(ad)
        }
        val elapsedNanos = System.nanoTime() - startNanos
        val elapsedSeconds = elapsedNanos / 1e9
        println(s"Finished by $elapsedSeconds")

        (elapsedSeconds < 10) shouldBe true
      }
    }
  }
}
