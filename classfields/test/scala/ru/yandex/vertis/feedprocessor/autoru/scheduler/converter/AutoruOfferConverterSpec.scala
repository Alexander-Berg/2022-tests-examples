package ru.yandex.vertis.feedprocessor.autoru.scheduler.converter

import java.net.URLEncoder
import akka.actor.ActorSystem
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.Multiposting.Classified
import ru.auto.api.ApiOfferModel.Multiposting.Classified.ClassifiedName
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CommonModel
import ru.auto.api.CommonModel.{DiscountPrice => DiscountPriceApi}
import ru.auto.panoramas.PanoramasModel
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.model
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.{Generators, OfferNotice, ServiceInfo, TaskContext}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer.DiscountPrice
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.{
  CarExternalOffer,
  Currency,
  Delivery,
  ExteriorPanoramaState,
  TruckExternalOffer
}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.mapper.{GeocodingMapper, Mapper}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.AutoruGenerators
import ru.yandex.vertis.feedprocessor.services.geocoder.GeocodeResult
import ru.yandex.vertis.feedprocessor.util.DummyOpsSupport
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

class AutoruOfferConverterSpec extends WordSpecBase with DummyOpsSupport with TestApplication with MockitoSupport {

  implicit lazy val actorSystem =
    ActorSystem(environment.serviceName, environment.config)

  import actorSystem.dispatcher

  implicit val meter = new Mapper.Meters(prometheusRegistry)

  val converter = new AutoruOfferConverter(
    environment.config.getConfig("feedprocessor.autoru")
  )

  val taskContextUsed = TaskContext(
    tasksGen(serviceInfoGen = generateServiceInfo(Section.USED)).next
  )

  val taskContextNew = model.TaskContext(
    tasksGen(serviceInfoGen = generateServiceInfo(Section.NEW)).next
  )

  val carsOfferGen = AutoruGenerators
    .carExternalOfferGen(Generators.newTasksGen)
    .map(_.copy(interiorCode = None, equipmentCodes = None))

  val trucksOfferGen = AutoruGenerators
    .truckExternalOfferGen(Generators.newTasksGen)

  private def generateServiceInfo(section: Section): Gen[ServiceInfo] =
    serviceInfoGen(
      categoryGen = Gen.const(15),
      sectionGen = Gen.const(section.getNumber)
    )

  private def urlEncode(string: String): String =
    URLEncoder.encode(string, "UTF-8")

  "cars add panorama" in {
    val offer: CarExternalOffer = {
      val origin = carsOfferGen.next
      val panorama: PanoramasModel.Panorama =
        PanoramasModel.Panorama.newBuilder().setId("111-xxx-yyy").build()

      origin.copy(
        exteriorPanoramaState = Some(ExteriorPanoramaState.Panorama("http://empty.ku", panorama))
      )
    }

    val res = converter.convert(offer)

    assert(res.offer.getState.hasExternalPanorama)
    assert(!res.offer.getState.getExternalPanorama.hasPublished)
    assert(res.offer.getState.getExternalPanorama.hasNext)
    assert(
      res.offer.getState.getExternalPanorama.getNext.getId == "111-xxx-yyy"
    )
  }

  "cars remove panorama" in {
    val offer: CarExternalOffer = {
      val origin = carsOfferGen.next
      origin.copy(exteriorPanoramaState = Some(ExteriorPanoramaState.Removed))
    }

    val res = converter.convert(offer)

    assert(res.offer.getState.hasExternalPanorama)
    assert(res.offer.getState.getExternalPanorama.hasPublished)
    assert(!res.offer.getState.getExternalPanorama.getPublished.getPublished)
    assert(!res.offer.getState.getExternalPanorama.hasNext)
  }

  "cars didn't add panorama" in {
    val offer: CarExternalOffer = {
      val origin = carsOfferGen.next
      origin.copy(exteriorPanoramaState = None)
    }

    val res = converter.convert(offer)

    assert(!res.offer.getState.hasExternalPanorama)
  }

