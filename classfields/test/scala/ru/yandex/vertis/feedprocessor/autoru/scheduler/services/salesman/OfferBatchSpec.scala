package ru.yandex.vertis.feedprocessor.autoru.scheduler.services.salesman

import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletionStage
import akka.Done
import akka.kafka.ConsumerMessage
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.testkit.ConsumerResultFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.{OfferStatus, Section}
import ru.auto.feedprocessor.FeedprocessorModel.Entity.FreshService
import ru.auto.feedprocessor.FeedprocessorModel.{Entity, MessageType, OffersResponse, UpdateStatus}
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model
import ru.yandex.vertis.feedprocessor.autoru.model.{OfferAnswers, SaleCategories, ServiceInfo, Task, TaskContext}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.salesman.SalesmanClient.{Goods, Products, Schedule}
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._

import scala.concurrent.Future
import scala.annotation.nowarn

/**
  * @author pnaydenov
  */
@nowarn("cat=other-match-analysis")
class OfferBatchSpec extends WordSpecBase {

  private val dummyOffset = ConsumerResultFactory.committableOffset("","", 0, 0L, "")
  private val tc = TaskContext(newTasksGen.next, 1L)
  private val Prefix = "all_sale_"
  private val Placement = Products.Placement.toString.replace(Prefix, "")
  private val Premium = Products.Premium.toString.replace(Prefix, "")
  private val Add = Products.Add.toString.replace(Prefix, "")
  private val Special = Products.Special.toString.replace(Prefix, "")
  private val time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
  private val Turbo = Products.Turbo.toString

  val response: OffersResponse = {
    OffersResponse
      .newBuilder()
      .setTimestamp(0)
      .setType(MessageType.OFFER_STREAM_BATCH)
      .addEntities(
        Entity
          .newBuilder()
          .setPosition(0)
          .addActiveServices(Premium)
          .setOfferId("0-0")
          .setOfferStatus(OfferStatus.ACTIVE)
          .setStatus(UpdateStatus.UPDATE)
          .setFreshService(
            FreshService
              .newBuilder()
              .addWeekdays(1)
              .addWeekdays(3)
              .setTime(time.toSecondOfDay)
              .setSwitch(true)
          )
      )
      .addEntities(
        Entity
          .newBuilder()
          .setPosition(1)
          .addActiveServices(Add)
          .setOfferId("1-1")
          .setOfferStatus(OfferStatus.NEED_ACTIVATION)
          .setStatus(UpdateStatus.INSERT)
          .setFreshService(FreshService.newBuilder().setSwitch(false))
      )
      .addEntities(
        Entity
          .newBuilder()
          .setPosition(2)
          .addActiveServices(Add)
          .setOfferId("2-2")
          .setOfferStatus(OfferStatus.BANNED)
          .setStatus(UpdateStatus.UPDATE)
      )
      .addEntities(
        Entity
          .newBuilder()
          .setPosition(3)
          .setOfferId("3-3")
          .addActiveServices(Special)
          .setStatus(UpdateStatus.ERROR)
      )
      .addEntities(
        Entity
          .newBuilder()
          .setPosition(4)
          .setStatus(UpdateStatus.ERROR)
      )
      .addEntities(
        Entity
          .newBuilder()
          .setPosition(5)
          .setOfferId("5-5")
          .setOfferStatus(OfferStatus.NEED_ACTIVATION)
          .setStatus(UpdateStatus.UPDATE)
      )
      .addEntities(
        Entity
          .newBuilder()
          .setPosition(6)
          .setOfferId("6-6")
          .setOfferStatus(OfferStatus.EXPIRED)
          .setStatus(UpdateStatus.UPDATE)
      )
      .addEntities(
        Entity
          .newBuilder()
          .setPosition(7)
          .setOfferId("7-7")
          .setOfferStatus(OfferStatus.EXPIRED)
          .setStatus(UpdateStatus.UPDATE)
          .addBadges("FOO")
          .addBadges("BAR")
      )
      .addEntities(
        Entity
          .newBuilder()
          .setPosition(8)
          .setOfferId("8-8")
          .setOfferStatus(OfferStatus.ACTIVE)
          .setStatus(UpdateStatus.UPDATE)
          .addBadges("FOO")
          .addBadges("BAR")
      )
      .addEntities(
        Entity
          .newBuilder()
          .setPosition(9)
          .setOfferId("9-9")
          .addActiveServices(Turbo)
          .setOfferStatus(OfferStatus.ACTIVE)
          .setStatus(UpdateStatus.UPDATE)
      )
      .addDeletedOfferIds("10-10")
      .addEntities(
        Entity
          .newBuilder()
          .setPosition(10)
          .addActiveServices(Placement)
          .setOfferId("11-11")
          .setOfferStatus(OfferStatus.NEED_ACTIVATION)
          .setStatus(UpdateStatus.INSERT)
      )
      .build()
  }

