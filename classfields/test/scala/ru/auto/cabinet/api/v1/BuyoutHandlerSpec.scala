package ru.auto.cabinet.api.v1

import java.util.NoSuchElementException

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Route.seal
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.Outcome
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import ru.auto.cabinet.api.v1.view.BuyoutTargetingMarshaller._
import ru.auto.cabinet.dao.jdbc.{BuyoutTargetingDao, UpdateResult}
import ru.auto.cabinet.model._

import scala.concurrent.Future.{failed, successful}

/** Specs [[BuyoutHandler]]
  */
class BuyoutHandlerSpec extends FixtureAnyFlatSpec with HandlerSpecTemplate {

  private val auth = new SecurityMocks
  import auth._

  private val tgt =
    BuyoutTargeting(0, client1Id, None, None, None, None, None, None, Nil)
  private val dummyFilter = BuyoutFilter(0, "BMW", "2110", 2015, 600000)

  private val dummyDealer =
    BuyoutDealer(0, "Dummy Client", Option("some@email"))
  private def tgtId = tgt.id

  case class FixtureParam(tgtDao: BuyoutTargetingDao, route: Route)

  override protected def withFixture(test: OneArgTest): Outcome = {
    val tgtDao = mock[BuyoutTargetingDao]
    val route: Route = wrapRequestMock(new BuyoutHandler(tgtDao).route)

    when(tgtDao.get(anyLong, anyLong)(any()))
      .thenReturn(failed(new NoSuchElementException))
    when(tgtDao.get(eq(tgtId), eq(tgt.clientId))(any()))
      .thenReturn(successful(tgt))
    when(tgtDao.create(any[BuyoutTargeting]())(any()))
      .thenReturn(successful(tgt))
    when(tgtDao.update(any[BuyoutTargeting]())(any()))
      .thenReturn(failed(new NoSuchElementException))

    when(tgtDao.update(argThat(new ArgumentMatcher[BuyoutTargeting] {

      override def matches(arg: BuyoutTargeting): Boolean =
        arg.id == tgtId && arg.clientId == tgt.clientId
    }))(any())).thenReturn(successful(UpdateResult(tgtId, 1)))

    when(tgtDao.remove(anyLong, anyLong)(any()))
      .thenReturn(failed(new NoSuchElementException))
    when(tgtDao.remove(eq(tgtId), eq(tgt.clientId))(any()))
      .thenReturn(successful(UpdateResult(tgtId, 1)))

    when(tgtDao.list(anyLong)(any()))
      .thenReturn(failed(new NoSuchElementException))
    when(tgtDao.list(eq(tgt.clientId))(any())).thenReturn(successful(Seq(tgt)))
    when(tgtDao.search(any[BuyoutFilter]())(any()))
      .thenReturn(successful(Seq(dummyDealer)))

    test(FixtureParam(tgtDao, route))
  }

  "Buyout targeting API" should "respond for correct authorization (not slash-ended)" in {
    f =>
      Get(
        s"/buyout/client/$client1Id/targeting/$tgtId") ~> headers1 ~> f.route ~> check {
        responseAs[BuyoutTargeting] should be(tgt)
      }
  }

  it should "respond for correct authorization" in { f =>
    Get(
      s"/buyout/client/$client1Id/targeting/$tgtId/") ~> headers1 ~> f.route ~> check {
      responseAs[BuyoutTargeting] should be(tgt)
    }
  }

  it should "not find targeting for the wrong client" in { f =>
    Get(s"/buyout/client/$client2Id/targeting/$tgtId/") ~> headers2 ~> seal(
      f.route) ~> check {
      status should be(NotFound)
    }
  }

  it should "create with correct authorization (not slash-ended)" in { f =>
    Post(
      s"/buyout/client/$client1Id/targetings",
      tgt) ~> headers1 ~> f.route ~> check {
      responseAs[BuyoutTargeting] should be(tgt)
    }
  }

  it should "create with correct authorization" in { f =>
    Post(
      s"/buyout/client/$client1Id/targetings/",
      tgt) ~> headers1 ~> f.route ~> check {
      responseAs[BuyoutTargeting] should be(tgt)
    }
  }

  it should "update with correct authorization (not slash-ended)" in { f =>
    Put(
      s"/buyout/client/$client1Id/targeting/$tgtId",
      tgt) ~> headers1 ~> f.route ~> check {
      status should be(OK)
    }
  }

  it should "update with correct authorization" in { f =>
    Put(
      s"/buyout/client/$client1Id/targeting/$tgtId/",
      tgt) ~> headers1 ~> f.route ~> check {
      status should be(OK)
    }
  }

  it should "not find targeting for update with the wrong client" in { f =>
    Put(
      s"/buyout/client/$client2Id/targeting/$tgtId/",
      tgt) ~> headers2 ~> seal(f.route) ~> check {
      status should be(NotFound)
    }
  }

  it should "remove with correct authorization (not slash-ended)" in { f =>
    Delete(
      s"/buyout/client/$client1Id/targeting/$tgtId") ~> headers1 ~> f.route ~> check {
      status should be(OK)
    }
  }

  it should "remove with correct authorization" in { f =>
    Delete(
      s"/buyout/client/$client1Id/targeting/$tgtId/") ~> headers1 ~> f.route ~> check {
      status should be(OK)
    }
  }

  it should "not find targeting for removal with the wrong client" in { f =>
    Delete(s"/buyout/client/$client2Id/targeting/$tgtId/") ~> headers2 ~> seal(
      f.route) ~> check {
      status should be(NotFound)
    }
  }

  it should "list targetings for correct authorization (not slash-ended)" in {
    f =>
      Get(
        s"/buyout/client/$client1Id/targetings") ~> headers1 ~> f.route ~> check {
        responseAs[Seq[BuyoutTargeting]] should be(Seq(tgt))
      }
  }

  it should "list targetings for correct authorization" in { f =>
    Get(
      s"/buyout/client/$client1Id/targetings/") ~> headers1 ~> f.route ~> check {
      responseAs[Seq[BuyoutTargeting]] should be(Seq(tgt))
    }
  }

  it should "search targetings for car sale (not slash-ended)" in { f =>
    Post(
      s"/buyout/targetings",
      dummyFilter) ~> requestHeader ~> f.route ~> check {
      responseAs[Seq[BuyoutDealer]] should be(Seq(dummyDealer))
    }
  }

  it should "search targetings for car sale" in { f =>
    Post(
      s"/buyout/targetings/",
      dummyFilter) ~> requestHeader ~> f.route ~> check {
      responseAs[Seq[BuyoutDealer]] should be(Seq(dummyDealer))
    }
  }

}