  "multiposting" when {
    "disabled" should {
      "not add multiposting" when {
        "empty classifieds" in {
          val feed: CarExternalOffer = {
            val origin = carsOfferGen.next
            origin.copy(classifieds = Some(Nil), multipostingEnabled = Some(false))
          }

          val result = converter.convert(feed)
          val vosOffer = result.offer

          assert(!vosOffer.hasMultiposting)
        }

        "empty classifieds: trucks" in {
          val feed: TruckExternalOffer = {
            val origin = trucksOfferGen.next
            origin.copy(classifieds = Some(Nil), multipostingEnabled = Some(false))
          }

          val result = converter.convert(feed)
          val vosOffer = result.offer

          assert(!vosOffer.hasMultiposting)
        }

        "without classifieds" in {
          val feed: CarExternalOffer = {
            val origin = carsOfferGen.next
            origin.copy(classifieds = None, multipostingEnabled = Some(false))
          }

          val result = converter.convert(feed)
          val vosOffer = result.offer

          assert(!vosOffer.hasMultiposting)
        }

        "without classifieds: trucks" in {
          val feed: TruckExternalOffer = {
            val origin = trucksOfferGen.next
            origin.copy(classifieds = None, multipostingEnabled = Some(false))
          }

          val result = converter.convert(feed)
          val vosOffer = result.offer

          assert(!vosOffer.hasMultiposting)
        }

        "with classifieds" in {
          val feed: CarExternalOffer = {
            val origin = carsOfferGen.next
            origin.copy(classifieds = Some(Seq("avito", "autoru", "drom")), multipostingEnabled = Some(false))
          }

          val result = converter.convert(feed)
          val vosOffer = result.offer

          assert(!vosOffer.hasMultiposting)
        }

        "with classifieds: trucks" in {
          val feed: TruckExternalOffer = {
            val origin = trucksOfferGen.next
            origin.copy(classifieds = Some(Seq("avito", "autoru", "drom")), multipostingEnabled = Some(false))
          }

          val result = converter.convert(feed)
          val vosOffer = result.offer

          assert(!vosOffer.hasMultiposting)
        }
      }
    }

    "enabled" should {
      "add multiposting" when {
        "empty classifieds => preserveClassifieds = false" in {
          val feedShow: CarExternalOffer = {
            val origin = carsOfferGen.next
            origin.copy(classifieds = Some(Nil), action = Some("show"), multipostingEnabled = Some(true))
          }
          val feedHide: CarExternalOffer = {
            val origin = carsOfferGen.next
            origin.copy(classifieds = Some(Nil), action = Some("hide"), multipostingEnabled = Some(true))
          }

          val resultShow = converter.convert(feedShow)
          val vosOfferShow = resultShow.offer
          val resultHide = converter.convert(feedHide)
          val vosOfferHide = resultHide.offer

          assert(vosOfferShow.hasMultiposting)
          assert(vosOfferShow.getMultiposting.getStatus == OfferStatus.ACTIVE)
          assert(vosOfferShow.getMultiposting.getClassifiedsList.asScala.isEmpty)
          assert(!vosOfferShow.getMultiposting.getPreserveClassifieds)

          assert(vosOfferHide.hasMultiposting)
          assert(vosOfferHide.getMultiposting.getStatus == OfferStatus.INACTIVE)
          assert(vosOfferHide.getMultiposting.getClassifiedsList.asScala.isEmpty)
          assert(!vosOfferHide.getMultiposting.getPreserveClassifieds)
        }

        "empty classifieds: trucks => preserveClassifieds = false" in {
          val feedShow: TruckExternalOffer = {
            val origin = trucksOfferGen.next
            origin.copy(classifieds = Some(Nil), action = Some("show"), multipostingEnabled = Some(true))
          }
          val feedHide: TruckExternalOffer = {
            val origin = trucksOfferGen.next
            origin.copy(classifieds = Some(Nil), action = Some("hide"), multipostingEnabled = Some(true))
          }

          val resultShow = converter.convert(feedShow)
          val vosOfferShow = resultShow.offer
          val resultHide = converter.convert(feedHide)
          val vosOfferHide = resultHide.offer

          assert(vosOfferShow.hasMultiposting)
          assert(vosOfferShow.getMultiposting.getStatus == OfferStatus.ACTIVE)
          assert(vosOfferShow.getMultiposting.getClassifiedsList.asScala.isEmpty)
          assert(!vosOfferShow.getMultiposting.getPreserveClassifieds)

          assert(vosOfferHide.hasMultiposting)
          assert(vosOfferHide.getMultiposting.getStatus == OfferStatus.INACTIVE)
          assert(vosOfferHide.getMultiposting.getClassifiedsList.asScala.isEmpty)
          assert(!vosOfferHide.getMultiposting.getPreserveClassifieds)
        }

        "without classifieds => preserveClassifieds = true" in {
          val feed: CarExternalOffer = {
            val origin = carsOfferGen.next
            origin.copy(classifieds = None, action = Some("show"), multipostingEnabled = Some(true))
          }

          val result = converter.convert(feed)
          val vosOffer = result.offer

          assert(vosOffer.hasMultiposting)
          assert(vosOffer.getMultiposting.getStatus == OfferStatus.ACTIVE)
          assert(vosOffer.getMultiposting.getClassifiedsList.asScala.isEmpty)
          assert(vosOffer.getMultiposting.getPreserveClassifieds)
        }

        "without classifieds: trucks => preserveClassifieds = false" in {
          val feed: TruckExternalOffer = {
            val origin = trucksOfferGen.next
            origin.copy(classifieds = None, action = Some("show"), multipostingEnabled = Some(true))
          }

          val result = converter.convert(feed)
          val vosOffer = result.offer

          assert(vosOffer.hasMultiposting)
          assert(vosOffer.getMultiposting.getStatus == OfferStatus.ACTIVE)
          assert(!vosOffer.getMultiposting.getPreserveClassifieds)

          val autoruClassified = vosOffer.getMultiposting.getClassifiedsList.asScala.find(
            _.getName == ClassifiedName.AUTORU
          )

          assert(autoruClassified.nonEmpty)
          assert(autoruClassified.map(_.getStatus).contains(OfferStatus.ACTIVE))
        }

        "with classifieds => preserveClassifieds = false" should {
          "action - show" in {
            val feed: CarExternalOffer = {
              carsOfferGen.next.copy(
                classifieds = Some(Seq("avito", "autoru", "drom")),
                avitoSaleServices = Seq("avito_first", "avito_second"),
                saleServices = Seq("autoru_first", "autoru_second"),
                dromSaleServices = Seq("drom_first", "drom_second"),
                avitoDescription = Some("avito description"),
                dromDescription = Some("drom description"),
                description = Some("autoru description"),
                action = Some("show"),
                multipostingEnabled = Some(true)
              )
            }

            val result = converter.convert(feed)
            val vosOffer = result.offer

            assert(vosOffer.hasMultiposting)
            assert(vosOffer.getMultiposting.getStatus.equals(OfferStatus.ACTIVE))
            assert(!vosOffer.getMultiposting.getPreserveClassifieds)

            assert(vosOffer.getMultiposting.getClassifiedsCount == 3)
            val classifieds4 = vosOffer.getMultiposting.getClassifiedsList.asScala
              .map { classified =>
                val services = classified.getServicesList.asScala
                  .map(s => s.toBuilder.clearCreateDate().build())
                classified.toBuilder
                  .clearServices()
                  .clearCreateDate()
                  .addAllServices(services.asJava)
                  .build()
              }
            assert(
              classifieds4.contains(
                Multiposting.Classified
                  .newBuilder()
                  .setName(ClassifiedName.AVITO)
                  .setStatus(OfferStatus.ACTIVE)
                  .setEnabled(true)
                  .addAllServices(
                    Seq(
                      Classified.Service
                        .newBuilder()
                        .setService("avito_first")
                        .setIsActive(true)
                        .build(),
                      Classified.Service
                        .newBuilder()
                        .setService("avito_second")
                        .setIsActive(true)
                        .build()
                    ).asJava
                  )
                  .setDescription("avito description")
                  .build()
              )
            )
            assert(
              classifieds4.contains(
                Multiposting.Classified
                  .newBuilder()
                  .setName(ClassifiedName.AUTORU)
                  .setStatus(OfferStatus.ACTIVE)
                  .setEnabled(true)
                  .addAllServices(
                    Seq(
                      Classified.Service
                        .newBuilder()
                        .setService("autoru_first")
                        .setIsActive(true)
                        .build(),
                      Classified.Service
                        .newBuilder()
                        .setService("autoru_second")
                        .setIsActive(true)
                        .build()
                    ).asJava
                  )
                  .setDescription("autoru description")
                  .build()
              )
            )
            assert(
              classifieds4.contains(
                Multiposting.Classified
                  .newBuilder()
                  .setName(ClassifiedName.DROM)
                  .setStatus(OfferStatus.ACTIVE)
                  .setEnabled(true)
                  .addAllServices(
                    Seq(
                      Classified.Service
                        .newBuilder()
                        .setService("drom_first")
                        .setIsActive(true)
                        .build(),
                      Classified.Service
                        .newBuilder()
                        .setService("drom_second")
                        .setIsActive(true)
                        .build()
                    ).asJava
                  )
                  .setDescription("drom description")
                  .build()
              )
            )
          }

          "action - show: trucks" in {
            val feed: TruckExternalOffer = {
              trucksOfferGen.next.copy(
                classifieds = Some(Seq("avito", "autoru", "drom")),
                avitoSaleServices = Seq("avito_first", "avito_second"),
                saleServices = Seq("autoru_first", "autoru_second"),
                dromSaleServices = Seq("drom_first", "drom_second"),
                avitoDescription = Some("avito description"),
                dromDescription = Some("drom description"),
                description = Some("autoru description"),
                action = Some("show"),
                multipostingEnabled = Some(true)
              )
            }

            val result = converter.convert(feed)
            val vosOffer = result.offer

            assert(vosOffer.hasMultiposting)
            assert(vosOffer.getMultiposting.getStatus.equals(OfferStatus.ACTIVE))
            assert(!vosOffer.getMultiposting.getPreserveClassifieds)

            assert(vosOffer.getMultiposting.getClassifiedsCount == 3)
            val classifieds4 = vosOffer.getMultiposting.getClassifiedsList.asScala
              .map { classified =>
                val services = classified.getServicesList.asScala
                  .map(s => s.toBuilder.clearCreateDate().build())
                classified.toBuilder
                  .clearServices()
                  .clearCreateDate()
                  .addAllServices(services.asJava)
                  .build()
              }
            assert(
              classifieds4.contains(
                Multiposting.Classified
                  .newBuilder()
                  .setName(ClassifiedName.AVITO)
                  .setStatus(OfferStatus.ACTIVE)
                  .setEnabled(true)
                  .addAllServices(
                    Seq(
                      Classified.Service
                        .newBuilder()
                        .setService("avito_first")
                        .setIsActive(true)
                        .build(),
                      Classified.Service
                        .newBuilder()
                        .setService("avito_second")
                        .setIsActive(true)
                        .build()
                    ).asJava
                  )
                  .setDescription("avito description")
                  .build()
              )
            )
            assert(
              classifieds4.contains(
                Multiposting.Classified
                  .newBuilder()
                  .setName(ClassifiedName.AUTORU)
                  .setStatus(OfferStatus.ACTIVE)
                  .setEnabled(true)
                  .addAllServices(
                    Seq(
                      Classified.Service
                        .newBuilder()
                        .setService("autoru_first")
                        .setIsActive(true)
                        .build(),
                      Classified.Service
                        .newBuilder()
                        .setService("autoru_second")
                        .setIsActive(true)
                        .build()
                    ).asJava
                  )
                  .setDescription("autoru description")
                  .build()
              )
            )
            assert(
              classifieds4.contains(
                Multiposting.Classified
                  .newBuilder()
                  .setName(ClassifiedName.DROM)
                  .setStatus(OfferStatus.ACTIVE)
                  .setEnabled(true)
                  .addAllServices(
                    Seq(
                      Classified.Service
                        .newBuilder()
                        .setService("drom_first")
                        .setIsActive(true)
                        .build(),
                      Classified.Service
                        .newBuilder()
                        .setService("drom_second")
                        .setIsActive(true)
                        .build()
                    ).asJava
                  )
                  .setDescription("drom description")
                  .build()
              )
            )
          }

          "action - hide" in {
            val feed: CarExternalOffer = {
              carsOfferGen.next.copy(
                classifieds = Some(Seq("avito", "autoru", "drom")),
                avitoSaleServices = Seq("avito_first", "avito_second"),
                saleServices = Seq("autoru_first", "autoru_second"),
                dromSaleServices = Seq("drom_first", "drom_second"),
                avitoDescription = Some("avito description"),
                dromDescription = Some("drom description"),
                description = Some("autoru description"),
                action = Some("hide"),
                multipostingEnabled = Some(true)
              )
            }

            val result = converter.convert(feed)
            val vosOffer = result.offer

            assert(vosOffer.hasMultiposting)
            assert(vosOffer.getMultiposting.getStatus.equals(OfferStatus.INACTIVE))
            assert(!vosOffer.getMultiposting.getPreserveClassifieds)

            assert(vosOffer.getMultiposting.getClassifiedsCount == 3)
            val classifieds5 = vosOffer.getMultiposting.getClassifiedsList.asScala
              .map { classified =>
                val services = classified.getServicesList.asScala
                  .map(s => s.toBuilder.clearCreateDate().build())
                classified.toBuilder
                  .clearServices()
                  .clearCreateDate()
                  .addAllServices(services.asJava)
                  .build()
              }
            assert(
              classifieds5.contains(
                Multiposting.Classified
                  .newBuilder()
                  .setName(ClassifiedName.AVITO)
                  .setStatus(OfferStatus.INACTIVE)
                  .setEnabled(true)
                  .addAllServices(
                    Seq(
                      Classified.Service
                        .newBuilder()
                        .setService("avito_first")
                        .setIsActive(false)
                        .build(),
                      Classified.Service
                        .newBuilder()
                        .setService("avito_second")
                        .setIsActive(false)
                        .build()
                    ).asJava
                  )
                  .setDescription("avito description")
                  .build()
              )
            )
            assert(
              classifieds5.contains(
                Multiposting.Classified
                  .newBuilder()
                  .setName(ClassifiedName.AUTORU)
                  .setStatus(OfferStatus.INACTIVE)
                  .setEnabled(true)
                  .addAllServices(
                    Seq(
                      Classified.Service
                        .newBuilder()
                        .setService("autoru_first")
                        .setIsActive(false)
                        .build(),
                      Classified.Service
                        .newBuilder()
                        .setService("autoru_second")
                        .setIsActive(false)
                        .build()
                    ).asJava
                  )
                  .setDescription("autoru description")
                  .build()
              )
            )
            assert(
              classifieds5.contains(
                Multiposting.Classified
                  .newBuilder()
                  .setName(ClassifiedName.DROM)
                  .setStatus(OfferStatus.INACTIVE)
                  .setEnabled(true)
                  .addAllServices(
                    Seq(
                      Classified.Service
                        .newBuilder()
                        .setService("drom_first")
                        .setIsActive(false)
                        .build(),
                      Classified.Service
                        .newBuilder()
                        .setService("drom_second")
                        .setIsActive(false)
                        .build()
                    ).asJava
                  )
                  .setDescription("drom description")
                  .build()
              )
            )
          }

          "action - hide: trucks" in {
            val feed: TruckExternalOffer = {
              trucksOfferGen.next.copy(
                classifieds = Some(Seq("avito", "autoru", "drom")),
                avitoSaleServices = Seq("avito_first", "avito_second"),
                saleServices = Seq("autoru_first", "autoru_second"),
                dromSaleServices = Seq("drom_first", "drom_second"),
                avitoDescription = Some("avito description"),
                dromDescription = Some("drom description"),
                description = Some("autoru description"),
                action = Some("hide"),
                multipostingEnabled = Some(true)
              )
            }

            val result = converter.convert(feed)
            val vosOffer = result.offer

            assert(vosOffer.hasMultiposting)
            assert(vosOffer.getMultiposting.getStatus.equals(OfferStatus.INACTIVE))
            assert(!vosOffer.getMultiposting.getPreserveClassifieds)

            assert(vosOffer.getMultiposting.getClassifiedsCount == 3)
            val classifieds5 = vosOffer.getMultiposting.getClassifiedsList.asScala
              .map { classified =>
                val services = classified.getServicesList.asScala
                  .map(s => s.toBuilder.clearCreateDate().build())
                classified.toBuilder
                  .clearServices()
                  .clearCreateDate()
                  .addAllServices(services.asJava)
                  .build()
              }
            assert(
              classifieds5.contains(
                Multiposting.Classified
                  .newBuilder()
                  .setName(ClassifiedName.AVITO)
                  .setStatus(OfferStatus.INACTIVE)
                  .setEnabled(true)
                  .addAllServices(
                    Seq(
                      Classified.Service
                        .newBuilder()
                        .setService("avito_first")
                        .setIsActive(false)
                        .build(),
                      Classified.Service
                        .newBuilder()
                        .setService("avito_second")
                        .setIsActive(false)
                        .build()
                    ).asJava
                  )
                  .setDescription("avito description")
                  .build()
              )
            )
            assert(
              classifieds5.contains(
                Multiposting.Classified
                  .newBuilder()
                  .setName(ClassifiedName.AUTORU)
                  .setStatus(OfferStatus.INACTIVE)
                  .setEnabled(true)
                  .addAllServices(
                    Seq(
                      Classified.Service
                        .newBuilder()
                        .setService("autoru_first")
                        .setIsActive(false)
                        .build(),
                      Classified.Service
                        .newBuilder()
                        .setService("autoru_second")
                        .setIsActive(false)
                        .build()
                    ).asJava
                  )
                  .setDescription("autoru description")
                  .build()
              )
            )
            assert(
              classifieds5.contains(
                Multiposting.Classified
                  .newBuilder()
                  .setName(ClassifiedName.DROM)
                  .setStatus(OfferStatus.INACTIVE)
                  .setEnabled(true)
                  .addAllServices(
                    Seq(
                      Classified.Service
                        .newBuilder()
                        .setService("drom_first")
                        .setIsActive(false)
                        .build(),
                      Classified.Service
                        .newBuilder()
                        .setService("drom_second")
                        .setIsActive(false)
                        .build()
                    ).asJava
                  )
                  .setDescription("drom description")
                  .build()
              )
            )
          }
        }
      }
    }

    "undefined" should {
      "throw error" in {
        val feed: CarExternalOffer = {
          val origin = carsOfferGen.next
          origin.copy(multipostingEnabled = None)
        }

        assertThrows[IllegalStateException](converter.convert(feed))
      }
    }
  }

