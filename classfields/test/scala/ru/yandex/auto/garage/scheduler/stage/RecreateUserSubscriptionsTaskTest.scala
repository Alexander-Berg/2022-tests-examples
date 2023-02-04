package ru.yandex.auto.garage.scheduler.stage

import io.opentracing.noop.{NoopTracer, NoopTracerFactory}
import org.mockito.Mockito.{reset, times, verify, verifyNoInteractions}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.garage.GarageApiModel.CardTypeInfo.CardType
import ru.yandex.auto.garage.dao.CardsService
import ru.yandex.auto.garage.dao.cards.CardsTableRow
import ru.yandex.auto.garage.managers.SubscriptionsManager
import ru.yandex.auto.garage.utils.features.GarageFeaturesRegistry
import ru.yandex.auto.vin.decoder.amazon.S3Storage
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.GarageCard
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.scheduler.model.Payload.Async

import java.io.File
import java.nio.file.Files
import java.time.Instant
import scala.concurrent.Future

class RecreateUserSubscriptionsTaskTest
  extends AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfter
  with ScalaFutures {

  implicit override val patienceConfig = PatienceConfig(Span(5, Seconds), Span(10, Millis))

  private val cardsService = mock[CardsService]
  private val subscriptionsManager = mock[SubscriptionsManager]
  private val s3 = mock[S3Storage]
  private val features = mock[GarageFeaturesRegistry]

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit private val tracer: NoopTracer = NoopTracerFactory.create()

  private val data =
    """user:1
        |user:2
        |user:3
        |user:4
        |user:5
        |user:6
        |user:7
        |user:8
        |user:9
        |""".stripMargin

  private val cardsTableRow = CardsTableRow(
    123L,
    "user:123",
    None,
    None,
    Instant.now(),
    GarageCard.Status.ACTIVE,
    CardType.CURRENT_CAR,
    GarageCard.newBuilder().build(),
    None,
    None,
    None,
    Instant.ofEpochMilli(System.currentTimeMillis())
  )

  before {
    reset(cardsService)
    reset(subscriptionsManager)
    reset(s3)
    reset(features)
  }

  "RecreateUserSubscriptionsTask" should {

    "not process finished file" in {

      when(features.EnableRecreateUserSubscriptionsTask).thenReturn(Feature("enabled", _ => true))
      when(features.RecreateUserSubscriptionsTaskSource).thenReturn(Feature("source", _ => "some path"))
      when(features.RecreateUserSubscriptionsTaskIdx).thenReturn(Feature("idx", _ => 9))
      when(features.RecreateUserSubscriptionsTaskBatchSize).thenReturn(Feature("idx", _ => 1))

      when(s3.download(eq("some path"), ?)(?)).thenAnswer(inv => {
        val file = inv.getArgument(1).asInstanceOf[File]
        Files.write(file.toPath, data.getBytes())
        ()
      })

      val task = new RecreateUserSubscriptionsTask(cardsService, subscriptionsManager, s3, features)

      task.task.payload.asInstanceOf[Async].run().futureValue

      verifyNoInteractions(cardsService, subscriptionsManager)

    }

    "process file" in {

      when(features.EnableRecreateUserSubscriptionsTask).thenReturn(Feature("enabled", _ => true))
      when(features.RecreateUserSubscriptionsTaskSource).thenReturn(Feature("source", _ => "some path"))
      when(features.RecreateUserSubscriptionsTaskIdx).thenReturn(Feature("idx", _ => 0))
      when(features.RecreateUserSubscriptionsTaskBatchSize).thenReturn(Feature("idx", _ => 10))

      when(s3.download(eq("some path"), ?)(?)).thenAnswer(inv => {
        val file = inv.getArgument(1).asInstanceOf[File]
        Files.write(file.toPath, data.getBytes())
        ()
      })

      when(cardsService.getUserCards(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future(List(cardsTableRow)))
      when(subscriptionsManager.resubscribe(?, ?)(?)).thenReturn(Future(List("tag")))
      when(features.updateFeature[Int](?, ?)(?)).thenReturn(Future.unit)

      val task = new RecreateUserSubscriptionsTask(cardsService, subscriptionsManager, s3, features)

      task.task.payload.asInstanceOf[Async].run().futureValue

      verify(cardsService, times(9)).getUserCards(?, ?, ?, ?, ?, ?)(?)
      verify(subscriptionsManager, times(9)).resubscribe(?, ?)(?)
      verify(features, times(1)).updateFeature[Int](?, ?)(?)

    }

  }

}
