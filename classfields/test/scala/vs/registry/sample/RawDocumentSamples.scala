package vs.registry.sample

import cats.implicits.catsSyntaxOptionId
import com.google.protobuf.timestamp.Timestamp
import vertis.vasgen.common.RawValue.ValueTypeOneof
import vertis.vasgen.common.*
import vertis.vasgen.document.*
import vertis.vasgen.options.options.{
  EqualityIndex,
  RangeIndex,
  StorageIndex,
  VasgenFieldOptions,
  VasgenTextFieldOptions,
}

object RawDocumentSamples {

  val upsert1: RawDocument = RawDocument(
    pk = Some(PrimaryKey.of(PrimaryKey.Value.Str("123456-789-0001"))),
    version = 1L,
    epoch = 1,
    ttl = Some(Timestamp(200)),
    modifiedAt = Some(Timestamp(0)),
    action = Action.UPSERT,
    quorumText = Seq("deadbeef title string", "message with smth", "1"),
    text = Seq(
      TextField
        .defaultInstance
        .withMetadata(VasgenTextFieldOptions(name = Some("title")))
        .addContent(
          TextValue.defaultInstance.withText("deadbeef title string"),
        ),
      TextField
        .defaultInstance
        .withMetadata(VasgenTextFieldOptions(name = Some("message")))
        .addContent(
          TextValue.defaultInstance.withText("message with deadbeef string"),
        ),
      TextField
        .defaultInstance
        .withMetadata(
          VasgenTextFieldOptions.defaultInstance.withName("default"),
        )
        .addContent(TextValue.defaultInstance.withText("One")),
      TextField
        .defaultInstance
        .withMetadata(
          VasgenTextFieldOptions.defaultInstance.withName("default"),
        )
        .addContent(TextValue.defaultInstance.withText("Two")),
      TextField
        .defaultInstance
        .withMetadata(
          VasgenTextFieldOptions.defaultInstance.withName("default"),
        )
        .addContent(TextValue.defaultInstance.withText("1999")),
    ),
    fields = Seq(
      RawField(
        id = None,
        metadata = Some(
          VasgenFieldOptions(
            name = Some("stone"),
            equality = Some(EqualityIndex()),
          ),
        ),
        values = Seq(
          RawValue(valueType =
            ValueTypeOneof
              .Integer(IntegerValue(IntegerValue.Primitive.Sint64(1L))),
          ),
          RawValue(valueType =
            ValueTypeOneof
              .Integer(IntegerValue(IntegerValue.Primitive.Sint64(2L))),
          ),
        ),
      ),
      RawField(
        id = None,
        metadata = Some(
          VasgenFieldOptions(
            name = Some("grouping_i63"),
            range = RangeIndex().some,
          ),
        ),
        values = Seq(
          RawValue(valueType =
            ValueTypeOneof
              .Integer(IntegerValue(IntegerValue.Primitive.Sint64(1L))),
          ),
          RawValue(valueType =
            ValueTypeOneof
              .Integer(IntegerValue(IntegerValue.Primitive.Sint64(-2L))),
          ),
        ),
      ),
      RawField(
        id = None,
        metadata = Some(
          VasgenFieldOptions(
            name = Some("negative_grouping_i63"),
            range = RangeIndex().some,
          ),
        ),
        values = Seq(
          RawValue(valueType =
            ValueTypeOneof
              .Integer(IntegerValue(IntegerValue.Primitive.Sint64(-2L))),
          ),
        ),
      ),
      RawField(
        id = None,
        metadata = Some(
          VasgenFieldOptions(
            name = Some("empty"),
            equality = Some(EqualityIndex()),
          ),
        ),
        values = Seq(),
      ),
      RawField(
        id = None,
        metadata = Some(
          VasgenFieldOptions(
            name = Some("offer.price.price_in_currency"),
            range = Some(RangeIndex()),
          ),
        ),
        values = Seq(
          RawValue(valueType =
            ValueTypeOneof
              .Integer(IntegerValue(IntegerValue.Primitive.Sint32(1999))),
          ),
        ),
      ),
      RawField(
        id = None,
        metadata = Some(
          VasgenFieldOptions(name = Some("prop"), storage = StorageIndex().some),
        ),
        values = Seq(
          RawValue(valueType = ValueTypeOneof.String("just property")),
        ),
      ),
      RawField(
        id = None,
        metadata = Some(
          VasgenFieldOptions(name = Some("model"), range = Some(RangeIndex())),
        ),
        values = Seq(
          RawValue(valueType =
            ValueTypeOneof.String("please, sort by me properly"),
          ),
        ),
      ),
    ),
    embeddedVectors = Seq(
      EmbeddedVector
        .defaultInstance
        .withName("vector")
        .withVersion(1)
        .withVector(FloatVector.defaultInstance.withValues(Seq(1f, 2f, 3f))),
    ),
  )

