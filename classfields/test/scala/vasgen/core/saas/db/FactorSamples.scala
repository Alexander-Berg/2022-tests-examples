package vasgen.core.saas.db

import vasgen.core.saas.domain._

object FactorSamples {

  val inLove: Factor = Factor(
    Factor.Static,
    SaasName("f_user_in_love_t"),
    1,
    None,
  )

  val inHate: Factor = Factor(
    Factor.Static,
    SaasName("f_user_in_hate_t"),
    2,
    None,
  )

  val staticConfused12: Factor = Factor(
    Factor.Static,
    SaasName("f_user_confused_t"),
    3,
    Some(1.2d),
  )

  val staticConfused34: Factor = Factor(
    Factor.Static,
    SaasName("f_user_confused_t"),
    3,
    Some(3.4d),
  )

  val dynamicConfused: Factor = Factor(
    Factor.Dynamic,
    SaasName("f_user_confused_t"),
    4,
    None,
  )

}
