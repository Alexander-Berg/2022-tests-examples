package ru.yandex.vertis.moisha.impl.autoru.v3

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy

/**
  * Specs on [[AutoRuPolicyV3]]
  */
@RunWith(classOf[JUnitRunner])
class AutoRuPolicyV3Spec extends AutoRuPlacementPolicySpec with AutoRuBadgeSpec with AutoRuCertificationSpec {

  val policy: AutoRuPolicy = new AutoRuPolicyV3

}
