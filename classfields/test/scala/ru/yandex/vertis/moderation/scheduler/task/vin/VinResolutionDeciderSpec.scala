package ru.yandex.vertis.moderation.scheduler.task.vin

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{AutoruEssentialsGen, InstanceGen}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.scheduler.task.vin.VinResolutionDecider.{
  Caution,
  MismatchedLicensePlate,
  MismatchedLicensePlateTag,
  Source,
  SuspiciousStatus
}
import ru.auto.api.vin.VinResolutionEnums.{ResolutionPart, Status}
import ru.auto.api.vin.VinResolutionModel.{ResolutionEntry, VinIndexResolution}

/**
  * @author semkagtn
  * @author slider5
  */
@RunWith(classOf[JUnitRunner])
class VinResolutionDeciderSpec extends SpecBase {

  private val licensePlate1 = "УУ001У11"
  private val licensePlate2 = "УУ002У22"
  private val licensePlate3 = "УУ003У33"
  private val licensePlateLatin = "Y811CA71"
  private val licensePlateCyrillic = "У811СА71"

  val decider: VinResolutionDecider = VinResolutionDecider.forService(Service.AUTORU)

  import VinResolutionDeciderSpec._

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "status=error",
        source =
          Source(
            InstanceGen.next,
            vinIndexResolution(status = Some(Status.ERROR))
          ),
        expectedResult = Set.empty
      ),
      TestCase(
        description = "status=in_progress",
        source =
          Source(
            InstanceGen.next,
            vinIndexResolution(status = Some(Status.IN_PROGRESS))
          ),
        expectedResult = Set.empty
      ),
      TestCase(
        description = "status=ok",
        source =
          Source(
            InstanceGen.next,
            vinIndexResolution(status = Some(Status.OK))
          ),
        expectedResult = Set.empty
      ),
      TestCase(
        description = "status=undefined",
        source =
          Source(
            InstanceGen.next,
            vinIndexResolution(status = Some(Status.UNDEFINED))
          ),
        expectedResult = Set.empty
      ),
      TestCase(
        description = "no status",
        source =
          Source(
            InstanceGen.next,
            vinIndexResolution(status = None)
          ),
        expectedResult = Set.empty
      ),
      TestCase(
        description = "status=untrusted",
        source =
          Source(
            InstanceGen.next,
            vinIndexResolution(status = Some(Status.UNTRUSTED))
          ),
        expectedResult = Set(SuspiciousStatus(Status.UNTRUSTED))
      ),
      TestCase(
        description = "status=invalid",
        source =
          Source(
            InstanceGen.next,
            vinIndexResolution(status = Some(Status.INVALID))
          ),
        expectedResult = Set(SuspiciousStatus(Status.INVALID))
      ),
      TestCase(
        description = "licensePlate Mismatch",
        source =
          Source(
            InstanceGen.next.copy(
              essentials =
                AutoruEssentialsGen.next.copy(
                  photosLicensePlate = None,
                  licensePlate = Some(licensePlate1)
                )
            ),
            vinIndexResolution(licensePlate = Some(licensePlate2))
          ),
        expectedResult =
          Set(
            MismatchedLicensePlate(
              tag = MismatchedLicensePlateTag.LicensePlate,
              instancePlate = licensePlate1.toUpperCase,
              resolutionPlate = licensePlate2.toUpperCase
            )
          )
      ),
      TestCase(
        description = "photosLicensePlate Mismatch",
        source =
          Source(
            InstanceGen.next.copy(
              essentials =
                AutoruEssentialsGen.next.copy(
                  photosLicensePlate = Some(licensePlate1),
                  hasLicensePlateOnPhotos = Some(true),
                  licensePlate = None
                )
            ),
            vinIndexResolution(licensePlate = Some(licensePlate2))
          ),
        expectedResult =
          Set(
            MismatchedLicensePlate(
              tag = MismatchedLicensePlateTag.PhotosLicensePlate,
              instancePlate = licensePlate1.toUpperCase,
              resolutionPlate = licensePlate2.toUpperCase
            )
          )
      ),
      TestCase(
        description = "photosLicensePlate Mismatch (hasLicensePlateOnPhoto=Some(false))",
        source =
          Source(
            InstanceGen.next.copy(
              essentials =
                AutoruEssentialsGen.next.copy(
                  photosLicensePlate = Some(licensePlate1),
                  hasLicensePlateOnPhotos = Some(false),
                  licensePlate = None
                )
            ),
            vinIndexResolution(licensePlate = Some(licensePlate2))
          ),
        expectedResult = Set.empty
      ),
      TestCase(
        description = "photosLicensePlate Mismatch (hasLicensePlateOnPhoto=None)",
        source =
          Source(
            InstanceGen.next.copy(
              essentials =
                AutoruEssentialsGen.next.copy(
                  photosLicensePlate = Some(licensePlate1),
                  hasLicensePlateOnPhotos = None,
                  licensePlate = None
                )
            ),
            vinIndexResolution(licensePlate = Some(licensePlate2))
          ),
        expectedResult = Set.empty
      ),
      TestCase(
        description = "licensePlate, photosLicensePlate Mismatch",
        source =
          Source(
            InstanceGen.next.copy(
              essentials =
                AutoruEssentialsGen.next.copy(
                  licensePlate = Some(licensePlate1),
                  photosLicensePlate = Some(licensePlate2),
                  hasLicensePlateOnPhotos = Some(true)
                )
            ),
            vinIndexResolution(licensePlate = Some(licensePlate3))
          ),
        expectedResult =
          Set(
            MismatchedLicensePlate(
              tag = MismatchedLicensePlateTag.LicensePlate,
              instancePlate = licensePlate1.toUpperCase,
              resolutionPlate = licensePlate3.toUpperCase
            ),
            MismatchedLicensePlate(
              tag = MismatchedLicensePlateTag.PhotosLicensePlate,
              instancePlate = licensePlate2.toUpperCase,
              resolutionPlate = licensePlate3.toUpperCase
            )
          )
      ),
      TestCase(
        description = "no license plate mismatch",
        source =
          Source(
            InstanceGen.next.copy(
              essentials =
                AutoruEssentialsGen.next.copy(
                  licensePlate = Some(licensePlate1),
                  photosLicensePlate = Some(licensePlate1)
                )
            ),
            vinIndexResolution(licensePlate = Some(licensePlate1))
          ),
        expectedResult = Set.empty
      ),
      TestCase(
        description = "no license plate mismatch in case of latin symbols from VinDecoder",
        source =
          Source(
            InstanceGen.next.copy(
              essentials =
                AutoruEssentialsGen.next.copy(
                  licensePlate = Some(licensePlateCyrillic),
                  photosLicensePlate = Some(licensePlateCyrillic)
                )
            ),
            vinIndexResolution(licensePlate = Some(licensePlateLatin))
          ),
        expectedResult = Set.empty
      )
    )

  "VinResolutionDecider" should {
    testCases.foreach { case TestCase(description, source, expectedResult) =>
      description in {
        val actualResult = decider(source).toSet
        actualResult shouldBe expectedResult
      }
    }
  }
}

object VinResolutionDeciderSpec {

  private case class TestCase(description: String, source: Source, expectedResult: Set[Caution])

  private def vinIndexResolution(status: Option[Status] = None,
                                 licensePlate: Option[String] = None
                                ): VinIndexResolution = {
    val vinIndexResolutionBuilder = VinIndexResolution.newBuilder()
    status.foreach { s: Status =>
      val entryBuilder = ResolutionEntry.newBuilder
      entryBuilder.setPart(ResolutionPart.SUMMARY)
      entryBuilder.setStatus(s)
      vinIndexResolutionBuilder.addEntries(entryBuilder.build)
    }
    licensePlate.foreach(vinIndexResolutionBuilder.setLicensePlate)
    vinIndexResolutionBuilder.build
  }
}
