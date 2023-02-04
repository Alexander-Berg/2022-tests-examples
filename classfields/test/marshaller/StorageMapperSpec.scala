package ru.yandex.vertis.general.gost.storage.test.marshaller

import ru.yandex.vertis.general.gost.model.Offer
import ru.yandex.vertis.general.gost.model.testkit.OfferGen._
import ru.yandex.vertis.general.gost.storage.marshaller.StorageMapper
import zio.test._
import zio.test.Assertion.equalTo

object StorageMapperSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("StorageMapper")(
      testM("Correct inactive status mappings") {
        check(anyInactiveOffer) { inactiveOffer =>
          val afterMapping = toStorageAndBack(inactiveOffer)
          assert(inactiveOffer.status)(equalTo(afterMapping.status))
        }
      },
      testM("Correct banned status mappings") {
        check(anyBannedOffer) { bannedOffer =>
          val afterMapping = toStorageAndBack(bannedOffer)
          assert(bannedOffer.status)(equalTo(afterMapping.status))
        }
      },
      testM("Correct removed status mappings") {
        check(anyRemovedOffer) { removedOffer =>
          val afterMapping = toStorageAndBack(removedOffer)
          assert(removedOffer.status)(equalTo(afterMapping.status))
        }
      }
    )

  private def toStorageAndBack(offer: Offer) =
    StorageMapper.fromStorageOffer(
      StorageMapper.toStorageOffer(offer),
      offer.feedInfo.map(StorageMapper.toStorageFeedInfo)
    )
}
