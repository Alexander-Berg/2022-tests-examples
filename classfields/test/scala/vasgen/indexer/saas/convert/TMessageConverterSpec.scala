package vasgen.indexer.saas.convert

object TMessageConverterSpec extends ZIOSpecDefault {

  val DefaultTextZoneSaas: SaasName = FieldMappingBase
    .fullTextZoneName(FieldMapping.DefaultTextZone)

  val TitleTextZone   = "z_title"
  val MessageTextZone = "z_message"

  val sample1 = TMessage(
    messageType = TMessageType.MODIFY_DOCUMENT,
    document = Some(
      TDocument(
        url = "vasgen/test/123456-789-0001",
        language = Some("ru"),
        language2 = Some("en"),
        version = Some(1),
        modificationTimestamp = Some(0),
        keyPrefix = Some(1L),
        deadlineMinutesUTC = Some(3L),
        rootZone = Some(
          TZone(children =
            Seq(
              TZone(
                name = Some(TitleTextZone),
                text = Some("deadbeef title string"),
              ),
              TZone(
                name = Some(MessageTextZone),
                text = Some("message with deadbeef string"),
              ),
              TZone(name = Some(DefaultTextZoneSaas), text = Some("One")),
              TZone(name = Some(DefaultTextZoneSaas), text = Some("Two")),
              TZone(name = Some(DefaultTextZoneSaas), text = Some("1999")),
            ),
          ),
        ),
        groupAttributes = Seq(
          TAttribute(
            name = "i_grouping_i63_test",
            value = "1",
            `type` = TAttributeType.INTEGER_ATTRIBUTE,
          ),
          TAttribute(
            name = "i_grouping_i63_test",
            value = (Long.MaxValue - 2L + 1L).toString,
            `type` = TAttributeType.INTEGER_ATTRIBUTE,
          ),
          TAttribute(
            name = "i_negative_grouping_i63_test",
            value = (Long.MaxValue - 2L + 1L).toString,
            `type` = TAttributeType.INTEGER_ATTRIBUTE,
          ),
          TAttribute(
            name = "i_offer_price_price_in_c_0_test",
            value = adaptI32(1999),
            `type` = TAttributeType.INTEGER_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_model_test",
            value = "please, sort by me properly",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_attributes",
            value = "grouping_i63",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_attributes",
            value = "negative_grouping_i63",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_attributes",
            value = "stone",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_attributes",
            value = "empty",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_attributes",
            value = "offer.price.price_in_currency",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_attributes",
            value = "model",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_attributes",
            value = "offer.region",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_attributes",
            value = "offer.old.region",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
        ),
        searchAttributes = Seq(
          TAttribute(
            name = "i_offer_price_price_in_c_0_test",
            value = adaptI32(1999),
            `type` = TAttributeType.INTEGER_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_model_test",
            value = "please, sort by me properly",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_stone_test",
            value = "1",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_stone_test",
            value = "2",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_grouping_i63_test",
            value = "1",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_negative_grouping_i63_test",
            value = "-2",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_grouping_i63_test",
            value = "-2",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_offer_region_test",
            value = "213",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_offer_region_test",
            value = "214",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_offer_region_test",
            value = "215",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "i_epoch",
            value = "1",
            `type` = TAttributeType.INTEGER_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_version",
            value = "1",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "i_ttl",
            value = "200",
            `type` = TAttributeType.INTEGER_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_pk",
            value = "123456-789-0001",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
        ),
        documentProperties = Seq(
          TProperty(name = "i_offer_price_price_in_c_0_test", value = "1999"),
          TProperty(
            name = "s_model_test",
            value = "please, sort by me properly",
          ),
          TProperty(name = "s_stone_test", value = "1"),
          TProperty(name = "s_stone_test", value = "2"),
          TProperty(name = "s_grouping_i63_test", value = "1"),
          TProperty(name = "s_grouping_i63_test", value = "-2"),
          TProperty(name = "s_negative_grouping_i63_test", value = "-2"),
          TProperty(name = "s_offer_region_test", value = "213"),
          TProperty(name = "s_offer_region_test", value = "214"),
          TProperty(name = "s_offer_region_test", value = "215"),
          TProperty(name = "__prop", value = "just property"),
          TProperty(name = "i_epoch", value = "1"),
          TProperty(name = "s_version", value = "1"),
          TProperty(name = "i_ttl", value = "200"),
          TProperty(name = "s_pk", value = "123456-789-0001"),
          TProperty("s_zones", "title;message;default.text.zone"),
          TProperty(
            "s_all_text",
            "deadbeef title string.message with deadbeef string.One",
          ),
        ),
        factors = Some(
          TErfInfo(
            names = Seq(
              "f_no_photo_test",
              "f_ctr_test",
              "f_f_offer_from_feed_test",
            ),
            values = TFactorValues(
              Seq(
                TFactorValues.TValue(value = 1f),
                TFactorValues.TValue(value = 0.25f),
                TFactorValues.TValue(value = 1f),
              ),
            ),
          ),
        ),
        geoData = Some(
          TGeoData(layers =
            Seq(
              TMessage.TGeoLayer(
                layer = Some("s_offer_region_test"),
                geoDoc = Seq(
                  TGeoObject(
                    data = Seq(37.600f, 55.7421f, 39.600f, 56.7421f),
                    `type` = Some("p"),
                  ),
                ),
              ),
              TMessage.TGeoLayer(
                layer = Some("s_offer_old_region_test"),
                geoDoc = Seq(
                  TGeoObject(
                    data = Seq(54.700931f, 72.997014f, 53.99488f, 72.702f),
                    `type` = Some("p"),
                  ),
                ),
              ),
            ),
          ),
        ),
        embeddings = Seq(
          TEmbedding
            .defaultInstance
            .withName("vector")
            .withVersion("1")
            .withValue(DssmHelper.toByteStr(Array(1f, 2f, 3f))),
        ),
        annData = Some(
          TAnnData(
            List(
              TSentenceData(
                text = "deadbeef title string message with smth 1",
                textLanguage = None,
                streamsByRegion = Seq(factorSection),
              ),
            ),
          ),
        ),
      ),
    ),
  )

  val sampleDelete = TMessage(
    messageType = TMessageType.DELETE_DOCUMENT,
    document = Some(
      TDocument(
        url = "vasgen/test/123456-789-0002",
        language = Some("ru"),
        language2 = Some("en"),
        version = Some(1),
        modificationTimestamp = Some(0),
        keyPrefix = Some(1L),
      ),
    ),
  )

  val sample2 = TMessage(
    messageType = TMessageType.MODIFY_DOCUMENT,
    document = Some(
      TDocument(
        url = "vasgen/test/123456-789-0003",
        language = Some("ru"),
        language2 = Some("en"),
        version = Some(1),
        modificationTimestamp = Some(0),
        deadlineMinutesUTC = Some(3L),
        keyPrefix = Some(1L),
        rootZone = Some(
          TZone(children =
            Seq(
              TZone(
                name = Some(TitleTextZone),
                text = Some("deadbeef title string"),
              ),
              TZone(
                name = Some(MessageTextZone),
                text = Some("message with deadbeef string"),
              ),
            ),
          ),
        ),
        groupAttributes = Seq(
          TAttribute(
            name = "s_attributes",
            value = "float",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_attributes",
            value = "has_photo",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_attributes",
            value = "empty",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
        ),
        searchAttributes = Seq(
          TAttribute(
            name = "i_float_test",
            value = adaptF32(1.2345f),
            `type` = TAttributeType.INTEGER_ATTRIBUTE,
          ),
          TAttribute(
            name = "i_float_test",
            value = adaptF32(2.3456f),
            `type` = TAttributeType.INTEGER_ATTRIBUTE,
          ),
          TAttribute(
            name = "i_has_photo_test",
            value = "1",
            `type` = TAttributeType.INTEGER_ATTRIBUTE,
          ),
          TAttribute(
            name = "i_epoch",
            value = "1",
            `type` = TAttributeType.INTEGER_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_version",
            value = "1",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
          TAttribute(
            name = "i_ttl",
            value = "200",
            `type` = TAttributeType.INTEGER_ATTRIBUTE,
          ),
          TAttribute(
            name = "s_pk",
            value = "123456-789-0003",
            `type` = TAttributeType.LITERAL_ATTRIBUTE,
          ),
        ),
        documentProperties = Seq(
          TProperty(name = "i_float_test", value = "1.2345"),
          TProperty(name = "i_float_test", value = "2.3456"),
          TProperty(name = "i_has_photo_test", value = "1"),
          TProperty(name = "i_epoch", value = "1"),
          TProperty(name = "s_version", value = "1"),
          TProperty(name = "i_ttl", value = "200"),
          TProperty(name = "s_pk", value = "123456-789-0003"),
          TProperty("s_zones", "title;message"),
          TProperty(
            "s_all_text",
            "deadbeef title string.message with deadbeef string",
          ),
        ),
        factors = Some(
          TErfInfo(
            names = Seq("f_ctr_test"),
            values = TFactorValues(Seq(TFactorValues.TValue(value = 0.543f))),
          ),
        ),
        annData = Some(
          TAnnData(
            List(
              TSentenceData(
                text = "title",
                textLanguage = None,
                streamsByRegion = Seq(factorSection),
              ),
            ),
          ),
        ),
      ),
    ),
  )

  val mapping1 =
    Seq(
      FieldMapping(
        1,
        FieldName("grouping_i63"),
        SaasName("s_grouping_i63_test").some,
        SaasName("i_grouping_i63_test").some,
        model.str,
      ),
      FieldMapping(
        1,
        FieldName("negative_grouping_i63"),
        SaasName("s_negative_grouping_i63_test").some,
        SaasName("i_negative_grouping_i63_test").some,
        model.str,
      ),
      FieldMapping(
        1,
        FieldName("stone"),
        SaasName("s_stone_test").some,
        SaasName("s_stone_test").some,
        model.str,
      ),
      FieldMapping(
        1,
        FieldName("offer.price.price_in_currency"),
        SaasName("i_offer_price_price_in_c_0_test").some,
        SaasName("i_offer_price_price_in_c_0_test").some,
        model.f32,
      ),
      FieldMapping(
        1,
        FieldName("model"),
        SaasName("s_model_test").some,
        SaasName("s_model_test").some,
        model.str,
      ),
      FieldMapping(
        1,
        FieldName("offer.region"),
        SaasName("s_offer_region_test").some,
        SaasName("s_offer_region_test").some,
        model.str,
      ),
      FieldMapping(
        1,
        FieldName("offer.old.region"),
        SaasName("s_offer_old_region_test").some,
        SaasName("s_offer_old_region_test").some,
        model.str,
      ),
      FieldMapping(
        1,
        FieldName("empty"),
        SaasName("empty").some,
        SaasName("empty").some,
        model.str,
      ),
      FieldMapping(
        1,
        FieldName("prop"),
        SaasName("__prop").some,
        None,
        model.str,
      ),
    ).map(meta => meta.name -> meta).toMap

  val mapping2 =
    Seq(
      FieldMapping(
        1,
        FieldName("float"),
        SaasName("i_float_test").some,
        SaasName("i_float_test").some,
        model.i64,
      ),
      FieldMapping(
        1,
        FieldName("has_photo"),
        SaasName("i_has_photo_test").some,
        SaasName("i_has_photo_test").some,
        model.i64,
      ),
      FieldMapping(
        1,
        FieldName("empty"),
        SaasName("s_empty_test").some,
        SaasName("s_empty_test").some,
        model.str,
      ),
    ).map(meta => meta.name -> meta).toMap

  val factors: Set[SaasName] = Set(
    SaasName("f_no_photo_t"),
    SaasName("f_f_offer_from_feed_t"),
    SaasName("f_ctr_t"),
  )

  override def spec = {

    val config = ConverterConfig("vasgen", "test", "ru", "en", 1L, true)

    val converter =
      new TMessageConverter(
        config,
        FieldMappingConverter(
          new FieldMappingStorageStub,
          FieldMappingConverter.Config("t"),
        ),
      )

    suite("TMessageConverter")(
      test("convertUpdate") {
        val result: SaasConverter.Result[TMessage] =
          converter.convertDocument(mapping1, factors, true)(
            RawDocumentSamples.upsert1,
          )
        assert(result.issues)(
          hasSameElements(
            List(DocumentIssue(FieldName("empty"), EmptyFieldValue)),
          ),
        ) && assertTMessage(result.value)(sample1)

      },
      test("convertUpdateWithAlterText") {
        val result =
          converter.convertDocument(mapping2, factors, true)(
            RawDocumentSamples.upsert2,
          )
        assert(result.issues)(
          hasSameElements(
            List(DocumentIssue(FieldName("empty"), EmptyFieldValue)),
          ),
        ) && assertTMessage(result.value)(sample2)
      },
      test("convertDelete") {
        val result =
          converter.convertDocument(Map.empty, factors, false)(
            RawDocumentSamples.delete1,
          )
        assert(result)(equalTo(SaasConverter.Success(sampleDelete, Seq.empty)))
      },
      test("errorWithoutIdField") {
        val result =
          converter.convertDocument(Map.empty, factors, false)(
            RawDocumentSamples.withoutId1,
          )

        assert(result)(
          equalTo(
            SaasConverter.Failure[TMessage](
              DocumentIssue(DocumentIssue.AtPrimaryKey, EmptyFieldValue),
            ),
          ),
        )
      },
      test("errorWithoutIdValue") {
        val result =
          converter.convertDocument(Map.empty, factors, false)(
            RawDocumentSamples.withoutId2,
          )
        assert(result)(
          equalTo(
            SaasConverter.Failure[TMessage](
              DocumentIssue(DocumentIssue.AtPrimaryKey, EmptyFieldValue),
            ),
          ),
        )
      },
    )
  }

  private def assertTMessage(maybe: Option[TMessage])(sample: TMessage) =
    assert(maybe)(isSome(anything)) &&
      assert(maybe.get.document.get.url)(equalTo(sample.document.get.url)) &&
      assert(maybe.get.document.get.rootZone.get.children)(
        hasSameElements(sample.document.get.rootZone.get.children),
      ) &&
      assert(maybe.get.document.get.searchAttributes)(
        hasSameElements(sample.document.get.searchAttributes),
      ) &&
      assert(maybe.get.document.get.groupAttributes)(
        hasSameElements(sample.document.get.groupAttributes),
      ) &&
      assert(maybe.get.document.get.documentProperties)(
        hasSameElements(sample.document.get.documentProperties),
      ) &&
      assert(maybe.get.document.get.geoData.map(_.layers).getOrElse(Seq.empty))(
        hasSameElements(
          sample.document.get.geoData.map(_.layers).getOrElse(Seq.empty),
        ),
      ) &&
      assert(maybe.get.document.get.embeddings)(
        hasSameElements(sample.document.get.embeddings),
      ) &&
      assert(maybe.get.document.get.annData)(
        equalTo(sample.document.get.annData),
      )

  def adaptI32(value: Int): String =
    RawValueUtil
      .adapt(Raw.i32(value), SaasIndexType.SearchAttribute)
      .map(_.searchValue)
      .getOrElse("")

  def adaptF32(value: Float): String =
    RawValueUtil
      .adapt(Raw.f32(value), SaasIndexType.Factor)
      .map(_.searchValue)
      .getOrElse("")

}
