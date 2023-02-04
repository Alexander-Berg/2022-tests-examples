package vasgen.saas.converter

import java.net.URLEncoder
import java.nio.charset.Charset
import java.text.DecimalFormat

object RequestConverterSpec extends ZIOSpecDefault {

  val initEpoch = 8L

  val requestConverter = new RequestConverterImpl(epochService(initEpoch))

  val initMapping = Seq(
    FieldMapping(
      epoch = 8,
      name = FieldName("offer.price.price_in_currency"),
      searchName = SaasName("i_offer_price_price_in_currency").some,
      groupName = SaasName("i_offer_price_price_in_currency").some,
      stype = model.u32,
    ),
    FieldMapping(
      epoch = 8,
      name = FieldName("offer.photos.has_photos"),
      searchName = SaasName("i_offer_photos_has_photos").some,
      groupName = SaasName("i_offer_photos_has_photos").some,
      stype = model.boolean,
    ),
    FieldMapping(
      epoch = 8,
      name = FieldName("seller_id.user_id"),
      searchName = SaasName("s_seller_id_user_id").some,
      groupName = SaasName("s_seller_id_user_id").some,
      stype = model.u32,
    ),
    FieldMapping(
      epoch = 8,
      name = FieldName("offer.region"),
      searchName = SaasName("s_offer_region_g").some,
      groupName = SaasName("s_offer_region_g").some,
      stype = model.geo,
    ),
    FieldMapping(
      epoch = 8,
      name = FieldName("offer.region.signed"),
      searchName = SaasName("s_offer_region_signed_g").some,
      groupName = SaasName("s_offer_region_signed_g").some,
      stype = model.i32,
    ),
    FieldMapping(
      epoch = 8,
      name = FieldName("offer.attributes.diagonal-tochno"),
      searchName = SaasName("i_offer_attributes_diagonal").some,
      groupName = SaasName("i_offer_attributes_diagonal").some,
      stype = model.f32,
    ),
    FieldMapping(
      epoch = 8,
      name = FieldName("offer.publish_date"),
      searchName = SaasName("i_offer_publish_date").some,
      groupName = SaasName("i_offer_publish_date").some,
      stype = model.timestamp,
    ),
    FieldMapping(
      epoch = 8,
      name = FieldName("offer.category.all"),
      searchName = SaasName("s_offer_category_all").some,
      groupName = SaasName("s_offer_category_all").some,
      stype = model.str,
    ),
  )

  val onlyEpoch   = s"""i_epoch:>="${initEpoch}""""
  val epochPrefix = s"""${onlyEpoch} <<"""

