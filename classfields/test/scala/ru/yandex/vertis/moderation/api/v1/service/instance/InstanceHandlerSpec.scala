package ru.yandex.vertis.moderation.api.v1.service.instance

import org.joda.time.Interval
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.{any => argAny, eq => meq}
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.dao.InstanceDao
import ru.yandex.vertis.moderation.model.SignalKey
import ru.yandex.vertis.moderation.model.context.ContextSource
import ru.yandex.vertis.moderation.model.generators.CoreGenerators
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{EssentialsPatch, ExternalId, Instance, User}
import ru.yandex.vertis.moderation.model.meta.{Metadata, MetadataFetchRequest, MetadataSet}
import ru.yandex.vertis.moderation.model.signal.{
  HoboSignalSource,
  HoboSignalSourceInternal,
  ManualSource,
  Signal,
  SignalSet,
  SignalSource,
  SignalSwitchOff,
  Tombstone
}
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service}
import ru.yandex.vertis.moderation.util.{DateTimeUtil, HandlerSpecBase}
import ru.yandex.vertis.moderation.view.ViewCompanion.MarshallingContext
import ru.yandex.vertis.moderation.{Globals, RequestContext, SpecBase}

import scala.collection.mutable
import scala.concurrent.Future

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class InstanceHandlerSpec extends SpecBase {

  import akka.http.scaladsl.model.StatusCodes.{BadRequest, NotFound, OK}

  def nonHoboSignalSourceGen(service: Service): Gen[SignalSource] =
    signalSourceGenerator(service).suchThat {
      case _: HoboSignalSource => false
      case _                   => true
    }

  private def signalSourceGenerator(service: Service): Gen[SignalSource] =
    CoreGenerators
      .signalSourceGen(service)
      .suchThat(!_.isInstanceOf[HoboSignalSourceInternal])
      .withoutMarker

  private def instanceGenerator(service: Service): Gen[Instance] =
    CoreGenerators
      .instanceGen(service)
      .map(_.copy(metadata = MetadataSet.Empty))

  trait TestContext extends HandlerSpecBase {
    val service: Service = ServiceGen.next
    val apiService = environmentRegistry(service).apiService
    val instanceDao = environmentRegistry(service).instanceDao
    implicit override lazy val marshallingContext = MarshallingContext(service)

    override def basePath: String = s"/api/1.x/$service/instance"
  }

  "getInstanceById" should {

    "works correctly" in {
      new TestContext {
        val instance = instanceGenerator(service).next
        updateInstanceDao(instance, instanceDao)()
        Get(url(s"/${instance.id}")) ~> route ~> check {
          status shouldBe OK
          responseAs[Instance] should smartEqual(instance)
        }
      }
    }

    "returns 404 if no such instance" in {
      new TestContext {
        val instanceId = InstanceIdGen.next
        apiService.get(instanceId).returns(Future.failed(new NoSuchElementException))
        Get(url(s"/$instanceId")) ~> route ~> check {
          status shouldBe NotFound
        }
      }
    }
  }

  "getInstancesByExternalId" should {

    "works correctly" in {
      new TestContext {
        val instance = instanceGenerator(service).next
        val externalId = instance.externalId
        val expectedResult = Seq(instance)
        updateInstanceDao(instance, instanceDao)()
        Get(url(s"/${externalId.user.key}/${externalId.objectId}")) ~> route ~> check {
          status shouldBe OK
          responseAs[Seq[Instance]] shouldBe expectedResult
        }
      }
    }
  }

  "appendSignals" should {

    "invoke correct method" in {
      new TestContext {
        val instance = instanceGenerator(service).next.copy(signals = SignalSet.Empty)
        val signalSources = nonHoboSignalSourceGen(service).next(2).toSeq

        updateInstanceDao(instance, instanceDao)()

        Put(url(s"/${instance.id}/signal"), signalSources) ~> route ~> check {
          status shouldBe OK
          there.was(one(apiService).appendSignals(meq(instance.id), meq(signalSources))(argAny[RequestContext]))
          responseAs[Instance]
        }
      }
    }

    "fail with 400 if apiService returned Failed(IllegalArgumentException)" in {
      new TestContext {
        val instance =
          instanceGenerator(service).next.copy(
            signals = SignalSet.Empty,
            essentials = RealtyEssentialsGen.next,
            metadata = MetadataSet.Empty
          )
        val signalSource =
          HoboSignalSourceGen.withoutMarker.next.copy(
            `type` = HoboCheckType.BANNED_REVALIDATION
          )
        val signalSources = Seq(signalSource, signalSourceGenerator(service).next)
        Put(url(s"/${instance.id}/signal"), signalSources) ~> route ~> check {
          there.was(one(apiService).appendSignals(meq(instance.id), meq(signalSources))(argAny[RequestContext]))
          status shouldBe BadRequest
        }
      }
    }

    "returns 404 if no such instance" in {
      new TestContext {
        val instanceId = InstanceIdGen.next
        val signalSource = signalSourceGenerator(service).withoutMarker.next
        val signalSources = Seq(signalSource)

        Put(url(s"/$instanceId/signal"), signalSources) ~> route ~> check {
          status shouldBe NotFound
        }
      }
    }
  }

  "appendSignalsByExternalId" should {

    "invoke correct method" in {
      new TestContext {
        val instance = instanceGenerator(service).next
        val userId = instance.externalId.user.key
        val objectId = instance.externalId.objectId
        val signalSources = nonHoboSignalSourceGen(service).next(2).toSeq

        updateInstanceDao(instance, instanceDao)()

        Put(url(s"/$userId/$objectId/signal"), signalSources) ~> route ~> check {
          status shouldBe OK
          there.was(
            one(apiService).appendSignals(meq(instance.externalId), meq(signalSources), meq(false), meq(false))(
              argAny[RequestContext]
            )
          )
          responseAs[Instance]
        }
      }
    }

    "invoke correct method if allow expired" in {
      new TestContext {
        val instance = instanceGenerator(service).next
        val userId = instance.externalId.user.key
        val objectId = instance.externalId.objectId
        val signalSources = nonHoboSignalSourceGen(service).next(2).toSeq

        updateInstanceDao(instance, instanceDao)()

        Put(url(s"/$userId/$objectId/signal?allow_expired=true"), signalSources) ~> route ~> check {
          status shouldBe OK
          there.was(
            one(apiService).appendSignals(meq(instance.externalId), meq(signalSources), meq(true), meq(false))(
              argAny[RequestContext]
            )
          )
          responseAs[Instance]
        }
      }
    }

    "invoke correct method with force journal update" in {
      new TestContext {
        val instance = instanceGenerator(service).next
        val userId = instance.externalId.user.key
        val objectId = instance.externalId.objectId
        val signalSources = nonHoboSignalSourceGen(service).next(2).toSeq

        updateInstanceDao(instance, instanceDao)()

        Put(url(s"/$userId/$objectId/signal?force_journal_update=true"), signalSources) ~> route ~> check {
          status shouldBe OK
          there.was(
            one(apiService).appendSignals(meq(instance.externalId), meq(signalSources), meq(false), meq(true))(
              argAny[RequestContext]
            )
          )
          responseAs[Instance]
        }
      }
    }

    "returns 404 if no such instance" in {
      new TestContext {
        val externalId = ExternalIdGen.next
        val userId = externalId.user.key
        val objectId = externalId.objectId
        val signalSource =
          signalSourceGenerator(service).withoutMarker.suchThat {
            case _: HoboSignalSource => false
            case _                   => true
          }.next
        val signalSources = Seq(signalSource)

        Put(url(s"/$userId/$objectId/signal"), signalSources) ~> route ~> check {
          status shouldBe NotFound
        }
      }
    }
  }

  "deleteSignal" should {

    "invoke correct method" in {
      new TestContext {
        val instance = instanceGenerator(service).next
        val signalKey = SignalGen.withoutMarker.next.key

        updateInstanceDao(instance, instanceDao)()

        Delete(url(s"/${instance.id}/signal/$signalKey")) ~> route ~> check {
          status shouldBe OK
          there.was(one(apiService).removeSignal(meq(instance.id), meq(signalKey), meq(None))(argAny[RequestContext]))
          responseAs[Instance]
        }
      }
    }

    "invoke correct method when moderator_id is specified" in {
      new TestContext {
        val instance = instanceGenerator(service).next
        val signalKey = SignalGen.withoutMarker.next.key
        val moderatorId = UserIdGen.next
        val manualSourceOpt = Some(ManualSource(moderatorId))

        updateInstanceDao(instance, instanceDao)()

        Delete(url(s"/${instance.id}/signal/$signalKey?moderator_id=$moderatorId")) ~> route ~> check {
          status shouldBe OK
          there.was(
            one(apiService).removeSignal(meq(instance.id), meq(signalKey), meq(manualSourceOpt))(argAny[RequestContext])
          )
          responseAs[Instance]
        }
      }
    }

    "returns 404 if no such instance" in {
      new TestContext {
        val instanceId = InstanceIdGen.next
        val signalKey = SignalGen.withoutMarker.next.key

        Delete(url(s"/$instanceId/signal/$signalKey")) ~> route ~> check {
          status shouldBe NotFound
        }
      }
    }

    "returns 404 if no such instance when moderator_id is specified" in {
      new TestContext {
        val instanceId = InstanceIdGen.next
        val signalKey = SignalGen.withoutMarker.next.key
        val moderatorId = UserIdGen.next

        Delete(url(s"/$instanceId/signal/$signalKey?moderator_id=$moderatorId")) ~> route ~> check {
          status shouldBe NotFound
        }
      }
    }
  }

  "deleteSignals" should {

    "invoke correct method" in {
      new TestContext {
        val instance = instanceGenerator(service).next
        val signalKey = SignalGen.withoutMarker.next.key

        updateInstanceDao(instance, instanceDao)()

        Delete(url(s"/${instance.id}/signal?keys=$signalKey")) ~> route ~> check {
          status shouldBe OK
          there.was(
            one(apiService).removeSignals(meq(instance.id), meq(Set(signalKey)), meq(None))(argAny[RequestContext])
          )
          responseAs[Instance]
        }
      }
    }

    "invoke correct method when moderator_id is specified" in {
      new TestContext {
        val instance = instanceGenerator(service).next
        val signalKey = SignalGen.withoutMarker.next.key
        val moderatorId = UserIdGen.next
        val manualSourceOpt = Some(ManualSource(moderatorId))

        updateInstanceDao(instance, instanceDao)()

        Delete(url(s"/${instance.id}/signal?keys=$signalKey&moderator_id=$moderatorId")) ~> route ~> check {
          status shouldBe OK
          there.was(
            one(apiService).removeSignals(meq(instance.id), meq(Set(signalKey)), meq(manualSourceOpt))(
              argAny[RequestContext]
            )
          )
          responseAs[Instance]
        }
      }
    }

    "returns 404 if no such instance" in {
      new TestContext {
        val instanceId = InstanceIdGen.next
        val signalKey = SignalGen.withoutMarker.next.key

        Delete(url(s"/$instanceId/signal?keys=$signalKey")) ~> route ~> check {
          status shouldBe NotFound
        }
      }
    }
  }

  "deleteSignalsByExternalId" should {

    "invoke correct method" in {
      new TestContext {
        val instance = instanceGenerator(service).next
        val signalKey = SignalGen.withoutMarker.next.key

        updateInstanceDao(instance, instanceDao)()
        val user = instance.externalId.user.key
        val objectId = instance.externalId.objectId

        Delete(url(s"/$user/$objectId/signal?keys=$signalKey")) ~> route ~> check {
          status shouldBe OK
          there.was(
            one(apiService).removeSignals(meq(instance.externalId), meq(Set(signalKey)), meq(None))(
              argAny[RequestContext]
            )
          )
          responseAs[Instance]
        }
      }
    }

    "invoke correct method when moderator_id is specified" in {
      new TestContext {
        val instance = instanceGenerator(service).next
        val signalKey = SignalGen.withoutMarker.next.key

        updateInstanceDao(instance, instanceDao)()
        val user = instance.externalId.user.key
        val objectId = instance.externalId.objectId
        val moderatorId = UserIdGen.next
        val manualSourceOpt = Some(ManualSource(moderatorId))

        Delete(url(s"/$user/$objectId/signal?keys=$signalKey&moderator_id=$moderatorId")) ~> route ~> check {
          status shouldBe OK
          there.was(
            one(apiService).removeSignals(meq(instance.externalId), meq(Set(signalKey)), meq(manualSourceOpt))(
              argAny[RequestContext]
            )
          )
          responseAs[Instance]
        }
      }
    }

    "returns 404 if no such instance" in {
      new TestContext {
        val externalId = ExternalIdGen.next
        val signalKey = SignalGen.withoutMarker.next.key
        val user = externalId.user.key
        val objectId = externalId.objectId

        Delete(url(s"/$user/$objectId/signal?keys=$signalKey")) ~> route ~> check {
          status shouldBe NotFound
        }
      }
    }
  }

  "changeContextVisibility" should {

    "invoke correct method" in {
      new TestContext {
        val instance = instanceGenerator(service).next
        val instanceId = instance.id
        val userId = instance.externalId.user.key
        val objectId = instance.externalId.objectId
        val visibility = VisibilityGen.next
        val tag = "test-tag"
        val contextSource = ContextSource(visibility, Some(tag))

        updateInstanceDao(instance, instanceDao)()

        Put(url(s"/$userId/$objectId/context?visibility=$visibility&tag=$tag")) ~> route ~> check {
          status shouldBe OK
          there.was(one(apiService).updateContext(meq(instance.externalId), meq(contextSource))(argAny[RequestContext]))
          responseAs[Instance]
        }
      }
    }

    "return 404 if no such instance" in {
      new TestContext {
        val externalId = ExternalIdGen.next
        val userId = externalId.user.key
        val objectId = externalId.objectId
        val visibility = VisibilityGen.next

        Put(url(s"/$userId/$objectId/context?visibility=$visibility")) ~> route ~> check {
          status shouldBe NotFound
        }
      }
    }
  }

  "getArchivedInstancesByExternalId" should {

    "invoke correct method" in {
      new TestContext {
        val externalId = ExternalIdGen.next
        val userId = externalId.user.key
        val objectId = externalId.objectId

        val from = DateTimeUtil.now().minusDays(3).withTimeAtStartOfDay
        val to = DateTimeUtil.now().withTimeAtStartOfDay
        val fromStr = DateTimeUtil.IsoDateFormatter.print(from)
        val toStr = DateTimeUtil.IsoDateFormatter.print(to)
        val timeInterval = new Interval(from, to)

        Get(url(s"/$userId/$objectId/archive?from=$fromStr&to=$toStr")) ~> route ~> check {
          status shouldBe OK
          there.was(one(apiService).getArchived(meq(externalId), meq(timeInterval), anyInt))
        }
      }
    }
  }

  "getInstancesDiffByExternalId" should {

    "invoke correct method" in {
      new TestContext {
        val externalId = ExternalIdGen.next
        val userId = externalId.user.key
        val objectId = externalId.objectId

        val from = DateTimeUtil.now().minusDays(3).withTimeAtStartOfDay
        val to = DateTimeUtil.now().withTimeAtStartOfDay
        val fromStr = DateTimeUtil.IsoDateFormatter.print(from)
        val toStr = DateTimeUtil.IsoDateFormatter.print(to)
        val timeInterval = new Interval(from, to)

        Get(url(s"/$userId/$objectId/diff?from=$fromStr&to=$toStr")) ~> route ~> check {
          status shouldBe OK
          there.was(one(apiService).diff(externalId, timeInterval, addCurrent = false, addSnapshot = true))
        }
      }
    }
  }

  "addSwitchOffs" should {

    "invoke correct method" in {
      new TestContext {
        val signal = signalGen(service).withoutSwitchOff.next
        val instance = instanceGenerator(service).next.copy(signals = SignalSet(signal))
        val userId = instance.externalId.user.key
        val objectId = instance.externalId.objectId

        updateInstanceDao(instance, instanceDao)()

        val switchOffSource = signalSwitchOffSourceGen(signal.key).next
        val switchOffSources = Seq(switchOffSource)

        Put(url(s"/$userId/$objectId/signal/switch-off"), switchOffSources) ~> route ~> check {
          status shouldBe OK
          there.was(
            one(apiService).addSwitchOffs(
              meq(instance.externalId),
              meq(switchOffSources)
            )(argAny[RequestContext])
          )
        }
      }
    }

    "returns 404 if no such instance" in {
      new TestContext {
        val signal = signalGen(service).withoutSwitchOff.next
        val externalId = ExternalIdGen.next
        val userId = externalId.user.key
        val objectId = externalId.objectId

        val switchOffSource = signalSwitchOffSourceGen(signal.key).next
        val switchOffSources = Seq(switchOffSource)

        Put(url(s"/$userId/$objectId/signal/switch-off"), switchOffSources) ~> route ~> check {
          status shouldBe NotFound
        }
      }
    }
  }

  "deleteSwitchOffs" should {

    "invoke correct method" in {
      new TestContext {
        val signal = signalGen(service).withoutMarker.next.withSwitchOff(Some(SignalSwitchOffGen.next))
        val instance = instanceGenerator(service).next.copy(signals = SignalSet(signal))
        val userId = instance.externalId.user.key
        val objectId = instance.externalId.objectId

        updateInstanceDao(instance, instanceDao)()

        Delete(url(s"/$userId/$objectId/signal/switch-off?keys=${signal.key}")) ~> route ~> check {
          status shouldBe OK
          there.was(
            one(apiService)
              .deleteSwitchOffs(meq(ExternalId(User(userId), objectId)), meq(Set(signal.key)), meq(None))(
                argAny[RequestContext]
              )
          )
          responseAs[Instance]
        }
      }
    }

    "invoke correct method when moderator_id is specified" in {
      new TestContext {
        val signal = signalGen(service).withoutMarker.next.withSwitchOff(Some(SignalSwitchOffGen.next))
        val instance = instanceGenerator(service).next.copy(signals = SignalSet(signal))
        val userId = instance.externalId.user.key
        val objectId = instance.externalId.objectId
        val moderatorId = UserIdGen.next
        val manualSourceOpt = Some(ManualSource(moderatorId))

        updateInstanceDao(instance, instanceDao)()

        Delete(
          url(s"/$userId/$objectId/signal/switch-off?keys=${signal.key}&moderator_id=$moderatorId")
        ) ~> route ~> check {
          status shouldBe OK
          there.was(
            one(apiService)
              .deleteSwitchOffs(meq(ExternalId(User(userId), objectId)), meq(Set(signal.key)), meq(manualSourceOpt))(
                argAny[RequestContext]
              )
          )
          responseAs[Instance]
        }
      }
    }

    "returns 404 if no such instance" in {
      new TestContext {
        val signal = SignalGen.withoutMarker.next
        val externalId = ExternalIdGen.next
        val userId = externalId.user.key
        val objectId = externalId.objectId

        Delete(url(s"/$userId/$objectId/signal/switch-off?keys=${signal.key}")) ~> route ~> check {
          status shouldBe NotFound
        }
      }
    }
  }

  "upsertMetadata" should {

    "invoke correct method" in {
      new TestContext {
        val metadataSource = MetadataSourceGen.next
        val instance = instanceGenerator(service).next
        updateInstanceDao(instance, instanceDao)()
        val externalId = instance.externalId
        val userId = externalId.user.key
        val objectId = externalId.objectId

        Put(url(s"/$userId/$objectId/metadata"), metadataSource) ~> route ~> check {
          status shouldBe OK
          there.was(one(apiService).upsertMetadata(meq(externalId), argAny[Metadata])(argAny[RequestContext]))
        }
      }
    }

    "return 404 if no such instance" in {
      new TestContext {
        val metadataSource = MetadataSourceGen.next
        val externalId = ExternalIdGen.next
        val userId = externalId.user.key
        val objectId = externalId.objectId
        Put(url(s"/$userId/$objectId/metadata"), metadataSource) ~> route ~> check {
          status shouldBe NotFound
        }
      }
    }
  }

  "fetchMetadata" should {

    "invoke correct method" in {
      new TestContext {
        val (metadataFetchRequest, instance) =
          MetadataFetchRequestGen.next match {
            case MetadataFetchRequest.Phones(_) =>
              val essentials =
                AutoruEssentialsGen.next.copy(
                  timestampCreate = Some(Globals.OfMinTimestamp.plus(1))
                )
              (
                MetadataFetchRequest.Phones(essentials.getPhones.toSet),
                instanceGenerator(service).next.copy(essentials = essentials)
              )
            case MetadataFetchRequest.YandexMoneyPhones(_) =>
              val essentials = AutoruEssentialsGen.next
              (
                MetadataFetchRequest.YandexMoneyPhones(essentials.getPhones.toSet),
                instanceGenerator(service).next.copy(essentials = essentials)
              )
            case req =>
              (
                req,
                instanceGenerator(service).next
              )
          }

        updateInstanceDao(instance, instanceDao)()
        val externalId = instance.externalId
        val userId = externalId.user.key
        val objectId = externalId.objectId

        Post(url(s"/$userId/$objectId/metadata"), metadataFetchRequest) ~> route ~> check {
          status shouldBe OK
          there.was(one(apiService).fetchMetadata(meq(externalId), meq(metadataFetchRequest))(argAny[RequestContext]))
        }
      }
    }

    "return 404 if no such instance" in {
      new TestContext {
        val metadataFetchRequest = MetadataFetchRequestGen.next
        val externalId = ExternalIdGen.next
        val userId = externalId.user.key
        val objectId = externalId.objectId
        Post(url(s"/$userId/$objectId/metadata"), metadataFetchRequest) ~> route ~> check {
          status shouldBe NotFound
        }
      }
    }
  }

  "deteteMetadata" should {

    val metadata = ProvenOwnerMetadataGen.next
    val metadataSet = MetadataSet(metadata)
    val instance = InstanceGen.next.copy(metadata = metadataSet)
    val externalId = instance.externalId
    val userId = externalId.user.key
    val objectId = externalId.objectId
    val moderatorId = UserIdGen.next
    val manualSource = ManualSource(moderatorId)
    val metaType = "proven_owner"

    "works correctly for proven_owner" in {
      new TestContext {
        updateInstanceDao(instance, instanceDao)()

        Delete(url(s"/$userId/$objectId/metadata/$metaType?moderator_id=$moderatorId")) ~> route ~> check {
          status shouldBe OK
          there.was(
            one(apiService).deleteMetadata(meq(externalId), meq("proven_owner"), meq(manualSource))(
              argAny[RequestContext]
            )
          )
        }
      }
    }

    "return 404 if no such instance" in {
      new TestContext {

        Delete(url(s"/$userId/$objectId/metadata/$metaType?moderator_id=$moderatorId")) ~> route ~> check {
          status shouldBe NotFound
        }
      }
    }
  }
}
