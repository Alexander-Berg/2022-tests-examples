package ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.mapper

import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.Inside
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.Messages.OfferMessage
import ru.yandex.vertis.feedprocessor.autoru.model.{ExternalOffer, OfferNotice, TaskContext}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.mapper
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.mapper.PicaPicaMapper.WithImages
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.util.PicapicaUtils
import ru.yandex.vertis.feedprocessor.services.mds.PhotoID
import ru.yandex.vertis.feedprocessor.services.picapica.PicaPicaClient
import ru.yandex.vertis.feedprocessor.services.picapica.PicaPicaClient.{FailedUpload, OkUpload, TaskResult}
import ru.yandex.vertis.feedprocessor.util.{DummyOpsSupport, StreamTestBase}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

/**
  * @author pnaydenov
  */
class PicaPicaMapperSpec
  extends StreamTestBase
  with MockitoSupport
  with DummyOpsSupport
  with TestApplication
  with Inside {

  case class TestExternalOfferImpl(
      position: Int,
      taskContext: TaskContext,
      images: Seq[String],
      imageToPhotoId: Map[String, PhotoID] = Map.empty,
      imageToError: Map[String, String] = Map.empty)
    extends ExternalOffer
    with WithImages[TestExternalOfferImpl] {

    override def getAllImages: Seq[String] = images

    override def addPhotoIds(ids: Map[String, PhotoID], failures: Map[String, String]): TestExternalOfferImpl =
      this.copy(imageToPhotoId = imageToPhotoId ++ ids, imageToError = imageToError ++ failures)

    override def addNotice(notice: OfferNotice): TestExternalOfferImpl = ???

    override def notices: List[OfferNotice] = ???
  }

  val config = environment.config.getConfig("feedprocessor.autoru")
  implicit val meters = new mapper.Mapper.Meters(prometheusRegistry)
  val tc = TaskContext(newTasksGen.next)

  "PicaPicaMapper" should {
    "skip already completed images" in {
      val picaPicaClient = mock[PicaPicaClient]
      val offer = TestExternalOfferImpl(
        0,
        tc,
        images = Seq("foo", "bar"),
        imageToPhotoId = Map("foo" -> PhotoID("autoru-all", "11-aa"))
      )
      val mapper = new PicaPicaMapper[TestExternalOfferImpl](picaPicaClient, config)
      when(picaPicaClient.send(?, ?, ?)).thenReturn(Future.successful(TaskResult(Map.empty)))
      val (pub, sub) = createPubSub(mapper.flow())
      sub.request(1)
      pub.sendNext(OfferMessage(offer))
      sub.expectNextPF {
        case OfferMessage(offerResult) =>
          offerResult shouldEqual offer
      }
      val taskArg: ArgumentCaptor[Seq[PicaPicaClient.Task]] = ArgumentCaptor.forClass(classOf[Seq[PicaPicaClient.Task]])
      verify(picaPicaClient).send(taskArg.capture(), ?, ?)
      val task = taskArg.getValue.head
      task.urls should have size (1)
      inside(task.urls.head) {
        case PicaPicaClient.Image(_, "bar") =>
      }
    }

    "don't invoke pica-pica if all images completed" in {
      val picaPicaClient = mock[PicaPicaClient]
      val offer = TestExternalOfferImpl(
        0,
        tc,
        images = Seq("foo", "bar"),
        imageToPhotoId = Map("foo" -> PhotoID("autoru-all", "11-aa"), "bar" -> PhotoID("autoru-all", "22-bb"))
      )
      val mapper = new PicaPicaMapper[TestExternalOfferImpl](picaPicaClient, config)
      val (pub, sub) = createPubSub(mapper.flow())
      sub.request(1)
      pub.sendNext(OfferMessage(offer))
      sub.expectNextPF {
        case OfferMessage(offerResult) =>
          offerResult shouldEqual offer
      }
    }

    "apply pica results" in {
      val picaPicaClient = mock[PicaPicaClient]
      val offer = TestExternalOfferImpl(
        0,
        tc,
        images = Seq("foo", "bar", "baz"),
        imageToPhotoId = Map("baz" -> PhotoID("autoru-orig", "33-ccc"))
      )
      val mapper = new PicaPicaMapper[TestExternalOfferImpl](picaPicaClient, config)
      stub(picaPicaClient.send _) {
        case (tasks, partitionId, namespace) =>
          val task = tasks.head
          task.urls should have size (2)
          val foo = task.urls.find(_.srcUrl == "foo").get
          val bar = task.urls.find(_.srcUrl == "bar").get
          val response = Map(foo.imageId -> OkUpload("11"), bar.imageId -> OkUpload("22"))
          Future.successful(TaskResult(Map(partitionId -> response)))
      }
      val (pub, sub) = createPubSub(mapper.flow())
      sub.request(1)
      pub.sendNext(OfferMessage(offer))
      sub.expectNextPF {
        case OfferMessage(offerResult: TestExternalOfferImpl) =>
          offerResult.incompleteImages shouldBe empty
          offerResult.imageToPhotoId should have size (3)
          offerResult.imageToPhotoId("foo").name should startWith("11-")
          offerResult.imageToPhotoId("foo").namespace shouldBe "autoru-orig"
          offerResult.imageToPhotoId("bar").name should startWith("22-")
          offerResult.imageToPhotoId("baz").namespace shouldBe "autoru-orig"
      }
      verify(picaPicaClient).send(?, ?, ?)
    }

    "apply pica errors" in {
      val picaPicaClient = mock[PicaPicaClient]
      val offer = TestExternalOfferImpl(
        0,
        tc,
        images = Seq("foo", "bar", "baz"),
        imageToPhotoId = Map("baz" -> PhotoID("autoru-all", "33-ccc"))
      )
      val mapper = new PicaPicaMapper[TestExternalOfferImpl](picaPicaClient, config)

      when(picaPicaClient.send(?, ?, ?)).thenReturn(
        Future.successful(
          TaskResult(
            Map(
              s"ac_${tc.task.clientId}" ->
                Map(
                  PicapicaUtils.generateImageId("foo", tc.task.clientId) -> FailedUpload(Some("some-error")),
                  PicapicaUtils.generateImageId("bar", tc.task.clientId) -> OkUpload("bar")
                )
            )
          )
        ),
        Future.successful(
          TaskResult(
            Map(
              s"ac_${tc.task.clientId}" ->
                Map(PicapicaUtils.generateImageId("foo", tc.task.clientId) -> FailedUpload(Some("some-error")))
            )
          )
        ),
        Future.successful(
          TaskResult(
            Map(
              s"ac_${tc.task.clientId}" ->
                Map(PicapicaUtils.generateImageId("foo", tc.task.clientId) -> FailedUpload(Some("some-error")))
            )
          )
        )
      )

      val (pub, sub) = createPubSub(mapper.flow())
      sub.request(1)
      pub.sendNext(OfferMessage(offer))
      sub.expectNextPF {
        case OfferMessage(offerResult: TestExternalOfferImpl) =>
          offerResult.imageToPhotoId shouldEqual Map(
            "baz" -> PhotoID("autoru-all", "33-ccc"),
            "bar" -> PhotoID("autoru-orig", s"bar-${PicapicaUtils.generateImageId("bar", tc.task.clientId)}")
          )
          offerResult.imageToError shouldEqual Map("foo" -> "some-error")
          offerResult.incompleteImages shouldEqual Seq("foo")
      }
      verify(picaPicaClient, times(3)).send(?, ?, ?)
    }
  }
}
