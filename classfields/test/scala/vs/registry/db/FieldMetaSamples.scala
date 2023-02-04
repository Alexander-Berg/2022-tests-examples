package vs.registry.db

import strict.PrimitiveTypeTag
import vs.registry.domain.FieldMeta

object FieldMetaSamples {

  val a1str: FieldMeta = FieldMeta(1, "a", PrimitiveTypeTag.Utf8Type)

  val b1i32: FieldMeta = FieldMeta(1, "b", PrimitiveTypeTag.Int32Type)

  val c1str: FieldMeta = FieldMeta(1, "c", PrimitiveTypeTag.Utf8Type)

  val d1i64: FieldMeta = FieldMeta(1, "d", PrimitiveTypeTag.Int64Type)

  val e1str: FieldMeta = FieldMeta(1, "e", PrimitiveTypeTag.Utf8Type)

  val a2i32: FieldMeta = FieldMeta(2, "a", PrimitiveTypeTag.Int32Type)

  val b2str: FieldMeta = FieldMeta(2, "b", PrimitiveTypeTag.Utf8Type)

}
