package ru.yandex.vertis.general.favorites.api.mappers.test

import general.search.api.SearchOffersRequest
import general.search.model.{Between, Equal, GreaterThan, In, LessThan, RawValue, SearchArea, SearchFilter}
import general.search.model.SearchArea.{Coordinates, Toponyms}
import ru.yandex.vertis.general.favorites.api.mappers.SearchIdMapper
import ru.yandex.vertis.general.search.model.testkit.SearchOffersRequestGen
import zio.test.{assert, checkM, suite, testM, DefaultRunnableSpec, ZSpec}
import zio.test.Assertion.{equalTo, not}

object SearchIdMapperTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("SearchIdMapperTest")(
      testM("different id for different request") {
        checkM(SearchOffersRequestGen.anySearchOffersRequest) { request1 =>
          for {
            id1 <- SearchIdMapper.toSearchId(request1)
            id2 <- SearchIdMapper.toSearchId(request1.withCategoryIds(request1.categoryIds.map(_ + "test")))
          } yield assert(id1)(not(equalTo(id2)))
        }
      },
      testM("different id for same request in different regions") {
        checkM(SearchOffersRequestGen.anySearchOffersRequest) { request1 =>
          for {
            id1 <- SearchIdMapper.toSearchId(
              request1.withArea(SearchArea(SearchArea.Area.Toponyms(Toponyms(region = 1))))
            )
            id2 <- SearchIdMapper.toSearchId(
              request1.withArea(SearchArea(SearchArea.Area.Toponyms(Toponyms(region = 2))))
            )
          } yield assert(id1)(not(equalTo(id2)))
        }
      },
      testM("same id for same request") {
        checkM(SearchOffersRequestGen.anySearchOffersRequest) { request =>
          for {
            id1 <- SearchIdMapper.toSearchId(request)
            id2 <- SearchIdMapper.toSearchId(request)
          } yield assert(id1)(equalTo(id2))
        }
      },
      testM("same id for same request with different fields order 1") {
        val request1 = SearchOffersRequest(
          text = "text",
          categoryIds = Seq("shkaf"),
          area = Some(SearchArea().withCoordinates(Coordinates(1, 2, 3))),
          parameter = Seq(
            SearchFilter().withKey("key1").withEqual(Equal().withValue(RawValue().withString("123"))),
            SearchFilter()
              .withKey("key2")
              .withIn(In().withValue(Seq(RawValue().withString("123"), RawValue().withString("blabla")))),
            SearchFilter().withKey("key3").withEqual(Equal().withValue(RawValue().withBoolean(true)))
          )
        )
        val request2 = SearchOffersRequest(
          text = "text",
          categoryIds = Seq("shkaf"),
          area = Some(SearchArea().withCoordinates(Coordinates(1, 2, 3))),
          parameter = Seq(
            SearchFilter().withKey("key3").withEqual(Equal().withValue(RawValue().withBoolean(true))),
            SearchFilter()
              .withKey("key2")
              .withIn(In().withValue(Seq(RawValue().withString("blabla"), RawValue().withString("123")))),
            SearchFilter().withKey("key1").withEqual(Equal().withValue(RawValue().withString("123")))
          )
        )
        for {
          id1 <- SearchIdMapper.toSearchId(request1)
          id2 <- SearchIdMapper.toSearchId(request2)
        } yield assert(id1)(equalTo(id2))

      },
      testM("same id for same request with different fields order 2") {
        val request1 = SearchOffersRequest(
          text = "text",
          categoryIds = Seq("shkaf"),
          area = Some(SearchArea().withToponyms(Toponyms(1, Seq(3, 2, 1), Seq(3, 2, 1)))),
          parameter = Seq(
            SearchFilter().withKey("key1").withLessThan(LessThan().withValue(RawValue().withDouble(123))),
            SearchFilter().withKey("key1").withGreaterThan(GreaterThan().withValue(RawValue().withString("123"))),
            SearchFilter()
              .withKey("key2")
              .withBetween(
                Between()
                  .withFrom(RawValue().withDouble(123))
                  .withTo(RawValue().withDouble(123))
                  .withOrEqualsFrom(true)
                  .withOrEqualsTo(true)
              )
          )
        )
        val request2 = SearchOffersRequest(
          text = "text",
          categoryIds = Seq("shkaf"),
          area = Some(SearchArea().withToponyms(Toponyms(1, Seq(1, 2, 3), Seq(1, 2, 3)))),
          parameter = Seq(
            SearchFilter()
              .withKey("key2")
              .withBetween(
                Between()
                  .withTo(RawValue().withDouble(123))
                  .withFrom(RawValue().withDouble(123))
                  .withOrEqualsFrom(true)
                  .withOrEqualsTo(true)
              ),
            SearchFilter().withKey("key1").withGreaterThan(GreaterThan().withValue(RawValue().withString("123"))),
            SearchFilter().withKey("key1").withLessThan(LessThan().withValue(RawValue().withDouble(123)))
          )
        )
        for {
          id1 <- SearchIdMapper.toSearchId(request1)
          id2 <- SearchIdMapper.toSearchId(request2)
        } yield assert(id1)(equalTo(id2))

      }
    )
  }
}
