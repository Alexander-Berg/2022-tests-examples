package vasgen.indexer

import vertis.vasgen.common.IntegerValue.Primitive.Sint64
import vertis.vasgen.common.{IntegerValue, RawValue}
import vertis.vasgen.document._
import vertis.vasgen.options.options.VasgenFieldOptions.IndexType
import vertis.vasgen.options.options.{StorageIndex, VasgenFieldOptions}

package object scandex {

  def pk(v: Int): PrimaryKey =
    PrimaryKey.of(
      vertis
        .vasgen
        .document
        .PrimaryKey
        .Value
        .Int(IntegerValue(Sint64(v.toLong))),
    )

  def rawValue(v: String): RawValue =
    RawValue(vertis.vasgen.common.RawValue.ValueTypeOneof.String(v))

  def createStringDoc(
    docId: Long,
    fields: Map[String, Set[String]],
    version: Long = 0L,
    action: Action = Action.UPSERT,
  ): RawDocument =
    RawDocument(
      pk = Some(pk(docId.toInt)),
      fields
        .map { case (fieldId, values: Set[String]) =>
          RawField(
            Some(FieldId(fieldId, List(fieldId))),
            Some(
              VasgenFieldOptions(
                indexType = IndexType.Storage(StorageIndex()),
                name = Some(fieldId),
                ns = Some("autoru"),
                useInFulltextSearch = Some(true),
              ),
            ),
            values.map(rawValue).toList,
          )
        }
        .toList,
      version = version,
      action = action,
    )

}