  "delivery info" in {

    val offerNonDeliveryInfo: CarExternalOffer = {
      val origin = carsOfferGen.next
      origin.copy(deliveryInfo = None)
    }

    val offerWithDeliveryInfoEmpty: CarExternalOffer = {
      val origin = carsOfferGen.next
      val deliveries = List.empty
      origin.copy(deliveryInfo = Some(deliveries))
    }

    val offerWithDeliveryInfoAddress: CarExternalOffer = {
      val origin = carsOfferGen.next

      val adr1 = "г.Москва, м,Юго-Западная, ул.Академика Анохина, д.6, корп.6"
      val adr2 = "г.Москва, м,Юго-Западная, ул.Академика Анохина, д.6, корп.7"

      val deliveries = List(Delivery(adr1), Delivery(adr2))

      val geocoding: GeocodingMapper.Addresses = Map(
        adr1 -> GeocodeResult(id = 1, lat = 1L, lng = 1L, address = "address 1", city = Some("Gotham")),
        adr2 -> GeocodeResult(id = 2, lat = 2L, lng = 2L, address = "address 2", city = Some("Gotham"))
      )

      origin.copy(deliveryInfo = Some(deliveries)).withGeocoding(geocoding)
    }

    val res1 = converter.convert(offerNonDeliveryInfo)
    val vosOffer1 = res1.offer

    assert(!vosOffer1.hasDeliveryInfo)

    val res2 = converter.convert(offerWithDeliveryInfoEmpty)
    val vosOffer2 = res2.offer

    assert(vosOffer2.hasDeliveryInfo)
    assert(vosOffer2.getDeliveryInfo.getDeliveryRegionsCount == 0)

    val res3 = converter.convert(offerWithDeliveryInfoAddress)
    val vosOffer3 = res3.offer

    assert(vosOffer3.hasDeliveryInfo)
    assert(vosOffer3.getDeliveryInfo.getDeliveryRegionsCount > 0)
    assert(vosOffer3.getDeliveryInfo.getDeliveryRegionsCount == 2)

  }

