package ru.yandex.vertis.scheduler.impl.zk

import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.scheduler.producer.Producer
import ru.yandex.vertis.scheduler.{Generators, LastJob}

import scala.util.Success

/**
  * Spec on [[LastJobSerializer]]
  *
  * @author dimas
  */
class LastJobSerializerSpec
  extends Matchers
    with WordSpecLike {

  "LastJobSerializer" should {
    "serialize and deserialize all job variations" in {
      Generators.LastJobGen.next(100).foreach {
        job => roundTrip(job)
      }
    }
  }

  private def roundTrip(lastJob: LastJob): Unit = {
    val serialized = LastJobSerializer.serialize(lastJob)
    val result = LastJobSerializer.deserialize(serialized)
    result should be(Success(lastJob))
  }
}