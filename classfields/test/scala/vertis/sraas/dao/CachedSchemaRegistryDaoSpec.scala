package vertis.sraas.dao

import com.google.protobuf.DescriptorProtos.{FileDescriptorProto, FileDescriptorSet}
import ru.yandex.vertis.ops.test.TestOperationalSupport
import vertis.sraas.BaseSpec
import vertis.sraas.model.SchemaVersion

import scala.concurrent.duration._

/** @author neron, kusaeva
  */
class CachedSchemaRegistryDaoSpec extends BaseSpec with TestOperationalSupport {

  private val fileProto =
    FileDescriptorProto
      .newBuilder()
      .setName("test.proto")
      .setPackage("test_package")

  private val version = SchemaVersion(0, 0, 1)
  private val fds = FileDescriptorSet.newBuilder().build()
  private val otherFds = FileDescriptorSet.newBuilder().addFile(fileProto).build()

  "CachedSchemaRegistryDao" should {
    "load existing schema from cache" in {
      val mockedDao = new TestSchemaRegistryDao[SchemaVersion]
      val cachedDao = new CachedSchemaRegistryDao(
        mockedDao,
        prometheusRegistry
      )
      mockedDao.set(version, fds).futureValue
      cachedDao.get(version).futureValue shouldBe Some(fds)

      mockedDao.remove(version).futureValue
      mockedDao.get(version).futureValue shouldBe None
      cachedDao.get(version).futureValue shouldBe Some(fds)
    }

    "reload obsolete 'absence' of schema" in {
      val mockedDao = new TestSchemaRegistryDao[SchemaVersion]
      val cachedDao = new CachedSchemaRegistryDao(
        mockedDao,
        prometheusRegistry,
        versionsExpirationAfterWrite = 60.millis
      )
      cachedDao.get(version).futureValue shouldBe None
      mockedDao.set(version, fds).futureValue

      eventually {
        cachedDao.get(version).futureValue shouldBe Some(fds)
      }
    }
  }

}