  "all fields should be equals" in {
    val offer: CarExternalOffer = carsOfferGen.next
    val res = converter.convert(offer)
    val vosOffer = res.offer

    assert(vosOffer.getCategory == Category.CARS)
    assert(vosOffer.getSection == offer.section)
    assert(vosOffer.getCarInfo.getMark == offer.mark)
    assert(vosOffer.getCarInfo.getModel == offer.model)
    assert(vosOffer.getCarInfo.getSteeringWheel == wheelMap(offer.wheel))
    assert(vosOffer.getCarInfo.getBodyType == offer.bodyType)
    assert(vosOffer.getDocuments.getVin == offer.vin.get)
    assert(vosOffer.getColorHex == AutoruOfferConverter.COLOR_MAP(offer.color))
    assert(vosOffer.getAvailability == availabilityMap(offer.availability))
    assert(vosOffer.getDocuments.getCustomCleared == customMap(offer.custom))
    assert(
      vosOffer.getState.getCondition ==
        stateMap.getOrElse(offer.state.getOrElse(""), Condition.CONDITION_OK)
    )
    assert(vosOffer.getState.getVideo.getYoutubeId === "MJIC9MWhrrs")
    assert(
      vosOffer.getDocuments.getOwnersNumber == ownersNumberMap(
        offer.ownersNumber
      )
    )
    assert(vosOffer.getState.getMileage == offer.run.getOrElse(0))
    assert(vosOffer.getDocuments.getYear == offer.year)
    assert(vosOffer.getPriceInfo.getPrice == offer.price)
    assert(
      Currency
        .from(
          vosOffer.getPriceInfo.getCurrency
        )
        .currency == offer.currency.currency
    )

    assert(vosOffer.getCarInfo.getEquipmentCount == 3)
    assert(vosOffer.getCarInfo.getEquipmentMap.get("audiosystem-cd"))
    assert(vosOffer.getCarInfo.getEquipmentMap.get("airbag-side"))
    assert(vosOffer.getCarInfo.getEquipmentMap.get("electro-trunk"))

    assert(
      offer.creditDiscount.forall(_ == vosOffer.getDiscountOptions.getCredit)
    )
    assert(
      offer.tradeinDiscount.forall(_ == vosOffer.getDiscountOptions.getTradein)
    )
    assert(
      offer.insuranceDiscount
        .forall(_ == vosOffer.getDiscountOptions.getInsurance)
    )
    assert(
      offer.maxDiscount.forall(_ == vosOffer.getDiscountOptions.getMaxDiscount)
    )

    val mandatorySuffixes = "#lang=ru" + urlEncode(
      "#hidehotspots" + "#!hidecarousel"
    )
    assert(offer.panoramas.get.spinCarUrl.forall(url => {
      "https://" + url
        .split('#')
        .head + mandatorySuffixes == vosOffer.getState.getPanoramas.getSpincarExteriorUrl
    }))

    if (offer.discountPrice.isEmpty) {
      assert(vosOffer.getDiscountPrice.getPrice == 0)
      assert(
        vosOffer.getDiscountPrice.getStatus == DiscountPriceApi.DiscountPriceStatus.DISCOUNT_STATUS_UNKNOWN
      )
    } else {
      assert(
        vosOffer.getDiscountPrice.getPrice == offer.discountPrice.get.price
      )
      assert(
        vosOffer.getDiscountPrice.getStatus ==
          discountPriceActionMap.getOrElse(
            offer.discountPrice.get.action,
            DiscountPriceApi.DiscountPriceStatus.UNRECOGNIZED
          )
      )
    }

    assert(offer.description.contains(vosOffer.getDescription))
    assert(offer.sts.contains(vosOffer.getDocuments.getSts))
    assert(vosOffer.getDocuments.getPts == ptsMap(offer.pts.getOrElse("")))

    assert(res.notices.isEmpty)
    assert(
      offer.originalPrice.forall(_ == vosOffer.getOriginalPrice.getPrice.toInt)
    )
    assert(offer.onlineViewAvailable.forall(_ == vosOffer.getAdditionalInfo.getOnlineViewAvailable))
    assert(offer.bookingAllowed.forall(_ == vosOffer.getAdditionalInfo.getBooking.getAllowed))
    assert(offer.pledgeNumber.forall(_ == vosOffer.getDocuments.getPledgeNumber))
    assert(offer.notRegisteredInRussia.forall(_ == vosOffer.getDocuments.getNotRegisteredInRussia))
  }

