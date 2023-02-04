package ru.yandex.realty.rent.dao.util

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.rent.dao.actions.FlatDbActions
import ru.yandex.realty.rent.dao.actions.impl.FlatDbActionsImpl
import ru.yandex.realty.rent.dao.{CleanSchemaBeforeEach, RentSpecBase}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.{Flat, FlatUtils}

@RunWith(classOf[JUnitRunner])
class EvalShardKeyFunctionSpec extends WordSpecLike with RentSpecBase with CleanSchemaBeforeEach with RentModelsGen {

  import jdbcProfile.api._

  "eval_shard" should {
    "evaluate shard key which equals to ru.yandex.realty.rent.model.FlatUtils.evaluateShardKey" in new Wiring
    with Data {
      // prepare dataset
      database.run(script"sql/utils/eval_shard.sql").futureValue
      database.run(flatDbActions.insert(sampleFlats)).futureValue
      database.run(sql"select count(*) from flat".as[Int]).futureValue shouldEqual Vector(sampleFlats.size)

      // verify evaluated shards
      sampleShardKeys.foreach {
        case (str, expectedShardKey) =>
          database.run(sql"select eval_shard('#$str')".as[Int]).futureValue shouldEqual Vector(expectedShardKey)
      }

      // verify evaluated shards for real records
      val result: Seq[(String, Int, Int)] =
        database.run(sql"select flat_id, shard_key, eval_shard(flat_id) from flat".as[(String, Int, Int)]).futureValue

      result.forall {
        case (_, shardKey, evaluatedShardKey) => shardKey == evaluatedShardKey
      } shouldEqual true
    }
  }

  trait Wiring {
    val flatDbActions: FlatDbActions = new FlatDbActionsImpl
  }

  trait Data {
    val sampleStrings: Iterable[String] = readableString.next(10)
    val expectedShards: Iterable[Int] = sampleStrings.map(FlatUtils.evaluateShardKey)
    val sampleShardKeys: Iterable[(String, Int)] = sampleStrings.zip(expectedShards)

    val sampleFlats: Iterable[Flat] = flatGen().next(1000)
  }
}
