package ru.yandex.vertis.moderation.instance

import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.dao.InstanceDao
import ru.yandex.vertis.moderation.instance.InstancePatch.{AddMetadata, DeleteMetadataByType}
import ru.yandex.vertis.moderation.model.SignalKey
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{EssentialsPatch, Instance}
import ru.yandex.vertis.moderation.model.meta.{Metadata, MetadataSet}
import ru.yandex.vertis.moderation.model.signal.{NoMarker, SignalSet, Tombstone}
import ru.yandex.vertis.moderation.util.DateTimeUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * @author semkagtn
  */
trait InstancePatcherSpecBase[F[_]] extends SpecBase {

  def instanceDao: InstanceDao[Future]
  val patcher: InstancePatcher = InstancePatcherImpl
  lazy val dbPatcher: DbInstancePatcher = new DbInstancePatcherImpl(instanceDao)

  private val now = DateTimeUtil.now()
  private val someDaysAgo = now.minusDays(2)

  private val signalWithSwitchOff =
    BanSignalGen.next
      .copy(
        switchOff = Some(signalSwitchOffGen),
        timestamp = someDaysAgo
      )
      .withMarker(NoMarker)

  private val signalWithoutSwitchOff =
    WarnSignalGen.next
      .copy(
        switchOff = None,
        timestamp = someDaysAgo
      )
      .withMarker(NoMarker)

  private val testContext =
    ContextGen.next.copy(
      updateTime = Some(someDaysAgo)
    )
  private val roomsCount = 1
  private val testEssentials =
    RealtyEssentialsGen.next.copy(
      rooms = Some(roomsCount)
    )
  private val testInstance =
    InstanceGen.next.copy(
      essentials = testEssentials,
      signals = SignalSet(signalWithSwitchOff, signalWithoutSwitchOff),
      metadata = MetadataSet.Empty,
      context = testContext,
      essentialsUpdateTime = someDaysAgo
    )
  private val metadataTtl = 1.day
  private val testMetadata = AutoruPhotoLicensePlateMetadataGen.next.copy(ttl = Some(metadataTtl))

  case class Input(inputInstance: Instance,
                   deleteSignal: Option[SignalKey] = None,
                   addMetadata: Option[Metadata] = None
                  )

  case class TestCase(description: String, input: Input, patch: InstancePatch)

