package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vos2.workers.workdistribution.WorkersTokens
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class WorkersNamingTest extends AnyWordSpec {

  val namesSet = WorkersTokens.values.map(_.name)
  assert(namesSet.size == WorkersTokens.values.size)
}
