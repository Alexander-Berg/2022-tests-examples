package ru.yandex.vertis.moisha.impl.autoru.v9

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy

/**
  * Specs on [[AutoRuPolicyV9]]
  */
@RunWith(classOf[JUnitRunner])
class AutoRuPolicyV9Spec
  extends AutoRuPlacementSpec
  with AutoRuPremiumSpec
  with AutoRuSpecialOfferSpec
  with AutoRuBoostSpec
  with AutoRuBadgeSpec {

  val policy: AutoRuPolicy = new AutoRuPolicyV9

}
