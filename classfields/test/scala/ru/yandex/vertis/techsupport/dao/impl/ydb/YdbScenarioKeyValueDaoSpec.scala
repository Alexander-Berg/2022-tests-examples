package ru.yandex.vertis.vsquality.techsupport.dao.impl.ydb

import org.scalacheck.{Arbitrary, Gen}
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoJson._
import ru.yandex.vertis.vsquality.utils.ydb_utils.WithTransaction
import ru.yandex.vertis.vsquality.techsupport.conversion.proto.ProtoFormatInstances._
import ru.yandex.vertis.vsquality.techsupport.conversion.string.StringFormatInstances._
import ru.yandex.vertis.vsquality.techsupport.dao.impl.ydb.YdbKeyValueDao.{
  KeySerializationSchema,
  ValueSerializationSchema
}
import ru.yandex.vertis.vsquality.techsupport.dao.{KeyValueDao, KeyValueDaoSpec}
import ru.yandex.vertis.vsquality.techsupport.model.ScenarioId
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.ExternalGraphScenario
import ru.yandex.vertis.vsquality.techsupport.util.ydb.YdbSpecBase
import ru.yandex.vertis.vsquality.techsupport.util.{scenarioFromFile, Clearable}

/**
  * @author potseluev
  */
class YdbScenarioKeyValueDaoSpec extends KeyValueDaoSpec[ScenarioId.External, ExternalGraphScenario] with YdbSpecBase {
  private val tableName = "autoru_scenarios"
  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._

  implicit private val keySerializationSchema: KeySerializationSchema[ScenarioId.External] =
    KeySerializationSchema.String()

  implicit private val valueSerializationSchema: ValueSerializationSchema[ExternalGraphScenario] =
    ValueSerializationSchema.Json()

  private val ser: KVStorageSerialization[ScenarioId.External, ExternalGraphScenario] =
    new KVStorageSerialization(tableName)

  override protected def keyValueDao: KeyValueDao[F, ScenarioId.External, ExternalGraphScenario] =
    new YdbKeyValueDao(ydb, tableName)(ser)

  override protected def keyArb: Arbitrary[ScenarioId.External] = implicitly

  private val greetingScenario: ExternalGraphScenario = scenarioFromFile("greeting_scenario.json")
  private val chooseScenario: ExternalGraphScenario = scenarioFromFile("choose_scenario.json")

  override protected def valueArb: Arbitrary[ExternalGraphScenario] =
    Arbitrary(Gen.oneOf(greetingScenario, chooseScenario))

  implicit override protected def clearableDao[C[_], K, V]: Clearable[KeyValueDao[C, K, V]] =
    () => ydb.runTx(ydb.execute(s"DELETE FROM $tableName;")).void.await
}
