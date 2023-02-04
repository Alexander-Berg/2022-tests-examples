package ru.yandex.vertis.moderation.api.v1.service.instance.single

import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.{eq => meq}
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.RequestContext
import ru.yandex.vertis.moderation.dao.InstanceDao
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{EssentialsPatch, ExpiredInstance, Instance}
import ru.yandex.vertis.moderation.model.signal.{NoMarker, Signal, SignalSet, SignalSwitchOff, Tombstone}
import ru.yandex.vertis.moderation.model.{InstanceId, ModerationRequest, SignalKey}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.{DateTimeUtil, HandlerSpecBase}

import scala.collection.mutable
import scala.concurrent.Future

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class InstanceSingleHandlerSpec extends HandlerSpecBase {

  import akka.http.scaladsl.model.StatusCodes.{NotFound, OK}

  private val service: Service = ServiceGen.next
  private val pushApiService = environmentRegistry(service).pushApiInstanceService
  private val instanceDao = environmentRegistry(service).instanceDao

  override def basePath: String = s"/api/1.x/$service/instance/single"

  "pushInstance" should {

    "invoke correct method" in {
      val instanceSource = InstanceSourceGen.next.copy(signals = Set.empty)
      Post(url("/"), instanceSource) ~> route ~> check {
        status shouldBe OK
        there.was(one(pushApiService).upsert(meq(instanceSource))(any[RequestContext]))
      }
    }
  }

  "getInstanceByExternalId" should {

    "invoke correct method" in {
      val instance = InstanceGen.next
      updateInstanceDao(instance, instanceDao)
      val externalId = instance.externalId
      Post(url("/current"), externalId) ~> route ~> check {
        status shouldBe OK
        there.was(one(pushApiService).get(externalId))
      }
    }

    "returns 404 if no such instance" in {
      val externalId = ExternalIdGen.next
      Post(url("/current"), externalId) ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "appendSignals" should {

    "invoke correct method" in {
      val instance = InstanceGen.next
      updateInstanceDao(instance, instanceDao)
      val externalId = instance.externalId
      val signalSource = SignalSourceGen.next.withMarker(NoMarker)
      val request = ModerationRequest.AppendSignals.withInitialDepth(externalId, Seq(signalSource), DateTimeUtil.now())
      Put(url("/signals"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).appendSignals(meq(externalId), meq(Seq(signalSource)), meq(false))(any[RequestContext])
        )
      }
    }

    "returns 200 if no such instance" in {
      val externalId = ExternalIdGen.next
      val signalSource = SignalSourceGen.next.withMarker(NoMarker)
      val request = ModerationRequest.AppendSignals.withInitialDepth(externalId, Seq(signalSource), DateTimeUtil.now())
      Put(url("/signals"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).appendSignals(meq(externalId), meq(Seq(signalSource)), meq(false))(any[RequestContext])
        )
      }
    }

    "invoke correct method with force update journal" in {
      val instance = InstanceGen.next
      updateInstanceDao(instance, instanceDao)
      val externalId = instance.externalId
      val signalSource = SignalSourceGen.next.withMarker(NoMarker)
      val request = ModerationRequest.AppendSignals.withInitialDepth(externalId, Seq(signalSource), DateTimeUtil.now())
      Put(url("/signals?force_journal_update=true"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).appendSignals(meq(externalId), meq(Seq(signalSource)), meq(true))(any[RequestContext])
        )
      }
    }
  }

  "removeSignals" should {

    "invoke correct method" in {
      val signal = SignalGen.withoutMarker.next
      val instance = InstanceGen.next.copy(signals = SignalSet(signal))
      updateInstanceDao(instance, instanceDao)
      val request =
        ModerationRequest.RemoveSignals.withInitialDepth(instance.externalId, Set(signal.key), None, DateTimeUtil.now())
      Put(url("/signals/remove"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).removeSignals(meq(instance.externalId), meq(Set(signal.key)), meq(None))(
            any[RequestContext]
          )
        )
      }
    }

    "invoke correct method when source is specified" in {
      val signal = SignalGen.withoutMarker.next
      val instance = InstanceGen.next.copy(signals = SignalSet(signal))
      val sourceOpt = Some(ManualSourceGen.next)
      updateInstanceDao(instance, instanceDao)
      val request =
        ModerationRequest.RemoveSignals.withInitialDepth(
          instance.externalId,
          Set(signal.key),
          sourceOpt,
          DateTimeUtil.now()
        )
      Put(url("/signals/remove"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).removeSignals(meq(instance.externalId), meq(Set(signal.key)), meq(sourceOpt))(
            any[RequestContext]
          )
        )
      }
    }

    "returns 200 if no such instance" in {
      val externalId = ExternalIdGen.next
      val signalKey = SignalGen.withoutMarker.next.key

      val request =
        ModerationRequest.RemoveSignals.withInitialDepth(externalId, Set(signalKey), None, DateTimeUtil.now())
      Put(url("/signals/remove"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).removeSignals(meq(externalId), meq(Set(signalKey)), meq(None))(any[RequestContext])
        )
      }
    }

    "returns 200 if no such instance when source is specified" in {
      val externalId = ExternalIdGen.next
      val signalKey = SignalGen.withoutMarker.next.key
      val sourceOpt = Some(ManualSourceGen.next)

      val request =
        ModerationRequest.RemoveSignals.withInitialDepth(externalId, Set(signalKey), sourceOpt, DateTimeUtil.now())
      Put(url("/signals/remove"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).removeSignals(meq(externalId), meq(Set(signalKey)), meq(sourceOpt))(any[RequestContext])
        )
      }
    }
  }

  "addSwitchOffs" should {

    "invoke correct method" in {
      val signal = SignalGen.withoutMarker.next
      val instance = InstanceGen.next.copy(signals = SignalSet(signal))
      updateInstanceDao(instance, instanceDao)
      val signalSwitchOffSource = signalSwitchOffSourceGen(signal.key).next
      val request =
        ModerationRequest.AddSwitchOffs.withInitialDepth(
          instance.externalId,
          Seq(signalSwitchOffSource),
          DateTimeUtil.now()
        )
      Put(url("/switch-offs"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).addSwitchOffs(meq(instance.externalId), meq(Seq(signalSwitchOffSource)))(
            any[RequestContext]
          )
        )
      }
    }

    "returns 200 if no such signal" in {
      val instance = InstanceGen.next
      updateInstanceDao(instance, instanceDao)
      val signalSwitchOffSource = SignalSwitchOffSourceGen.next
      val request =
        ModerationRequest.AddSwitchOffs.withInitialDepth(
          instance.externalId,
          Seq(signalSwitchOffSource),
          DateTimeUtil.now()
        )
      Put(url("/switch-offs"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).addSwitchOffs(meq(instance.externalId), meq(Seq(signalSwitchOffSource)))(
            any[RequestContext]
          )
        )
      }
    }

    "returns 200 if no such instance" in {
      val externalId = ExternalIdGen.next
      val signalSwitchOffSource = SignalSwitchOffSourceGen.next
      val request =
        ModerationRequest.AddSwitchOffs.withInitialDepth(externalId, Seq(signalSwitchOffSource), DateTimeUtil.now())
      Put(url("/switch-offs"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).addSwitchOffs(meq(externalId), meq(Seq(signalSwitchOffSource)))(any[RequestContext])
        )
      }
    }
  }

  "removeSwitchOffs" should {

    "invoke correct method" in {
      val signal = SignalGen.withoutMarker.next
      val instance = InstanceGen.next.copy(signals = SignalSet(signal))
      val signalSwitchOff = SignalSwitchOffGen.next
      updateInstanceDao(instance, instanceDao)
      addSwitchOffs(instance.id, Map(signal.key -> signalSwitchOff), instance.signals)

      val request =
        ModerationRequest.DeleteSwitchOffs.withInitialDepth(
          instance.externalId,
          Set(signal.key),
          None,
          DateTimeUtil.now()
        )
      Put(url("/switch-offs/remove"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).deleteSwitchOffs(meq(instance.externalId), meq(Set(signal.key)), meq(None))(
            any[RequestContext]
          )
        )
      }
    }

    "invoke correct method when source is specified" in {
      val signal = SignalGen.withoutMarker.next
      val instance = InstanceGen.next.copy(signals = SignalSet(signal))
      val signalSwitchOff = SignalSwitchOffGen.next
      val sourceOpt = Some(ManualSourceGen.next)
      updateInstanceDao(instance, instanceDao)
      addSwitchOffs(instance.id, Map(signal.key -> signalSwitchOff), instance.signals)

      val request =
        ModerationRequest.DeleteSwitchOffs.withInitialDepth(
          instance.externalId,
          Set(signal.key),
          sourceOpt,
          DateTimeUtil.now()
        )
      Put(url("/switch-offs/remove"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).deleteSwitchOffs(meq(instance.externalId), meq(Set(signal.key)), meq(sourceOpt))(
            any[RequestContext]
          )
        )
      }
    }

    "returns 200 if no such switchOff" in {
      val signal = SignalGen.withoutMarker.next
      val instance = InstanceGen.next.copy(signals = SignalSet(signal))
      updateInstanceDao(instance, instanceDao)

      val request =
        ModerationRequest.DeleteSwitchOffs.withInitialDepth(
          instance.externalId,
          Set(signal.key),
          None,
          DateTimeUtil.now()
        )
      Put(url("/switch-offs/remove"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).deleteSwitchOffs(meq(instance.externalId), meq(Set(signal.key)), meq(None))(
            any[RequestContext]
          )
        )
      }
    }

    "returns 200 if no such switchOff when source is specified" in {
      val signal = SignalGen.withoutMarker.next
      val instance = InstanceGen.next.copy(signals = SignalSet(signal))
      val sourceOpt = Some(ManualSourceGen.next)
      updateInstanceDao(instance, instanceDao)

      val request =
        ModerationRequest.DeleteSwitchOffs.withInitialDepth(
          instance.externalId,
          Set(signal.key),
          sourceOpt,
          DateTimeUtil.now()
        )
      Put(url("/switch-offs/remove"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).deleteSwitchOffs(meq(instance.externalId), meq(Set(signal.key)), meq(sourceOpt))(
            any[RequestContext]
          )
        )
      }
    }

    "returns 200 if no such instance" in {
      val externalId = ExternalIdGen.next
      val signalKey = SignalGen.withoutMarker.next.key

      val request =
        ModerationRequest.DeleteSwitchOffs.withInitialDepth(externalId, Set(signalKey), None, DateTimeUtil.now())
      Put(url("/switch-offs/remove"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).deleteSwitchOffs(meq(externalId), meq(Set(signalKey)), meq(None))(any[RequestContext])
        )
      }
    }

    "returns 200 if no such instance when source is specified" in {
      val externalId = ExternalIdGen.next
      val signalKey = SignalGen.withoutMarker.next.key
      val sourceOpt = Some(ManualSourceGen.next)

      val request =
        ModerationRequest.DeleteSwitchOffs.withInitialDepth(externalId, Set(signalKey), sourceOpt, DateTimeUtil.now())
      Put(url("/switch-offs/remove"), request) ~> route ~> check {
        status shouldBe OK
        there.was(
          one(pushApiService).deleteSwitchOffs(meq(externalId), meq(Set(signalKey)), meq(sourceOpt))(
            any[RequestContext]
          )
        )
      }
    }
  }

  private def updateInstanceDao(newInstance: Instance, instanceDao: InstanceDao[Future]): Unit = {
    instanceDao.upsert(EssentialsPatch.fromInstance(newInstance)).futureValue
    instanceDao.updateContext(newInstance.id, newInstance.context).futureValue
    instanceDao
      .changeSignalsAndSwitchOffs(
        newInstance.id,
        newInstance.signals.signalMap,
        newInstance.signals.switchOffMap,
        SignalSet.Empty
      )
      .futureValue
  }

  private def addSwitchOffs(id: InstanceId, switchOffs: Map[SignalKey, SignalSwitchOff], oldSignals: SignalSet) =
    instanceDao.changeSignalsAndSwitchOffs(id, Map.empty, switchOffs.mapValues(Right(_)), oldSignals).futureValue
}
