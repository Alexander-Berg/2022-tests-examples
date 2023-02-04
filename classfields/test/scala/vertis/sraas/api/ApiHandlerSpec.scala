package vertis.sraas.api

import akka.http.scaladsl.model.StatusCodes
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import org.scalatest.BeforeAndAfter
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport
import vertis.sraas.api.v1.ApiHandlerImpl
import vertis.sraas.api.v1.proto.ApiProtoFormats
import vertis.sraas.exception.FileDescriptorSetNotFound
import vertis.sraas.model.{FileDescriptorSetResponse, MessageNamesResponse, SchemaVersion, VersionsResponse}
import vertis.sraas.model.test.FakeSchemaVersion
import vertis.sraas.service.SchemaRegistryService

import scala.concurrent.Future

/** @author neron
  */
class ApiHandlerSpec extends BaseHandlerSpec with BeforeAndAfter with ApiProtoFormats with ProtobufSupport {

  private val v001 = SchemaVersion(0, 0, 1)
  private val v002 = SchemaVersion(0, 0, 2)
  private val v003 = SchemaVersion(0, 0, 3)
  private val v004 = SchemaVersion(0, 0, 4)
  private val versions = Seq(v003, v002, v001)
  private val fqn1 = "a.b.c.Message1"
  private val fqn2 = "x.y.z.Message2"
  private val fqn3 = "x.y.z.Message3"
  private val messageNames = Seq(fqn1, fqn2)
  private val fds = FileDescriptorSet.newBuilder().build()

  private val service = new SchemaRegistryService[SchemaVersion] {
    override def getFull(versionOpt: Option[SchemaVersion]): Future[FileDescriptorSetResponse[SchemaVersion]] = ???

    override def get(
        fullyQualifiedName: String,
        versionOpt: Option[SchemaVersion]): Future[FileDescriptorSetResponse[SchemaVersion]] =
      if (messageNames.contains(fullyQualifiedName)) {
        Future.successful(
          FileDescriptorSetResponse(versionOpt.getOrElse(FakeSchemaVersion), fullyQualifiedName, fds)
        )
      } else {
        Future.failed(new FileDescriptorSetNotFound(versionOpt.getOrElse(FakeSchemaVersion)))
      }

    override def getVersions: Future[VersionsResponse[SchemaVersion]] =
      Future.successful(VersionsResponse(versions))

    override def getMessageNames(versionOpt: Option[SchemaVersion]): Future[MessageNamesResponse[SchemaVersion]] =
      if (versionOpt.forall(versions.contains)) {
        Future.successful(MessageNamesResponse(v001, messageNames))
      } else {
        Future.failed(new FileDescriptorSetNotFound(versionOpt.getOrElse("none")))
      }

    override def getMessageNamesByOption(
        optionName: String,
        versionOpt: Option[SchemaVersion]): Future[MessageNamesResponse[SchemaVersion]] = ???

    override def getVersionName(version: Option[SchemaVersion]): String = ???
  }

  private val route = seal(new ApiHandlerImpl[SchemaVersion](service))

  "GET /versions" should {
    "return all versions" in {
      val expected = VersionsResponse(versions)

      Get("/versions") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[VersionsResponse[SchemaVersion]] should ===(expected)
      }
    }
  }

  "GET /versions/<version>/message-names" should {
    "return all message names for certain version" in {
      val expected = MessageNamesResponse(v001, messageNames)

      Get(s"/versions/$v001/message-names") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[MessageNamesResponse[SchemaVersion]] should ===(expected)
      }
    }
    "return 404 when fds does not exists" in {
      Get(s"/versions/$v004/message-names") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

  "GET /versions/<version>/descriptors/<fqn>" should {
    "return file descriptor set" in {
      val expected = FileDescriptorSetResponse(v001, fqn1, fds)

      Get(s"/versions/$v001/descriptors/$fqn1") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[FileDescriptorSetResponse[SchemaVersion]] should ===(expected)
      }
    }
    "return 404 when fqn does not exists" in {
      Get(s"/versions/$v001/descriptors/$fqn3") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

}
