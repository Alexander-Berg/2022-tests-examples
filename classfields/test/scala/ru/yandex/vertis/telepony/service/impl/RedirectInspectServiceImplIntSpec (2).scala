package ru.yandex.vertis.telepony.service.impl

import org.mockito.Mockito
import org.mockito.Mockito.times
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => equ}
import ru.yandex.vertis.telepony.dummy.DummyGeoFallbackRulesService
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.Operators._
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.RedirectInspectService.GetResult._
import ru.yandex.vertis.telepony.service.RedirectInspectService.{ComputedCreateRequest, OperatorNumberAttributes}
import ru.yandex.vertis.telepony.service.RedirectServiceV2.CreateRequest
import ru.yandex.vertis.telepony.service.impl.GeoIdService.GeoCandidatesResponse
import ru.yandex.vertis.telepony.service.logging.LoggingRedirectInspectService
import ru.yandex.vertis.telepony.service.strategy.ChooseOperatorStrategy.OperatorWrapper
import ru.yandex.vertis.telepony.service.strategy.CompositeStrategy
import ru.yandex.vertis.telepony.service.{ComplaintService, OperatorLabelService, PhoneAnalyzerService}
import ru.yandex.vertis.telepony.util.Threads
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}

import scala.concurrent.Future

/**
  * @author neron
  */
class RedirectInspectServiceImplIntSpec extends SpecBase with MockitoSupport with IntegrationSpecTemplate {

  import Threads.lightWeightTasksEc

  trait Test {
    val mockedGeoIdS = mock[GeoIdService]

    val mockedLabel = {
      val m = mock[OperatorLabelService]
      when(m.getUnhealthy).thenReturn(Set.empty[Operator])
      when(m.getSuspended).thenReturn(Set.empty[Operator])
      m
    }
    val mockedStrategy = mock[CompositeStrategy]
    val mockedComplaint = mock[ComplaintService]

    val mockedPhoneAnalyzer = {
      val m = mock[PhoneAnalyzerService]
      when(m.guessOperator(?)).thenReturn(Future.successful(None))
      m
    }

    val dummyGeoFallbackRulesService = new DummyGeoFallbackRulesService()

    val service =
      new RedirectInspectServiceImpl(
        mockedGeoIdS,
        mockedLabel,
        mockedStrategy,
        mockedComplaint,
        mockedPhoneAnalyzer,
        dummyGeoFallbackRulesService
      ) with LoggingRedirectInspectService
  }

  private def createRequest(key: RedirectKey) =
    CreateRequest(
      key = key,
      geoId = None,
      phoneType = None,
      ttl = None,
      antiFraudOptions = Set.empty,
      preferredOperator = None,
      operatorNumber = None,
      options = None
    )

  private def geoCandidates(opn: OperatorNumber) = Future.successful(
    GeoCandidatesResponse(
      generalizedGeoId = opn.geoId,
      inferredPhoneType = opn.phoneType,
      geoCandidates = Seq(opn.geoId)
    )
  )

