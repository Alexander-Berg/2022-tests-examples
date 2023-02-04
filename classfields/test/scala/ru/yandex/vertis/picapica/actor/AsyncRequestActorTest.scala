package ru.yandex.vertis.picapica.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.junit.runner.RunWith
import org.mockito.ArgumentMatcher
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.picapica.actor.AsyncRequestActor.Result.ImagesResult
import ru.yandex.vertis.picapica.actor.AsyncRequestActor.{DownloadRequest, Response}
import ru.yandex.vertis.picapica.client.ApiVersion
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema.Metadata
import ru.yandex.vertis.picapica.dao.KeyValueDao
import ru.yandex.vertis.picapica.generators.Generators._
import ru.yandex.vertis.picapica.generators.Producer._
import ru.yandex.vertis.picapica.misc.Queue
import ru.yandex.vertis.picapica.model.AvatarsResponse.AvatarsData
import ru.yandex.vertis.picapica.model._
import ru.yandex.vertis.picapica.ops.TestOps
import ru.yandex.vertis.picapica.service.AvatarsMetaService.{Always, MetaTaskRequest, Sometimes}
import ru.yandex.vertis.picapica.service.{AvatarsService, StorageService}

import scala.concurrent.Future

/**
  * @author pnaydenov
  */
// scalastyle:off
@RunWith(classOf[JUnitRunner])
class AsyncRequestActorTest extends TestKit(ActorSystem("Test"))
                                with ImplicitSender
                                with WordSpecLike
                                with MockitoSugar
                                with Matchers
                                with TestOps {

  private val intGen = Gen.choose(0, 100)
  import system.dispatcher

  abstract class Fixture {
    val storageService = mock[StorageService]
    val avatarsService = mock[AvatarsService]
    val couchbaseDao = mock[KeyValueDao[AvatarsData]]
    val metaProcessor = mock[Queue[MetaTaskRequest]]
    val RequiredField = "required_field"
    val RequiredFields = Set(RequiredField)
    val rawMeta = """{"raw": "meta"}"""

    lazy val tasks = Vector(TaskGen.next.copy(url = "http://host/namespace/111/name123/sizetype"))
    val taskIds = tasks.map(_.id)
    val taskId = taskIds.head
    val partitioningId = PartitionId(intGen.next.toString, intGen.next)
    def service: Service = AutoruOrig

    // values from `storedData` will be returned like response of storageService
    def storedData: Option[StoredAvatarsData]

    when(storageService.get(eqTo(partitioningId.groupId), eqTo(taskIds))).
      thenAnswer(new Answer[Future[Iterable[(Id, StoredAvatarsData)]]] {
      override def answer(invocation: InvocationOnMock) = Future {
        storedData.map(data => Seq(taskId -> data) ).getOrElse(Nil)
      }
    })
    doNothing().when(metaProcessor).offer(any())
    val actor = TestActorRef(
      new AsyncRequestActor(
        service,
        storageService,
        avatarsService,
        couchbaseDao,
        metaProcessor,
        RequiredFields))
  }

  def expectArgPF[T](pf: PartialFunction[Any, Unit]): T = argThat(new ArgumentMatcher[T] {
    override def matches(argument: scala.Any): Boolean =
      if (pf.isDefinedAt(argument.asInstanceOf[T])) {
        pf.apply(argument.asInstanceOf[T])
        true
      } else {
        false
      }
  })

  "AsyncRequestActorTest" should {
    "api v2" should {
      "sometimes fetch metadata" in new Fixture {
        override def storedData: Option[StoredAvatarsData] =
          Some(StoredAvatarsData(ActualIdGen.next, Map(RequiredField -> "has"), None))

        actor ! DownloadRequest(tasks, partitioningId, Unit, None, false, ApiVersion.V2, 0)
        val images = expectMsgType[Response].result.asInstanceOf[ImagesResult].images
        images shouldNot be(empty)
        images.head._1 should be(taskId)
        images.head._2.meta shouldNot be(empty)
        verify(metaProcessor).offer(expectArgPF {
          case task: MetaTaskRequest =>
            task.regenerate(storedData, RequiredFields) shouldBe Some(Sometimes)
            task.download(storedData, RequiredFields) shouldEqual Some(Sometimes)
        })
        verify(avatarsService, never()).store(any())(any())
        expectNoMsg()
      }

      "return relevant meta" in new Fixture {
        override def storedData: Option[StoredAvatarsData] =
          Some(StoredAvatarsData(ActualIdGen.next, Map(RequiredField -> "has"),
            Some(Metadata.newBuilder()
              .setVersion(1)
              .setIsFinished(true)
              .setRawMeta(rawMeta)
              .build())))

        actor ! DownloadRequest(tasks, partitioningId, Unit, None, false, ApiVersion.V2, 0)
        val images = expectMsgType[Response].result.asInstanceOf[ImagesResult].images
        images shouldNot be(empty)
        images.head._2.meta shouldEqual Map(RequiredField -> "has")
        verify(metaProcessor, never()).offer(any())
        verify(avatarsService, never()).store(any())(any())
      }

      "actualize meta with absent keys" in new Fixture {
        override def storedData: Option[StoredAvatarsData] =
          Some(StoredAvatarsData(ActualIdGen.next, Map("foo" -> "bar"),
            Some(Metadata.newBuilder().setVersion(1).setIsFinished(true).build())))

        actor ! DownloadRequest(tasks, partitioningId, Unit, None, false, ApiVersion.V2, 0)
        val images = expectMsgType[Response].result.asInstanceOf[ImagesResult].images
        images shouldNot be(empty)
        verify(metaProcessor).offer(expectArgPF {
          case task: MetaTaskRequest =>
            task.regenerate(storedData, RequiredFields) shouldEqual Some(Sometimes)
            task.download(storedData, RequiredFields) shouldEqual Some(Always)
        })
        verify(avatarsService, never()).store(any())(any())
      }

      "download non existing image" in new Fixture {
        override def storedData: Option[StoredAvatarsData] = None

        actor ! DownloadRequest(tasks, partitioningId, Unit, None, false, ApiVersion.V2, 0)
        val images = expectMsgType[Response].result.asInstanceOf[ImagesResult].images
        images should be(empty)
        verify(metaProcessor, never()).offer(any())
        verify(avatarsService).store(any())(any())
      }
    }

    "api v3" should {
      "existing image" should {
        "return relevant metadata" in new Fixture {
          val metadata = Metadata.newBuilder()
            .setVersion(5)
            .setIsFinished(true)
            .setRawMeta(rawMeta)
            .build()
          override def storedData: Option[StoredAvatarsData] =
            Some(StoredAvatarsData(ActualIdGen.next, Map(RequiredField -> "has"),
              Some(metadata)))

          actor ! DownloadRequest(tasks, partitioningId, Unit, Some(5), true, ApiVersion.V3, 0)
          val images = expectMsgType[Response].result.asInstanceOf[ImagesResult].images
          images shouldNot be(empty)
          images.head._1 should be(taskId)
          images.head._2.metadata shouldEqual Some(metadata)
          verify(metaProcessor, never()).offer(any())
          verify(avatarsService, never()).store(any())(any())
        }

        "don't return response w/o metadata" in new Fixture {
          override def storedData: Option[StoredAvatarsData] =
            Some(StoredAvatarsData(ActualIdGen.next, Map(RequiredField -> "has"), None))

          actor ! DownloadRequest(tasks, partitioningId, Unit, Some(5), true, ApiVersion.V3, 0)
          val images = expectMsgType[Response].result.asInstanceOf[ImagesResult].images
          images should be(empty)
          verify(metaProcessor).offer(expectArgPF {
            case task @ MetaTaskRequest(service, _, _, _, Some(5), false, _, _, _) =>
              task.regenerate(storedData, RequiredFields) shouldBe Some(Sometimes)
              task.download(storedData, RequiredFields) shouldEqual Some(Always)
          })
          verify(avatarsService, never()).store(any())(any())
        }

        "regenerate outdated metadata" in new Fixture {
          override def storedData: Option[StoredAvatarsData] =
            Some(StoredAvatarsData(ActualIdGen.next, Map(RequiredField -> "has"),
              Some(Metadata.newBuilder().setVersion(1).setIsFinished(true).build())))

          actor ! DownloadRequest(tasks, partitioningId, Unit, Some(5), true, ApiVersion.V3, 0)
          val images = expectMsgType[Response].result.asInstanceOf[ImagesResult].images
          images should be(empty)
          verify(metaProcessor).offer(expectArgPF {
            case task @ MetaTaskRequest(service, _, _, _, Some(5), false, _, _, _) =>
              task.regenerate(storedData, RequiredFields) shouldEqual Some(Always)
          })
          verify(avatarsService, never()).store(any())(any())
        }

        "only download MDS meta for missing row" in new Fixture {
          override def storedData: Option[StoredAvatarsData] = None

          actor ! DownloadRequest(tasks, partitioningId, Unit, Some(5), true, ApiVersion.V3, 0)
          val images = expectMsgType[Response].result.asInstanceOf[ImagesResult].images
          images should be(empty)
          verify(metaProcessor).offer(expectArgPF {
            case task @ MetaTaskRequest(service, _, _, _, Some(5), true, _, _, _) =>
              task.regenerate(storedData, RequiredFields) shouldEqual None
              task.download(storedData, RequiredFields) shouldEqual Some(Always)
          })
          verify(avatarsService, never()).store(any())(any())
        }

        "use elementId of existing avatars without offerid" should {
          "removed row" in new Fixture {
            override def storedData: Option[StoredAvatarsData] = None

            actor ! DownloadRequest(tasks, partitioningId, Unit, Some(5), true, ApiVersion.V3, 0)
            expectMsgType[Response]
            expectNoMsg()
            verify(metaProcessor).offer(expectArgPF {
              case MetaTaskRequest(service, id@ExistingId(_, _), _, _, _, _, _, _, _) =>
                service.toUrlPart(id) shouldEqual id.elementId
            })
          }

          "has row" in new Fixture {
            override def storedData: Option[StoredAvatarsData] =
              Some(StoredAvatarsData(ActualIdGen.next, Map(RequiredField -> "has"), None))

            actor ! DownloadRequest(tasks, partitioningId, Unit, Some(5), true, ApiVersion.V3, 0)
            expectMsgType[Response]
            expectNoMsg()
            verify(metaProcessor).offer(expectArgPF {
              case MetaTaskRequest(service, id@ExistingId(_, _), _, _, _, false, _, _, _) =>
                service.toUrlPart(id) shouldEqual id.elementId
            })
          }
        }
      }

      "new image" should {
        "download non existing image" in new Fixture {
          override def storedData: Option[StoredAvatarsData] = None

          actor ! DownloadRequest(tasks, partitioningId, Unit, None, false, ApiVersion.V3, 0)
          val images = expectMsgType[Response].result.asInstanceOf[ImagesResult].images
          images should be(empty)
          verify(metaProcessor, never()).offer(any())
          verify(avatarsService).store(any())(any())
        }

        "ignore absence of keys in old meta" in new Fixture {
          val metadata = Metadata.newBuilder()
            .setVersion(1)
            .setIsFinished(true)
            .setRawMeta(rawMeta)
            .build()
          override def storedData: Option[StoredAvatarsData] =
            Some(StoredAvatarsData(ActualIdGen.next, Map.empty,
              Some(metadata)))

          actor ! DownloadRequest(tasks, partitioningId, Unit, None, false, ApiVersion.V3, 0)
          val images = expectMsgType[Response].result.asInstanceOf[ImagesResult].images
          images shouldNot be(empty)
          images.head._2.metadata shouldEqual Some(metadata)
          verify(metaProcessor, never()).offer(any())
          verify(avatarsService, never()).store(any())(any())
        }

        "use custom url generator for autoru-all" in new Fixture {
          override def storedData: Option[StoredAvatarsData] = None
          override def service: Service = AutoruOrig

          actor ! DownloadRequest(tasks, partitioningId, Unit, None, false, ApiVersion.V3, 0)
          val images = expectMsgType[Response].result.asInstanceOf[ImagesResult].images
          images should be(empty)
          verify(metaProcessor, never()).offer(any())
          verify(avatarsService).store(expectArgPF {
            case tasks: Iterable[_] =>
              tasks should have size(1)
              val id = tasks.head.asInstanceOf[Task].id
              service.toUrlPart(id) shouldEqual taskId.elementId
          })(any())
        }
      }

      "always regenerate outdated" in new Fixture {
        val metadata = Metadata.newBuilder().setVersion(1).setIsFinished(true).build()
        override def storedData: Option[StoredAvatarsData] =
          Some(StoredAvatarsData(ActualIdGen.next, Map(RequiredField -> "has"), Some(metadata)))

        actor ! DownloadRequest(tasks, partitioningId, Unit, Some(5), false, ApiVersion.V3, 0)
        val images = expectMsgType[Response].result.asInstanceOf[ImagesResult].images
        images shouldNot be(empty)
        images.head._2.metadata shouldEqual Some(metadata)
        verify(metaProcessor).offer(expectArgPF {
          case task: MetaTaskRequest =>
            task.regenerate(storedData, RequiredFields) shouldEqual Some(Always)
        })
      }
    }
  }
}
// scalastyle:on
