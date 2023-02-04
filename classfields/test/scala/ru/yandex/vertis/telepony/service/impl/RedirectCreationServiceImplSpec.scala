package ru.yandex.vertis.telepony.service.impl

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import org.mockito.Mockito
import org.scalacheck.Gen
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.client.TeleponyClientImpl.TeleponyClientException
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.json.redirect.ActualRedirectView
import ru.yandex.vertis.telepony.model.RedirectOptions.RedirectCallbackInfo
import ru.yandex.vertis.telepony.model.{ActualRedirect, Phone, RedirectKey, RedirectOptions}
import ru.yandex.vertis.telepony.service.RedirectInspectService.ComputedCreateRequest
import ru.yandex.vertis.telepony.service.RedirectInspectService.GetResult.{Complained, NoRedirects, Suitable, Unhealthy}
import ru.yandex.vertis.telepony.service.distributed.ServiceProvider
import ru.yandex.vertis.telepony.service.{RedirectCreationService, RedirectInspectService, RedirectServiceV2}
import ru.yandex.vertis.telepony.util.Threads
import ru.yandex.vertis.telepony.util.random.IdUtil
import ru.yandex.vertis.telepony.{SampleHelper, SpecBase}

import scala.concurrent.Future

/**
  * @author neron
  */
class RedirectCreationServiceImplSpec extends SpecBase with MockitoSupport {

  import Threads.lightWeightTasksEc

  trait Test {
    val mockRIS = mock[RedirectInspectService]
    val mockRS = mock[RedirectServiceV2]
    val mockLocalRCS = mock[RedirectCreationService]
    val mockSP = mock[ServiceProvider[RedirectCreationService]]
    val mockRCS = mock[RedirectCreationServiceImpl] // local or remote

    val rc = SampleHelper.rc

    val request = {
      val r = createRequestV2Gen(PhoneGen).next
      r.copy(options = r.options.map(_.copy(callbackInfo = None)))
    }

    def toKey(redirectKey: RedirectKey): String = {
      val tagValue = s"tag_${redirectKey.tag.asOption.getOrElse("")}"
      val hashBytes = Hashing
        .murmur3_128()
        .newHasher()
        .putString(redirectKey.objectId.value, Charsets.UTF_8)
        .putString(redirectKey.target.value, Charsets.UTF_8)
        .putString(tagValue, Charsets.UTF_8)
        .hash()
        .asBytes()
      IdUtil.compact64(hashBytes)
    }

    val redirect = ActualRedirectGen.next
    val redirectView = ActualRedirectView.asView(redirect)
    val keyHash = toKey(request.key)
    val allRedirects: Iterable[ActualRedirect] = List(redirect)
  }

  private val UkrainianNumber = Phone("+380664324213")

