package vasgen.indexer.saas.convert

import java.time.Instant

object ConverterSpec extends ZIOSpecDefault with Logging {

  private val converterMock = {
    new SaasConverter[TMessage] {

      override val config: ConverterConfig = ConverterConfig(
        "",
        "",
        "",
        "",
        0,
        enableHighlighting = false,
      )

      override protected def constructUpdateMessage(
        url: String,
        zones: Map[SaasIndexType.Value, Seq[KeyValues]],
        version: Long,
        modifiedAt: Instant,
        ttl: Option[Instant],
      ): TMessage = TMessage.defaultInstance

      override protected def constructRemoveMessage(
        url: String,
        version: Long,
        modifiedAt: Instant,
      ): TMessage = TMessage.defaultInstance

      override val fieldConverter: FieldMappingConverter.Service =
        FieldMappingConverter(
          new FieldMappingStorageStub,
          FieldMappingConverter.Config("t"),
        )
    }
  }
  private val vector             = Array(1f, 2f, 3f)
  private val vectorAsByteString = DssmHelper.toByteStr(vector)

  override def spec
    : Spec[TestEnvironment, TestFailure[Nothing], TestSuccess] = {
    suite("converter")(
      test("[embeddedVectorZones] empty vector") {
        assert(converterMock.embeddedVectorZones(RawDocument.defaultInstance))(
          equalTo(Nil),
        )
      },
      test("[embeddedVectorZones] single vector") {
        val doc = RawDocument
          .defaultInstance
          .withEmbeddedVectors(Seq(mkVector("vector", 1, vector)))
        assert(converterMock.embeddedVectorZones(doc))(
          equalTo(
            Seq(
              NamedValues("vector", Seq(EmbeddingValue(1, vectorAsByteString))),
            ),
          ),
        )
      },
      test("[embeddedVectorZones] 2 vectors, different versions") {
        val doc = RawDocument
          .defaultInstance
          .withEmbeddedVectors(
            Seq(mkVector("vector", 1, vector), mkVector("vector", 2, vector)),
          )
        assert(converterMock.embeddedVectorZones(doc))(
          equalTo(
            Seq(
              NamedValues(
                "vector",
                Seq(
                  EmbeddingValue(1, vectorAsByteString),
                  EmbeddingValue(2, vectorAsByteString),
                ),
              ),
            ),
          ),
        )
      },
      test("[embeddedVectorZones] 2 vectors, same versions") {
        val doc = RawDocument
          .defaultInstance
          .withEmbeddedVectors(
            Seq(mkVector("vector", 1, vector), mkVector("vector", 1, vector)),
          )
        assert(converterMock.embeddedVectorZones(doc))(
          equalTo(
            Seq(
              NamedValues(
                "vector",
                Seq(
                  EmbeddingValue(1, vectorAsByteString),
                  EmbeddingValue(1, vectorAsByteString),
                ),
              ),
            ),
          ),
        )
      },
      test("[embeddedVectorZones] 2 zones, 3vectors") {
        val doc = RawDocument
          .defaultInstance
          .withEmbeddedVectors(
            Seq(
              mkVector("vector", 1, vector),
              mkVector("vector", 2, vector),
              mkVector("array", 3, vector),
            ),
          )
        assert(converterMock.embeddedVectorZones(doc).sortBy(_.name))(
          equalTo(
            Seq(
              NamedValues("array", Seq(EmbeddingValue(3, vectorAsByteString))),
              NamedValues(
                "vector",
                Seq(
                  EmbeddingValue(1, vectorAsByteString),
                  EmbeddingValue(2, vectorAsByteString),
                ),
              ),
            ),
          ),
        )
      },
      test("[textZones] no text zones") {
        val doc = RawDocument.defaultInstance
        assert(
          converterMock
            .textZones(doc)
            .map(y =>
              NamedValues(
                FieldMappingBase.fullTextZoneName(FieldName(y.name)),
                y.values,
              ),
            ),
        )(hasSameElements(Nil))
      },
      test("[textZones] zone with no name") {
        val doc = RawDocument
          .defaultInstance
          .withText(
            Seq(
              TextField
                .defaultInstance
                .withMetadata(
                  VasgenTextFieldOptions.defaultInstance.withName(""),
                )
                .withContent(Seq(TextValue.defaultInstance.withText("text"))),
            ),
          )
        assert(
          converterMock
            .textZones(doc)
            .map(y =>
              NamedValues(
                FieldMappingBase.fullTextZoneName(FieldName(y.name)),
                y.values,
              ),
            ),
        )(hasSameElements(Nil))
      },
      test("[textZones] zone with no values") {
        val doc = RawDocument
          .defaultInstance
          .withText(
            Seq(
              TextField
                .defaultInstance
                .withMetadata(
                  VasgenTextFieldOptions.defaultInstance.withName("zone"),
                )
                .withContent(Nil),
            ),
          )
        assert(
          converterMock
            .textZones(doc)
            .map(y =>
              NamedValues(
                FieldMappingBase.fullTextZoneName(FieldName(y.name)),
                y.values,
              ),
            ),
        )(hasSameElements(Nil))
      },
      test("[textZones] zone with empty values") {
        val doc = RawDocument
          .defaultInstance
          .withText(
            Seq(
              TextField
                .defaultInstance
                .withMetadata(
                  VasgenTextFieldOptions.defaultInstance.withName("zone"),
                )
                .withContent(Seq(TextValue.defaultInstance.withText(""))),
            ),
          )
        assert(
          converterMock
            .textZones(doc)
            .map(y =>
              NamedValues(
                FieldMappingBase.fullTextZoneName(FieldName(y.name)),
                y.values,
              ),
            ),
        )(hasSameElements(Nil))
      },
      test("[textZones] single zone") {
        val doc = RawDocument
          .defaultInstance
          .withText(
            Seq(
              TextField
                .defaultInstance
                .withMetadata(
                  VasgenTextFieldOptions.defaultInstance.withName("zone"),
                )
                .withContent(Seq(TextValue.defaultInstance.withText("text"))),
            ),
          )
        assert(
          converterMock
            .textZones(doc)
            .map(y =>
              NamedValues(
                FieldMappingBase.fullTextZoneName(FieldName(y.name)),
                y.values,
              ),
            ),
        )(hasSameElements(Seq(NamedValues("z_zone", Seq(StringValue("text"))))))
      },
      test("[textZones] 2 zones 3 values") {
        val doc = RawDocument
          .defaultInstance
          .withText(
            Seq(
              TextField
                .defaultInstance
                .withMetadata(
                  VasgenTextFieldOptions.defaultInstance.withName("zone0"),
                )
                .withContent(
                  Seq(
                    TextValue.defaultInstance.withText("text0"),
                    TextValue.defaultInstance.withText("text1"),
                  ),
                ),
              TextField
                .defaultInstance
                .withMetadata(
                  VasgenTextFieldOptions.defaultInstance.withName("zone2"),
                )
                .withContent(Seq(TextValue.defaultInstance.withText("text2"))),
            ),
          )
        assert(
          converterMock
            .textZones(doc)
            .map(y =>
              NamedValues(
                FieldMappingBase.fullTextZoneName(FieldName(y.name)),
                y.values,
              ),
            ),
        )(
          hasSameElements(
            Seq(
              NamedValues(
                "z_zone0",
                Seq(StringValue("text0"), (StringValue("text1"))),
              ),
              NamedValues("z_zone2", Seq(StringValue("text2"))),
            ),
          ),
        )
      },
    )
  }

  private def mkVector(
    name: String,
    version: Int,
    array: Array[Float],
  ): EmbeddedVector = {
    EmbeddedVector
      .defaultInstance
      .withName(name)
      .withVersion(version)
      .withVector(FloatVector.defaultInstance.withValues(array.toIndexedSeq))
  }

}
