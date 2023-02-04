package vasgen.indexer.saas.integration.convert

object DocumentConverterServiceSpec extends ZIOSpecDefault with Logging {

  private val keeperExpectations = FactorKeeperMock.GetFactors(
    equalTo(Factor.Static),
    value(
      Seq(
        "f_f_offer_photos_has_photos_g",
        "f_f_seller_active_offers_g",
        "f_f_offer_price_g",
        "f_f_offer_from_feed_g",
      ).zipWithIndex
        .map { case (name, index) =>
          Factor(Factor.Static, SaasName(name), index, None)
        },
    ),
  )

  override def spec =
    suite("DocumentConversionService")(
      test("ADocSample") {
        for {
          service  <- ZIO.service[DocumentConversionService.Service]
          messages <- service.convertToProto(Seq(RealDocumentSample.sample1))
          document <- ZIO
            .fromOption(messages.headOption.flatMap(_.document))
            .orElseFail(VasgenEmptyValue)
        } yield {
          assert(document.factors)(
            isSome(
              equalTo(
                TErfInfo(
                  names = Seq(
                    "f_f_offer_photos_has_photos_g",
                    "f_f_seller_active_offers_g",
                    "f_f_offer_price_g",
                    "f_f_offer_from_feed_g",
                  ),
                  values = TFactorValues
                    .defaultInstance
                    .withValues(
                      Seq(
                        TValue.defaultInstance.withValue(1f),
                        TValue.defaultInstance.withValue(0.98397446f),
                        TValue.defaultInstance.withValue(2232f),
                        TValue.defaultInstance.withValue(1f),
                      ),
                    ),
                ),
              ),
            ),
          )
        }
      }.provideLayer(
        keeperExpectations >>>
          (ConvertContext.documentConversionServiceLayer ++ Clock.live ++
            Tracing.noop),
      ),
    )

}
