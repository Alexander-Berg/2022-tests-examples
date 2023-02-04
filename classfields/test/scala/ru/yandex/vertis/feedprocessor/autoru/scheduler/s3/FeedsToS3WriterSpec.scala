package ru.yandex.vertis.feedprocessor.autoru.scheduler.s3

import com.typesafe.config.ConfigFactory
import org.scalatest.Ignore
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.ApiOfferModel.Multiposting.Classified.ClassifiedName
import ru.yandex.vertis.feedprocessor.AsyncWordSpecBase
import ru.yandex.vertis.s3edr.core.s3.{S3Auth, S3ClientFactory, S3Settings}
import zio.blocking.effectBlocking

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration._

// чтобы не зависеть от состояния внешней инфрастуктуры на CI
@Ignore
class FeedsToS3WriterSpec extends AsyncWordSpecBase {

  val config = ConfigFactory.defaultApplication()
  val bucket = "feedprocessor-test"

  val s3Settings = S3Settings(
    url = "http://s3.mdst.yandex.net",
    auth = S3Auth(
      key = "",
      secret = ""
    ),
    bucket = bucket,
    region = "yndx",
    connectionTimeout = FiniteDuration(5000, TimeUnit.MILLISECONDS),
    requestTimeout = FiniteDuration(60000, TimeUnit.MILLISECONDS),
    numRetries = 3,
    maxConnections = 5
  )

  val client = S3ClientFactory(s3Settings)
  val writer = FeedsToS3Writer(client, bucket)

  "FeedsToS3Writer" should {
    "write feed to s3 correctly" in {
      for {
        _ <- writer.write("Testing s3 record".getBytes, clientId = 20101L, Category.CARS, Some(ClassifiedName.AUTORU))
        data <- readS3Object(clientId = 20101L, Category.CARS, Some(ClassifiedName.AUTORU))
        _ <- writer.write("Overwrite s3 record".getBytes, clientId = 20101L, Category.CARS, Some(ClassifiedName.AUTORU))
        overwrittenData <- readS3Object(clientId = 20101L, Category.CARS, Some(ClassifiedName.AUTORU))
      } yield {
        new String(data, StandardCharsets.UTF_8) shouldBe "Testing s3 record"
        new String(overwrittenData, StandardCharsets.UTF_8) shouldBe "Overwrite s3 record"
      }
    }
  }

  private def readS3Object(
      clientId: Long,
      category: Category,
      classified: Option[ClassifiedName]): Future[Array[Byte]] = {
    Future {
      val key =
        s"$clientId-${category.name.toLowerCase}${classified.map(c => s"-${c.name.toLowerCase}").getOrElse("")}"

      zio.Runtime.default.unsafeRun(
        effectBlocking(client.getObject(bucket, s"$key.xml").getObjectContent)
          .flatMap { is =>
            zio.ZIO.effect {
              val buffer = new Array[Byte](is.available()) // done intentionally for simplicity
              is.read(buffer)
              buffer
            }
          }
      )
    }
  }

}
