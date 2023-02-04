package ru.yandex.realty.sitemap.service.entry

import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.model.message.ExtDataSchema.DeveloperGeoStatistic
import ru.yandex.realty.sitemap.model.payload.DeveloperPayload
import ru.yandex.realty.sitemap.service.entry.live.DeveloperEntries
import ru.yandex.realty.sitemap.testkit.EntriesSpec
import ru.yandex.realty.sites.DeveloperGeoStatisticStorage
import ru.yandex.realty.sites.DeveloperGeoStatisticStorage.{DeveloperId, Rgid}
import ru.yandex.vertis.mockito.MockitoSupport
import zio.ZLayer
import zio.test.ZSpec
import zio.test.junit.JUnitRunnableSpec
import zio.magic._

class DeveloperEntriesSpec extends JUnitRunnableSpec with MockitoSupport {

  private def makeStatProvider(ids: Seq[Long]): Provider[DeveloperGeoStatisticStorage] =
    () => {
      val storage = mock[DeveloperGeoStatisticStorage]
      val result: Map[DeveloperId, Map[Rgid, DeveloperGeoStatistic]] =
        ids.map(_ -> mock[Map[Rgid, DeveloperGeoStatistic]]).toMap

      when(storage.statisticByDeveloper).thenReturn(result)

      storage
    }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("DeveloperEntries") {
      testM("should correctly return entries") {
        val ids: Seq[Long] = 1L to 100

        EntriesSpec
          .specEffect[DeveloperPayload](
            ids.map(DeveloperPayload(_)),
            preSort = Some(Ordering.by(_.id))
          )
          .inject(
            ZLayer.succeed(makeStatProvider(ids)),
            DeveloperEntries.layer
          )
      }
    }
}
