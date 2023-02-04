package ru.yandex.vertis.moderation.service.impl

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import org.junit.Ignore
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer._

import scala.concurrent.ExecutionContext

/**
  * @author potseluev
  */
@Ignore("For manually running")
class S3ServiceImplSpec extends SpecBase {

  implicit private val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private val s3Client: AmazonS3 = {
    val endpoint = new EndpointConfiguration("http://s3.mdst.yandex.net", "us-east-1")
    val credentials =
      new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(
          "yE5Wa4bP8GFc6OpLC296",
          "etS16lPTUJqPYBgbtakRHNgBtQzlSn+UtjvCRMJV"
        )
      )
    AmazonS3ClientBuilder
      .standard()
      .withCredentials(credentials)
      .withEndpointConfiguration(endpoint)
      .build()
  }

  private val requiredPrefix = "moderation"
  private val s3Service = new S3ServiceImpl(s3Client, ec, requiredPrefix)

  "removeByPrefix" should {
    "correctly remove existent objects" in {
      val url = "s3://misc/moderation-flink/flink-savepoints/savepoint-cc55f0-0700f0b823d4"
      s3Service.removeByPrefix(url).futureValue
      s3Service.exists(url).futureValue shouldBe false
    }

    "do nothing if no objects objects do not exist" in {
      val url = s"s3://misc/moderation-flink/${StringGen.next}"
      s3Service.exists(url).futureValue shouldBe false
      s3Service.removeByPrefix(url).futureValue
    }

    "fail if key doesn't have required prefix" in {
      val url = s"s3://misc/${StringGen.next}"
      s3Service.removeByPrefix(url).shouldCompleteWithException[IllegalArgumentException]
    }
  }

  "exists" should {
    "return true if there are several objects with specified prefix" in {
      s3Service
        .exists("s3://misc/moderation-flink/flink-savepoints/savepoint-cc55f0-0700f0b823d4")
        .futureValue shouldBe true
    }

    "return true if there are one object with specified prefix" in {
      s3Service
        .exists(
          "s3://misc/moderation-flink/flink-savepoints/savepoint-cc55f0-0700f0b823d4/a73485d8-6eb6-4179-b93d-19ee258498c3"
        )
        .futureValue shouldBe true
    }

    "return false if there are no objects with specified prefix" in {
      s3Service.exists(s"s3://misc/moderation-flink/${StringGen.next}").futureValue shouldBe false
    }
  }
}
