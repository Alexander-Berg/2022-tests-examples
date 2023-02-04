package ru.yandex.realty.sitemap.service.entry

import org.mockito.invocation.InvocationOnMock
import ru.yandex.realty.model.offer.SalesAgentCategory
import ru.yandex.realty.sitemap.model.payload.AuthorPayload
import ru.yandex.realty.sitemap.model.payload.AuthorPayload.AuthorType
import ru.yandex.realty.sitemap.service.entry.live.AuthorEntries
import ru.yandex.realty.sitemap.testkit.CustomAssertions
import ru.yandex.realty.traffic.model.payload.LocationPayload
import ru.yandex.realty.traffic.service.RegionService
import ru.yandex.realty.traffic.service.RegionService.RegionService
import ru.yandex.realty.urls.common.ShortOfferInfo
import ru.yandex.vertis.mockito.MockitoSupport
import zio._
import zio.magic._
import zio.test.Assertion._
import zio.test._
import zio.test.junit.JUnitRunnableSpec

import java.time.Instant
import java.util.Date

class AuthorEntriesSpec extends JUnitRunnableSpec with MockitoSupport {

  private val SubjectFederationRgid = 1L
  private val SubjectFederationChild = 2L
  private val NoSubjectFederationParent = 3L

  object RegionServiceMock {

    def testLayer: ULayer[RegionService] = {
      ZLayer.succeed {
        val service = mock[RegionService.Service]
        when(service.getSubjectFederationRgid(?)).thenAnswer { (invocationOnMock: InvocationOnMock) =>
          {
            val rgid = invocationOnMock.getArgument[Long](0)

            Seq(SubjectFederationRgid, SubjectFederationChild)
              .find(_ == rgid)
              .map(_ => ZIO.succeed(SubjectFederationRgid))
              .getOrElse(ZIO.fail(None))
          }
        }
        service
      }
    }

  }

  private def shortOfferInfo(rgid: Long, author: ShortOfferInfo.Author) =
    ShortOfferInfo(
      id = 1,
      creationDate = Date.from(Instant.ofEpochSecond(1000)),
      regionGraphId = rgid,
      author = Some(author)
    )

  private val offers: Seq[ShortOfferInfo] = {
    Seq(
      shortOfferInfo(SubjectFederationRgid, ShortOfferInfo.Author(1, SalesAgentCategory.AGENT)),
      shortOfferInfo(SubjectFederationRgid, ShortOfferInfo.Author(1, SalesAgentCategory.OWNER)),
      shortOfferInfo(SubjectFederationRgid, ShortOfferInfo.Author(1, SalesAgentCategory.UNKNOWN)),
      shortOfferInfo(SubjectFederationRgid, ShortOfferInfo.Author(1, SalesAgentCategory.AGENCY)),
      shortOfferInfo(SubjectFederationRgid, ShortOfferInfo.Author(1, SalesAgentCategory.AGENCY)),
      shortOfferInfo(SubjectFederationRgid, ShortOfferInfo.Author(1, SalesAgentCategory.AGENCY)),
      shortOfferInfo(SubjectFederationRgid, ShortOfferInfo.Author(1, SalesAgentCategory.DEVELOPER)),
      shortOfferInfo(SubjectFederationRgid, ShortOfferInfo.Author(1, SalesAgentCategory.PRIVATE_AGENT)),
      shortOfferInfo(SubjectFederationChild, ShortOfferInfo.Author(1, SalesAgentCategory.AD_AGENCY)), // non subject federation
      shortOfferInfo(NoSubjectFederationParent, ShortOfferInfo.Author(2, SalesAgentCategory.AGENT)),
      shortOfferInfo(SubjectFederationRgid, ShortOfferInfo.Author(2, SalesAgentCategory.AGENCY))
    )
  }

  private def uidHasExpectedTypes(actual: Seq[AuthorPayload], uid: Long)(expected: AuthorPayload.AuthorType*) = {
    val actualTypes = actual.filter(_.id == uid).map(_.`type`).sortBy(_.entryName)

    val expectedTypes = expected.sortBy(_.entryName)

    assert(actualTypes)(
      CustomAssertions.seqEquals(expectedTypes, loggedPrefix = Some(s"author types for $uid"), preSort = None)
    )
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("AuthorEntries") {
      testM("should correctly produce entries") {

        Entries
          .all[AuthorPayload]
          .runCollect
          .map { actual =>
            val onlyExpectedUids =
              assert(actual.map(_.id).toSet)(hasSameElements(Set(1, 2)))

            val onlySubjectFederationRgids = assert(
              actual
            )(forall(hasField("location", _.location, equalTo(LocationPayload(rgids = Set(SubjectFederationRgid))))))

            onlyExpectedUids && onlySubjectFederationRgids &&
            uidHasExpectedTypes(actual, 2)(AuthorType.Agency) &&
            uidHasExpectedTypes(actual, 1)(AuthorType.values: _*)
          }
          .inject(
            ZLayer.succeed(offers),
            RegionServiceMock.testLayer,
            AuthorEntries.layer
          )
      }
    }
}
