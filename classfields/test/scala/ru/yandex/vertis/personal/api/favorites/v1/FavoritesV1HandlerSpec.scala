package ru.yandex.vertis.personal.api.favorites.v1

import akka.http.scaladsl.model.StatusCodes
import ru.yandex.vertis.personal.ServiceRegistry
import ru.yandex.vertis.personal.api.favorites.backend.MockFavoritesBackend
import ru.yandex.vertis.personal.api.favorites.v1.model.GetResponse
import ru.yandex.vertis.personal.favorites.FavoritesBackend2
import ru.yandex.vertis.personal.model.{Domains, Services, UserRef}
import ru.yandex.vertis.personal.util.HandlerSpec

/**
  * Specs on 1.0 favorite API
  *
  * @author dimas
  */
class FavoritesV1HandlerSpec extends HandlerSpec {
  val version = "1.0"
  val service = Services.Realty
  val domain = Domains.Offers

  val favoritesBackend = new MockFavoritesBackend(service, domain)

  val registry = new ServiceRegistry[FavoritesBackend2]
  registry.register(favoritesBackend)
  val backend = new FavoritesV1Backend(registry)

  val user = UserRef("uid:123")

  val route = sealRoute(new FavoritesV1Handler(service, user, backend).routes)

  "FavoritesV1Handler" should {
    val item = "item1"
    val items = Seq("a", "b", "c")

    "create user's favorite" in {
      Post(s"/$item") ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "respond conflict in repeated favorite creation" in {
      Post(s"/$item") ~> route ~> check {
        status shouldBe StatusCodes.Conflict
      }
    }

    "merge repeated favorite creation" in {
      Put(s"/$item") ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "get created item" in {
      Get() ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[GetResponse] shouldBe GetResponse.from(
          service,
          version,
          user,
          Seq(item)
        )
      }
    }

    "merge several items" in {
      Put(s"/${items.mkString(",")}") ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "get created items" in {
      Get() ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[GetResponse] shouldBe GetResponse.from(
          service,
          version,
          user,
          Seq(item) ++ items
        )
      }
    }

    "check item existence" in {
      Get(s"/$item") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[GetResponse] shouldBe GetResponse.from(
          service,
          version,
          user,
          Seq(item)
        )
      }
      Get(s"/not-exist-item") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[GetResponse] shouldBe GetResponse.from(
          service,
          version,
          user,
          Seq.empty
        )
      }
    }

    "delete item" in {
      Delete(s"/$item") ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
      Get(s"/$item") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[GetResponse] shouldBe GetResponse.from(
          service,
          version,
          user,
          Seq.empty
        )
      }
    }

    "delete several items" in {
      Delete(s"/${items.mkString(",")}") ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
      Get(s"/${items.mkString(",")}") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[GetResponse] shouldBe GetResponse.from(
          service,
          version,
          user,
          Seq.empty
        )
      }
    }
  }
}
