package ru.yandex.vertis.moderation.photo.duplicates

import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import org.joda.time.DateTime
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.moderation.dao.CvHashDao
import ru.yandex.vertis.moderation.dao.impl.inmemory.{
  InMemoryInstanceDao,
  InMemorySearchInstanceDao,
  InMemoryStorageImpl
}
import ru.yandex.vertis.moderation.dao.impl.ydb.YdbCvHashDao
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{Essentials, EssentialsPatch, ExternalId, Instance}
import ru.yandex.vertis.moderation.model.meta.MetadataSet
import ru.yandex.vertis.moderation.model.signal.SignalSet
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.Visibility
import ru.yandex.vertis.moderation.util.DateTimeUtil
import ru.yandex.vertis.moderation.{Globals, YdbSpecBase}
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author semkagtn
  */
trait CvHashBasedPhotoDuplicateDeciderSpecBase extends YdbSpecBase {

  implicit val materializer: Materializer =
    ActorMaterializer(
      ActorMaterializerSettings(actorSystem)
        .withSupervisionStrategy(_ => Supervision.Resume)
    )(actorSystem)

  def nextCvHash(): String = stringGen(10, 10).next

  val cvHash1: String = nextCvHash()
  val cvHash2: String = nextCvHash()
  val cvHash3: String = nextCvHash()
  val nonexistentCvHash: String = nextCvHash()
  val today: DateTime = DateTimeUtil.now()
  val yesterday: DateTime = today.minusDays(1)

  class TestContext(val service: Model.Service = ServiceGen.next,
                    val minIntersection: Int = 2,
                    val getLimit: Int = 100
                   ) {
    private val inMemoryStorageImpl = new InMemoryStorageImpl
    val instanceDao = new InMemoryInstanceDao(service, inMemoryStorageImpl)
    val opinionCalculator = Globals.opinionCalculator(service)
    private val searchStorage = new InMemoryStorageImpl
    val searchInstanceDao = new InMemorySearchInstanceDao(searchStorage, opinionCalculator)
    val featureRegistry = new InMemoryFeatureRegistry(BasicFeatureTypes)
    val cvHashDao: CvHashDao = new YdbCvHashDao[F, WithTransaction[F, *]](ydbWrapper)
    implicit val tc: TestContext = this

    def createInstance(createTime: DateTime,
                       essentials: Essentials,
                       visibility: Visibility = Visibility.VISIBLE,
                       signals: SignalSet = SignalSet.Empty,
                       externalId: ExternalId = ExternalIdGen.next
                      ): Instance = {
      val context = ContextGen.next.copy(visibility = visibility)
      val instance =
        instanceGen(externalId).next.copy(
          essentials = essentials,
          createTime = createTime,
          context = context,
          metadata = MetadataSet.Empty,
          signals = signals
        )
      instanceDao.upsert(EssentialsPatch.fromInstance(instance)).futureValue
      instanceDao.updateContext(instance.id, instance.context)
      instanceDao.changeSignalsAndSwitchOffs(
        instance.id,
        instance.signals.signalMap,
        instance.signals.switchOffMap,
        SignalSet.Empty
      )
      cvHashDao.updateCvHashes(instance)
      searchInstanceDao.submit(instance, kps = Seq.empty).futureValue
      instance
    }

    def reset(): Unit = {
      searchStorage.clear()
      inMemoryStorageImpl.clear()
    }
  }
}
