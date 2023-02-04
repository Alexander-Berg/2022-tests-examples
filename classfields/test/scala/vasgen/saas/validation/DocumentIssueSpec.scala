package vasgen.saas.validation

object DocumentIssueSpec extends ZIOSpecDefault with Logging {

  val spec =
    suite("DocumentIssue")(
      test("") {
        assert(
          DocumentIssue(
            DocumentIssue.AtField(
              FieldName("offer.price"),
              Set(SaasIndexType.Property),
              Some(model.str),
            ),
            UnsupportedValueType(Some(model.i32), SaasIndexType.SearchAttribute),
          ).toString,
        )(
          equalTo(
            "The `offer.price` field of type `str/Property` does not support value of type `i32/SearchAttribute`",
          ),
        )

      },
    )

}
