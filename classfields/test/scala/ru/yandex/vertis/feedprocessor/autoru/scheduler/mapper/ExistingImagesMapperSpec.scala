package ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper

import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.Messages.OfferMessage
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.AutoruExternalOffer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.mapper
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.AutoruGenerators._
import ru.yandex.vertis.feedprocessor.services.mds.PhotoID
import ru.yandex.vertis.feedprocessor.util.{DummyOpsSupport, StreamTestBase}
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * @author pnaydenov
  */
class ExistingImagesMapperSpec extends StreamTestBase with MockitoSupport with DummyOpsSupport with TestApplication {

  implicit val meters = new mapper.Mapper.Meters(prometheusRegistry)

  val config = environment.config.getConfig("feedprocessor.autoru")

  "ExistingImagesMapper" should {
    "find MDS images" in {
      val Url = "https://avatars.mdst.yandex.net/get-autoru-vos/40265/d8320a0ccbe533858db0340999cedf27/1200x900"
      val offer = truckExternalOfferGen(newTasksGen).next.copy(images = Seq(Url))
      val mapper = new ExistingImagesMapper(config)
      val (pub, sub) = createPubSub(mapper.flow())
      sub.request(1)
      pub.sendNext(OfferMessage(offer))
      val offerResponse = sub.expectNextPF {
        case OfferMessage(offer: AutoruExternalOffer) =>
          offer
      }
      offerResponse.incompleteImages shouldBe empty
      offerResponse.imageToPhotoId shouldEqual Map(
        Url -> PhotoID("autoru-vos", "40265-d8320a0ccbe533858db0340999cedf27")
      )
    }

    "correctly merge MDS and not existing images" in {
      val MdsUrl = "https://avatars.mdst.yandex.net/get-autoru-vos/40265/d8320a0ccbe533858db0340999cedf27/1200x900"
      val NewUrl = "http://foo.bar/baz.jpg"
      val offer = truckExternalOfferGen(newTasksGen).next.copy(images = Seq(MdsUrl, NewUrl))
      val mapper = new ExistingImagesMapper(config)
      val (pub, sub) = createPubSub(mapper.flow())
      sub.request(1)
      pub.sendNext(OfferMessage(offer))
      val offerResponse = sub.expectNextPF {
        case OfferMessage(offer: AutoruExternalOffer) =>
          offer
      }
      offerResponse.incompleteImages shouldEqual Seq(NewUrl)
      offerResponse.imageToPhotoId shouldEqual
        Map(MdsUrl -> PhotoID("autoru-vos", "40265-d8320a0ccbe533858db0340999cedf27"))
    }

    "don't fail on MDS like but incorrect urls" in {
      val Url = "https://avatars.mdst.yandex.net/fail.bin"
      val offer = truckExternalOfferGen(newTasksGen).next.copy(images = Seq(Url))
      val mapper = new ExistingImagesMapper(config)
      val (pub, sub) = createPubSub(mapper.flow())
      sub.request(1)
      pub.sendNext(OfferMessage(offer))
      val offerResponse = sub.expectNextPF {
        case OfferMessage(offer: AutoruExternalOffer) =>
          offer
      }
      offerResponse.incompleteImages shouldEqual Seq(Url)
    }
  }
}
