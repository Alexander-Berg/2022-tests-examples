package ru.auto.cabinet.api.v1

import org.mockito.Mockito._
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import ru.auto.cabinet.AclResponse._
import ru.auto.cabinet.test.TestUtil._
import ru.auto.cabinet.dao.jdbc.UpdateResult
import ru.auto.cabinet.model.CustomerTypes
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport._

class AclHandlerSpec extends FlatSpec with HandlerSpecTemplate {

  private val auth = new SecurityMocks
  import auth._

  private val route = wrapRequestMock(new AclHandler(securityProvider).route)

  private val testGroup =
    Group
      .newBuilder()
      .setId(1L)
      .setName("Access group")
      .build()

  it should "get group by id" in {
    val groupId = 1L

    when(securityProvider.getAccessGroup(eq(groupId))(any()))
      .thenReturnF(testGroup)

    Get(s"/acl/group/$groupId") ~> headers1 ~> route ~> check {
      responseAs[Group] shouldBe testGroup
    }
  }

  it should "get client groups" in {
    val groups =
      GroupsList
        .newBuilder()
        .addGroups(testGroup)
        .build()

    when(
      securityProvider.getCustomerAccessGroups(
        eq(client1Id),
        eq(CustomerTypes.Client))(any()))
      .thenReturnF(groups)

    Get(s"/acl/client/$client1Id/groups") ~> headers1 ~> route ~> check {
      responseAs[GroupsList] shouldBe groups
    }
  }

  it should "put client group" in {
    val group =
      Group
        .newBuilder()
        .setName("Access group")
        .build()

    when(
      securityProvider
        .putCustomerAccessGroup(
          eq(client1Id),
          eq(CustomerTypes.Client),
          eq(group))(any()))
      .thenReturnF(UpdateResult(id = 3L, rowCount = 1))

    when(securityProvider.getAccessGroup(groupId = eq(3L))(any()))
      .thenReturnF(group)

    Put(s"/acl/client/$client1Id/group", group) ~> headers1 ~> route ~> check {
      responseAs[Group] shouldBe group
    }
  }

  it should "delete client group" in {
    val groupId = 3L
    val deleteResult = UpdateResult(groupId, rowCount = 1)

    when(
      securityProvider
        .deleteCustomerAccessGroup(
          eq(client1Id),
          eq(CustomerTypes.Client),
          eq(groupId))(any()))
      .thenReturnF(deleteResult)

    Delete(
      s"/acl/client/$client1Id/group/$groupId") ~> headers1 ~> route ~> check {
      responseAs[UpdateResult] shouldBe deleteResult
    }
  }

  it should "get resources list" in {
    val resources =
      ResourcesList
        .newBuilder()
        .addResources {
          Resource
            .newBuilder()
            .setAlias(ResourceAlias.OFFERS)
            .setAccess(AccessLevel.READ_WRITE)
        }
        .build()

    when(
      securityProvider
        .getAccessResources(eq(client1Id), eq(CustomerTypes.Client))(any()))
      .thenReturnF(resources)

    Get(s"/acl/client/$client1Id/resources") ~> headers1 ~> route ~> check {
      responseAs[ResourcesList] shouldBe resources
    }
  }

}
