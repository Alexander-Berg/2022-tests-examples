package vasgen.saas.request

object GroupingParamsSpec extends ZIOSpecDefault {

  val paging = Paging.defaultInstance.withPageSize(15)
  val sample1: Grouping = Grouping
    .defaultInstance
    .withPageSize(20)
    .withGroupSize(10)
    .withAttribute("seller")

  def spec =
    suite("GroupingParams")(
      test("sample1") {
        assert(
          GroupingParams
            .build(sample1.withAttribute("s_seller_t"), Sorting.Empty)
            .params,
        )(equalTo(Map("g" -> "1.s_seller_t.20.10.....rlv.0.")))
      },
    )

}