  val initialBatch = OfferBatch(
    CommittableMessage(new ConsumerRecord("topic", 0, 0, 1L, OfferAnswers(response, Some(tc))), dummyOffset),
    existingServicesByOffer = None,
    existingFreshServiceByOffer = None
  )

  val serviceInfoWithLeaveServices: Gen[ServiceInfo] =
    serviceInfoGen(sectionGen = Gen.const(Section.NEW.getNumber), leaveServices = true)

  val tcWithLeaveServices = model.TaskContext(
    tasksGen(Gen.const(Task.Status.New), serviceInfoWithLeaveServices).next,
    1L
  )

  val batchWithLeaveServices = OfferBatch(
    CommittableMessage(
      new ConsumerRecord("topic", 0, 0, 1L, OfferAnswers(response, Some(tcWithLeaveServices))),
      dummyOffset
    ),
    existingServicesByOffer = None,
    existingFreshServiceByOffer = None
  )

  "OfferBatch" should {
    "services" should {
      "create" in {
        val batch = initialBatch.copy(existingServicesByOffer = Some(Map()))
        batch.servicesForCreate("0-0") shouldEqual Set(Products.Premium)
        batch.servicesForCreate("1-1") shouldEqual Set(Products.Add)
        batch.servicesForCreate("2-2") shouldBe empty
        batch.servicesForCreate("3-3") shouldBe empty
      }

      "skip existing" in {
        val batch = initialBatch.copy(existingServicesByOffer =
          Some(
            Map(
              "0-0" -> Seq(
                Goods(0, SaleCategories.Cars, Products.Placement, None),
                Goods(0, SaleCategories.Cars, Products.Premium, None)
              )
            )
          )
        )
        batch.servicesForCreate("0-0") shouldBe empty
      }

      "remove currently non active services" in {
        val batch = initialBatch.copy(existingServicesByOffer =
          Some(Map("0-0" -> Seq(Goods(0, SaleCategories.Cars, Products.Add, None))))
        )
        batch.servicesForCreate("0-0") shouldEqual Set(Products.Premium)
        batch.servicesForDelete("0-0", deleted = false) shouldEqual Set(Products.Add)
      }

      "remove for deleted offer" in {
        val batch = initialBatch.copy(existingServicesByOffer =
          Some(
            Map(
              "10-10" -> Seq(
                Goods(10, SaleCategories.Cars, Products.Placement, None),
                Goods(10, SaleCategories.Cars, Products.Add, None)
              )
            )
          )
        )
        batch.servicesForDelete("10-10", deleted = true) shouldEqual Set(Products.Add, Products.Placement)
      }

      "remove for disabled offer" in {
        val batch = initialBatch.copy(existingServicesByOffer =
          Some(
            Map(
              "2-2" -> Seq(
                Goods(2, SaleCategories.Cars, Products.Placement, None),
                Goods(2, SaleCategories.Cars, Products.Add, None)
              )
            )
          )
        )
        batch.servicesForDelete("2-2", deleted = false) shouldEqual Set(Products.Add, Products.Placement)
      }

      "not add placement for any active offer" in {
        val batch = initialBatch.copy(existingServicesByOffer = Some(Map()))
        batch.servicesForCreate("5-5") shouldBe empty
      }

      "not add placement for offer even if the service is specified in the feed" in {
        val batch = initialBatch.copy(existingServicesByOffer = Some(Map()))
        batch.servicesForCreate("11-11") shouldBe empty
      }

      "don't touch fresh service" in {
        val batch = initialBatch.copy(existingServicesByOffer =
          Some(
            Map(
              "0-0" -> Seq(Goods(0, SaleCategories.Cars, Products.Fresh, None)),
              "2-2" -> Seq(Goods(2, SaleCategories.Cars, Products.Fresh, None)),
              "10-10" -> Seq(Goods(10, SaleCategories.Cars, Products.Fresh, None))
            )
          )
        )
        batch.servicesForCreate("0-0") shouldEqual Set(Products.Premium)
        batch.servicesForDelete("2-2", deleted = false) shouldBe empty
        batch.servicesForDelete("10-10", deleted = true) shouldBe empty
      }

      "don't touch badges service" in {
        val batch = initialBatch.copy(existingServicesByOffer =
          Some(
            Map(
              "0-0" -> Seq(Goods(0, SaleCategories.Cars, Products.Badge, Some("FOO"))),
              "2-2" -> Seq(Goods(2, SaleCategories.Cars, Products.Badge, Some("BAR"))),
              "10-10" -> Seq(Goods(10, SaleCategories.Cars, Products.Badge, Some("BAZ")))
            )
          )
        )
        batch.servicesForCreate("0-0") shouldEqual Set(Products.Premium)
        batch.servicesForCreate("7-7") shouldBe empty
        batch.servicesForCreate("8-8") shouldBe empty
        batch.servicesForDelete("0-0", deleted = false) shouldBe empty
        batch.servicesForDelete("2-2", deleted = false) shouldBe empty
        batch.servicesForDelete("10-10", deleted = true) shouldBe empty
      }

      "don't delete turbo-package service" in {
        val batch = initialBatch.copy(existingServicesByOffer =
          Some(
            Map(
              "0-0" -> Seq(Goods(0, SaleCategories.Cars, Products.Turbo, None)),
              "9-9" -> Seq(Goods(0, SaleCategories.Cars, Products.Placement, None)),
              "10-10" -> Seq(Goods(0, SaleCategories.Cars, Products.Turbo, None))
            )
          )
        )
        batch.servicesForDelete("0-0", deleted = false) shouldBe empty
        batch.servicesForCreate("9-9") shouldEqual Set(Products.Turbo)
        batch.servicesForDelete("10-10", deleted = true) shouldBe empty
      }

      "don't delete service if leave_service=true" in {
        val batch = batchWithLeaveServices.copy(existingServicesByOffer =
          Some(Map("5-5" -> Seq(Goods(5, SaleCategories.Cars, Products.Add, None))))
        )
        batch.servicesForDelete("5-5", deleted = false) shouldBe empty
      }

      "delete service of removed offers even if leave_service=true" in {
        val batch = batchWithLeaveServices.copy(existingServicesByOffer =
          Some(
            Map(
              "10-10" ->
                Seq(Goods(10, SaleCategories.Cars, Products.Add, None))
            )
          )
        )
        batch.servicesForDelete("10-10", deleted = true) shouldEqual Set(Products.Add)
      }

      "do not send all_sale_activate service for expired offers" in {
        val batchWithPlacement = initialBatch.copy(existingServicesByOffer =
          Some(
            Map(
              "6-6" ->
                Seq(Goods(6, SaleCategories.Cars, Products.Placement, None))
            )
          )
        )
        val batchWithoutPlacement =
          initialBatch.copy(existingServicesByOffer = Some(Map("6-6" -> Seq())))
        val batchWithoutPlacement2 =
          initialBatch.copy(existingServicesByOffer = Some(Map()))

        batchWithPlacement.servicesForCreate("6-6") shouldEqual Set()
        batchWithoutPlacement.servicesForCreate("6-6") shouldEqual Set()
        batchWithoutPlacement2.servicesForCreate("6-6") shouldEqual Set()
      }
    }

    "fresh" should {
      "create" in {
        val batch = initialBatch.copy(existingFreshServiceByOffer = Some(Map()))
        batch.schedulesForCreateOrUpdate("0-0").get match {
          case Schedule("0-0", List(1, 3), _, _) =>
        }
      }

      "update" in {
        val batch = initialBatch.copy(
          existingFreshServiceByOffer =
            Some(Map("0-0" -> Schedule("1-1", List(1, 3, 5), time, SalesmanClient.DefaulTimezone)))
        )
        batch.schedulesForCreateOrUpdate("0-0").get match {
          case Schedule("0-0", List(1, 3), _, _) =>
        }
      }

      "skip existing" in {
        val batch = initialBatch.copy(existingFreshServiceByOffer =
          Some(Map("0-0" -> Schedule("0-0", List(1, 3), time, SalesmanClient.DefaulTimezone)))
        )
        batch.schedulesForCreateOrUpdate("0-0") shouldBe empty
      }

      "skip non switched" in {
        val batch = initialBatch.copy(existingFreshServiceByOffer = Some(Map()))
        batch.schedulesForCreateOrUpdate("1-1") shouldBe empty
      }

      "remove non switched" in {
        val batch = initialBatch.copy(existingFreshServiceByOffer =
          Some(Map("1-1" -> Schedule("1-1", List(1, 3), time, SalesmanClient.DefaulTimezone)))
        )
        batch.scheduleForDelete("1-1", deleted = false) shouldBe defined
        batch.schedulesForCreateOrUpdate("1-1") shouldBe empty
      }

      "remove for non-active" in {
        val schedule = Schedule("1-1", List(1, 3), time, SalesmanClient.DefaulTimezone)
        val batch = initialBatch.copy(
          existingFreshServiceByOffer = Some(
            Map(
              "2-2" -> schedule.copy(offerId = "2-2"),
              "3-3" -> schedule.copy(offerId = "3-3"),
              "4-4" -> schedule.copy(offerId = "4-4"),
              "10-10" -> schedule.copy(offerId = "10-10")
            )
          )
        )
        batch.scheduleForDelete("2-2", deleted = false) shouldBe defined
        batch.scheduleForDelete("3-3", deleted = false) shouldBe defined
        batch.scheduleForDelete("4-4", deleted = false) shouldBe defined
        batch.scheduleForDelete("10-10", deleted = true) shouldBe defined

        batch.schedulesForCreateOrUpdate("2-2") shouldBe empty
        batch.schedulesForCreateOrUpdate("3-3") shouldBe empty
        batch.schedulesForCreateOrUpdate("4-4") shouldBe empty
        batch.schedulesForCreateOrUpdate("10-10") shouldBe empty
      }

      "delete if created before but not specified yet" in {
        val batch = initialBatch.copy(existingFreshServiceByOffer =
          Some(Map("5-5" -> Schedule("5-5", List(1, 3), time, SalesmanClient.DefaulTimezone)))
        )
        batch.scheduleForDelete("5-5", deleted = false) shouldBe defined
        batch.schedulesForCreateOrUpdate("5-5") shouldBe empty
      }

      "don't delete service if leave_service=true" in {
        val batch = batchWithLeaveServices.copy(existingFreshServiceByOffer =
          Some(Map("5-5" -> Schedule("5-5", List(1, 3), time, SalesmanClient.DefaulTimezone)))
        )
        batch.scheduleForDelete("5-5", deleted = false) shouldBe empty
      }

      "delete service of removed offers even if leave_service=true" in {
        val batch = batchWithLeaveServices.copy(existingFreshServiceByOffer =
          Some(Map("10-10" -> Schedule("10-10", List(1, 3), time, SalesmanClient.DefaulTimezone)))
        )
        batch.scheduleForDelete("10-10", deleted = true) shouldBe defined
      }

      "don't delete placement and other services of active offers" in {
        val batch = initialBatch.copy(existingServicesByOffer =
          Some(
            Map(
              "0-0" -> Seq(
                Goods(2, SaleCategories.Cars, Products.Placement, None),
                Goods(2, SaleCategories.Cars, Products.Premium, None)
              )
            )
          )
        )
        batch.servicesForDelete("0-0", deleted = false) shouldEqual Set()
      }
    }

    "badges" should {
      "be ignored if offer doesn't have a badges" in {
        initialBatch.badgesForUpdate("0-0") shouldBe empty
        initialBatch.badgesForUpdate("1-1") shouldBe empty
      }

      "be ignored for non active offers" in {
        val batch = initialBatch.copy(existingServicesByOffer =
          Some(Map("7-7" -> Seq(Goods(7, SaleCategories.Cars, Products.Badge, Some("FOO")))))
        )
        batch.badgesForUpdate("7-7") shouldBe empty
        batch.badgesForUpdate("10-10") shouldBe empty
        batch.badgesForUpdate("100500-100500") shouldBe empty
      }

      "create" in {
        val batch = initialBatch.copy(existingServicesByOffer = Some(Map("8-8" -> Seq())))
        batch.badgesForUpdate("8-8").get.toSet shouldEqual Set("FOO", "BAR")

        val batch2 = initialBatch.copy(existingServicesByOffer = Some(Map.empty))
        batch2.badgesForUpdate("8-8").get.toSet shouldEqual Set("FOO", "BAR")
      }

      "push empty state" in {
        val batch = initialBatch.copy(existingServicesByOffer =
          Some(
            Map(
              "0-0" -> Seq(
                Goods(0, SaleCategories.Cars, Products.Badge, Some("FOO")),
                Goods(0, SaleCategories.Cars, Products.Badge, Some("BAZ"))
              )
            )
          )
        )
        batch.badgesForUpdate("0-0") shouldBe defined
        batch.badgesForUpdate("0-0").get shouldBe empty
      }

      "don't push empty state if leave services enabled" in {
        val batch = batchWithLeaveServices.copy(existingServicesByOffer =
          Some(
            Map(
              "0-0" -> Seq(
                Goods(0, SaleCategories.Cars, Products.Badge, Some("FOO")),
                Goods(0, SaleCategories.Cars, Products.Badge, Some("BAZ"))
              )
            )
          )
        )
        batch.badgesForUpdate("0-0") shouldBe empty
      }

      "skip if uncganged" in {
        val batch = initialBatch.copy(existingServicesByOffer =
          Some(
            Map(
              "8-8" -> Seq(
                Goods(8, SaleCategories.Cars, Products.Badge, Some("FOO")),
                Goods(8, SaleCategories.Cars, Products.Badge, Some("BAR"))
              )
            )
          )
        )
        batch.badgesForUpdate("8-8") shouldBe empty
      }

      "update" in {
        val batch = initialBatch.copy(existingServicesByOffer =
          Some(
            Map(
              "8-8" -> Seq(
                Goods(8, SaleCategories.Cars, Products.Badge, Some("FOO")),
                Goods(8, SaleCategories.Cars, Products.Badge, Some("BAZ"))
              )
            )
          )
        )
        batch.badgesForUpdate("8-8").get.toSet shouldEqual Set("FOO", "BAR")
      }

      "don't fail on badges w/o name" in {
        val batch = initialBatch.copy(existingServicesByOffer =
          Some(
            Map(
              "8-8" -> Seq(
                Goods(8, SaleCategories.Cars, Products.Badge, badge = None),
                Goods(8, SaleCategories.Cars, Products.Badge, badge = None)
              )
            )
          )
        )
        batch.badgesForUpdate("8-8").get.toSet shouldEqual Set("FOO", "BAR")
      }
    }
  }
}
