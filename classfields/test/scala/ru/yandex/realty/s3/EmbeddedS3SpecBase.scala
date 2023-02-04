package ru.yandex.realty.s3

import com.amazonaws.ClientConfiguration
import com.amazonaws.services.s3.AmazonS3
import ru.yandex.realty.mds.s3.S3ClientFactory
import io.findify.s3mock.S3Mock
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

trait EmbeddedS3SpecBase extends FlatSpec with Matchers with BeforeAndAfterAll {

  private val s3Mock: S3Mock = new S3Mock.Builder().withPort(8001).withInMemoryBackend.build

  private val endpoint = new EndpointConfiguration("http://localhost:8001", "qwer")

  val s3Client: AmazonS3 = S3ClientFactory.buildFromConfig(
    endpoint,
    new AWSStaticCredentialsProvider(new AnonymousAWSCredentials),
    new ClientConfiguration(),
    Some(true)
  )

  override def beforeAll(): Unit = {
    s3Mock.start
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    s3Mock.stop
    super.afterAll()
  }
}
