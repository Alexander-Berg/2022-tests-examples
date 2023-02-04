package ru.yandex.vertis.moisha.impl.autoru.v5

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy

/**
  * Specs on [[AutoRuPolicyV5]]
  */
@RunWith(classOf[JUnitRunner])
class AutoRuPolicyV5Spec extends AutoRuPlacementPolicySpec with AutoRuBoostSpec with AutoRuBadgeSpec {

  val policy: AutoRuPolicy = new AutoRuPolicyV5

}