  val tests: Spec[
    Annotations with FieldMappingReaderLayer with Clock with EpochLayer with Tracing,
    TestFailure[List[FilterIssue]],
    TestSuccess,
  ] =
    suite("convert")(
      test("check consistency") {
        val complexFilter = Filter(
          or = Seq(
            Filter(and =
              Seq(
                Filter(op = eq("offer.price.price_in_currency", Raw.i32(5))),
                Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              ),
            ),
            Filter(or =
              Seq(
                Filter(op = eq("offer.price.price_in_currency", Raw.i32(10))),
                Filter(and =
                  Seq(
                    Filter(op = eq("offer.photos.has_photos", Raw.i32(7))),
                    Filter(op = less("seller_id.user_id", Raw.u64(6L))),
                  ),
                ),
              ),
            ),
          ),
          op = eq("offer.price.price_in_currency", Raw.u32(10)),
        )
        assertZIO(requestConverter.convert(complexFilter, true).run)(
          fails(hasSameElements(Seq(InconsistentFilter(complexFilter)))),
        )
      },
      suite("AND")(
        test(
          "plain AND: offer.price.price_in_currency=5 & offer.photos.has_photos>6",
        ) {
          val filter = Filter(and =
            Seq(
              Filter(op = eq("offer.price.price_in_currency", Raw.i32(5))),
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
            ),
          )
          assertZIO(requestConverter.convert(filter, true).map(_.query))(
            equalTo(
              s"""$epochPrefix i_offer_price_price_in_currency:"5" << i_offer_photos_has_photos:>"1"""",
            ),
          )
        },
        test(
          "plain AND: offer.price.price_in_currency=5 & offer.photos.has_photos>6 & offer.region.signed = 100",
        ) {
          val filter = Filter(and =
            Seq(
              Filter(op = eq("offer.price.price_in_currency", Raw.i32(5))),
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              Filter(op = eq("offer.region.signed", Raw.i32(100))),
            ),
          )
          assertZIO(requestConverter.convert(filter, true).map(_.query))(
            equalTo(
              s"""$epochPrefix i_offer_price_price_in_currency:"5" << i_offer_photos_has_photos:>"1" << s_offer_region_signed_g:"${adaptI32(
                100,
              )}"""",
            ),
          )
        },
        test(
          "plain AND: offer.price.price_in_currency=5 & offer.photos.has_photos>6 & offer.region.signed = -100",
        ) {
          val filter = Filter(and =
            Seq(
              Filter(op = eq("offer.price.price_in_currency", Raw.i32(5))),
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              Filter(op = eq("offer.region.signed", Raw.i32(-100))),
            ),
          )
          assertZIO(requestConverter.convert(filter, true).map(_.query))(
            equalTo(
              s"""$epochPrefix i_offer_price_price_in_currency:"5" << i_offer_photos_has_photos:>"1" << s_offer_region_signed_g:"${adaptI32(
                -100,
              )}"""",
            ),
          )
        },
        test("plain AND with errors: empty for invalid filter") {
          val undefined = Filter(op =
            Filter.Op.Gte(GreaterThan(field = "offer.region", value = None)),
          )
          val filter = Filter(and =
            Seq(
              Filter(op = eq("not.exist.attribute", Raw.i32(5))),
              Filter(op = more("other.not.exist.attribute", Raw.i32(6))),
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              Filter(op =
                more("seller_id.user_id", Raw.bytes("1212".getBytes)),
              ),
              undefined,
            ),
          )
          assertZIO(requestConverter.convert(filter, true).run)(
            fails(
              hasSameElements(
                Seq(
                  UnsupportedAttributeName("not.exist.attribute"),
                  UnsupportedAttributeName("other.not.exist.attribute"),
                  UnsupportedFilterValue(Raw.bytes("1212".getBytes)),
                  MissingFilterValue(undefined, "offer.region"),
                ),
              ),
            ),
          )
        },
        test(
          "plain AND with errors: empty for invalid filters(more than one error per filter)",
        ) {
          val filter = Filter(and =
            Seq(
              Filter(op =
                more("not.exist.attribute", Raw.bytes("1212".getBytes)),
              ),
              Filter(op = more("other.not.exist.attribute", Raw.i32(6))),
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
            ),
          )
          assertZIO(requestConverter.convert(filter, true).run)(
            fails(
              hasSameElements(
                Seq(
                  UnsupportedAttributeName("not.exist.attribute"),
                  UnsupportedAttributeName("other.not.exist.attribute"),
                  UnsupportedFilterValue(Raw.bytes("1212".getBytes)),
                ),
              ),
            ),
          )
        },
        test(
          "plain AND with errors: A AND NOT(not.exist.attribute AND exist.attribute)",
        ) {
          val filter = Filter(and =
            Seq(
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              Filter(not =
                Some(
                  Filter(and =
                    Seq(
                      Filter(op = more("not.exist.attribute", Raw.i32(10))),
                      Filter(op =
                        eq("offer.price.price_in_currency", Raw.i32(10)),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          )
          for {
            res <- requestConverter.convert(filter, true)
          } yield assert(res.query)(
            equalTo(
              s"""$epochPrefix i_offer_photos_has_photos:>"1" ~ (i_offer_price_price_in_currency:"10")""",
            ),
          ) &&
            assert(res.errors)(
              hasSameElements(
                Seq(UnsupportedAttributeName("not.exist.attribute")),
              ),
            )
        },
        test("plain AND with errors: in OR inner branch") {
          val filter = Filter(and =
            Seq(
              Filter(or =
                Seq(
                  Filter(op =
                    more("not.exist.attribute", Raw.bytes("1212".getBytes)),
                  ),
                  Filter(op = more("other.not.exist.attribute", Raw.i32(6))),
                ),
              ),
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
            ),
          )
          assertZIO(requestConverter.convert(filter, true).run)(
            fails(
              hasSameElements(
                Seq(
                  UnsupportedAttributeName("not.exist.attribute"),
                  UnsupportedAttributeName("other.not.exist.attribute"),
                  UnsupportedFilterValue(Raw.bytes("1212".getBytes)),
                ),
              ),
            ),
          )
        },
      ),
      suite("OR")(
        test(
          "plain OR: offer.price.price_in_currency=5 | offer.photos.has_photos>6",
        ) {
          val filter = Filter(or =
            Seq(
              Filter(op = eq("offer.price.price_in_currency", Raw.i32(5))),
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
            ),
          )
          assertZIO(requestConverter.convert(filter, true).map(_.query))(
            equalTo(
              s"""$epochPrefix (i_offer_price_price_in_currency:"5" | i_offer_photos_has_photos:>"1")""",
            ),
          )
        },
        test(
          "plain OR with error: not.exist.attribute=<bytes> | offer.photos.has_photos>6",
        ) {
          val filter = Filter(or =
            Seq(
              Filter(op =
                eq("not.exist.attribute", Raw.bytes("1212".getBytes)),
              ),
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
            ),
          )
          assertZIO(requestConverter.convert(filter, true))(
            equalTo(
              FilterQuery(
                s"""$epochPrefix i_offer_photos_has_photos:>"1"""",
                "",
                List(
                  UnsupportedAttributeName("not.exist.attribute"),
                  UnsupportedFilterValue(Raw.bytes("1212".getBytes)),
                ),
              ),
            ),
          )
        },
        test("plain OR with errors: cut invalid filter") {
          val filter = Filter(or =
            Seq(
              Filter(op = eq("not.exist.attribute", Raw.u32(5))),
              Filter(op = more("other.not.exist.attribute", Raw.i32(6))),
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              Filter(op = more("seller_id.user_id", Raw.u64(456L))),
            ),
          )
          assertZIO(requestConverter.convert(filter, true))(
            equalTo(
              FilterQuery(
                s"""$epochPrefix (i_offer_photos_has_photos:>"1" | s_seller_id_user_id:>"456")""",
                "",
                List(
                  UnsupportedAttributeName("not.exist.attribute"),
                  UnsupportedAttributeName("other.not.exist.attribute"),
                ),
              ),
            ),
          )
        },
      ),
      suite("AND NOT")(
        test("plain one condition AND NOT") {
          val filter = Filter(and =
            Seq(
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              Filter(not =
                Some(Filter(op = eq("seller_id.user_id", Raw.u64(456L)))),
              ),
            ),
          )
          assertZIO(requestConverter.convert(filter, true))(
            equalTo(
              FilterQuery(
                s"""$epochPrefix i_offer_photos_has_photos:>"1" ~ (s_seller_id_user_id:"456")""",
                "",
                Nil,
              ),
            ),
          )
        },
        test("plain one condition AND NOT and external geo") {
          val filter = Filter(and =
            Seq(
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              Filter(not =
                Some(Filter(op = eq("seller_id.user_id", Raw.u64(456L)))),
              ),
              Filter(op =
                geoRadius(latitude = 56.8, longitude = 34.8, radius = 5),
              ),
            ),
          )

          for {
            result <- requestConverter.convert(filter, true)
          } yield assert(result.query)(
            equalTo(
              s"""$epochPrefix i_offer_photos_has_photos:>"1" ~ (s_seller_id_user_id:"456")""",
            ),
          ) &&
            assert(result.geoQuery)(
              equalTo(
                """comp:geo;lonlat:34.8,56.8;spn:0.16408563578376043,0.08998076661122682""",
              ),
            )
        },
        test("plain positive one AND two negative conditions") {
          val filter = Filter(and =
            Seq(
              Filter(not =
                Some(Filter(op = eq("seller_id.user_id", Raw.u64(123L)))),
              ),
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              Filter(not =
                Some(Filter(op = eq("seller_id.user_id", Raw.u64(456L)))),
              ),
            ),
          )
          assertZIO(requestConverter.convert(filter, true))(
            equalTo(
              FilterQuery(
                s"""$epochPrefix i_offer_photos_has_photos:>"1" ~ (s_seller_id_user_id:"123" | s_seller_id_user_id:"456")""",
                "",
                Nil,
              ),
            ),
          )
        },
        test("complex: positive one AND NOT (two conditions with OR)") {
          val filter = Filter(and =
            Seq(
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              Filter(not =
                Some(
                  Filter(or =
                    Seq(
                      Filter(op = eq("seller_id.user_id", Raw.u64(123L))),
                      Filter(op = eq("seller_id.user_id", Raw.u64(456L))),
                    ),
                  ),
                ),
              ),
            ),
          )
          assertZIO(requestConverter.convert(filter, true))(
            equalTo(
              FilterQuery(
                s"""$epochPrefix i_offer_photos_has_photos:>"1" ~ ((s_seller_id_user_id:"123" | s_seller_id_user_id:"456"))""",
                "",
                Nil,
              ),
            ),
          )
        },
        // A&(¬B|¬C) = A&¬(B&C) --> A ~ (B << C)
        test("complex: positive one AND (two conditions with NOT)") {
          val filter = Filter(and =
            Seq(
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              Filter(or =
                Seq(
                  Filter(not =
                    Some(Filter(op = eq("seller_id.user_id", Raw.u64(123L)))),
                  ),
                  Filter(not =
                    Some(Filter(op = eq("seller_id.user_id", Raw.u64(456L)))),
                  ),
                ),
              ),
            ),
          )
          assertZIO(requestConverter.convert(filter, true))(
            equalTo(
              FilterQuery(
                """i_offer_photos_has_photos:>"1" ~ (s_seller_id_user_id:"123" << s_seller_id_user_id:"456")""",
                "",
                Nil,
              ),
            ),
          )
        } @@ ignore,
      ),
      // A&¬(B|(C&¬D)) --> A ~ (B | C ~ D)
      test("complex case: inner NOT condition") {
        val filter = Filter(and =
          Seq(
            Filter(op = more("offer.photos.has_photos", Raw.i32(6))), // A
            Filter(not =
              Some(
                Filter(or =
                  Seq(
                    Filter(op = eq("seller_id.user_id", Raw.u64(123L))), // B
                    Filter(and =
                      Seq(
                        Filter(op =
                          eq("seller_id.user_id", Raw.u64(345L)),
                        ), // С
                        Filter(not =
                          Some(
                            Filter(op =
                              eq("offer.price.price_in_currency", Raw.u32(1000)),
                            ),
                          ),
                        ), // D
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        )
        assertZIO(requestConverter.convert(filter, true))(
          equalTo(
            FilterQuery(
              s"""$epochPrefix i_offer_photos_has_photos:>"1" ~ ((s_seller_id_user_id:"123" | s_seller_id_user_id:"345" ~ (i_offer_price_price_in_currency:"1000")))""",
              "",
              Nil,
            ),
          ),
        )
      },
      test("AND NOT with geo: exclude geo") {
        val not = Filter(not =
          Some(
            Filter(op =
              geoRadius(latitude = 56.8, longitude = 34.8, radius = 5),
            ),
          ),
        )
        val filter = Filter(and =
          Seq(
            Filter(not =
              Some(Filter(op = eq("seller_id.user_id", Raw.u64(123L)))),
            ),
            Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
            not,
          ),
        )
        assertZIO(requestConverter.convert(filter, true))(
          equalTo(
            FilterQuery(
              s"""$epochPrefix i_offer_photos_has_photos:>"1" ~ (s_seller_id_user_id:"123")""",
              "",
              List(UnsupportedFilter(not)),
            ),
          ),
        )
      },
      test("OR NOT: ignore error") {
        val filter = Filter(or =
          Seq(
            Filter(not =
              Some(Filter(op = eq("seller_id.user_id", Raw.u64(123L)))),
            ),
            Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
          ),
        )

        for {
          result <- requestConverter.convert(filter, true)
        } yield assert(result.query)(
          equalTo(s"""$epochPrefix i_offer_photos_has_photos:>"1""""),
        ) &&
          assert(result.errors)(hasSameElements(Seq(UnsupportedFilter(filter))))
      },
      suite("BETWEEN")(
        test("plain BETWEEN: offer.price.price_in_currency between 5 and 10") {
          val filter = Filter(op =
            between("offer.price.price_in_currency", Raw.u32(5), Raw.u32(10)),
          )

          assertZIO(requestConverter.convert(filter, true).map(_.query))(
            equalTo(
              s"""$epochPrefix i_offer_price_price_in_currency:>"5" << i_offer_price_price_in_currency:<"10"""",
            ),
          )

        },
        test("plain BETWEEN with error: not.exist.attribute between 5 and 10") {
          val filter = Filter(op =
            between("not.exist.attribute", Raw.u32(5), Raw.u32(10)),
          )

          for {
            res <- requestConverter.convert(filter, true)
          } yield assert(res.query)(equalTo(onlyEpoch)) &&
            assert(res.errors)(
              hasSameElements(
                Seq(UnsupportedAttributeName("not.exist.attribute")),
              ),
            )
        },
        test(
          "plain BETWEEN with errors: not.exist.attribute between <bytes> and nothing",
        ) {
          val filter = Filter(op =
            Filter
              .Op
              .Between(
                Between(
                  field = "not.exist.attribute",
                  from = Some(Raw.bytes("5".getBytes)),
                ),
              ),
          )
          assertZIO(requestConverter.convert(filter, true).run)(
            fails(
              hasSameElements(
                Seq(
                  UnsupportedAttributeName("not.exist.attribute"),
                  UnsupportedFilterValue(Raw.bytes("5".getBytes)),
                  MissingFilterValue(filter, "not.exist.attribute"),
                ),
              ),
            ),
          )
        },
        test(
          "plain BETWEEN with errors: not.exist.attribute between <bytes> and <bytes>",
        ) {
          val filter = Filter(op =
            Filter
              .Op
              .Between(
                Between(
                  field = "not.exist.attribute",
                  from = Some(Raw.bytes("5".getBytes)),
                  to = Some(Raw.bytes("6".getBytes)),
                ),
              ),
          )
          assertZIO(requestConverter.convert(filter, true).run)(
            fails(
              hasSameElements(
                Seq(
                  UnsupportedAttributeName("not.exist.attribute"),
                  UnsupportedFilterValue(Raw.bytes("5".getBytes)),
                  UnsupportedFilterValue(Raw.bytes("6".getBytes)),
                ),
              ),
            ),
          )
        },
      ),
      suite("IN")(
        test("plain IN: offer.price.price_in_currency in {2,4,6}") {
          val filter = Filter(op =
            in(
              "offer.price.price_in_currency",
              Seq(Raw.u32(2), Raw.u32(4), Raw.u32(6)),
            ),
          )

          for {
            result <- requestConverter.convert(filter, true).map(_.query)
          } yield assert(result)(
            equalTo(
              s"""$epochPrefix (i_offer_price_price_in_currency:"2" | i_offer_price_price_in_currency:"4" | i_offer_price_price_in_currency:"6")""",
            ),
          )
        },
        test("plain IN: offer.photos.has_photos in {true}") {
          val filter = Filter(op =
            in("offer.photos.has_photos", Seq(Raw.boolean(true))),
          )

          for {
            result <- requestConverter.convert(filter, true).map(_.query)
          } yield assert(result)(
            equalTo(s"""$epochPrefix (i_offer_photos_has_photos:"1")"""),
          )
        },
        test("plain IN with error: not.exist.attribute in {2,4,6}") {
          val filter = Filter(op =
            in("not.exist.attribute", Seq(Raw.u32(2), Raw.u32(4), Raw.u32(6))),
          )
          for {
            result <- requestConverter.convert(filter, true)
          } yield assert(result.query)(equalTo(onlyEpoch)) &&
            assert(result.errors)(
              hasSameElements(
                Seq(UnsupportedAttributeName("not.exist.attribute")),
              ),
            )
        },
        test(
          "plain IN with errors: offer.price.price_in_currency in {2, <bytes>, <bytes>}",
        ) {
          val filter = Filter(op =
            Filter
              .Op
              .In(
                In(
                  field = "offer.price.price_in_currency",
                  value = Seq(
                    Raw.i32(2),
                    Raw.bytes("4".getBytes),
                    Raw.bytes("6".getBytes),
                  ),
                ),
              ),
          )
          for {
            result <- requestConverter.convert(filter, true)
          } yield assert(result.query)(
            equalTo(s"""$epochPrefix (i_offer_price_price_in_currency:"2")"""),
          ) &&
            assert(result.errors)(
              hasSameElements(
                Seq(
                  UnsupportedFilterValue(Raw.bytes("4".getBytes)),
                  UnsupportedFilterValue(Raw.bytes("6".getBytes)),
                ),
              ),
            )
        },
        test(
          "plain IN with all errors: not.exist.attribute in {<bytes>, <bytes>, <bytes>}",
        ) {
          val filter = Filter(op =
            Filter
              .Op
              .In(
                In(
                  field = "not.exist.attribute",
                  value = Seq("2", "4", "6").map(v => Raw.bytes(v.getBytes)),
                ),
              ),
          )
          assertZIO(requestConverter.convert(filter, true).run)(
            fails(
              hasSameElements(
                Seq(
                  UnsupportedAttributeName("not.exist.attribute"),
                  UnsupportedFilterValue(Raw.bytes("2".getBytes)),
                  UnsupportedFilterValue(Raw.bytes("4".getBytes)),
                  UnsupportedFilterValue(Raw.bytes("6".getBytes)),
                ),
              ),
            ),
          )
        },
      ),
      test("complex: with OR as first link") {
        val complexFilter = Filter(or =
          Seq(
            Filter(op = eq("seller_id.user_id", Raw.u64(4051370494L))),
            Filter(or =
              Seq(
                Filter(op =
                  less("offer.price.price_in_currency", Raw.u32(1000)),
                ),
                Filter(op =
                  more("offer.price.price_in_currency", Raw.u32(10000)),
                ),
              ),
            ),
          ),
        )
        assertZIO(requestConverter.convert(complexFilter, true).map(_.query))(
          equalTo(
            s"""$epochPrefix (s_seller_id_user_id:"4051370494" | (i_offer_price_price_in_currency:<"1000" | i_offer_price_price_in_currency:>"10000"))""",
          ),
        )
      },
      suite("GEO")(
        test("plain eq: offer.region") {
          val filter = Filter(op = in("offer.region", values = Seq()))
          assertZIO(requestConverter.convert(filter, false).run)(
            fails(
              hasSameElements(Seq(MissingFilterValue(filter, "offer.region"))),
            ),
          )
        },
        test("simple radius geo") {
          val complexFilter = Filter(and =
            Seq(
              Filter(op = eq("offer.price.price_in_currency", Raw.u32(5))),
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              Filter(op =
                geoRadius(latitude = 56.8, longitude = 34.8, radius = 5),
              ),
            ),
          )
          for {
            result <- requestConverter.convert(complexFilter, true)
          } yield assert(result.query)(
            equalTo(
              s"""$epochPrefix i_offer_price_price_in_currency:"5" << i_offer_photos_has_photos:>"1"""",
            ),
          ) &&
            assert(result.geoQuery)(
              equalTo(
                """comp:geo;lonlat:34.8,56.8;spn:0.16408563578376043,0.08998076661122682""",
              ),
            )
        },
        test("geo: and with one branch") {
          val radius = 10000L
          val complexFilter = Filter(and =
            Seq(
              Filter(op =
                Filter
                  .Op
                  .Geo(
                    LocationFilter(
                      LocationFilter
                        .Filter
                        .Radius(
                          LocationFilter.RadiusFilter(
                            LocationFilter
                              .RadiusFilter
                              .Center
                              .Location(
                                LocationFilter
                                  .Location(latitude = 56.8, longitude = 34.8),
                              ),
                            radius / 1000,
                          ),
                        ),
                    ),
                  ),
              ),
            ),
          )
          assertZIO(
            requestConverter.convert(complexFilter, true).map(_.geoQuery),
          )(
            equalTo(
              """comp:geo;lonlat:34.8,56.8;spn:0.32817127156752085,0.17996153322245365""",
            ),
          )
        },
        test("radius geo with error") {
          val geoSubFilter = Filter
            .Op
            .Geo(
              LocationFilter(
                vertis
                  .vasgen
                  .query
                  .LocationFilter
                  .Filter
                  .Radius(
                    vertis
                      .vasgen
                      .query
                      .LocationFilter
                      .RadiusFilter(center = Center.RegionId(213), 5),
                  ),
              ),
            )
          val complexFilter = Filter(and =
            Seq(
              Filter(op = eq("offer.price.price_in_currency", Raw.u32(5))),
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              Filter(op = geoSubFilter),
            ),
          )

          assertZIO(requestConverter.convert(complexFilter, true).run)(
            fails(
              hasSameElements(Seq(UnsupportedFilter(Filter(op = geoSubFilter)))),
            ),
          )
        },
        test("geo region") {
          val complexFilter = Filter(and =
            Seq(
              Filter(op = eq("offer.price.price_in_currency", Raw.u32(5))),
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              Filter(op = geoRegion(213)),
            ),
          )
          assertZIO(requestConverter.convert(complexFilter, true).map(_.query))(
            equalTo(
              s"""$epochPrefix i_offer_price_price_in_currency:"5" << i_offer_photos_has_photos:>"1" << s_offer_region_g:"213"""",
            ),
          )
        },
        test("geo in or") {
          val complexFilter = Filter(or =
            Seq(
              Filter(op = eq("offer.price.price_in_currency", Raw.u32(5))),
              Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              Filter(op = geoRegion(213)),
            ),
          )
          assertZIO(requestConverter.convert(complexFilter, true).map(_.query))(
            equalTo(
              s"""$epochPrefix (i_offer_price_price_in_currency:"5" | i_offer_photos_has_photos:>"1" | s_offer_region_g:"213")""",
            ),
          )
        },
        test("geo: duplicated region") {
          val complexFilter = Filter(or =
            Seq(
              Filter(op = eq("offer.price.price_in_currency", Raw.u32(5))),
              Filter(op = eq("offer.region", Raw.u32(213))),
              Filter(op = geoRegion(213)),
            ),
          )
          assertZIO(requestConverter.convert(complexFilter, true).map(_.query))(
            equalTo(
              s"""$epochPrefix (i_offer_price_price_in_currency:"5" | s_offer_region_g:"213" | s_offer_region_g:"213")""",
            ),
          )
        },
      ),
      test("timestamp as filter value") {
        val filter = Filter(and =
          Seq(
            Filter(op = eq("offer.price.price_in_currency", Raw.i32(5))),
            Filter(op = more("offer.publish_date", Raw.timestamp(1614193173L))),
          ),
        )
        assertZIO(requestConverter.convert(filter, true).map(_.query))(
          equalTo(
            s"""$epochPrefix i_offer_price_price_in_currency:"5" << i_offer_publish_date:>"1614193173"""",
          ),
        )
      },
      test("pk as filter field") {
        val filter = Filter(and =
          Seq(
            Filter(op = eq("offer.price.price_in_currency", Raw.i32(5))),
            Filter(op = eq("pk.s", Raw.str("1614193173"))),
          ),
        )
        assertZIO(requestConverter.convert(filter, true).map(_.query))(
          equalTo(
            s"""$epochPrefix i_offer_price_price_in_currency:"5" << s_pk:"1614193173"""",
          ),
        )
      },
      test("success complex") {
        val complexFilter = Filter(or =
          Seq(
            Filter(and =
              Seq(
                Filter(op = eq("offer.price.price_in_currency", Raw.u32(5))),
                Filter(op = more("offer.photos.has_photos", Raw.i32(6))),
              ),
            ),
            Filter(or =
              Seq(
                Filter(op = eq("offer.price.price_in_currency", Raw.u32(10))),
                Filter(and =
                  Seq(
                    Filter(op = eq("offer.photos.has_photos", Raw.u32(7))),
                    Filter(op = less("seller_id.user_id", Raw.i32(6))),
                  ),
                ),
              ),
            ),
          ),
        )
        assertZIO(requestConverter.convert(complexFilter, true).map(_.query))(
          equalTo(
            s"""$epochPrefix (i_offer_price_price_in_currency:"5" << i_offer_photos_has_photos:>"1" | (i_offer_price_price_in_currency:"10" | i_offer_photos_has_photos:"1" << s_seller_id_user_id:<"6"))""",
          ),
        )
      },
      test("combine for invalid filter in complex") {
        val complexFilter = Filter(and =
          Seq(
            Filter(op = eq("offer.photos.has_photos", Raw.u32(1))),
            Filter(or =
              Seq(
                Filter(op =
                  less("offer.attributes.diagonal-tochno", Raw.f64(1000.0d)),
                ),
                Filter(or =
                  Seq(
                    Filter(op = eq("seller_id.user_id", Raw.u64(1L))),
                    Filter(op = eq("not.exist.attribute", Raw.u64(1L))),
                  ),
                ),
                Filter(op =
                  more("other.not.exist.attribute", Raw.f64(10000.123456789d)),
                ),
              ),
            ),
          ),
        )
        for {
          result <- requestConverter.convert(complexFilter, true)
        } yield assert(result.query)(
          equalTo(
            s"""$epochPrefix i_offer_photos_has_photos:"1" << (i_offer_attributes_diagonal:<"${adaptF64(
              1000.0d,
            )}" | s_seller_id_user_id:"1")""",
          ),
        ) &&
          assert(result.errors)(
            hasSameElements(
              List(
                UnsupportedAttributeName("not.exist.attribute"),
                UnsupportedAttributeName("other.not.exist.attribute"),
              ),
            ),
          )
      },
      test("realtime sample") {
        val complexFilter = Filter(or =
          Seq(
            Filter(op = more("offer.price.price_in_currency", Raw.f64(1234d))),
            Filter(or =
              Seq(
                Filter(op =
                  less("offer.attributes.diagonal-tochno", Raw.f64(55.5d)),
                ),
                Filter(op =
                  more("offer.attributes.diagonal-tochno", Raw.f64(43.8899d)),
                ),
              ),
            ),
          ),
        )
        assertZIO(requestConverter.convert(complexFilter, true).map(_.query))(
          equalTo(
            s"""$epochPrefix (i_offer_price_price_in_currency:>"1234" | (i_offer_attributes_diagonal:<"${adaptF64(
              55.5d,
            )}" | i_offer_attributes_diagonal:>"${adaptF64(43.8899d)}"))""",
          ),
        )
      },
      test("url encoder") {
        assert(URLEncoder.encode(" << ", Charset.defaultCharset()))(
          equalTo("+%3C%3C+"),
        ) &&
        assert(URLEncoder.encode(" | ", Charset.defaultCharset()))(
          equalTo("+%7C+"),
        ) &&
        assert(URLEncoder.encode(">=", Charset.defaultCharset()))(
          equalTo("%3E%3D"),
        ) &&
        assert(URLEncoder.encode("<=", Charset.defaultCharset()))(
          equalTo("%3C%3D"),
        )
      },
      test("cut float") {
        val formatter = new DecimalFormat("#.######")
        val decimalD  = 12345678.1234567890d
        val wholeD    = 1234d
        assert(formatter.format(decimalD))(equalTo("12345678.123457")) &&
        assert(formatter.format(wholeD))(equalTo("1234"))
      },
    )

  override def spec =
    tests.provideCustomLayerShared(
      Clock.live ++ fieldConverterLayer(initMapping) ++ epoch(initEpoch) ++
        Tracing.noop,
    ) @@ TestAspect.timeout(60.seconds)

  def eq(field: String, value: RawValue): Op.Eq =
    Filter.Op.Eq(vertis.vasgen.query.Eq(field = field, value = Some(value)))

  def more(field: String, value: RawValue): Op.Gte =
    Filter.Op.Gte(GreaterThan(field = field, value = Some(value)))

  def less(field: String, value: RawValue): Op.Lte =
    Filter.Op.Lte(LessThan(field = field, value = Some(value)))

  def between(field: String, from: RawValue, to: RawValue): Op.Between =
    Filter.Op.Between(Between(field = field, from = Some(from), to = Some(to)))

  def in(field: String, values: Seq[RawValue]): Op.In =
    Filter.Op.In(In(field = field, value = values))

  def geoRadius(latitude: Double, longitude: Double, radius: Long) =
    Filter
      .Op
      .Geo(
        LocationFilter(
          vertis
            .vasgen
            .query
            .LocationFilter
            .Filter
            .Radius(
              vertis
                .vasgen
                .query
                .LocationFilter
                .RadiusFilter(
                  center = Center.Location(
                    vertis
                      .vasgen
                      .query
                      .LocationFilter
                      .Location(latitude, longitude),
                  ),
                  radius,
                ),
            ),
        ),
      )

  def geoRegion(region: Int) =
    Filter
      .Op
      .Geo(
        LocationFilter(
          vertis.vasgen.query.LocationFilter.Filter.RegionId(region),
        ),
      )

  def adaptF64(value: Double): String =
    RawValueUtil
      .adapt(Raw.f64(value), SaasIndexType.Factor)
      .map(_.searchValue)
      .getOrElse("")

  def adaptI32(value: Int): String =
    RawValueUtil
      .adapt(Raw.i32(value), SaasIndexType.SearchAttribute)
      .map(_.searchValue)
      .getOrElse("")

}
