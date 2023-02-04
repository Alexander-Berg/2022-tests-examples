package ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper

import java.time.temporal.ChronoUnit

import com.typesafe.config.Config
import org.mockito.Mockito._
import org.mockito.internal.stubbing.defaultanswers.ReturnsMocks
import ru.auto.api.unification.Unification.CarsUnificationCollection
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.app.CoreComponents
import ru.yandex.vertis.feedprocessor.autoru.dao.LogsOffice7Dao
import ru.yandex.vertis.feedprocessor.autoru.dao.LogsOffice7Dao.FileRef
import ru.yandex.vertis.feedprocessor.autoru.model.Generators
import ru.yandex.vertis.feedprocessor.autoru.scheduler.app.SchedulerComponents
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.AutoruExternalOffer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.AutoruMapperFlow
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.unificator.UnificatorClient
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.AutoruGenerators
import ru.yandex.vertis.feedprocessor.dao.KVClient
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.Messages.OfferMessage
import ru.yandex.vertis.feedprocessor.util.{DummyOpsSupport, StreamTestBase}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future
import scala.concurrent.duration._

class AutoruMapperFlowSpec extends StreamTestBase with TestApplication with MockitoSupport with DummyOpsSupport {

  val kvClient: KVClient = mock[KVClient]
  val logsOffice7Dao: LogsOffice7Dao = mock[LogsOffice7Dao](new ReturnsMocks)
  val mockConfig: Config = mock[Config](new ReturnsMocks)
  val unificatorClient: UnificatorClient = mock[UnificatorClient]
  val coreComponents: CoreComponents = mock[CoreComponents](new ReturnsMocks)
  val components: SchedulerComponents = mock[SchedulerComponents](new ReturnsMocks)
  when(components.coreComponents).thenReturn(coreComponents)
  when(components.unificatorClient).thenReturn(unificatorClient)
  when(components.coreComponents.logsOffice7Dao).thenReturn(logsOffice7Dao)
  when(components.coreComponents.appConf).thenReturn(mockConfig)
  when(coreComponents.kvClient).thenReturn(kvClient)
  when(kvClient.bulkGet(?)).thenReturn(Future.successful(Map.empty[String, String]))
  when(kvClient.withPrefix(?)(?)).thenReturn(kvClient)
  when(unificatorClient.carsUnify(?)).thenReturn(Future.successful(CarsUnificationCollection.getDefaultInstance))
  // cause we need specifically non-0 config params down the code flow @aafa
  when(mockConfig.getDuration(?)).thenReturn(java.time.Duration.of(1, ChronoUnit.HOURS))
  when(mockConfig.getInt(?)).thenReturn(1)

  val autoruMapperFlow = new AutoruMapperFlow(components)

  "AutoruMapperFlow" should {
    "report image counters" in {
      val offer = AutoruGenerators.carExternalOfferGen(Generators.newTasksGen).next
      val fileRef = FileRef(100, "test")
      when(logsOffice7Dao.createOrFindFile(eq(offer.taskContext), ?)).thenReturn(fileRef)

      val (pub, sub) = createPubSub(autoruMapperFlow.flow())

      sub.request(1)
      pub.sendNext(new OfferMessage[AutoruExternalOffer](offer))

      sub.receiveWithin(5.seconds, 1)

      verify(logsOffice7Dao).createOrFindFile(eq(offer.taskContext), ?)
      verify(logsOffice7Dao).incrementImageCounters(
        eq(fileRef.id),
        eq(offer.images.size),
        eq(offer.imageToPhotoId.size)
      )
    }
  }
}
