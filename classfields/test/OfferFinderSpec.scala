package auto.dealers.match_maker.scheduler

import java.util.UUID

import ru.auto.api.ApiOfferModel.{Category, Offer, Salon, Section}
import auto.dealers.match_maker.logic.clients.PublicApiClient
import auto.dealers.match_maker.logic.clients.PublicApiClient.PublicApiClient
import ru.auto.match_maker.model.api.ApiModel.{MatchApplication, Target}
import auto.dealers.match_maker.scheduler.engine.OfferFinder.OfferFinderEnv
import auto.dealers.match_maker.scheduler.SchedulerConfigProvider.{SchedulerConfig, SchedulerConfigProvider}
import auto.dealers.match_maker.scheduler.engine.OfferFinder
import ru.yandex.vertis.mockito.MockitoSupport
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, _}
import zio.{Task, ZIO, ZLayer}

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._

object OfferFinderSpec extends DefaultRunnableSpec {
  import OfferFinderSpecOps._

  def spec =
    suite("OfferFinder")(
      testWithContextProvided { implicit context =>
        testM("Simple case") {
          for {
            config <- SchedulerConfigProvider.config
            _ <- mockUsingMethods(returnOffers = Task(getDefaultOffers(config.offersPerApplication)))

            res <-
              OfferFinder
                .findTargetOffers(MatchApplication.getDefaultInstance, Seq.empty[String], Seq.empty[String])
          } yield assert(res.getTargetCount)(equalTo(config.offersPerApplication))
        }
      },
      testWithContextProvided { implicit context =>
        testM("OfferFinder should set section and category to target from offer") {
          for {
            config <- SchedulerConfigProvider.config
            _ <- mockUsingMethods(
              returnOffers = Task(getDefaultOffers(config.offersPerApplication))
            )

            res <-
              OfferFinder
                .findTargetOffers(MatchApplication.getDefaultInstance, Seq.empty[String], Seq.empty[String])
          } yield assert(
            res.getTargetList.asScala.forall(t => t.getOfferSection != null && t.getOfferCategory != null)
          )(isTrue)
        }
      },
      testWithContextProvided { implicit context =>
        testM("When public api returns more targets then N = OffersPerApplication, take first N") {
          for {
            config <- SchedulerConfigProvider.config
            _ <- mockUsingMethods(returnOffers = Task(getDefaultOffers(config.offersPerApplication + 1)))

            res <-
              OfferFinder
                .findTargetOffers(MatchApplication.getDefaultInstance, Seq.empty[String], Seq.empty[String])
          } yield assert(res.getTargetCount)(equalTo(config.offersPerApplication))
        }
      },
      testWithContextProvided { implicit context =>
        testM("When application already has k offers, OfferFinder should add (OffersPerApplication - k)") {
          for {
            config <- SchedulerConfigProvider.config
            _ <- mockUsingMethods(returnOffers = Task(getDefaultOffers(config.offersPerApplication + 1)))

            res <- OfferFinder.findTargetOffers(
              MatchApplication
                .newBuilder()
                .addTarget(Target.newBuilder().setClientId(UUID.randomUUID().toString))
                .build(),
              Seq.empty[String],
              Seq.empty[String]
            )
          } yield assert(res.getTargetCount)(equalTo(config.offersPerApplication))
        }
      },
      testWithContextProvided { implicit context =>
        testM("Select dealers unless they have already been selected") {
          for {
            config <- SchedulerConfigProvider.config

            dealerId <- ZIO.effectTotal(UUID.randomUUID().toString)
            _ <- mockUsingMethods(
              returnOffers = Task(
                getDefaultOffers(config.offersPerApplication, Some(dealerId))
                  ++ getDefaultOffers(1)
              )
            )

            res <- OfferFinder.findTargetOffers(
              MatchApplication.newBuilder().addTarget(Target.newBuilder().setClientId(dealerId)).build(),
              Seq.empty[String],
              Seq.empty[String]
            )
          } yield assert(res.getTargetCount)(equalTo(2))
        }
      },
      testWithContextProvided { implicit context =>
        testM("When recentMatchApplications non empty, should consider low priority of some dealers") {
          for {
            config <- SchedulerConfigProvider.config

            dealerId <- ZIO.effectTotal(UUID.randomUUID().toString)
            _ <- mockUsingMethods(
              returnOffers = Task(
                getDefaultOffers(1, Some(dealerId))
                  ++ getDefaultOffers(config.offersPerApplication)
              )
            )

            res <- OfferFinder.findTargetOffers(
              MatchApplication.getDefaultInstance,
              Seq(dealerId),
              Seq.empty[String]
            )
          } yield assert(res.getTargetCount)(equalTo(3)) &&
            assert(res.getTargetList.asScala.count(t => t.getClientId == dealerId))(equalTo(0))
        }
      },
      testWithContextProvided { implicit context =>
        testM("Select low priority dealers, when new dealers amount is not enough") {
          for {
            config <- SchedulerConfigProvider.config

            dealerId <- ZIO.effectTotal(UUID.randomUUID().toString)
            _ <- mockUsingMethods(
              returnOffers = Task(
                getDefaultOffers(1, Some(dealerId))
                  ++ getDefaultOffers(config.offersPerApplication - 2)
              )
            )

            res <- OfferFinder.findTargetOffers(
              MatchApplication.getDefaultInstance,
              Seq(dealerId),
              Seq.empty[String]
            )
          } yield assert(res.getTargetCount)(equalTo(config.offersPerApplication - 1)) &&
            assert(res.getTargetList.asScala.count(t => t.getClientId == dealerId))(equalTo(1))
        }
      },
      testWithContextProvided { implicit context =>
        testM("Select low priority dealers, when new dealers amount is not enough and targets not empty") {
          for {
            config <- SchedulerConfigProvider.config

            dealerId <- ZIO.effectTotal(UUID.randomUUID().toString)
            _ <- mockUsingMethods(
              returnOffers = Task(
                getDefaultOffers(1, Some(dealerId))
                  ++ getDefaultOffers(config.offersPerApplication - 2)
              )
            )

            res <- OfferFinder.findTargetOffers(
              MatchApplication
                .newBuilder()
                .addTarget(Target.newBuilder().setClientId(UUID.randomUUID().toString))
                .build,
              Seq(dealerId),
              Seq.empty[String]
            )
          } yield assert(res.getTargetCount)(equalTo(config.offersPerApplication)) &&
            assert(res.getTargetList.asScala.count(t => t.getClientId == dealerId))(equalTo(1))
        }
      },
      testWithContextProvided { implicit context =>
        testM("Select dealers unless they can not be handled by salesman") {
          for {
            dealerId <- ZIO.effectTotal(UUID.randomUUID().toString)
            _ <- mockUsingMethods(
              returnOffers = Task(
                getDefaultOffers(1, Some(dealerId))
                  ++ getDefaultOffers(1)
              )
            )

            res <- OfferFinder.findTargetOffers(
              MatchApplication.getDefaultInstance,
              Seq.empty[String],
              Seq(dealerId)
            )
          } yield assert(res.getTargetCount)(equalTo(1))
        }
      }
    )
}

