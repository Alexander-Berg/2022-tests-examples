package ru.auto.cabinet.tasks.impl.tagging

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import ru.auto.cabinet.dao.jdbc.JdbcKeyValueDao
import ru.auto.cabinet.service.salesman.{Good, SalesmanClient}
import ru.auto.cabinet.service.vos.VosClient

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class VasUntaggingTaskSpec extends TaggingTaskSpec with VasTags {

  private val salesmanClient = mock[SalesmanClient]
  private val keyValueDao = mock[JdbcKeyValueDao]
  private val vosClient = mock[VosClient]

  private val task =
    new VasUntaggingTask(salesmanClient, keyValueDao, vosClient)
      with TestInstrumented

  "VAS untagging task" should "return time for first invocation" in {
    when(salesmanClient.recentGoods(argThat(afterTestStart))(any()))
      .thenReturn(Future.successful(Seq()))
    task.doTagging(None).futureValue.get.isAfter(testStart) shouldBe true
  }

  it should "return LastTagging again if no new goods" in {
    when(salesmanClient.recentGoods(LastTagging.get))
      .thenReturn(Future.successful(Seq()))
    task.doTagging(LastTagging).futureValue shouldBe None
  }

  it should "untag offers and return max offer time" in {
    when(salesmanClient.recentGoods(LastTagging.get)).thenReturn(
      Future.successful(
        Seq(
          Good(Offer1Id, Offer1Odt),
          Good(Offer2Id, Offer2Odt),
          Good(Offer3Id, Offer3Odt),
          Good(Offer4Id, Offer4Odt)
        )))
    when(vosClient.deleteTags(any(), any())(any())).thenReturn(futureUnit)
    task.doTagging(LastTagging).futureValue shouldBe
      Some(Seq(Offer1Odt, Offer2Odt, Offer3Odt, Offer4Odt).max)
    for (offerId <- Seq(Offer1Id, Offer2Id, Offer3Id, Offer4Id))
      verify(vosClient).deleteTags(offerId, Tags)
  }
}
