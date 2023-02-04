package ru.yandex.vertis.general.gost.model.test

import ru.yandex.vertis.general.gost.model.Offer
import ru.yandex.vertis.general.gost.model.Photo
import ru.yandex.vertis.general.gost.model.Photo._
import ru.yandex.vertis.general.gost.model.testkit.{OfferGen, OfferUpdateGen}
import zio.test.Assertion._
import zio.test._

object OfferUpdateSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("OfferUpdate")(
      testM("Не затирает результаты скачивания фотографий") {
        checkN(1)(OfferGen.anyOffer, OfferUpdateGen.anyOfferUpdate) { case (offer, update) =>
          val testUpdate = update.copy(photos =
            Seq(
              CompletePhoto(MdsImage("test", "o", 1), Some("http://1.jpg"), Some(PhotoMeta("abc1")), None),
              IncompletePhoto("http://2.jpg", error = false),
              IncompletePhoto("http://4.jpg", error = false)
            )
          )
          val testOffer = offer.copy(photos =
            Seq(
              CompletePhoto(MdsImage("test", "o", 1), Some("http://1.jpg"), Some(PhotoMeta("abc1")), None),
              CompletePhoto(MdsImage("test", "o", 2), Some("http://2.jpg"), Some(PhotoMeta("abc2")), None),
              IncompletePhoto("http://3.jpg", error = false)
            )
          )

          assert(testUpdate.apply(testOffer))(
            hasField(
              "photos",
              _.photos,
              hasSameElements(
                Seq[Photo](
                  CompletePhoto(MdsImage("test", "o", 1), Some("http://1.jpg"), Some(PhotoMeta("abc1")), None),
                  CompletePhoto(MdsImage("test", "o", 2), Some("http://2.jpg"), Some(PhotoMeta("abc2")), None),
                  IncompletePhoto("http://4.jpg", error = false)
                )
              )
            )
          )
        }
      }
    )
  }
}
