package vasgen.core.saas

object FieldMappingReaderSpec extends ZIOSpecDefault {

  private val readerLayer: ZLayer[Any, Nothing, FieldMappingReader.Service] =
    ZIO
      .succeed {
        val stub = new FieldMappingStorageStub
        stub.put(
          FieldMapping(
            epoch = 8,
            name = FieldName("offer.region"),
            searchName = SaasName("s_offer_region").some,
            groupName = SaasName("s_offer_region").some,
            stype = model.i32,
          ),
        )
        FieldMappingReader(stub)
      }
      .toLayer

  override def spec =
    suite("FieldMappingConverterSpec.Service")(
      test("defineSaasMeta: success") {
        assertZIO(
          (
            for {
              reader <- ZIO.service[FieldMappingReader.Service]
              m      <- reader.defineSaasMeta(8, "offer.region")
            } yield m
          ).exit,
        )(
          succeeds(
            equalTo(
              FieldMapping(
                epoch = 8,
                name = FieldName("offer.region"),
                searchName = SaasName("s_offer_region").some,
                groupName = SaasName("s_offer_region").some,
                stype = model.i32,
              ),
            ),
          ),
        )
      },
      test("defineSaasMeta: reserved field with epoch !=0") {
        assertZIO(
          (
            for {
              reader <- ZIO.service[FieldMappingReader.Service]
              m      <- reader.defineSaasMeta(8, "pk.s")
            } yield m
          ).exit,
        )(
          succeeds(
            equalTo(
              FieldMapping(
                8,
                FieldName("pk.s"),
                SaasName("s_pk").some,
                SaasName("s_pk").some,
                model.str,
              ),
            ),
          ),
        )
      },
      test("defineSaasMeta: wrong epoch") {
        assertZIO(
          (
            for {
              reader <- ZIO.service[FieldMappingReader.Service]
              fail   <- reader.defineSaasMeta(0, "offer.region")
            } yield fail
          ).exit,
        )(fails(equalTo(UnsupportedAttributeName("offer.region"))))
      },
    ).provideLayerShared(readerLayer ++ Clock.live) @@ TestAspect.ignore

}
