package ru.yandex.vertis.general.wizard.meta.rules

import common.geobase.Region
import common.geobase.model.RegionIds
import ru.yandex.vertis.general.wizard.core.service.RegionService
import ru.yandex.vertis.general.wizard.meta.rules.impl.ForeignGeoBan.UnsupportedForeignGeo
import ru.yandex.vertis.general.wizard.meta.rules.impl.ForeignGeoBan
import ru.yandex.vertis.general.wizard.model.{MetaWizardRequest, ParseState, RequestMatch}
import ru.yandex.vertis.mockito.MockitoSupport
import zio.{Task, ZIO}
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, ZSpec, _}

object ForeignGeoBanSpec extends DefaultRunnableSpec with MockitoSupport {

  private val InRussiaGeoId: Long = 1
  private val NotInRussiaGeoId: Long = 2

  private def buildRegion(id: Long): Region = {
    val region = mock[Region]
    when(region.id).thenReturn(id)
    when(region.toString).thenReturn(s"[mocked region $id]")
    region
  }

  private val regionService: RegionService.Service = new RegionService.Service {

    override def getRegion(regionId: RegionIds.RegionId): Task[Option[Region]] =
      ZIO.none

    override def getPathToRoot(regionId: RegionIds.RegionId): Task[Seq[Region]] = Task {
      regionId.id match {
        case InRussiaGeoId =>
          Seq(InRussiaGeoId, RegionIds.Russia.id).map(buildRegion)
        case NotInRussiaGeoId =>
          Seq(buildRegion(NotInRussiaGeoId))
        case _ =>
          Seq.empty
      }
    }
  }

  private val node = ForeignGeoBan(regionService)

  private def parseState(requestGeo: Option[Long], geoMatchId: Option[Long]): ParseState =
    ParseState
      .empty(MetaWizardRequest.empty("").copy(geoId = requestGeo))
      .copy(geoMatch = geoMatchId.map(id => RequestMatch.Geo.userInputIndices(Set.empty[Int], buildRegion(id))))

  private def assertFail(parseState: ParseState) = {
    val result = node.process(parseState).run

    assertM(result)(fails(isSubtype[UnsupportedForeignGeo](anything)))
  }

  private def assertPass(parseState: ParseState) =
    for {
      verdict <- node.process(parseState)
    } yield assert(verdict)(equalTo(Seq(parseState)))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ForeignGeoBan RuleNode")(
      testM("pass only geo match") {
        assertPass(parseState(geoMatchId = Some(InRussiaGeoId), requestGeo = None))
      },
      testM("pass only request geo") {
        assertPass(parseState(requestGeo = Some(InRussiaGeoId), geoMatchId = None))
      },
      testM("ban foreign in geo match") {
        assertFail(parseState(geoMatchId = Some(NotInRussiaGeoId), requestGeo = None))
      },
      testM("ban foreign in request") {
        assertFail(parseState(requestGeo = Some(NotInRussiaGeoId), geoMatchId = None))
      },
      testM("pass empties geos") {
        assertPass(parseState(requestGeo = None, geoMatchId = None))
      },
      testM("pass when geo match in russia and request geo not in russia") {
        assertPass(parseState(geoMatchId = Some(InRussiaGeoId), requestGeo = Some(NotInRussiaGeoId)))
      },
      testM("fail when geo match not in russia and request geo in russia") {
        assertFail(parseState(requestGeo = Some(InRussiaGeoId), geoMatchId = Some(NotInRussiaGeoId)))
      }
    )
}
