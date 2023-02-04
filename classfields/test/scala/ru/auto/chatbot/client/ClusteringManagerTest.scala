package ru.auto.chatbot.client

import org.scalatest.FunSuite
import ru.auto.chatbot.exception.OwnCarReviewException
import ru.auto.chatbot.manager.ClusteringManager
import ru.auto.chatbot.model.ClusterUser
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-03-04.
  */
class ClusteringManagerTest extends FunSuite with MockitoSupport {

  val listClusterUsers = List(
    ClusterUser("auto.ru", "23190052"),
    ClusterUser("auto.ru", "23363990")
  )

  private val client = mock[ClusteringClient]
  when(client.getCluster(?)).thenReturn(Future.successful(listClusterUsers))

  val manager = new ClusteringManager(client)

  test("same cluster") {
    assertThrows[OwnCarReviewException](Await.result(manager.checkSameClusterUsers("23190052", "23363990"), 10.seconds))
  }

  test("different clusters") {
    Await.result(manager.checkSameClusterUsers("2319005299", "23363991"), 10.seconds)
  }

}
