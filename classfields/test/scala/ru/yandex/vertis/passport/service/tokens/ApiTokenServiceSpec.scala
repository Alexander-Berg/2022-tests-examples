package ru.yandex.vertis.passport.service.tokens

import org.mockito.Mockito._
import org.scalatest.{OptionValues, WordSpec}
import ru.yandex.passport.model.api.ApiModel.ApiTokenHistoryListRequest
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.dao.{ApiTokenCache, ApiTokenDao}
import ru.yandex.vertis.passport.model.tokens.ApiTokenFormats.ApiTokenWriter
import ru.yandex.vertis.passport.test.ModelGenerators.ApiTokenHistoryRowGen
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.util.tokens.ApiTokenGenerator

import scala.collection.JavaConverters._
import scala.concurrent.Future

class ApiTokenServiceSpec extends WordSpec with SpecBase with MockitoSupport with OptionValues {

  import scala.concurrent.ExecutionContext.Implicits.global

  class Context {
    val tokenDao: ApiTokenDao = mock[ApiTokenDao]
    val tokenCache: ApiTokenCache = mock[ApiTokenCache]
    val apiTokenGenerator: ApiTokenGenerator = new ApiTokenGenerator {}

    val tokenService = new ApiTokenServiceImpl(tokenDao, tokenCache, apiTokenGenerator)
  }

  def withContext(f: Context => Unit): Unit = {
    f(new Context)
  }

  "ApiTokenService" should {
    "create new token" in withContext { ctx =>
      import ctx.{eq => _, _}
      val params = ModelGenerators.ApiTokenCreateParamsGen.next

      when(tokenDao.insert(?)(?)).thenReturn(Future.unit)
      when(tokenCache.upsert(?)(?)).thenReturn(Future.unit)
      val res = tokenService.createToken(params).futureValue

      res.getToken.getPayload shouldBe params.getPayload
      res.getToken.getId should startWith(params.getName)

      verify(tokenDao).insert(?)(?)
      verify(tokenCache).upsert(?)(?)
    }

    "create new token with empty requester" in withContext { ctx =>
      import ctx.{eq => _, _}
      val params = ModelGenerators.ApiTokenCreateParamsGen.next.toBuilder.clearRequester().build()

      when(tokenDao.insert(?)(?)).thenReturn(Future.unit)
      when(tokenCache.upsert(?)(?)).thenReturn(Future.unit)

      val ex = intercept[IllegalArgumentException] {
        tokenService.createToken(params).futureValue
      }

      ex.getMessage shouldBe "Requester must be non-empty!"

      verifyZeroInteractions(tokenDao)
      verifyZeroInteractions(tokenCache)
    }

    "create new token with empty comment" in withContext { ctx =>
      import ctx.{eq => _, _}
      val params = ModelGenerators.ApiTokenCreateParamsGen.next.toBuilder.clearComment().build()

      when(tokenDao.insert(?)(?)).thenReturn(Future.unit)
      when(tokenCache.upsert(?)(?)).thenReturn(Future.unit)

      val ex = intercept[IllegalArgumentException] {
        tokenService.createToken(params).futureValue
      }

      ex.getMessage shouldBe "Comment must be non-empty!"

      verifyZeroInteractions(tokenDao)
      verifyZeroInteractions(tokenCache)
    }

    "get token" in withContext { ctx =>
      import ctx.{eq => _, _}
      val token = ModelGenerators.ApiTokenGen.next

      when(tokenDao.find(?)(?)).thenReturn(Future.successful(Some(token)))
      when(tokenCache.get(?)(?)).thenReturn(Future.successful(None))
      when(tokenCache.upsert(?)(?)).thenReturn(Future.unit)
      val res = tokenService.getToken(token.token).futureValue

      res.getToken shouldBe ApiTokenWriter.write(token)

      verify(tokenDao).find(eq(token.token))(?)
      verify(tokenCache).get(eq(token.token))(?)
      verify(tokenCache).upsert(?)(?)
    }

    "throw an error if token not found" in withContext { ctx =>
      import ctx.{eq => _, _}
      val token = ModelGenerators.ApiTokenGen.next

      when(tokenDao.find(?)(?)).thenReturn(Future.successful(None))
      when(tokenCache.get(?)(?)).thenReturn(Future.successful(None))
      val res = tokenService.getToken(token.token).failed.futureValue

      res shouldBe an[NoSuchElementException]

      verify(tokenDao).find(eq(token.token))(?)
      verify(tokenCache).get(eq(token.token))(?)
    }

    "distinct grants" in withContext { ctx =>
      import ctx.{eq => _, _}
      val params = ModelGenerators.ApiTokenCreateParamsGen.next
      val params2 = {
        val builder = params.toBuilder
        builder.getPayloadBuilder.addAllGrants(params.getPayload.getGrantsList).build()
        builder.build()
      }

      when(tokenDao.insert(?)(?)).thenReturn(Future.unit)
      when(tokenCache.upsert(?)(?)).thenReturn(Future.unit)
      val res = tokenService.createToken(params2).futureValue

      res.getToken.getPayload shouldBe params.getPayload

      verify(tokenDao).insert(?)(?)
      verify(tokenCache).upsert(?)(?)
    }

    "listHistory" in withContext { ctx =>
      import ctx.{eq => _, _}

      val requestedTokens: Seq[String] = Seq.fill(5)(ModelGenerators.readableString.next)
      val request = ApiTokenHistoryListRequest
        .newBuilder()
        .addAllTokens(requestedTokens.asJava)
        .build()

      val dbModel = ApiTokenHistoryRowGen.next

      when(tokenDao.listHistory(?)(?)).thenReturn(Future.successful(Seq(dbModel)))

      val protoResponse = tokenService.listHistory(request).futureValue
      val apiResponse = protoResponse.getTokenHistory(0)

      apiResponse.getToken shouldBe dbModel.token
      apiResponse.getHistoryCount shouldBe 1
      val info = apiResponse.getHistory(0)

      info.getMoment.getSeconds shouldBe dbModel.moment.getMillis / 1000
      info.getRequester shouldBe dbModel.requester
      info.getComment shouldBe dbModel.comment
      info.getVersion shouldBe dbModel.version
      info.getPayload shouldBe dbModel.payload
      val versions = apiResponse.getHistoryList.asScala.map(_.getVersion)
      versions shouldBe versions.sorted

      verify(tokenDao).listHistory(eq(requestedTokens))(?)
    }

  }
}
