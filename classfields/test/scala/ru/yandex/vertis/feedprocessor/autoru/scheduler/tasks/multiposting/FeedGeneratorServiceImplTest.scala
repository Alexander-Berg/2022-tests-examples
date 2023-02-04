package ru.yandex.vertis.feedprocessor.autoru.scheduler.tasks.multiposting

import com.amazonaws.services.s3.transfer.model.UploadResult
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{times, verify}
import org.scalatest.time.{Millis, Seconds, Span}
import ru.auto.api.ApiOfferModel.{Category, OfferStatus}
import ru.auto.api.ApiOfferModel.Multiposting.Classified.ClassifiedName
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.scheduler.converter.ApiOfferModelToAutoruExternalOfferConverter
import ru.yandex.vertis.feedprocessor.autoru.scheduler.s3.FeedsToS3Writer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.palma.MultipostingSettingsService
import ru.yandex.vertis.feedprocessor.autoru.scheduler.tasks.multiposting.FeedGeneratorService.ClientMultipostingStatus
import ru.yandex.vertis.feedprocessor.autoru.scheduler.tasks.multiposting.FeedGeneratorServiceImplTest.{
  TestClientId,
  TestClientId2
}
import ru.yandex.vertis.feedprocessor.services.vos.VosClient
import ru.yandex.vertis.feedprocessor.services.vos.VosClient.SearchFilter
import ru.yandex.vertis.feedprocessor.util.{DummyOpsSupport, StreamTestBase}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class FeedGeneratorServiceImplTest
  extends StreamTestBase
  with MockitoSupport
  with TestApplication
  with DummyOpsSupport {

  private val defaultPatienceConfig: PatienceConfig =
    PatienceConfig(
      timeout = scaled(Span(15, Seconds)),
      interval = scaled(Span(150, Millis))
    )

  implicit override val patienceConfig: PatienceConfig = defaultPatienceConfig

  val vosClient = mock[VosClient]
  val s3Writer = mock[FeedsToS3Writer]
  val converter = mock[ApiOfferModelToAutoruExternalOfferConverter]
  val redirects = mock[ExternalRedirectsService]
  val settings = mock[MultipostingSettingsService]

  val task = new FeedGeneratorServiceImpl(vosClient, s3Writer, converter, redirects, settings)

  "FeedGeneratorService" should {
    "call generate successfully" in {
      when(vosClient.getClientOffers(?, ?)).thenReturn(Future(List.empty))
      when(converter.convertOffers(?, ?)).thenReturn(Future(Seq.empty))
      when(s3Writer.write(?, ?, ?, ?)(?)).thenReturn(Future.successful(new UploadResult))
      when(redirects.redirectsForOffers(?, ?, ?)(?, ?)).thenReturn(Future.successful(None))
      when(settings.hasMultipostingSiteEnabled(?)(?))
        .thenReturn(
          Future.successful(
            List(
              ClientMultipostingStatus(TestClientId, siteMultipostingStatus = true),
              ClientMultipostingStatus(TestClientId2, siteMultipostingStatus = false)
            )
          )
        )

      task.generate(Set(TestClientId, TestClientId2)).futureValue

      val searchFilterCaptor = ArgumentCaptor.forClass(classOf[SearchFilter])
      val classifiedCaptor = ArgumentCaptor.forClass(classOf[Option[ClassifiedName]])
      val invocationsCount = times(5)
      val invocationsCountFirstClient = times(3)
      val invocationsCountSecondClient = times(2)
      val redirectInvocationCount = times(4)

      verify(vosClient, invocationsCountFirstClient).getClientOffers(eq(TestClientId), searchFilterCaptor.capture())
      verify(vosClient, invocationsCountSecondClient).getClientOffers(eq(TestClientId2), searchFilterCaptor.capture())
      verify(converter, invocationsCount).convertOffers(?, ?)
      verify(s3Writer, invocationsCount).write(?, ?, ?, classifiedCaptor.capture())(?)
      verify(redirects, redirectInvocationCount).redirectsForOffers(?, ?, ?)(?, ?)

      val expectedSearchFilterForAvito = SearchFilter(
        category = Category.CARS,
        multipostingStatus = Some(OfferStatus.ACTIVE),
        tags = Some(Seq("avito_posted"))
      )

      val expectedSearchFilterForDrom = SearchFilter(
        category = Category.CARS,
        multipostingStatus = Some(OfferStatus.ACTIVE),
        tags = Some(Seq("drom_posted"))
      )

      val expectedSearchFilterForSite = SearchFilter(
        category = Category.CARS,
        multipostingStatus = Some(OfferStatus.ACTIVE),
        tags = Some(Seq.empty)
      )
      assert(searchFilterCaptor.getAllValues.get(0) == expectedSearchFilterForAvito)
      assert(searchFilterCaptor.getAllValues.get(1) == expectedSearchFilterForDrom)
      assert(searchFilterCaptor.getAllValues.get(2) == expectedSearchFilterForSite)
      assert(searchFilterCaptor.getAllValues.get(3) == expectedSearchFilterForAvito)
      assert(searchFilterCaptor.getAllValues.get(4) == expectedSearchFilterForDrom)
      assert(classifiedCaptor.getAllValues.contains(Some(ClassifiedName.DROM)))
      assert(classifiedCaptor.getAllValues.contains(Some(ClassifiedName.AVITO)))
      assert(classifiedCaptor.getAllValues.contains(None))
      assert(classifiedCaptor.getAllValues.size() == 5)
    }
  }
}

object FeedGeneratorServiceImplTest {
  val TestClientId = 16453L
  val TestClientId2 = 76543L
}
