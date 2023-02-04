package ru.yandex.vertis.moisha.impl.autoru.v2

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy

/**
  * Specs on [[AutoRuPolicyV2]]
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
@RunWith(classOf[JUnitRunner])
class AutoRuPolicyV1Spec
  extends AutoRuPlacementPolicySpec
  with AutoRuSpecialPolicySpec
  with AutoRuPremiumSpec
  with AutoRuBoostSpec
  with AutoRuCertificationSpec
  with AutoRuOldVasSpec {

  val policy: AutoRuPolicy = new AutoRuPolicyV2

}