  val upsert2: RawDocument = RawDocument(
    pk = Some(PrimaryKey.of(PrimaryKey.Value.Str("123456-789-0003"))),
    version = 1L,
    epoch = 1,
    ttl = Some(Timestamp(200)),
    action = Action.UPSERT,
    modifiedAt = Some(Timestamp(0)),
    quorumText = Seq("", "title                  "),
    text = Seq(
      TextField
        .defaultInstance
        .withMetadata(VasgenTextFieldOptions(name = Some("title")))
        .addContent(
          TextValue.defaultInstance.withText("deadbeef title string"),
        ),
      TextField
        .defaultInstance
        .withMetadata(VasgenTextFieldOptions(name = Some("message")))
        .addContent(
          TextValue.defaultInstance.withText("message with deadbeef string"),
        ),
    ),
    fields = Seq(
      RawField(
        id = None,
        metadata = Some(
          VasgenFieldOptions(
            name = Some("empty"),
            equality = Some(EqualityIndex()),
          ),
        ),
        values = Seq(RawValue(valueType = ValueTypeOneof.String(""))),
      ),
      RawField(
        id = None,
        metadata = Some(
          VasgenFieldOptions(
            name = Some("float"),
            equality = Some(EqualityIndex()),
          ),
        ),
        values = Seq(
          RawValue(valueType =
            ValueTypeOneof
              .Fp(FloatValue.of(FloatValue.Primitive.Double(1.2345f))),
          ),
          RawValue(valueType =
            ValueTypeOneof
              .Fp(FloatValue.of(FloatValue.Primitive.Double(2.3456))),
          ),
        ),
      ),
      RawField(
        id = None,
        metadata = Some(
          VasgenFieldOptions(
            name = Some("has_photo"),
            equality = Some(EqualityIndex()),
          ),
        ),
        values = Seq(
          RawValue(valueType = ValueTypeOneof.Boolean(BooleanValue.of(true))),
        ),
      ),
    ),
  )

  val delete1: RawDocument = RawDocument(
    pk = Some(PrimaryKey.of(PrimaryKey.Value.Str("123456-789-0002"))),
    version = 1L,
    epoch = 1,
    action = Action.DELETE,
    modifiedAt = Some(Timestamp(0)),
    fields = Seq(),
  )

  val withoutId1: RawDocument = RawDocument(
    version = 1L,
    action = Action.UPSERT,
    fields = Seq(
      RawField(
        id = None,
        metadata = Some(
          VasgenFieldOptions(
            name = Some("title"),
            equality = Some(EqualityIndex()),
          ),
        ),
        values = Seq(
          RawValue(valueType = ValueTypeOneof.String("deadbeef title string")),
        ),
      ),
      RawField(
        id = None,
        metadata = Some(
          VasgenFieldOptions(
            name = Some("message"),
            equality = Some(EqualityIndex()),
          ),
        ),
        values = Seq(
          RawValue(valueType =
            ValueTypeOneof.String("message with deadbeef string"),
          ),
        ),
      ),
    ),
  )

  val withoutId2: RawDocument = RawDocument(
    pk = Some(PrimaryKey.of(PrimaryKey.Value.Empty)),
    version = 1L,
    action = Action.UPSERT,
    fields = Seq(
      RawField(
        id = None,
        metadata = Some(
          VasgenFieldOptions(
            name = Some("title"),
            equality = Some(EqualityIndex()),
          ),
        ),
        values = Seq(
          RawValue(valueType = ValueTypeOneof.String("deadbeef title string")),
        ),
      ),
      RawField(
        id = None,
        metadata = Some(
          VasgenFieldOptions(
            name = Some("message"),
            equality = Some(EqualityIndex()),
          ),
        ),
        values = Seq(
          RawValue(valueType =
            ValueTypeOneof.String("message with deadbeef string"),
          ),
        ),
      ),
    ),
  )

}