  "should generate notices" in {
    val offer: CarExternalOffer = carsOfferGen.next

    val offerAction = offer.copy(action = Some("unknown"))
    val resAction = converter.convert(offerAction)
    assert(resAction.notices.size == 1)
    assert(
      resAction.notices(0) == OfferNotice(
        "Неизвестное значение",
        "Action",
        "unknown",
        "offer_converter"
      )
    )

    val offerCustom = offer
      .copy(section = Section.NEW)
      .copy(custom = "bad custom")
      .copy(taskContext = taskContextNew)
    val resCustom = converter.convert(offerCustom)
    assert(resCustom.notices.size == 1)
    assert(
      resCustom.notices(0) ==
        OfferNotice(
          "Не удалось обработать поле",
          "Таможенный статус",
          "bad custom",
          "offer_converter"
        )
    )

    val offerColor = offer.copy(color = "ццвет")
    val resColor = converter.convert(offerColor)
    assert(resColor.notices.size == 1)
    assert(
      resColor.notices(0) ==
        OfferNotice(
          "Указано неправильное значение",
          "Цвет",
          "ццвет",
          "offer_converter"
        )
    )

    val offerExchange = offer.copy(exchange = Some("undefined"))
    val resExchange = converter.convert(offerExchange)
    assert(resExchange.notices.size == 1)
    assert(
      resExchange.notices(0) ==
        OfferNotice(
          "Указано неправильное значение",
          "Обмен",
          "undefined",
          "offer_converter"
        )
    )

    val offerDiscountAction =
      offer.copy(
        discountPrice = Some(DiscountPrice(offer.price - 0.01, "bad action"))
      )
    val resDiscountAction = converter.convert(offerDiscountAction)
    assert(resDiscountAction.notices.size == 1)
    assert(
      resDiscountAction.notices(0) ==
        OfferNotice(
          "Указано неправильное значение",
          "discount.status",
          "bad action",
          "offer_converter"
        )
    )

  }

