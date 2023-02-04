package ru.yandex.realty.offers.scan.service.service

import org.junit.runner.RunWith
import ru.yandex.realty.offers.scan.service.config.ExportConfig
import ru.yandex.realty.offers.scan.service.service.OffersScanExporter.UploadFailed
import ru.yandex.realty.offers.scan.service.service.OffersScanExporterSpec.VersionedDaoMock
import ru.yandex.realty.traffic.model.offer.OfferWithRelevance
import ru.yandex.realty.traffic.service.dao.VersionedDao
import ru.yandex.realty.traffic.service.dao.VersionedDao.VersionedDao
import ru.yandex.realty.traffic.utils.StoredEntriesFile
import ru.yandex.realty.traffic.utils.StoredEntriesFile.StoredFormat
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestClock
import zio.test.junit._
import zio.test.mock.Expectation._
import zio.test.mock.{Expectation, Mock}
import zio._
import zio.clock.Clock
import zio.duration._
import zio.random.Random
import zio.test.TestAspect.sequential

@RunWith(classOf[ZTestJUnitRunner])
class OffersScanExporterSpec extends JUnitRunnableSpec {

  private val versionsToStore = 5
  private val configLayer: Layer[Nothing, Has[ExportConfig]] = ZLayer.succeed(ExportConfig(versionsToStore))

  private def baseSpec(
    failAssertion: Option[Assertion[Any]]
  )(calls: Long => Expectation[VersionedDao[OfferWithRelevance]]) = {

    def exportLayer(millis: Long): URLayer[Clock, OffersScanExporter.Requirements] = {
      val dao: ULayer[VersionedDao[OfferWithRelevance]] = calls(millis)

      dao ++ ZLayer.requires[Clock] ++ configLayer
    }

    val effect: ZIO[TestClock with Random with Clock, Any, Unit] =
      for {
        currentMillis <- Gen.long(100L, 100000L).runHead.map(_.get)
        _ <- TestClock.setTime(currentMillis.millis)
        _ <- OffersScanExporter
          .`export`(StoredEntriesFile.join(Seq.empty))
          .provideLayer(exportLayer(currentMillis))
      } yield ()

    if (failAssertion.isDefined) {
      assertM(effect.run)(fails(failAssertion.get))
    } else {
      effect.map(assert(_)(isUnit))
    }
  }

  //noinspection ScalaStyle
  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("OffersScanExporter")(
      testM("should correctly call dao when versions count of versions <= versions to store") {
        val returningVersions: Seq[Long] = 0L to 5

        checkAllM(Gen.fromIterable(0 until versionsToStore)) { existingVersionsCount =>
          baseSpec(None) { currentMillis =>
            VersionedDaoMock.expectUploadCall(currentMillis, anything) ++
              VersionedDaoMock.expectSetCurrentCall(currentMillis) ++
              VersionedDaoMock.expectListCall(returningVersions.take(existingVersionsCount) ++ Seq(currentMillis))
          }
        }
      },
      testM("should correctly call dao when versions count of versions > versions to store") {
        val returningVersions: Seq[Long] = 0L to 15

        checkAllM(Gen.fromIterable(versionsToStore to (versionsToStore + 5))) {
          existingVersionsCount =>
            baseSpec(None) {
              currentMillis =>
                val returningList: Seq[Long] = returningVersions.take(existingVersionsCount) ++ Seq(currentMillis)
                val shouldDropVersions =
                  returningList.sorted.take(returningList.size - versionsToStore)

                VersionedDaoMock.expectUploadCall(currentMillis, anything) ++
                  VersionedDaoMock.expectSetCurrentCall(currentMillis) ++
                  VersionedDaoMock.expectListCall(returningList) ++
                  shouldDropVersions
                    .map(VersionedDaoMock.expectDeleteCall)
                    .reduce(_ && _)
            }
        }
      },
      testM("should correctly call dao when versions count of versions > versions to store") {
        val returningVersions: Seq[Long] = 0L to 15

        checkAllM(Gen.fromIterable(versionsToStore to (versionsToStore + 5))) {
          existingVersionsCount =>
            baseSpec(None) {
              currentMillis =>
                val returningList: Seq[Long] = returningVersions.take(existingVersionsCount) ++ Seq(currentMillis)
                val shouldDropVersions =
                  returningList.sorted.take(returningList.size - versionsToStore)

                VersionedDaoMock.expectUploadCall(currentMillis, anything) ++
                  VersionedDaoMock.expectSetCurrentCall(currentMillis) ++
                  VersionedDaoMock.expectListCall(returningList) ++
                  shouldDropVersions
                    .map(VersionedDaoMock.expectDeleteCall)
                    .reduce(_ && _)
            }
        }
      },
      testM("should try to delete new version on uploading failure") {
        baseSpec(Some(isSubtype[UploadFailed](anything))) { currentMillis =>
          VersionedDaoMock.expectUploadCall(currentMillis, anything, failrure = true) ++
            VersionedDaoMock.expectDeleteCall(currentMillis)
        }
      }
    ) @@ sequential
}

