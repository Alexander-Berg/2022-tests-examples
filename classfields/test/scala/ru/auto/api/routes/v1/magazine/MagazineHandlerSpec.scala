package ru.auto.api.routes.v1.magazine

import org.mockito.Mockito.verify
import ru.auto.api.ApiSpec
import ru.auto.api.managers.magazine.MagazineManager
import ru.auto.api.services.MockedClients
import ru.auto.api.util.Protobuf
import akka.http.scaladsl.model.StatusCodes.OK
import ru.auto.api.ResponseModel.OfferCountResponse
import ru.auto.api.magazine.MagazineResponseModel.MagazineArticleSnippetListResponse
import ru.auto.api.model.ModelGenerators.SessionResultGen
import ru.auto.api.model.{NoSorting, Paging}
import ru.auto.api.model.magazine.ArticlesFilter
import ru.auto.api.model.magazine.ArticlesModelGenerators.ArticleGen

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-07-15.
  */
class MagazineHandlerSpec extends ApiSpec with MockedClients {

  override lazy val magazineManager: MagazineManager = mock[MagazineManager]

  "articles" should {
    "get snippet" in {
      val articleFilter = ArticlesFilter(Some("BMW"), Some("X5"), Some(123L), Seq("cat1", "cat2"))

      val article = ArticleGen.next
      val response = MagazineArticleSnippetListResponse.newBuilder().addArticles(article).build()
      val paging = Paging(1, 10)
      when(magazineManager.getArticlesListing(?, ?, ?)(?)).thenReturnF(response)
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      Get(
        "/1.0/magazine/articles/snippets?category=cat2&category=cat1&mark=BMW&model=X5&" +
          "super_gen_id=123&page=1&page_size=10"
      ) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe OK
            Protobuf.fromJson[MagazineArticleSnippetListResponse](response)
            verify(magazineManager).getArticlesListing(eq(articleFilter), eq(paging), eq(NoSorting))(?)
          }
        }
    }

    "get count" in {
      val articleFilter = ArticlesFilter(Some("BMW"), Some("X5"), Some(123L), Seq("cat1", "cat2"))

      val response = OfferCountResponse.newBuilder().setCount(10).build()
      when(magazineManager.getArticlesCounter(?)(?)).thenReturnF(response)
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      Get(
        "/1.0/magazine/articles/count?category=cat2&category=cat1&mark=BMW&model=X5&" +
          "super_gen_id=123&page=1&page_size=10"
      ) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe OK
            Protobuf.fromJson[OfferCountResponse](response)
            verify(magazineManager).getArticlesCounter(eq(articleFilter))(?)
          }
        }
    }
  }
}