  "should apply modification or complectation code" in {
    val template0: CarExternalOffer = carsOfferGen.next
    val offer = template0.copy(
      unification = template0.unification.map(_.copy(complectationId = None))
    )
    val offer0 = offer.copy(modificationCode = None, complectation = None)
    val offer1 =
      offer.copy(modificationCode = Some("92ABK1"), complectation = None)
    val offer2 =
      offer.copy(modificationCode = None, complectation = Some(Left(20103525)))
    val offer3 = offer.copy(
      modificationCode = None,
      complectation = Some(Right("Diesel Platinum Edition"))
    )
    val offer4 = offer.copy(
      modificationCode = Some("92ABK1"),
      complectation = Some(Left(20103525))
    )
    val offer5 = offer.copy(
      modificationCode = Some("92ABK1"),
      complectation = Some(Right("Diesel Platinum Edition"))
    )

    assert(
      !converter
        .convert(offer0)
        .offer
        .getCarInfo
        .getManufacturerInfo
        .hasModificationCode
    )
    assert(!converter.convert(offer0).offer.getCarInfo.hasComplectationId)

    assert(
      converter
        .convert(offer1)
        .offer
        .getCarInfo
        .getManufacturerInfo
        .getModificationCode == "92ABK1"
    )
    assert(!converter.convert(offer1).offer.getCarInfo.hasComplectationId)

    assert(
      converter.convert(offer2).offer.getCarInfo.getComplectationId == 20103525L
    )
    assert(
      !converter
        .convert(offer2)
        .offer
        .getCarInfo
        .getManufacturerInfo
        .hasModificationCode
    )

    assert(
      converter
        .convert(offer3)
        .offer
        .getCarInfo
        .getManufacturerInfo
        .getModificationCode
        == "Diesel Platinum Edition"
    )
    assert(!converter.convert(offer3).offer.getCarInfo.hasComplectationId)

    assert(
      converter.convert(offer4).offer.getCarInfo.getComplectationId == 20103525L
    )
    assert(
      !converter
        .convert(offer4)
        .offer
        .getCarInfo
        .getManufacturerInfo
        .hasModificationCode
    )

    assert(
      converter
        .convert(offer5)
        .offer
        .getCarInfo
        .getManufacturerInfo
        .getModificationCode
        == "Diesel Platinum Edition"
    )
    assert(!converter.convert(offer5).offer.getCarInfo.hasComplectationId)
  }