  val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "update instance",
        input = Input(testInstance),
        patch =
          InstancePatch(
            essentials =
              Some(
                EssentialsPatch(
                  id = testInstance.id,
                  essentials = testEssentials.copy(rooms = Some(roomsCount + 1)),
                  createTime = testInstance.createTime,
                  essentialsUpdateTime = testInstance.essentialsUpdateTime,
                  Seq.empty
                )
              )
          )
      ),
      TestCase(
        description = "update context",
        input = Input(testInstance),
        patch =
          InstancePatch(
            context = Some(ContextGen.next)
          )
      ),
      TestCase(
        description = "add new signals",
        input = Input(testInstance),
        patch =
          InstancePatch(
            addSignals = {
              val s = tagSignalGen().copy(switchOff = None)
              Map(s.key -> s)
            }
          )
      ),
      TestCase(
        description = "update existent signals",
        input = Input(testInstance),
        patch =
          InstancePatch(
            addSignals = {
              val s =
                signalWithoutSwitchOff.copy(
                  weight = signalWithoutSwitchOff.weight + 1,
                  timestamp = now
                )
              Map(s.key -> s)
            }
          )
      ),
      TestCase(
        description = "add signal if switch off already exists",
        input = Input(testInstance, deleteSignal = Some(signalWithSwitchOff.key)),
        patch =
          InstancePatch(
            addSignals = {
              val s = signalWithSwitchOff.withSwitchOff(None)
              Map(s.key -> s)
            }
          )
      ),
      TestCase(
        description = "update existent signals (changed timestamp only)",
        input = Input(testInstance),
        patch =
          InstancePatch(
            addSignals = {
              val s = signalWithoutSwitchOff.copy(timestamp = now)
              Map(s.key -> s)
            }
          )
      ),
      TestCase(
        description = "delete existing signal",
        input = Input(testInstance),
        patch =
          InstancePatch(
            deleteSignalKeys = Map(signalWithoutSwitchOff.key -> Tombstone(now, None))
          )
      ),
      TestCase(
        description = "delete non-existent signal",
        input = Input(testInstance),
        patch =
          InstancePatch(
            deleteSignalKeys = Map(tagSignalGen().key -> Tombstone(now, None))
          )
      ),
      TestCase(
        description = "add new switch off",
        input = Input(testInstance),
        patch =
          InstancePatch(
            addSwitchOffs = Map(signalWithoutSwitchOff.key -> signalSwitchOffGen())
          )
      ),
      TestCase(
        description = "update already existing switch off",
        input = Input(testInstance),
        patch =
          InstancePatch(
            addSwitchOffs = Map(signalWithSwitchOff.key -> signalSwitchOffGen())
          )
      ),
      TestCase(
        description = "add switch off to non-existent signal",
        input = Input(testInstance),
        patch =
          InstancePatch(
            addSwitchOffs = Map(tagSignalGen().key -> signalSwitchOffGen().copy(timestamp = now))
          )
      ),
      TestCase(
        description = "delete existing switch off",
        input = Input(testInstance),
        patch =
          InstancePatch(
            deleteSwitchOffs = Map(signalWithSwitchOff.key -> Tombstone(now, None))
          )
      ),
      TestCase(
        description = "delete non-existent switch off",
        input = Input(testInstance),
        patch =
          InstancePatch(
            deleteSwitchOffs = Map(tagSignalGen().key -> Tombstone(now, None))
          )
      ),
      TestCase(
        description = "add new metadata",
        input = Input(testInstance),
        patch =
          InstancePatch(
            metadataAction = Some(AuthMetadataGen.next).map(x => Left(AddMetadata(x)))
          )
      ),
      TestCase(
        description = "update existing metadata",
        input = Input(testInstance, addMetadata = Some(testMetadata)),
        patch =
          InstancePatch(
            metadataAction = Some(testMetadata.copy(ttl = Some(metadataTtl + 1.day))).map(x => Left(AddMetadata(x)))
          )
      ),
      TestCase(
        description = "delete exesting metadata",
        input =
          Input(
            testInstance,
            addMetadata = Some(ProvenOwnerMetadataGen.next)
          ),
        patch =
          InstancePatch(
            metadataAction = Some("proven_owner").map(x => Right(DeleteMetadataByType(x)))
          )
      )
    )

  private def tagSignalGen() = {
    val tagSignal = TagSignalGen.next
    tagSignal.withMarker(NoMarker)
  }

  private def signalSwitchOffGen() = {
    val switchOff = SignalSwitchOffGen.next
    switchOff.copy(source = switchOff.source.withMarker(NoMarker))
  }

  "instancePatcher" should {
    testCases.foreach { case TestCase(description, Input(inputInstance, deleteSignal, addMetadata), patch) =>
      description in {
        val payload =
          EssentialsPatch(
            inputInstance.id,
            inputInstance.essentials,
            inputInstance.createTime,
            inputInstance.essentialsUpdateTime,
            Seq.empty
          )
        val id = inputInstance.id
        val signalsToDelete = deleteSignal.map(signalKey => signalKey -> Tombstone(someDaysAgo, None)).toMap
        instanceDao.upsert(payload).futureValue
        instanceDao
          .changeSignalsAndSwitchOffs(
            id,
            inputInstance.signals.signalMapWithoutSwitchOffs.mapValues(Right(_)) ++ signalsToDelete.mapValues(Left(_)),
            inputInstance.signals.switchOffs.mapValues(Right(_)),
            inputInstance.signals
          )
          .futureValue

        addMetadata.foreach { metadata =>
          instanceDao.upsertMetadata(id, metadata, inputInstance.metadata).futureValue
        }
        val instance = instanceDao.getOpt(id, allowExpired = false).futureValue.get

        val patcherResult = patcher.applyPatch(instance, patch)
        dbPatcher.applyPatch(id, patch, inputInstance).futureValue
        val dbResult = instanceDao.getOpt(id, allowExpired = false).futureValue.get

        patcherResult should smartEqual(dbResult)
        patcherResult.updateTime shouldBe dbResult.updateTime
      }
    }
  }
}