object OfferFinderSpecOps {

  import MockitoSupport._

  type TestEnv = PublicApiClient with SchedulerConfigProvider

  def testWithContextProvided[E](
      test: PublicApiClient.Service => ZSpec[OfferFinderEnv, E]): Spec[Any, TestFailure[E], TestSuccess] = {

    val publicApiClientMock = mock[PublicApiClient.Service]

    val conf = ZLayer.succeed(
      SchedulerConfig(
        batchSize = 2,
        offersPerApplication = 3,
        applicationTTL = 7.day,
        dealersLowPriorityDays = 14.day,
        "TEST_TEMPLATE"
      )
    )

    val client = ZLayer.succeed(publicApiClientMock)

    test(publicApiClientMock).provideLayer(conf ++ client)
  }

  def getDefaultOffers(count: Int, dealerId: Option[String] = None): Seq[Offer] =
    for {
      _ <- 1 to count
    } yield genOffer(dealerId)

  def mockUsingMethods(returnOffers: Task[Seq[Offer]])(implicit context: PublicApiClient.Service): Task[Unit] =
    ZIO.effectTotal(when(context.search(?, ?, ?)).thenReturn(returnOffers)).unit

  private def genOffer(dealerId: Option[String]) = {
    val dealer = dealerId match {
      case None => UUID.randomUUID().toString
      case Some(id) => id
    }
    Offer
      .newBuilder()
      .setSalon(Salon.newBuilder().setClientId(dealer))
      .setSection(Section.NEW)
      .setCategory(Category.CARS)
      .build()
  }
}