  "RedirectCreationServiceImpl" should {
    "fail create" when {
      "target is not Russian number" in new Test {
        val service = new RedirectCreationServiceImpl(mockRIS, mockRS, mockLocalRCS, mockSP)
        val spoiledRequest = request.copy(key = request.key.copy(target = UkrainianNumber))
        val f = service.getOrCreate(spoiledRequest)(rc).failed.futureValue
        f shouldBe a[IllegalArgumentException]
      }
      "operator number is not Russian number" in new Test {
        val service = new RedirectCreationServiceImpl(mockRIS, mockRS, mockLocalRCS, mockSP)
        val spoiledRequest = request.copy(operatorNumber = Some(UkrainianNumber))
        val f = service.getOrCreate(spoiledRequest)(rc).failed.futureValue
        f shouldBe a[IllegalArgumentException]
      }
    }
    "return suitable if exists" in new Test {
      val getResult = Suitable(redirect)
      when(mockRS.get(request.key)(rc)).thenReturn(Future.successful(allRedirects))
      when(mockRIS.getExisting(allRedirects, request)).thenReturn(Future(getResult))
      when(mockRS.touchRedirectAsync(request, redirect)).thenAnswer(_ => ())

      val service = new RedirectCreationServiceImpl(mockRIS, mockRS, mockLocalRCS, mockSP)
      service.getOrCreate(request)(rc).futureValue

      Mockito.verify(mockRS).get(request.key)(rc)
      Mockito.verify(mockRIS).getExisting(allRedirects, request)
      Mockito.verify(mockRS).touchRedirectAsync(request, redirect)
    }

    "not try create if candidates is empty" in new Test {
      val fallbackResult = Gen.oneOf(Complained(redirect), Unhealthy(redirect)).next
      val computedResult =
        ComputedCreateRequest(request = null, geoResponse = null, candidates = Nil, getResult = fallbackResult)
      when(mockRS.get(request.key)(rc)).thenReturn(Future.successful(allRedirects))
      when(mockRIS.getExisting(allRedirects, request)).thenReturn(Future(fallbackResult))
      when(mockRIS.buildRequest(request, fallbackResult)).thenReturn(Future(computedResult))
      when(mockRS.touchRedirectAsync(request, redirect)).thenAnswer(_ => ())

      val service = new RedirectCreationServiceImpl(mockRIS, mockRS, mockLocalRCS, mockSP)
      service.getOrCreate(request)(rc).futureValue

      Mockito.verify(mockRS).get(request.key)(rc)
      Mockito.verify(mockRIS).getExisting(allRedirects, request)
      Mockito.verify(mockRIS).buildRequest(request, fallbackResult)
      Mockito.verify(mockRS).touchRedirectAsync(request, redirect)
    }

    "delegate redirect creation" in new Test {
      when(mockRS.get(request.key)(rc)).thenReturn(Future.successful(allRedirects))
      when(mockRIS.getExisting(allRedirects, request)).thenReturn(Future(NoRedirects))
      when(mockRCS.getOrCreate(request)(rc)).thenReturn(Future.successful(Right(redirectView)))
      when(mockSP.getForKey(keyHash)).thenReturn(mockRCS)

      val service = new RedirectCreationServiceImpl(mockRIS, mockRS, mockLocalRCS, mockSP)
      service.getOrCreate(request)(rc).futureValue

      Mockito.verify(mockRS).get(request.key)(rc)
      Mockito.verify(mockRIS).getExisting(allRedirects, request)
      Mockito.verify(mockSP).getForKey(keyHash)
      Mockito.verify(mockRCS).getOrCreate(request)(rc)
    }

    "recover unexpected client exception" in new Test {
      when(mockRS.get(request.key)(rc)).thenReturn(Future.successful(allRedirects))
      when(mockRIS.getExisting(allRedirects, request)).thenReturn(Future(NoRedirects))
      when(mockRCS.getOrCreate(request)(rc)).thenReturn(Future.failed(new RuntimeException("Oops")))
      when(mockSP.getForKey(keyHash)).thenReturn(mockRCS)
      when(mockLocalRCS.getOrCreate(request)(rc)).thenReturn(Future.successful(Right(redirectView)))

      val service = new RedirectCreationServiceImpl(mockRIS, mockRS, mockLocalRCS, mockSP)
      service.getOrCreate(request)(rc).futureValue

      Mockito.verify(mockRS).get(request.key)(rc)
      Mockito.verify(mockRIS).getExisting(allRedirects, request)
      Mockito.verify(mockSP).getForKey(keyHash)
      Mockito.verify(mockRCS).getOrCreate(request)(rc)
      Mockito.verify(mockLocalRCS).getOrCreate(request)(rc)
    }

    "not recover expected client exception" in new Test {
      when(mockRS.get(request.key)(rc)).thenReturn(Future.successful(allRedirects))
      when(mockRIS.getExisting(allRedirects, request)).thenReturn(Future(NoRedirects))
      when(mockRCS.getOrCreate(request)(rc)).thenReturn(Future.successful(Left(TeleponyClientException("hi", 500))))
      when(mockSP.getForKey(keyHash)).thenReturn(mockRCS)

      val service = new RedirectCreationServiceImpl(mockRIS, mockRS, mockLocalRCS, mockSP)
      val th = service.getOrCreate(request)(rc).futureValue.swap.getOrElse(???)
      th shouldBe a[TeleponyClientException]

      Mockito.verify(mockRS).get(request.key)(rc)
      Mockito.verify(mockRIS).getExisting(allRedirects, request)
      Mockito.verify(mockSP).getForKey(keyHash)
      Mockito.verify(mockRCS).getOrCreate(request)(rc)
    }

    "fail to getOrCreate if invalid options" in new Test {
      val service = new RedirectCreationServiceImpl(mockRIS, mockRS, mockLocalRCS, mockSP)
      val requestWithUnexpectedOptions: RedirectServiceV2.CreateRequest = request.copy(options = Some(
        RedirectOptions.Empty.copy(
          callerIdMode = Some(true),
          callbackInfo = Some(RedirectCallbackInfo(CallPeriodsGen.next, Some("some name")))
        )
      )
      )
      service
        .getOrCreate(requestWithUnexpectedOptions)(rc)
        .failed
        .futureValue shouldBe an[RedirectOptions.InvalidOptionsException]
    }

    "not to heck if invalid options and empty callback periods due getOrCreate call" in new Test {
      val service = new RedirectCreationServiceImpl(mockRIS, mockRS, mockLocalRCS, mockSP)
      val requestWithUnexpectedOptions: RedirectServiceV2.CreateRequest = request.copy(options = Some(
        RedirectOptions.Empty.copy(
          callerIdMode = Some(true),
          callbackInfo = Some(RedirectCallbackInfo(Seq.empty, Some("some another name")))
        )
      )
      )
      service
        .getOrCreate(requestWithUnexpectedOptions)(rc)
        .failed
        .futureValue should not be an[RedirectOptions.InvalidOptionsException]
    }
  }

}
