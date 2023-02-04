package ru.yandex.vertis.moisha.impl.autoru.v4

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy

/**
  * Specs on [[AutoRuPolicyV4]]
  */
@RunWith(classOf[JUnitRunner])
class AutoRuPolicyV4Spec extends AutoRuPlacementPolicySpec with AutoRuBadgeSpec {

  val policy: AutoRuPolicy = new AutoRuPolicyV4

}
