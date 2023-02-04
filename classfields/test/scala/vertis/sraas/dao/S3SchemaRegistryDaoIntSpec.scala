package vertis.sraas.dao

import com.amazonaws.services.s3.AmazonS3
import com.typesafe.config.ConfigFactory
import common.zio.clients.s3.S3Client.S3Config
import pureconfig.{ConfigFieldMapping, ConfigReader, ConfigSource}
import pureconfig.generic.ProductHint
import pureconfig.generic.semiauto.deriveReader
import vertis.sraas.BaseSpec
import vertis.sraas.client.S3ClientFactory
import vertis.sraas.model.SchemaVersion

import scala.jdk.CollectionConverters._

/** @author neron
  */
class S3SchemaRegistryDaoIntSpec extends BaseSpec {

  case class Config(s3: S3Config, s3Bucket: String)

  implicit val productHint: ProductHint[Config] =
    ProductHint[Config](ConfigFieldMapping.apply {
      case "s3" => "s3"
      case "s3-bucket" => "s3Bucket"
      case other => other
    })

  implicit val s3AuthReader: ConfigReader[Config] = deriveReader[Config]

  private lazy val config =
    ConfigSource
      .fromConfig(
        ConfigFactory
          .load("dao.int-test.conf")
      )
      .loadOrThrow[Config]

  private lazy val bucket = config.s3Bucket

  private lazy val s3Client: AmazonS3 =
    S3ClientFactory(config.s3)

  private lazy val dao =
    new S3SchemaRegistryDao[SchemaVersion](s3Client, ec, config = S3SchemaRegistryDao.Config(bucket))

  "S3SchemaRegistryDao" should {
    "return versions using pagination" in {
      val expected = s3Client.listObjects(bucket).getObjectSummaries.asScala.map(_.getKey) // without pagination
      expected.size should be <= 1000 // it does not make sense otherwise. See listObjects doc
      val actual = dao.listAllKeys(pageSize = 1)
      actual should contain theSameElementsAs expected
    }
    "return all versions" in {
      val allVersions = dao.allVersions().futureValue
      allVersions should not be empty
    }
    "return None for unknown version" in {
      val version = SchemaVersion.parse("v100.100.100")
      dao.get(version).futureValue should ===(None)
    }
    "return proto FileDescriptorSet" in {
      val allVersions = dao.allVersions().futureValue
      allVersions.collect { case v: SchemaVersion => v }.foreach { version =>
        dao.get(version).futureValue shouldBe defined
      }
    }
  }

}
