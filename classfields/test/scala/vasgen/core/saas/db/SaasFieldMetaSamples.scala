package vasgen.core.saas.db

import vasgen.core.saas.domain._
import vasgen.sdk.model
import cats.syntax.all._

object SaasFieldMetaSamples {

  val a1str: FieldMapping = FieldMapping(
    1,
    FieldName("a"),
    SaasName("s_a").some,
    SaasName("s_a").some,
    model.str,
  )

  val b1i32: FieldMapping = FieldMapping(
    1,
    FieldName("b"),
    SaasName("i_b").some,
    SaasName("i_b").some,
    model.i32,
  )

  val c1str: FieldMapping = FieldMapping(
    1,
    FieldName("c"),
    SaasName("s_c").some,
    SaasName("s_c").some,
    model.str,
  )

  val d1i64: FieldMapping = FieldMapping(
    1,
    FieldName("d"),
    SaasName("s_d").some,
    SaasName("s_d").some,
    model.i64,
  )

  val e1str: FieldMapping = FieldMapping(
    1,
    FieldName("e"),
    SaasName("s_e").some,
    SaasName("s_e").some,
    model.str,
  )

  val a2i32: FieldMapping = FieldMapping(
    2,
    FieldName("a"),
    SaasName("i_a").some,
    SaasName("i_a").some,
    model.i32,
  )

  val b2str: FieldMapping = FieldMapping(
    2,
    FieldName("b"),
    SaasName("s_b").some,
    SaasName("s_b").some,
    model.str,
  )

}