object OffersScanExporterSpec {

  object VersionedDaoMock extends Mock[VersionedDao[OfferWithRelevance]] {

    type UploadI = (Long, StoredEntriesFile[OfferWithRelevance], StoredFormat[OfferWithRelevance])

    case object ListVersions extends Effect[Unit, Throwable, Seq[Long]]
    case object CurrentVersion extends Effect[Unit, Throwable, Long]
    case object SetCurrentVersion extends Effect[Long, Throwable, Unit]
    case object LoadVersion
      extends Effect[(Long, StoredFormat[OfferWithRelevance]), Throwable, StoredEntriesFile[OfferWithRelevance]]
    case object DeleteVersion extends Effect[Long, Throwable, Unit]
    case object UploadVersion extends Effect[UploadI, Throwable, Unit]

    val compose: URLayer[Has[mock.Proxy], VersionedDao[OfferWithRelevance]] =
      ZLayer.fromServiceM { proxy =>
        withRuntime.map { rts =>
          new VersionedDao.Service[OfferWithRelevance] {
            override def listVersions(): Task[Seq[Long]] = proxy(ListVersions)

            override def currentVersion(): Task[Long] = proxy(CurrentVersion)

            override def setCurrentVersion(version: Long): Task[Unit] = proxy(SetCurrentVersion, version)

            override def loadVersion(version: Long)(
              implicit f: StoredEntriesFile.StoredFormat[OfferWithRelevance]
            ): Task[StoredEntriesFile[OfferWithRelevance]] = proxy(LoadVersion, version, f)

            override def deleteVersion(version: Long): Task[Unit] = proxy(DeleteVersion, version)

            override def uploadVersion(version: Long, entries: StoredEntriesFile[OfferWithRelevance])(
              implicit f: StoredEntriesFile.StoredFormat[OfferWithRelevance]
            ): Task[Unit] = proxy(UploadVersion, version, entries, f)
          }
        }
      }

    def expectListCall(result: Seq[Long]): Expectation[VersionedDao[OfferWithRelevance]] =
      ListVersions(value(result))

    def expectSetCurrentCall(version: Long): Expectation[VersionedDao[OfferWithRelevance]] =
      SetCurrentVersion(equalTo(version): Assertion[Long])

    def expectUploadCall(
      version: Long,
      entriesFileA: Assertion[StoredEntriesFile[OfferWithRelevance]],
      failrure: Boolean = false
    ): Expectation[VersionedDao[OfferWithRelevance]] = {
      val paramCheck: Assertion[(Long, StoredEntriesFile[OfferWithRelevance], StoredFormat[OfferWithRelevance])] =
        hasField[UploadI, Long](
          "version",
          _._1,
          equalTo(version)
        ) && hasField[UploadI, StoredEntriesFile[OfferWithRelevance]](
          "file",
          _._2,
          entriesFileA
        )

      if (!failrure) {
        UploadVersion(paramCheck)
      } else {
        UploadVersion(paramCheck, failure(new RuntimeException(s"Something went wrong")))
      }

    }

    def expectDeleteCall(version: Long): Expectation[VersionedDao[OfferWithRelevance]] =
      DeleteVersion(equalTo(version): Assertion[Long])
  }
}