  "should fill Booking allowed properly" in {
    val offer: CarExternalOffer = carsOfferGen.next

    val allowedRes = converter.convert(offer.copy(bookingAllowed = Some(true)))
    assert(allowedRes.offer.getAdditionalInfo.hasBooking)
    assert(allowedRes.offer.getAdditionalInfo.getBooking.getAllowed)

    val notAllowedRes = converter.convert(offer.copy(bookingAllowed = Some(false)))
    assert(notAllowedRes.offer.getAdditionalInfo.hasBooking)
    assert(!notAllowedRes.offer.getAdditionalInfo.getBooking.getAllowed)
  }

  "should leave Booking empty if not filled" in {
    val offer: CarExternalOffer = carsOfferGen.next

    val res = converter.convert(offer.copy(bookingAllowed = None))

    assert(!res.offer.getAdditionalInfo.hasBooking)
  }

  val wheelMap = Map(
    "Левый" -> CommonModel.SteeringWheel.LEFT,
    "Правый" -> CommonModel.SteeringWheel.RIGHT
  )

  val availabilityMap = Map(
    "На заказ" -> Availability.ON_ORDER,
    "В наличии" -> Availability.IN_STOCK
  )

  val customMap = Map("Не растаможен" -> false, "Растаможен" -> true)

  val ownersNumberMap = Map(
    None -> 0,
    Some("Не было владельцев") -> 0,
    Some("Один владелец") -> 1,
    Some("Два владельца") -> 2,
    Some("Три владельца") -> 3,
    Some("Три и более") -> 4,
    Some("Четыре и более") -> 4
  )

  val discountPriceActionMap = Map(
    "show" -> DiscountPriceApi.DiscountPriceStatus.ACTIVE,
    "hide" -> DiscountPriceApi.DiscountPriceStatus.INACTIVE
  )

  val ptsMap = Map(
    "" -> PtsStatus.PTS_UNKNOWN,
    "оригинал" -> PtsStatus.ORIGINAL,
    "дубликат" -> PtsStatus.DUPLICATE
  )

  val stateMap = Map("Требует ремонта" -> Condition.CONDITION_BROKEN)

}
