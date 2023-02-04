package vasgen.saas.validation

object DocumentValidatorSpec extends ZIOSpecDefault with Logging {

  override def spec =
    suite("DocumentValidation")(
      test("RawDocumentSamples.upsert1") {
        {
          for {
            service <- ZIO.service[DocumentValidator[TestSetup]]
            names   <- service.validateNames(RawDocumentSamples.upsert1)
            values  <- service.validateValues(RawDocumentSamples.upsert1)
            issues  <- service.validate(RawDocumentSamples.upsert1)
          } yield {
            assert(names)(
              equalTo(
                Seq(
                  DocumentIssue(FieldName("empty"), EmptyFieldValue),
                  DocumentIssue(
                    FieldName("no_photo"),
                    UnknownFactor(SaasName("f_no_photo_g")),
                  ),
                ),
              ),
            ) &&
            assert(values)(
              hasSameElements(
                Seq(
                  DocumentIssue(
                    DocumentIssue.AtField(
                      FieldMapping.DefaultTextZone,
                      Set(SaasIndexType.FullText),
                      Some(model.str),
                    ),
                    TooManyTextValues(FieldMapping.DefaultTextZone),
                  ),
                ),
              ),
            ) && assert(issues)(hasSameElements(names ++ values))
          }
        }
      }.provideLayer(layer),
      test("RawDocumentSamples.upsert2") {
        {
          for {
            service <- ZIO.service[DocumentValidator[TestSetup]]
            names   <- service.validateNames(RawDocumentSamples.upsert2)
            values  <- service.validateValues(RawDocumentSamples.upsert2)
            issues  <- service.validate(RawDocumentSamples.upsert2)
          } yield assert(names)(isEmpty) &&
            assert(values)(
              equalTo(Seq(DocumentIssue(FieldName("empty"), EmptyFieldValue))),
            ) && assert(issues)(equalTo(values))
        }
      }.provideLayer(layer),
      test("RawDocumentSamples.withoutId1") {
        {
          for {
            service <- ZIO.service[DocumentValidator[TestSetup]]
            issues  <- service.validate(RawDocumentSamples.withoutId1)
          } yield assert(issues)(
            equalTo(Seq(DocumentIssue(AtPrimaryKey, EmptyFieldValue))),
          )
        }
      }.provideLayer(layer),
      test("RawDocumentSamples.withoutId2") {
        {
          for {
            service <- ZIO.service[DocumentValidator[TestSetup]]
            issues  <- service.validate(RawDocumentSamples.withoutId2)
          } yield assert(issues)(
            equalTo(Seq(DocumentIssue(AtPrimaryKey, EmptyFieldValue))),
          )
        }
      }.provideLayer(layer),
      test("RawDocumentSamples.delete1") {
        {
          for {
            service <- ZIO.service[DocumentValidator[TestSetup]]
            issues  <- service.validate(RawDocumentSamples.delete1)
          } yield assert(issues)(isEmpty)
        }
      }.provideLayer(layer),
    )

  def layer =
    keeperExpectations >>>
      (Context.fieldValidatorLayer[TestSetup] ++ Clock.live ++ Tracing.noop)

  private def keeperExpectations =
    FactorKeeperMock
      .GetFactors(
        equalTo(Factor.Static),
        value(
          Seq("f_ctr_g", "f_f_offer_from_feed_g")
            .zipWithIndex
            .map { case (name, index) =>
              Factor(Factor.Static, SaasName(name), index, None)
            },
        ),
      )
      .repeats(0 to 2)

}
