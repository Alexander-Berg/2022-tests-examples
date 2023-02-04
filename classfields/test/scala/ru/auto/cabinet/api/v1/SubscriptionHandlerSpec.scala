package ru.auto.cabinet.api.v1

import java.sql.{BatchUpdateException, SQLIntegrityConstraintViolationException}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Route.seal
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito._
import org.scalatest.Outcome
import org.scalatest.flatspec.FixtureAnyFlatSpec
import ru.auto.cabinet.test.TestUtil.RichOngoingStub
import org.scalatestplus.mockito.MockitoSugar.mock
import ru.auto.cabinet.api.v1.view.SubscriptionMarshaller._
import ru.auto.cabinet.api.v1.view.{SubscriptionProto, SubscriptionSeqProto}
import ru.auto.cabinet.dao.jdbc.{SubscriptionDao, UpdateResult}
import ru.auto.cabinet.model._

/** Specs [[SubscriptionHandler]]
  */
class SubscriptionHandlerSpec
    extends FixtureAnyFlatSpec
    with HandlerSpecTemplate {

  private val auth = new SecurityMocks
  import auth._

  private val dummy =
    ClientSubscription(
      1,
      client1Id,
      SubscriptionCategory.info,
      "info@provider.me")

  private val agentDummy =
    ClientSubscription(2, agencyClient1Id, dummy.category, "info@agency")

  private val seqDummy = Seq(
    ClientSubscription(
      3,
      client1Id,
      SubscriptionCategory.cabinet,
      "CaBiNeT@provider.me"),
    ClientSubscription(
      4,
      client1Id,
      SubscriptionCategory.money,
      "money@provider.me"),
    ClientSubscription(
      5,
      client1Id,
      SubscriptionCategory.legal,
      "legal@provider.me"),
    dummy
  )
  private val protoDummy = makeBatch(seqDummy)

  private val withEmptyEmail =
    ClientSubscription(6, client1Id, SubscriptionCategory.info, "")

  private val withNoAtEmail =
    ClientSubscription(7, client1Id, SubscriptionCategory.info, "info")

  private val withNoDomainEmail =
    ClientSubscription(8, client1Id, SubscriptionCategory.info, "info@me.")

  private val withLocalhostEmail =
    ClientSubscription(
      9,
      client1Id,
      SubscriptionCategory.info,
      "info@localhost")

  private val withValidGroupEmail =
    ClientSubscription(
      10,
      client1Id,
      SubscriptionCategory.info,
      "username@autoservice.group")

  private val withCyrillicSymbolsEmail =
    ClientSubscription(
      11,
      client1Id,
      SubscriptionCategory.info,
      "пользователь@autoservice.group")

  private val withIncorrectEmailStructure =
    ClientSubscription(
      12,
      client1Id,
      SubscriptionCategory.info,
      "user@autoservice.group <additional info>")

  private val withEmptyEmailSpaces =
    ClientSubscription(13, client1Id, SubscriptionCategory.info, "     ")

  private val withEmptyEmailBatch = makeBatch(withEmptyEmail +: seqDummy)
  private val withNoAtEmailBatch = makeBatch(withNoAtEmail +: seqDummy)
  private val withNoDomainEmailBatch = makeBatch(withNoDomainEmail +: seqDummy)

  private val withLocalhostEmailBatch = makeBatch(
    withLocalhostEmail +: seqDummy)

  private def makeBatch(subscriptions: Seq[ClientSubscription]) =
    SubscriptionSeqProto {
      subscriptions.map { s =>
        SubscriptionProto(s.category.key, s.emailAddress)
      }
    }

  private def category = dummy.category.key

  case class FixtureParam(
      subsDao: SubscriptionDao,
      dupDao: SubscriptionDao,
      route: Route,
      dupRoute: Route
  )

  override protected def withFixture(test: OneArgTest): Outcome = {
    val subsDao = mock[SubscriptionDao]
    val dupDao = mock[SubscriptionDao]

    val route = wrapRequestMock(new SubscriptionHandler(subsDao).route)
    val dupRoute = wrapRequestMock(new SubscriptionHandler(dupDao).route)

    when(subsDao.get(anyLong, anyLong)(any()))
      .thenThrowF(new NoSuchElementException)
    when(subsDao.get(eq(dummy.id), eq(dummy.clientId))(any()))
      .thenReturnF(dummy)
    when(subsDao.list(anyLong)(any()))
      .thenThrowF(new NoSuchElementException)
    when(subsDao.list(eq(dummy.clientId))(any()))
      .thenReturnF(seqDummy)

    when(subsDao.list(eq(client2Id), eq(dummy.category))(any()))
      .thenThrowF(new NoSuchElementException)

    when(subsDao.list(eq(dummy.clientId), eq(dummy.category))(any()))
      .thenReturnF(Seq(dummy))

    when(subsDao.list(customerId = any(), category = eq(dummy.category))(any()))
      .thenThrowF(new NoSuchElementException)

    when(
      subsDao.list(customerId = eq(agent1), category = eq(dummy.category))(
        any()))
      .thenReturnF(Seq(dummy, agentDummy))

    when(subsDao.createOrUpdate(anyLong, any())(any()))
      .thenReturnF(UpdateResult(1, 1))

    when(subsDao.create(any())(any()))
      .thenReturnF(dummy)
    when(subsDao.update(any())(any()))
      .thenReturnF(UpdateResult(1, 1))

    when(subsDao.removeAll(anyLong)(any()))
      .thenReturnF(UpdateResult(-1, 0))

    when(subsDao.removeAll(eq(client1Id))(any()))
      .thenReturnF(UpdateResult(client1Id, seqDummy.size))

    when(
      subsDao.removeByCategory(eq(dummy.clientId), eq(dummy.category))(any()))
      .thenReturnF(UpdateResult(dummy.clientId, 1))

    when(subsDao.remove(anyLong, anyLong)(any()))
      .thenReturnF(UpdateResult(-1, 0))

    when(subsDao.remove(eq(dummy.id), eq(dummy.clientId))(any()))
      .thenReturnF(UpdateResult(dummy.id, 1))

    when(dupDao.create(any())(any()))
      .thenThrowF(new SQLIntegrityConstraintViolationException)

    when(dupDao.createOrUpdate(anyLong, any())(any()))
      .thenThrowF(new BatchUpdateException)

    test(FixtureParam(subsDao, dupDao, route, dupRoute))
  }

  "Email Subscription API" should "get client subscriptions (not slash-ended)" in {
    f =>
      Get(s"/subscriptions/client/$client1Id") ~> headers1 ~> f.route ~> check {
        responseAs[Seq[ClientSubscription]] should be(seqDummy)
      }
  }

  it should "get client subscriptions" in { f =>
    Get(s"/subscriptions/client/$client1Id/") ~> headers1 ~> f.route ~> check {
      responseAs[Seq[ClientSubscription]] should be(seqDummy)
    }
  }

  it should "not found subscription for unknown client" in { f =>
    Get(s"/subscriptions/client/$client2Id/") ~> adminHeaders ~> seal(
      f.route) ~> check {
      status should be(NotFound)
    }
  }

  it should "get single subscription (not slash-ended)" in { f =>
    Get(
      s"/subscription/${dummy.id}/client/$client1Id") ~> headers1 ~> f.route ~> check {
      responseAs[ClientSubscription] should be(dummy)
    }
  }

  it should "get single subscription" in { f =>
    Get(
      s"/subscription/${dummy.id}/client/$client1Id/") ~> headers1 ~> f.route ~> check {
      responseAs[ClientSubscription] should be(dummy)
    }
  }

  it should "not found single subscription for unknown client" in { f =>
    Get(
      s"/subscription/${dummy.id}/client/$client2Id/") ~> adminHeaders ~> seal(
      f.route) ~> check {
      status should be(NotFound)
    }
  }

  it should "get subscriptions by category (not slash-ended)" in { f =>
    Get(
      s"/subscriptions/client/$client1Id/category/$category") ~> headers1 ~> f.route ~> check {
      responseAs[Seq[ClientSubscription]] should be(Seq(dummy))
    }
  }

  it should "get subscriptions by category" in { f =>
    Get(
      s"/subscriptions/client/$client1Id/category/$category/") ~> headers1 ~> f.route ~> check {
      responseAs[Seq[ClientSubscription]] should be(Seq(dummy))
    }
  }

  it should "not found subscriptions by category for unknown client" in { f =>
    Get(
      s"/subscriptions/client/$client2Id/category/$category/") ~> adminHeaders ~> seal(
      f.route) ~> check {
      status should be(NotFound)
    }
  }

  it should "get subscriptions for customer (not slash-ended)" in { f =>
    Get(
      s"/subscriptions/agency/$client1Id/client/$agencyClient1Id/category/$category") ~>
      headers1 ~> f.route ~> check {
        responseAs[Seq[ClientSubscription]] should be(Seq(dummy, agentDummy))
      }
  }

  it should "get subscriptions for customer" in { f =>
    Get(
      s"/subscriptions/agency/$client1Id/client/$agencyClient1Id/category/$category/") ~>
      headers1 ~> f.route ~> check {
        responseAs[Seq[ClientSubscription]] should be(Seq(dummy, agentDummy))
      }
  }

  it should "not found subscription for unknown customer" in { f =>
    Get(
      s"/subscriptions/agency/$client2Id/client/$agencyClient2Id/category/$category/") ~>
      headers2 ~> seal(f.route) ~> check {
        status should be(NotFound)
      }
  }

  it should "create or update batch client subscriptions (not slash-ended)" in {
    f =>
      Post(
        s"/subscriptions/client/$client1Id",
        protoDummy) ~> headers1 ~> f.route ~> check {
        status should be(OK)
      }
  }

  it should "create or update batch client subscriptions" in { f =>
    Post(
      s"/subscriptions/client/$client1Id/",
      protoDummy) ~> headers1 ~> f.route ~> check {
      status should be(OK)
    }
  }

  it should "forbid batch creation of duplicate subscriptions" in { f =>
    Post(s"/subscriptions/client/$client1Id/", protoDummy) ~> headers1 ~> seal(
      f.dupRoute) ~> check {
      status should be(Conflict)
    }
  }

  it should "forbid batch creation of subscriptions with empty email in batch" in {
    f =>
      Post(
        s"/subscriptions/client/$client1Id",
        withEmptyEmailBatch) ~> headers1 ~> seal(f.route) ~> check {
        status should be(BadRequest)
      }
  }

  it should "forbid batch creation of subscriptions with no-at email in batch" in {
    f =>
      Post(
        s"/subscriptions/client/$client1Id",
        withNoAtEmailBatch) ~> headers1 ~> seal(f.route) ~> check {
        status should be(BadRequest)
      }
  }

  it should "forbid batch creation of subscriptions with no-domain email in batch" in {
    f =>
      Post(
        s"/subscriptions/client/$client1Id",
        withNoDomainEmailBatch) ~> headers1 ~> seal(f.route) ~> check {
        status should be(BadRequest)
      }
  }

  it should "forbid batch creation of subscriptions with localhost email in batch" in {
    f =>
      Post(
        s"/subscriptions/client/$client1Id",
        withLocalhostEmailBatch) ~> headers1 ~> seal(f.route) ~> check {
        status should be(BadRequest)
      }
  }

  it should "create single subscription (not slash-ended)" in { f =>
    Post(
      s"/subscription/client/$client1Id",
      dummy) ~> headers1 ~> f.route ~> check {
      responseAs[ClientSubscription] should be(dummy)
    }
  }

  it should "create single subscription" in { f =>
    Post(
      s"/subscription/client/$client1Id/",
      dummy) ~> headers1 ~> f.route ~> check {
      responseAs[ClientSubscription] should be(dummy)
    }
  }

  it should "forbid duplicate subscriptions" in { f =>
    Post(s"/subscription/client/$client1Id/", dummy) ~> headers1 ~> seal(
      f.dupRoute) ~> check {
      status should be(Conflict)
    }
  }

  it should "forbid creation of single subscription with empty email" in { f =>
    Post(
      s"/subscription/client/$client1Id",
      withEmptyEmail) ~> headers1 ~> seal(f.route) ~> check {
      status should be(BadRequest)
    }
  }

  it should "forbid creation of single subscription with no-at email" in { f =>
    Post(s"/subscription/client/$client1Id", withNoAtEmail) ~> headers1 ~> seal(
      f.route) ~> check {
      status should be(BadRequest)
    }
  }

  it should "forbid creation of single subscription with no-domain email" in {
    f =>
      Post(
        s"/subscription/client/$client1Id",
        withNoDomainEmail) ~> headers1 ~> seal(f.route) ~> check {
        status should be(BadRequest)
      }
  }

  it should "forbid creation of single subscription with localhost email" in {
    f =>
      Post(
        s"/subscription/client/$client1Id",
        withLocalhostEmail) ~> headers1 ~> seal(f.route) ~> check {
        status should be(BadRequest)
      }
  }

  it should "forbid creation of single subscription with email which contains cyrillic symbols" in {
    f =>
      Post(
        s"/subscription/client/$client1Id",
        withCyrillicSymbolsEmail) ~> headers1 ~> seal(f.route) ~> check {
        status should be(BadRequest)
      }
  }

  it should "forbid creation of single subscription with email with incorrect structure" in {
    f =>
      Post(
        s"/subscription/client/$client1Id",
        withIncorrectEmailStructure) ~> headers1 ~> seal(f.route) ~> check {
        status should be(BadRequest)
      }
  }

  it should "forbid creation of single subscription with email which consists of spaces" in {
    f =>
      Post(
        s"/subscription/client/$client1Id",
        withEmptyEmailSpaces) ~> headers1 ~> seal(f.route) ~> check {
        status should be(BadRequest)
      }
  }

  it should "create subscription with .group email" in { f =>
    Post(
      s"/subscription/client/$client1Id",
      withValidGroupEmail) ~> headers1 ~> seal(f.route) ~> check {
      status shouldBe OK
      responseAs[ClientSubscription] should be(dummy)
    }
  }

  it should "update single subscriptions (not slash-ended)" in { f =>
    Put(
      s"/subscription/${dummy.id}/client/$client1Id",
      dummy) ~> headers1 ~> f.route ~> check {
      status should be(OK)
    }
  }

  it should "update single subscriptions" in { f =>
    Put(
      s"/subscription/${dummy.id}/client/$client1Id/",
      dummy) ~> headers1 ~> f.route ~> check {
      status should be(OK)
    }
  }

  it should "forbid update of single subscription with empty email" in { f =>
    Put(
      s"/subscription/${dummy.id}/client/$client1Id",
      withEmptyEmail) ~> headers1 ~> seal(f.route) ~> check {
      status should be(BadRequest)
    }
  }

  it should "forbid update of single subscription with no-at email" in { f =>
    Put(
      s"/subscription/${dummy.id}/client/$client1Id",
      withNoAtEmail) ~> headers1 ~> seal(f.route) ~> check {
      status should be(BadRequest)
    }
  }

  it should "forbid update of single subscription with no-domain email" in {
    f =>
      Put(
        s"/subscription/${dummy.id}/client/$client1Id",
        withNoDomainEmail) ~> headers1 ~> seal(f.route) ~> check {
        status should be(BadRequest)
      }
  }

  it should "forbid update of single subscription with localhost email" in {
    f =>
      Put(
        s"/subscription/${dummy.id}/client/$client1Id",
        withLocalhostEmail) ~> headers1 ~> seal(f.route) ~> check {
        status should be(BadRequest)
      }
  }

  it should "remove batch client subscriptions (not slash-ended)" in { f =>
    Delete(
      s"/subscriptions/client/$client1Id") ~> headers1 ~> f.route ~> check {
      status should be(OK)
    }
  }

  it should "remove batch client subscriptions" in { f =>
    Delete(
      s"/subscriptions/client/$client1Id/") ~> headers1 ~> f.route ~> check {
      status should be(OK)
    }
  }

  it should "remove subscriptions by category (not slash-ended)" in { f =>
    Delete(
      s"/subscriptions/client/$client1Id/category/$category") ~> headers1 ~> f.route ~> check {
      status should be(OK)
    }
  }

  it should "remove subscriptions by category" in { f =>
    Delete(
      s"/subscriptions/client/$client1Id/category/$category/") ~> headers1 ~> f.route ~> check {
      status should be(OK)
    }
  }

  it should "remove single subscriptions (not slash-ended)" in { f =>
    Delete(
      s"/subscription/${dummy.id}/client/$client1Id") ~> headers1 ~> f.route ~> check {
      status should be(OK)
    }
  }

  it should "remove single subscriptions" in { f =>
    Delete(
      s"/subscription/${dummy.id}/client/$client1Id/") ~> headers1 ~> f.route ~> check {
      status should be(OK)
    }
  }
}
