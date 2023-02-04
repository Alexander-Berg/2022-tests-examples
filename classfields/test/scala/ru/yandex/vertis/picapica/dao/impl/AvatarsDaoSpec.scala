package ru.yandex.vertis.picapica.dao.impl

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import ru.yandex.vertis.picapica.dao.impl.avatars.AvatarsDaoImpl
import ru.yandex.vertis.picapica.dao.{PersistingAvatarsDao, StorageDao}
import ru.yandex.vertis.picapica.model.AvatarsResponse.AvatarsData
import ru.yandex.vertis.picapica.model.{AvatarsResponse, Id, PartitionId, Realty, Task}
import ru.yandex.vertis.picture.Mds.{ImageInfo, Size}
import ru.yandex.vertis.picture._
import ru.yandex.vertis.picture.excepton.TooManyConcurrentConnections

import scala.concurrent.Future

/**
  * @author @logab
  */
class AvatarsDaoSpec
    extends TestKit(ActorSystem("AvatarsDaoSpec"))
        with WordSpecLike
        with Matchers
        with BeforeAndAfterEach
        with ScalaFutures {
  private val testErrorMsg = "test"

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(50, Milliseconds))


  trait Test {
    def partitionId: PartitionId = PartitionId.build("1", Int.MaxValue)

    val mdsClientMock = mock(classOf[MdsClient])
    when(mdsClientMock.put(anyString(), anyString())).thenReturn(returnFunction)
    val storeDaoMock = mock(classOf[StorageDao])
    when(storeDaoMock.get(any(), any()))
      .thenReturn(Future.successful(Seq.empty))
    when(storeDaoMock.store(any(classOf[Map[Id, AvatarsResponse]]))(anyInt()))
        .thenReturn(Future.successful(()))
    val avatarsDao = new AvatarsDaoImpl(mdsClientMock, Realty) with PersistingAvatarsDao {
      override def persister(partitioningId: PartitionId): StorageDao = storeDaoMock
    }

    def check(result: AvatarsResponse): Unit

    def returnFunction: Future[ImageInfo]

    check(avatarsDao.store(Task(Id("a", "b"), "url", None))(partitionId).futureValue)
  }

  "PesistingAvatarsDao" should {
    "handle ok response" in new Test {
      private def id: String = partitionId.id

      override def returnFunction: Future[ImageInfo] =
        Future.successful(ImageInfo(id, Size(1, 2), None))

      override def check(result: AvatarsResponse): Unit = {
        result.id shouldEqual partitionId.groupId
      }
    }

    "handle already exist" ignore new Test {
      private def id: String = partitionId.id
      private val now = DateTime.now()

      override def returnFunction: Future[ImageInfo] =
        Future.failed(AlreadyExists(testErrorMsg, id, Some(now)))

      override def check(result: AvatarsResponse): Unit =
        result should matchPattern {
          case _ =>
          // case AvatarsData(id, Some(now)) =>
          // TODO: find a way to fix it
        }
    }

    "handle partner error" in new Test {
      override def returnFunction: Future[ImageInfo] =
        Future.failed(DownloaderRanOutOfAttempts(testErrorMsg))

      override def check(result: AvatarsResponse): Unit =
        result shouldEqual AvatarsResponse.Timeout
    }

    "handle bad request" in new Test {
      override def returnFunction: Future[ImageInfo] =
        Future.failed(BadRequest(testErrorMsg))

      override def check(result: AvatarsResponse): Unit =
        result shouldEqual AvatarsResponse.BadRequest
    }

    "handle absent file error" in new Test {
      override def returnFunction: Future[ImageInfo] =
        Future.failed(NotFound(testErrorMsg))

      override def check(result: AvatarsResponse): Unit =
        result shouldEqual AvatarsResponse.NotFound
    }

    "handle fobidden error" in new Test {
      override def returnFunction: Future[ImageInfo] =
        Future.failed(ForbiddenPicture(testErrorMsg))

      override def check(result: AvatarsResponse): Unit =
        result shouldEqual AvatarsResponse.BadRequest
    }

    "handle unexpected error" in new Test {
      override def returnFunction: Future[ImageInfo] =
        Future.failed(UnexpectedError(testErrorMsg, -1))

      override def check(result: AvatarsResponse): Unit =
        result shouldEqual AvatarsResponse.ServerError
    }

    "handle too many concurrent connections error" in new Test {
      override def returnFunction: Future[ImageInfo] =
        Future.failed(TooManyConcurrentConnections(new RuntimeException))

      override def check(result: AvatarsResponse): Unit =
        result shouldEqual AvatarsResponse.SkipError
    }
  }

}
