package vasgen.indexer.saas.convert

import java.util.concurrent.TimeUnit

object FieldMappingConverterSpec extends ZIOSpecDefault {

  val emptyCache = CacheBuilder
    .newBuilder()
    .expireAfterAccess(1, TimeUnit.SECONDS)
    .build[String, FieldMapping]()

  val suites =
    suite("FieldMappingConverterSpec.Service")(
      test("convert fulltext zone name") {
        for {
          converter <- ZIO.service[FieldMappingConverter]
        } yield assert(converter.convertZone(FieldName("description")))(
          equalTo(SaasName("z_description")),
        ) &&
          assert(
            converter
              .convertZone(FieldName("fulltext.zone.with.really.long.name")),
          )(equalTo(SaasName("z_fulltext_zone_with_re_3gjejm3")))

      },
      test("Create new property field") {
        stub.clear()
        for {
          converter <- ZIO.service[FieldMappingConverter]
          (mapping, failures) <- converter.convertAndUpdate(
            1,
            Seq(
              document.RawFieldTypeMeta(
                Set(SaasIndexType.Property),
                model.str,
                FieldName("snippet"),
              ),
            ),
          )
        } yield assert(mapping)(
          equalTo(
            Map(
              FieldName("snippet") ->
                FieldMapping(
                  1,
                  FieldName("snippet"),
                  SaasName("_snippet_t").some,
                  None,
                  model.str,
                ),
            ),
          ),
        ) && assert(failures)(isEmpty)
      },
      test("Create new string field") {
        stub.clear()
        for {
          converter <- ZIO.service[FieldMappingConverter]
          (mapping, failures) <- converter.convertAndUpdate(
            1,
            Seq(
              document.RawFieldTypeMeta(
                Set(SaasIndexType.SearchAttribute),
                model.str,
                FieldName("first"),
              ),
            ),
          )
        } yield assert(mapping)(
          equalTo(
            Map(
              FieldName("first") ->
                FieldMapping(
                  1,
                  FieldName("first"),
                  SaasName("s_first_t").some,
                  None,
                  model.str,
                ),
            ),
          ),
        ) && assert(failures)(isEmpty)
      },
      test("Convert existed field from cache") {
        for {
          converter <- ZIO.service[FieldMappingConverter]
          r1 <- converter.convertAndUpdate(
            1,
            Seq(
              RawFieldTypeMeta(
                Set(SaasIndexType.SearchAttribute),
                model.str,
                FieldName("first"),
              ),
            ),
          )
        } yield assert(r1)(
          equalTo(
            (
              Map(
                FieldName("first") ->
                  FieldMapping(
                    1,
                    FieldName("first"),
                    SaasName("s_first_t").some,
                    None,
                    model.str,
                  ),
              ),
              Seq.empty,
            ),
          ),
        )
      },
      test("Convert existed field from database") {
        for {
          converter <- ZIO.service[FieldMappingConverter]
          r1 <- converter.convertAndUpdate(
            1,
            Seq(
              document.RawFieldTypeMeta(
                Set(SaasIndexType.SearchAttribute),
                model.str,
                FieldName("first"),
              ),
            ),
          )
        } yield assert(r1)(
          equalTo(
            (
              Map(
                FieldName("first") ->
                  FieldMapping(
                    1,
                    FieldName("first"),
                    SaasName("s_first_t").some,
                    None,
                    model.str,
                  ),
              ),
              Seq.empty,
            ),
          ),
        )
      },
      test("Fail to convert existed field with mismatched supported type") {
        for {
          converter <- ZIO.service[FieldMappingConverter]
          (mapping, failures) <- converter.convertAndUpdate(
            1,
            Seq(
              document.RawFieldTypeMeta(
                Set(SaasIndexType.SearchAttribute),
                model.i32,
                FieldName("first"),
              ),
            ),
          )
        } yield assert(failures)(
          equalTo(
            Seq(
              document.DocumentIssue(
                document.RawFieldTypeMeta(
                  Set(SaasIndexType.SearchAttribute),
                  model.i32,
                  FieldName("first"),
                ),
                MismatchSupportedType(model.str),
              ),
            ),
          ),
        )
      },
      test("Convert existed field with different types at another epoch ") {
        for {
          converter <- ZIO.service[FieldMappingConverter]
          r1 <- converter.convertAndUpdate(
            2,
            Seq(
              document.RawFieldTypeMeta(
                Set(SaasIndexType.SearchAttribute),
                model.str,
                FieldName("first"),
              ),
            ),
          )
        } yield assert(r1)(
          equalTo(
            (
              Map(
                FieldName("first") ->
                  FieldMapping(
                    2,
                    FieldName("first"),
                    SaasName("s_first_t").some,
                    None,
                    model.str,
                  ),
              ),
              Seq.empty,
            ),
          ),
        )
      },
      test("Fail to convert new field with wrong metadata") {
        for {
          converter <- ZIO.service[FieldMappingConverter]
          (m1, f1) <- converter.convertAndUpdate(
            1,
            Seq(
              document.RawFieldTypeMeta(
                Set(SaasIndexType.GroupAttribute),
                model.i64,
                FieldName("second"),
              ),
            ),
          )
        } yield assert(m1)(
          equalTo(
            Map(
              FieldName("second") ->
                FieldMapping(
                  1,
                  FieldName("second"),
                  None,
                  SaasName("i_second_t").some,
                  model.i64,
                ),
            ),
          ),
        ) && assert(f1)(isEmpty)

      },
      test("Create some new fields") {
        for {
          converter <- ZIO.service[FieldMappingConverter]
          r1 <- converter.convertAndUpdate(
            3,
            Seq(
              document.RawFieldTypeMeta(
                Set(SaasIndexType.GroupAttribute),
                model.str,
                FieldName("cat"),
              ),
              document.RawFieldTypeMeta(
                Set(SaasIndexType.SearchAttribute),
                model.i64,
                FieldName("dog"),
              ),
              document.RawFieldTypeMeta(
                Set(SaasIndexType.GroupAttribute),
                model.str,
                FieldName("phone"),
              ),
              document.RawFieldTypeMeta(
                Set(SaasIndexType.SearchAttribute),
                model.i32,
                FieldName("long.name.with.many.words.and.dots"),
              ),
            ),
          )
          r2 <- converter.convertAndUpdate(
            3,
            Seq(
              document.RawFieldTypeMeta(
                Set(SaasIndexType.GroupAttribute),
                model.i32,
                FieldName("cat"),
              ),
              document.RawFieldTypeMeta(
                Set(SaasIndexType.SearchAttribute),
                model.i32,
                FieldName("long.name.with.many.words.and.dots"),
              ),
              document.RawFieldTypeMeta(
                Set(SaasIndexType.SearchAttribute),
                model.i64,
                FieldName("cow"),
              ),
              document.RawFieldTypeMeta(
                Set(SaasIndexType.SearchAttribute),
                model.i64,
                FieldName("boy"),
              ),
              document.RawFieldTypeMeta(
                Set(SaasIndexType.GroupAttribute),
                model.str,
                FieldName("phone"),
              ),
            ),
          )
        } yield assert(r1)(
          equalTo(
            (
              Map(
                FieldName("cat") ->
                  FieldMapping(
                    3,
                    FieldName("cat"),
                    None,
                    SaasName("s_cat_t").some,
                    model.str,
                  ),
                FieldName("dog") ->
                  FieldMapping(
                    3,
                    FieldName("dog"),
                    SaasName("s_dog_t").some,
                    None,
                    model.i64,
                  ),
                FieldName("phone") ->
                  FieldMapping(
                    3,
                    FieldName("phone"),
                    None,
                    SaasName("s_phone_t").some,
                    model.str,
                  ),
                FieldName("long.name.with.many.words.and.dots") ->
                  FieldMapping(
                    3,
                    FieldName("long.name.with.many.words.and.dots"),
                    SaasName("i_long_name_with_many_2439110_t").some,
                    None,
                    model.i32,
                  ),
              ),
              Seq.empty,
            ),
          ),
        ) &&
          assert(r2)(
            equalTo(
              (
                Map(
                  FieldName("phone") ->
                    FieldMapping(
                      3,
                      FieldName("phone"),
                      None,
                      SaasName("s_phone_t").some,
                      model.str,
                    ),
                  FieldName("long.name.with.many.words.and.dots") ->
                    FieldMapping(
                      3,
                      FieldName("long.name.with.many.words.and.dots"),
                      SaasName("i_long_name_with_many_2439110_t").some,
                      None,
                      model.i32,
                    ),
                  FieldName("cow") ->
                    FieldMapping(
                      3,
                      FieldName("cow"),
                      SaasName("s_cow_t").some,
                      None,
                      model.i64,
                    ),
                  FieldName("boy") ->
                    FieldMapping(
                      3,
                      FieldName("boy"),
                      SaasName("s_boy_t").some,
                      None,
                      model.i64,
                    ),
                ),
                Seq(
                  DocumentIssue(
                    document.RawFieldTypeMeta(
                      Set(SaasIndexType.GroupAttribute),
                      model.i32,
                      FieldName("cat"),
                    ),
                    MismatchSupportedType(model.str),
                  ),
                ),
              ),
            ),
          )
      },
    ) @@ TestAspect.sequential

  private val stub = new FieldMappingStorageStub

  private val converterLayer =
    ZIO
      .succeed(FieldMappingConverter(stub, FieldMappingConverter.Config("t")))
      .toLayer

  override def spec: Spec[Environment, Failure] =
    suites.provideLayerShared(converterLayer ++ Clock.live ++ Tracing.noop)

}