  "RedirectInspectService" should {
    "return newest existing redirect" in new Test {
      when(mockedComplaint.getActiveComplaint(?)).thenReturn(Future.successful(None))
      val r = ActualRedirectGen.next
      val redirects = ActualRedirectGen.next(5).map(_.copy(key = r.key, source = r.source))
      when(mockedGeoIdS.geoCandidates(?, ?, ?)).thenReturn(geoCandidates(r.source))
      val request = createRequest(r.key)
      val result = service.getExisting(redirects, request).futureValue
      result should ===(Suitable(redirects.maxBy(_.createTime.getMillis)))

      Mockito.verify(mockedGeoIdS).geoCandidates(?, ?, ?)
      Mockito.verify(mockedComplaint).getActiveComplaint(?)
    }

    "not return unhealthy redirects" in new Test {
      when(mockedComplaint.getActiveComplaint(?)).thenReturn(Future.successful(None))
      when(mockedLabel.getUnhealthy).thenReturn(Set(Mts))
      val r = ActualRedirectGen.next
      val mtsRedirect = {
        val mtsR = ActualRedirectGen.next.copy(key = r.key, source = r.source)
        mtsR.copy(source = mtsR.source.copy(account = OperatorAccounts.MtsShared, originOperator = Operators.Mts))
      }
      val mttRedirect = {
        val mttR = ActualRedirectGen.next.copy(key = r.key, source = r.source)
        mttR.copy(source = mttR.source.copy(account = OperatorAccounts.MttShared, originOperator = Operators.Mts))
      }
      when(mockedGeoIdS.geoCandidates(?, ?, ?)).thenReturn(geoCandidates(r.source))
      val request = createRequest(r.key)
      val result = service.getExisting(Seq(mtsRedirect, mttRedirect), request).futureValue
      result should ===(Suitable(mttRedirect))

      Mockito.verify(mockedGeoIdS).geoCandidates(?, ?, ?)
      Mockito.verify(mockedLabel, times(2)).getUnhealthy
      Mockito.verify(mockedComplaint).getActiveComplaint(?)
    }

    "not return suspended redirects" in new Test {
      when(mockedComplaint.getActiveComplaint(?)).thenReturn(Future.successful(None))
      when(mockedLabel.getSuspended).thenReturn(Set(Mts))
      val r = ActualRedirectGen.next
      val mtsRedirect = {
        val mtsR = ActualRedirectGen.next.copy(key = r.key, source = r.source)
        mtsR.copy(source = mtsR.source.copy(account = OperatorAccounts.MtsShared, originOperator = Operators.Mts))
      }
      val mttRedirect = {
        val mttR = ActualRedirectGen.next.copy(key = r.key, source = r.source)
        mttR.copy(source = mttR.source.copy(account = OperatorAccounts.MttShared, originOperator = Operators.Mts))
      }
      when(mockedGeoIdS.geoCandidates(?, ?, ?)).thenReturn(geoCandidates(r.source))
      val request = createRequest(r.key)
      val result = service.getExisting(Seq(mtsRedirect, mttRedirect), request).futureValue
      result should ===(Suitable(mttRedirect))

      Mockito.verify(mockedGeoIdS).geoCandidates(?, ?, ?)
      Mockito.verify(mockedLabel, times(2)).getUnhealthy
      Mockito.verify(mockedComplaint).getActiveComplaint(?)
    }

    "not return temporary redirects when all operators are healthy" in new Test {
      when(mockedComplaint.getActiveComplaint(?)).thenReturn(Future.successful(None))
      val r = ActualRedirectGen.next
      val redirect = {
        ActualRedirectGen.next.copy(key = r.key, source = r.source)
      }
      val temporaryRedirect = {
        val mttR = ActualRedirectGen.next.copy(key = r.key, source = r.source)
        mttR.copy(options = Some(RedirectOptions.Empty.copy(temporary = Some(true))))
      }
      when(mockedGeoIdS.geoCandidates(?, ?, ?)).thenReturn(geoCandidates(r.source))
      val request = createRequest(r.key)
      val result = service.getExisting(Seq(redirect, temporaryRedirect), request).futureValue
      result should ===(Suitable(redirect))

      Mockito.verify(mockedGeoIdS).geoCandidates(?, ?, ?)
      Mockito.verify(mockedComplaint).getActiveComplaint(?)
    }

    "return complained redirect as a fallback redirect" in new Test {
      val r = ActualRedirectGen.next
      val complainedRedirect = {
        ActualRedirectGen.next.copy(key = r.key, source = r.source)
      }

      when(mockedComplaint.getActiveComplaint(equ(complainedRedirect.id))).thenReturn(Future.successful(Some(null)))
      when(mockedGeoIdS.geoCandidates(?, ?, ?)).thenReturn(geoCandidates(r.source))
      val request = createRequest(r.key)
      val result = service.getExisting(Seq(complainedRedirect), request).futureValue
      result should ===(Complained(complainedRedirect))

      Mockito.verify(mockedGeoIdS).geoCandidates(?, ?, ?)
      Mockito.verify(mockedComplaint).getActiveComplaint(equ(complainedRedirect.id))
    }

    "build request" in new Test {
      val r = ActualRedirectGen.next

      when(mockedGeoIdS.geoCandidates(?, ?, ?)).thenReturn(geoCandidates(r.source))
      when(mockedComplaint.getComplainedAttributes(equ(r.key.target)))
        .thenReturn(Future.successful(Set.empty[OperatorNumberAttributes]))
      when(mockedStrategy.decide(equ(r.source.geoId), equ(PhoneTypes.Local), equ(NoRedirects), equ(None), equ(None)))
        .thenReturn(Future.successful(List.empty[OperatorWrapper]))
      when(mockedStrategy.decide(equ(r.source.geoId), equ(PhoneTypes.Mobile), equ(NoRedirects), equ(None), equ(None)))
        .thenReturn(Future.successful(List.empty[OperatorWrapper]))
      val request = createRequest(r.key)

      val computedRequest = service.buildRequest(request, NoRedirects).futureValue
      computedRequest should ===(
        ComputedCreateRequest(
          request = request,
          geoResponse = geoCandidates(r.source).futureValue,
          candidates = List(),
          getResult = NoRedirects
        )
      )

      Mockito.verify(mockedGeoIdS).geoCandidates(?, ?, ?)
      Mockito.verify(mockedComplaint).getComplainedAttributes(equ(r.key.target))
      Mockito
        .verify(mockedStrategy)
        .decide(equ(r.source.geoId), equ(PhoneTypes.Local), equ(NoRedirects), equ(None), equ(None))
      Mockito
        .verify(mockedStrategy)
        .decide(equ(r.source.geoId), equ(PhoneTypes.Mobile), equ(NoRedirects), equ(None), equ(None))
    }

    "return existing ActualRedirect with a different PhoneType (CreateRequest with PhoneTypes.Locale)" in new Test {
      when(mockedComplaint.getActiveComplaint(?)).thenReturn(Future.successful(None))
      val phone = PhoneGen.next
      val redirectKey: RedirectKey = RedirectKey(ObjectId("123"), phone, Tag(Some("testTag")))
      val createRedirectRequest = createRequest(redirectKey).copy(phoneType = Some(PhoneTypes.Local))

      var actualRedirect: ActualRedirect = ActualRedirectGen.next
      actualRedirect = actualRedirect.copy(
        key = redirectKey,
        source = actualRedirect.source.copy(phoneType = PhoneTypes.Mobile)
      )
      when(mockedGeoIdS.geoCandidates(?, ?, ?))
        .thenReturn(geoCandidates(actualRedirect.source))

      val existing = service.getExisting(Seq(actualRedirect), createRedirectRequest)

      existing.futureValue should ===(Suitable(actualRedirect))
    }

    "return existing ActualRedirect with a different PhoneType (CreateRequest with PhoneTypes.Mobile)" in new Test {
      when(mockedComplaint.getActiveComplaint(?)).thenReturn(Future.successful(None))
      val phone = PhoneGen.next
      val redirectKey: RedirectKey = RedirectKey(ObjectId("123"), phone, Tag(Some("testTag")))
      val createRedirectRequest = createRequest(redirectKey).copy(phoneType = Some(PhoneTypes.Mobile))

      var actualRedirect: ActualRedirect = ActualRedirectGen.next
      actualRedirect = actualRedirect.copy(
        key = redirectKey,
        source = actualRedirect.source.copy(phoneType = PhoneTypes.Local)
      )
      when(mockedGeoIdS.geoCandidates(?, ?, ?))
        .thenReturn(geoCandidates(actualRedirect.source))

      val existing = service.getExisting(Seq(actualRedirect), createRedirectRequest)

      existing.futureValue should ===(Suitable(actualRedirect))
    }

    "return existing ActualRedirect with a same PhoneType (CreateRequest with PhoneTypes.Mobile)" in new Test {
      when(mockedComplaint.getActiveComplaint(?)).thenReturn(Future.successful(None))
      val phone = PhoneGen.next
      val redirectKey: RedirectKey = RedirectKey(ObjectId("123"), phone, Tag(Some("testTag")))
      val createRedirectRequest = createRequest(redirectKey).copy(phoneType = Some(PhoneTypes.Mobile))

      var actualRedirectLocal: ActualRedirect = ActualRedirectGen.next
      actualRedirectLocal = actualRedirectLocal.copy(
        key = redirectKey,
        source = actualRedirectLocal.source.copy(phoneType = PhoneTypes.Local)
      )

      var actualRedirectMobile: ActualRedirect = ActualRedirectGen.next
      actualRedirectMobile = actualRedirectMobile.copy(
        key = redirectKey,
        source = actualRedirectMobile.source.copy(phoneType = PhoneTypes.Mobile)
      )

      when(mockedGeoIdS.geoCandidates(?, ?, ?))
        .thenReturn(
          Future.successful(
            GeoCandidatesResponse(
              generalizedGeoId = GeoIdGen.next,
              inferredPhoneType = PhoneTypeGen.next,
              geoCandidates = Seq(actualRedirectMobile.source.geoId, actualRedirectLocal.source.geoId)
            )
          )
        )

      val existing = service.getExisting(Seq(actualRedirectLocal, actualRedirectMobile), createRedirectRequest)

      existing.futureValue should ===(Suitable(actualRedirectMobile))
    }

    "return existing ActualRedirect with a same PhoneType (CreateRequest with PhoneTypes.Local)" in new Test {
      when(mockedComplaint.getActiveComplaint(?)).thenReturn(Future.successful(None))
      val phone = PhoneGen.next
      val redirectKey: RedirectKey = RedirectKey(ObjectId("123"), phone, Tag(Some("testTag")))
      val createRedirectRequest = createRequest(redirectKey).copy(phoneType = Some(PhoneTypes.Local))

      var actualRedirectLocal: ActualRedirect = ActualRedirectGen.next
      actualRedirectLocal = actualRedirectLocal.copy(
        key = redirectKey,
        source = actualRedirectLocal.source.copy(phoneType = PhoneTypes.Local)
      )

      var actualRedirectMobile: ActualRedirect = ActualRedirectGen.next
      actualRedirectMobile = actualRedirectMobile.copy(
        key = redirectKey,
        source = actualRedirectMobile.source.copy(phoneType = PhoneTypes.Mobile)
      )

      when(mockedGeoIdS.geoCandidates(?, ?, ?))
        .thenReturn(
          Future.successful(
            GeoCandidatesResponse(
              generalizedGeoId = GeoIdGen.next,
              inferredPhoneType = PhoneTypeGen.next,
              geoCandidates = Seq(actualRedirectMobile.source.geoId, actualRedirectLocal.source.geoId)
            )
          )
        )

      val existing = service.getExisting(Seq(actualRedirectLocal, actualRedirectMobile), createRedirectRequest)

      existing.futureValue should ===(Suitable(actualRedirectLocal))
    }
  }

}
