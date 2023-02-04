package ru.yandex.vertis.passport.dao

import org.scalacheck.Gen
import org.scalatest.FreeSpec
import ru.yandex.vertis.passport.test.Producer._
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ModelGenerators._
import ru.yandex.vertis.passport.model.tokens.ApiTokenRemove

import scala.concurrent.{ExecutionContext, Future}

trait ApiTokenDaoSpec extends FreeSpec with SpecBase {
  implicit private val ec: ExecutionContext = ExecutionContext.global

  def tokenDao: ApiTokenDao

  "ApiTokenDao" - {
    "return None if token not found" in {
      tokenDao.find("some_test_id").futureValue shouldBe None
    }

    "insert and find token" in {
      val token = ApiTokenUpdateGen.next
      tokenDao.insert(token).futureValue
      val res = tokenDao.find(token.token).futureValue

      res.get.token shouldEqual token.token
      res.get.payload shouldEqual token.payload
    }

    "remove" in {
      val token = ApiTokenUpdateGen.next
      tokenDao.insert(token).futureValue
      val remove = ApiTokenRemove(token.token, token.requester, token.comment)
      tokenDao.remove(remove).futureValue
      val res = tokenDao.find(token.token).futureValue

      res shouldBe None
    }

    "get list" in {
      val tokens = Gen.listOfN(3, ApiTokenUpdateGen).next.map(t => t.copy(token = "dealer-" + t.token))
      Future.traverse(tokens)(tokenDao.insert).futureValue

      val res = tokenDao.list(Some("dealer"), None, 10).futureValue

      res.map(_.token).toSet shouldEqual tokens.map(_.token).toSet
      res.map(_.payload).toSet shouldEqual tokens.map(_.payload).toSet
    }

    "list history" in {
      val changesCount = 3
      val sampleToken = ApiTokenUpdateGen.next.token

      val tokens = Seq.fill(changesCount)(ApiTokenUpdateGen.next.copy(token = sampleToken))

      def fetchHistory(tokenStr: String) = tokenDao.listHistory(Seq(tokenStr)).futureValue

      fetchHistory(sampleToken) shouldBe Seq.empty

      tokenDao.insert(tokens.head).futureValue
      tokens.tail.map(x => tokenDao.update(x).futureValue)

      fetchHistory("random-token") shouldBe Seq.empty
      val res = fetchHistory(sampleToken)

      res.last.version shouldBe changesCount
      res.length shouldBe changesCount
      res.map(_.payload) shouldBe tokens.map(_.payload)
    }
  }

}
