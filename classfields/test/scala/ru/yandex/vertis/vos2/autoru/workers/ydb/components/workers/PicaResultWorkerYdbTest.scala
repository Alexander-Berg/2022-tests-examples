package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import com.google.common.util.concurrent.RateLimiter
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.feedprocessor.FeedprocessorModel.Entity.PhotoInfo
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eqq}
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema.Metadata
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.BasicsModel.Photo
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.TestUtils.createOffer
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.getNow
import ru.yandex.vos2.services.pica.PicaPicaClient
import ru.yandex.vos2.services.pica.PicaPicaClient.{FailedUpload, OkUpload, TaskResult}

import scala.jdk.CollectionConverters._

class PicaResultWorkerYdbTest
  extends AnyWordSpec
  with Matchers
  with MockitoSupport
  with InitTestDbs
  with BeforeAndAfter {
  val limiter: RateLimiter = RateLimiter.create(10)

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val mockedPicapicaClient: PicaPicaClient
    val offer: Offer

    def worker =
      new PicaResultWorkerYdb(
        mockedPicapicaClient,
        components.mdsPhotoUtils,
        limiter,
        Some(limiter),
        None
      ) with YdbWorkerTestImpl {
        override def features: FeaturesManager = components.featuresManager
      }
  }

  "should not process offer without feedprocessor images" in new Fixture {
    override val mockedPicapicaClient: PicaPicaClient = mock[PicaPicaClient]
    val offerBuilder = createOffer(dealer = true)
    val offer = offerBuilder.build()
    worker.shouldProcess(offer, None).shouldProcess shouldBe false
  }

  "should not process offer without not uploaded feedprocessor images" in new Fixture {
    override val mockedPicapicaClient: PicaPicaClient = mock[PicaPicaClient]

    val offerBuilder = createOffer(dealer = true)
    addFeedprocessorPhoto(offerBuilder, "imageId1", "photo.com/123.jpeg", isLoaded = true, 1)

    val offer = offerBuilder.build()
    worker.shouldProcess(offer, None).shouldProcess shouldBe false
  }

  "should upload single photo" in new Fixture {
    override val mockedPicapicaClient: PicaPicaClient = mock[PicaPicaClient]
    when(mockedPicapicaClient.send(?, ?, ?, ?, ?, ?)(?))
      .thenReturn(
        TaskResult(
          Map(
            "id" -> Map(
              "imageId1" -> OkUpload(
                "groupId1",
                "imageId1",
                "autoru-all",
                PicaPicaClient.Metadata.V3(Metadata.newBuilder().setVersion(1).setIsFinished(false).build())
              )
            )
          )
        )
      )
    val offerBuilder = createOffer(dealer = true)
    addFeedprocessorPhoto(offerBuilder, "imageId1", "photo.com/123.jpeg", isLoaded = false, 1)
    val offer = offerBuilder.build()
    val res = worker.process(offer, None)
    val newOffer = res.updateOfferFunc.get(offer)
    assert(newOffer.getOfferAutoru.getFeedprocessorImagesCount == 1)
    assert(newOffer.getOfferAutoru.getFeedprocessorImages(0).getImageId === "imageId1")
    assert(newOffer.getOfferAutoru.getFeedprocessorImages(0).getSrcUrl === "photo.com/123.jpeg")
    assert(newOffer.getOfferAutoru.getFeedprocessorImages(0).getIsLoaded === true)
    assert(newOffer.getOfferAutoru.getFeedprocessorImages(0).getOrder === 1)

    assert(newOffer.getOfferAutoru.getPhotoCount == 1)
    assert(newOffer.getOfferAutoru.getPhoto(0).getName === "groupId1-imageId1")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrigName === "groupId1-imageId1")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrder === 1)
    assert(newOffer.getOfferAutoru.getPhoto(0).getIsMain)
  }

  "upload one of two photo" in new Fixture {
    override val mockedPicapicaClient: PicaPicaClient = mock[PicaPicaClient]
    when(mockedPicapicaClient.send(?, ?, ?, ?, ?, ?)(?))
      .thenReturn(
        TaskResult(
          Map(
            "id" -> Map(
              "imageId1" -> OkUpload(
                "groupId1",
                "imageId1",
                "autoru-all",
                PicaPicaClient.Metadata.V3(Metadata.newBuilder().setVersion(1).setIsFinished(false).build())
              )
            )
          )
        )
      )
    val offerBuilder = createOffer(dealer = true)
    addFeedprocessorPhoto(
      offerBuilder,
      Seq(
        ("imageId1", "photo.com/first_photo.jpeg", false, 1),
        ("imageId2", "photo.com/second_photo.jpeg", false, 2)
      )
    )

    val offer = offerBuilder.build()
    val res = worker.process(offer, None)
    val newOffer = res.updateOfferFunc.get(offer)

    assert(newOffer.getOfferAutoru.getFeedprocessorImagesCount == 2)

    assert(newOffer.getOfferAutoru.getFeedprocessorImages(0).getImageId === "imageId1")
    assert(newOffer.getOfferAutoru.getFeedprocessorImages(0).getSrcUrl === "photo.com/first_photo.jpeg")
    assert(newOffer.getOfferAutoru.getFeedprocessorImages(0).getIsLoaded === true)
    assert(newOffer.getOfferAutoru.getFeedprocessorImages(0).getOrder === 1)

    assert(newOffer.getOfferAutoru.getFeedprocessorImages(1).getImageId === "imageId2")
    assert(newOffer.getOfferAutoru.getFeedprocessorImages(1).getSrcUrl === "photo.com/second_photo.jpeg")
    assert(newOffer.getOfferAutoru.getFeedprocessorImages(1).getIsLoaded === false)
    assert(newOffer.getOfferAutoru.getFeedprocessorImages(1).getOrder === 2)

    assert(newOffer.getOfferAutoru.getPhotoCount == 1)
    assert(newOffer.getOfferAutoru.getPhoto(0).getName === "groupId1-imageId1")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrigName === "groupId1-imageId1")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrder === 1)
    assert(newOffer.getOfferAutoru.getPhoto(0).getIsMain)
  }

  "should not add already exist photo to offer" in new Fixture {
    override val mockedPicapicaClient: PicaPicaClient = mock[PicaPicaClient]

    val offerBuilder = createOffer(dealer = true)
    addFeedprocessorPhoto(offerBuilder, "imageId1", "photo.com/123.jpeg", isLoaded = true, 1)
    addPhoto(offerBuilder, "groupId1-imageId1", 1, isMain = true)

    val offer = offerBuilder.build()
    val res = worker.process(offer, None)
    val newOffer = res.updateOfferFunc.get(offer)

    assert(newOffer.getOfferAutoru.getFeedprocessorImagesCount == 1)

    assert(newOffer.getOfferAutoru.getFeedprocessorImages(0).getImageId === "imageId1")
    assert(newOffer.getOfferAutoru.getFeedprocessorImages(0).getSrcUrl === "photo.com/123.jpeg")
    assert(newOffer.getOfferAutoru.getFeedprocessorImages(0).getIsLoaded === true)
    assert(newOffer.getOfferAutoru.getFeedprocessorImages(0).getOrder === 1)

    assert(newOffer.getOfferAutoru.getPhotoCount == 1)
    assert(newOffer.getOfferAutoru.getPhoto(0).getName === "groupId1-imageId1")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrigName === "groupId1-imageId1")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrder === 1)
    assert(newOffer.getOfferAutoru.getPhoto(0).getIsMain)
  }

  "should remove error images from feedprocessor images" in new Fixture {
    override val mockedPicapicaClient: PicaPicaClient = mock[PicaPicaClient]
    when(mockedPicapicaClient.send(?, ?, ?, ?, ?, ?)(?))
      .thenReturn(
        TaskResult(
          Map(
            "id" -> Map(
              "imageId1" -> OkUpload(
                "groupId1",
                "imageId1",
                "autoru-all",
                PicaPicaClient.Metadata.V3(Metadata.newBuilder().setVersion(1).setIsFinished(false).build())
              ),
              "imageId2" -> FailedUpload(Some("error"))
            )
          )
        )
      )
    val offerBuilder = createOffer(dealer = true)
    addFeedprocessorPhoto(offerBuilder, "imageId1", "photo.com/123.jpeg", isLoaded = false, 1)
    addFeedprocessorPhoto(offerBuilder, "imageId2", "photo.com/321.jpeg", isLoaded = false, 2)

    val offer = offerBuilder.build()
    val res = worker.process(offer, None)
    val newOffer = res.updateOfferFunc.get(offer)

    assert(newOffer.getOfferAutoru.getFeedprocessorImagesCount == 2)
    assert(newOffer.getOfferAutoru.getFeedprocessorImagesList.asScala.forall(_.getIsLoaded))

    assert(newOffer.getOfferAutoru.getPhotoCount == 1)
    assert(newOffer.getOfferAutoru.getPhoto(0).getName === "groupId1-imageId1")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrigName === "groupId1-imageId1")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrder === 1)
    assert(newOffer.getOfferAutoru.getPhoto(0).getIsMain)
  }

  "should add all images in order from feed" in new Fixture {
    override val mockedPicapicaClient: PicaPicaClient = mock[PicaPicaClient]
    when(mockedPicapicaClient.send(?, ?, ?, ?, ?, ?)(?))
      .thenReturn(
        TaskResult(
          Map(
            "id" -> Map(
              "imageId1" -> OkUpload(
                "1",
                "imageId1",
                "autoru-all",
                PicaPicaClient.Metadata.V3(Metadata.newBuilder().setVersion(1).setIsFinished(false).build())
              ),
              "imageId3" -> OkUpload(
                "3",
                "imageId3",
                "autoru-all",
                PicaPicaClient.Metadata.V3(Metadata.newBuilder().setVersion(1).setIsFinished(false).build())
              )
            )
          )
        )
      )
    val offerBuilder = createOffer(dealer = true)
    addPhoto(offerBuilder, Seq(("2-imageId2", 1, true), ("4-imageId4", 2, false)))

    addFeedprocessorPhoto(
      offerBuilder,
      Seq(
        ("imageId1", "photo.com/123.jpeg", false, 4),
        ("imageId3", "photo.com/222.jpeg", false, 2),
        ("imageId2", "photo.com/321.jpeg", true, 1),
        ("imageId4", "photo.com/444.jpeg", true, 3)
      )
    )

    val offer = offerBuilder.build()
    val res = worker.process(offer, None)
    val newOffer = res.updateOfferFunc.get(offer)

    assert(newOffer.getOfferAutoru.getFeedprocessorImagesCount == 4)
    assert(newOffer.getOfferAutoruOrBuilder.getFeedprocessorImagesList.asScala.forall(_.getIsLoaded))

    assert(newOffer.getOfferAutoru.getPhotoCount == 4)

    assert(newOffer.getOfferAutoru.getPhoto(0).getName === "2-imageId2")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrigName === "2-imageId2")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrder === 1)
    assert(newOffer.getOfferAutoru.getPhoto(0).getIsMain)

    assert(newOffer.getOfferAutoru.getPhoto(1).getName === "4-imageId4")
    assert(newOffer.getOfferAutoru.getPhoto(1).getOrigName === "4-imageId4")
    assert(newOffer.getOfferAutoru.getPhoto(1).getOrder === 3)
    assert(!newOffer.getOfferAutoru.getPhoto(1).getIsMain)

    assert(newOffer.getOfferAutoru.getPhoto(2).getName === "3-imageId3")
    assert(newOffer.getOfferAutoru.getPhoto(2).getOrigName === "3-imageId3")
    assert(newOffer.getOfferAutoru.getPhoto(2).getOrder === 2)
    assert(!newOffer.getOfferAutoru.getPhoto(2).getIsMain)

    assert(newOffer.getOfferAutoru.getPhoto(3).getName === "1-imageId1")
    assert(newOffer.getOfferAutoru.getPhoto(3).getOrigName === "1-imageId1")
    assert(newOffer.getOfferAutoru.getPhoto(3).getOrder === 4)
    assert(!newOffer.getOfferAutoru.getPhoto(3).getIsMain)
  }

  "don't remove hand uploaded photo" in new Fixture {
    override val mockedPicapicaClient: PicaPicaClient = mock[PicaPicaClient]
    when(mockedPicapicaClient.send(?, ?, ?, ?, ?, ?)(?))
      .thenReturn(
        TaskResult(
          Map(
            "id" -> Map(
              "imageId1" -> OkUpload(
                "1",
                "imageId1",
                "autoru-all",
                PicaPicaClient.Metadata.V3(Metadata.newBuilder().setVersion(1).setIsFinished(false).build())
              ),
              "imageId3" -> OkUpload(
                "3",
                "imageId3",
                "autoru-all",
                PicaPicaClient.Metadata.V3(Metadata.newBuilder().setVersion(1).setIsFinished(false).build())
              )
            )
          )
        )
      )
    val offerBuilder = createOffer(dealer = true)
    addPhoto(offerBuilder, Seq(("2-imageId2", 2, false), ("4-imageId4", 3, false), ("0-handimage", 1, true)))

    addFeedprocessorPhoto(
      offerBuilder,
      Seq(
        ("imageId1", "photo.com/123.jpeg", false, 4),
        ("imageId3", "photo.com/222.jpeg", false, 2),
        ("imageId2", "photo.com/321.jpeg", true, 1),
        ("imageId4", "photo.com/444.jpeg", true, 3)
      )
    )

    val offer = offerBuilder.build()
    val res = worker.process(offer, None)
    val newOffer = res.updateOfferFunc.get(offer)

    assert(newOffer.getOfferAutoru.getFeedprocessorImagesCount == 4)
    assert(newOffer.getOfferAutoruOrBuilder.getFeedprocessorImagesList.asScala.forall(_.getIsLoaded))

    assert(newOffer.getOfferAutoru.getPhotoCount == 5)

    assert(newOffer.getOfferAutoru.getPhoto(0).getName === "2-imageId2")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrigName === "2-imageId2")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrder === 2)
    assert(!newOffer.getOfferAutoru.getPhoto(0).getIsMain)

    assert(newOffer.getOfferAutoru.getPhoto(1).getName === "4-imageId4")
    assert(newOffer.getOfferAutoru.getPhoto(1).getOrigName === "4-imageId4")
    assert(newOffer.getOfferAutoru.getPhoto(1).getOrder === 4)
    assert(!newOffer.getOfferAutoru.getPhoto(1).getIsMain)

    assert(newOffer.getOfferAutoru.getPhoto(2).getName === "0-handimage")
    assert(newOffer.getOfferAutoru.getPhoto(2).getOrigName === "0-handimage")
    assert(newOffer.getOfferAutoru.getPhoto(2).getOrder === 1)
    assert(newOffer.getOfferAutoru.getPhoto(2).getIsMain)

    assert(newOffer.getOfferAutoru.getPhoto(3).getName === "3-imageId3")
    assert(newOffer.getOfferAutoru.getPhoto(3).getOrigName === "3-imageId3")
    assert(newOffer.getOfferAutoru.getPhoto(3).getOrder === 3)
    assert(!newOffer.getOfferAutoru.getPhoto(3).getIsMain)

    assert(newOffer.getOfferAutoru.getPhoto(4).getName === "1-imageId1")
    assert(newOffer.getOfferAutoru.getPhoto(4).getOrigName === "1-imageId1")
    assert(newOffer.getOfferAutoru.getPhoto(4).getOrder === 5)
    assert(!newOffer.getOfferAutoru.getPhoto(4).getIsMain)
  }

  "upload images from different namespaces" in new Fixture {
    override val mockedPicapicaClient: PicaPicaClient = mock[PicaPicaClient]

    when(mockedPicapicaClient.send(?, ?, eqq("autoru-all"), ?, ?, ?)(?))
      .thenReturn(
        TaskResult(
          Map(
            "imageId1" -> Map(
              "imageId1" -> OkUpload(
                "1",
                "imageId1",
                "autoru-all",
                PicaPicaClient.Metadata.V3(Metadata.newBuilder().setVersion(1).setIsFinished(true).build())
              )
            )
          )
        )
      )
    when(mockedPicapicaClient.send(?, ?, eqq("autoru-vos"), ?, ?, ?)(?))
      .thenReturn(
        TaskResult(
          Map(
            "imageId2" -> Map(
              "imageId2" -> OkUpload(
                "2",
                "imageId2",
                "autoru-vos",
                PicaPicaClient.Metadata.V3(Metadata.newBuilder().setVersion(1).setIsFinished(true).build())
              )
            )
          )
        )
      )

    val offerBuilder = createOffer(dealer = true)
    addFeedprocessorPhoto(offerBuilder, "imageId1", "photo.com/123.jpeg", isLoaded = false, 1, namespace = "autoru-all")
    addFeedprocessorPhoto(offerBuilder, "imageId2", "photo.com/321.jpeg", isLoaded = false, 2, namespace = "autoru-vos")

    val offer = offerBuilder.build()
    val res = worker.process(offer, None)
    val newOffer = res.updateOfferFunc.get(offer)

    assert(newOffer.getOfferAutoru.getPhotoCount == 2)
    assert(newOffer.getOfferAutoru.getPhoto(0).getName == "1-imageId1")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrigName == "1-imageId1")
    assert(newOffer.getOfferAutoru.getPhoto(0).getNamespace == "autoru-all")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrigNamespace == "autoru-all")
    assert(newOffer.getOfferAutoru.getPhoto(0).getIsMain)
    assert(newOffer.getOfferAutoru.getPhoto(1).getName == "2-imageId2")
    assert(newOffer.getOfferAutoru.getPhoto(1).getNamespace == "autoru-vos")
    assert(newOffer.getOfferAutoru.getPhoto(1).getOrigName == "2-imageId2")
    assert(newOffer.getOfferAutoru.getPhoto(1).getOrigNamespace == "autoru-vos")
    assert(!newOffer.getOfferAutoru.getPhoto(1).getIsMain)
  }

  "upload images from different namespaces with errors" in new Fixture {
    override val mockedPicapicaClient: PicaPicaClient = mock[PicaPicaClient]
    when(mockedPicapicaClient.send(?, ?, eqq("autoru-all"), ?, ?, ?)(?))
      .thenReturn(TaskResult(Map("imageId1" -> Map("imageId1" -> FailedUpload(Some("not found"))))))
    when(mockedPicapicaClient.send(?, ?, eqq("autoru-vos"), ?, ?, ?)(?))
      .thenReturn(
        TaskResult(
          Map(
            "imageId2" -> Map(
              "imageId2" -> OkUpload(
                "2",
                "imageId2",
                "autoru-vos",
                PicaPicaClient.Metadata.V3(Metadata.newBuilder().setVersion(1).setIsFinished(true).build())
              )
            )
          )
        )
      )

    val offerBuilder = createOffer(dealer = true)
    addFeedprocessorPhoto(offerBuilder, "imageId1", "photo.com/123.jpeg", isLoaded = false, 1, namespace = "autoru-all")
    addFeedprocessorPhoto(offerBuilder, "imageId2", "photo.com/321.jpeg", isLoaded = false, 2, namespace = "autoru-vos")

    val offer = offerBuilder.build()
    val res = worker.process(offer, None)
    val newOffer = res.updateOfferFunc.get(offer)

    assert(newOffer.getOfferAutoru.getPhotoCount == 1)
    assert(newOffer.getOfferAutoru.getPhoto(0).getName == "2-imageId2")
    assert(newOffer.getOfferAutoru.getPhoto(0).getNamespace == "autoru-vos")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrigName == "2-imageId2")
    assert(newOffer.getOfferAutoru.getPhoto(0).getOrigNamespace == "autoru-vos")
    assert(newOffer.getOfferAutoru.getPhoto(0).getIsMain)
  }

  private def addFeedprocessorPhoto(builder: Offer.Builder, photo: Seq[(String, String, Boolean, Int)]): Unit = {
    photo.foreach(p => addFeedprocessorPhoto(builder, p._1, p._2, p._3, p._4))
  }

  private def addFeedprocessorPhoto(builder: Offer.Builder,
                                    imageId: String,
                                    srcUrl: String,
                                    isLoaded: Boolean,
                                    order: Int,
                                    namespace: String = "autoru-all"): AutoruOffer.Builder = {
    builder.getOfferAutoruBuilder.addFeedprocessorImages(
      PhotoInfo
        .newBuilder()
        .setImageId(imageId)
        .setSrcUrl(srcUrl)
        .setIsLoaded(isLoaded)
        .setOrder(order)
        .setNamespace(namespace)
    )
  }

  private def addPhoto(builder: Offer.Builder, photo: Seq[(String, Int, Boolean)]): Unit = {
    photo.foreach(p => addPhoto(builder, p._1, p._2, p._3))
  }

  private def addPhoto(builder: Offer.Builder, name: String, order: Int, isMain: Boolean) = {
    builder.getOfferAutoruBuilder.addPhoto(
      Photo
        .newBuilder()
        .setName(name)
        .setOrigName(name)
        .setCreated(getNow)
        .setOrder(order)
        .setIsMain(isMain)
    )
  }

}
