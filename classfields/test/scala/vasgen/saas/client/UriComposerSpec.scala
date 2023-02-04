package vasgen.saas.client

import java.net.URLEncoder
import java.nio.charset.Charset
import scala.concurrent.duration.DurationInt

object UriComposerSpec extends ZIOSpecDefault with Logging {

  val DefaultTextZoneSaas: SaasName = FieldMappingBase
    .fullTextZoneName(FieldMapping.DefaultTextZone)

  val constAssert: Assertion[Iterable[String]] =
    exists(
      matchesRegex(
        "calc=offer_time_diff:clamp\\(diff\\(\\d{10},#f_f_offer_publish_date_g\\),0,2592000\\)",
      ),
    ) &&
      exists(
        matchesRegex(
          "calc=f_publish_date_diff:diff\\(\\d{10},#f_f_offer_publish_date_g\\)",
        ),
      ) &&
      exists(
        matchesRegex(
          "calc=f_activate_date_diff:diff\\(\\d{10},#f_f_offer_activate_date_g\\)",
        ),
      ) &&
      hasSubset(
        Seq(
          "all_factors",
          "calc=f_publish_date_normed:mul(#f_publish_date_diff, 0.0000003858)",
          "calc=f_activate_date_normed:clamp(#f_activate_date_diff, 0, 2592000)",
          "calc=sort_hash:fnvhash_f32(zdocid_i64())",
        ),
      )

  private val testSaasConfig = SaasConfig(
    host = "saas-searchproxy-testing.yandex.net",
    port = 80,
    service = "test",
    tvmId = 0,
    timeout = 1.second,
  )

  override def spec
    : Spec[TestEnvironment, TestFailure[Nothing], TestSuccess] = {
    suite("compose url")(
      suite("relevancePruningParams")(
        test("empty pruning") {
          assert(
            uriComposer.relevancePruningParams(RelevancePruning.defaultInstance),
          )(isEmptyString)
        },
        test("empty formula name") {
          assert(uriComposer.relevancePruningParams(RelevancePruning("", 10f)))(
            isEmptyString,
          )
        },
        test("threshold is zero") {
          assert(
            uriComposer.relevancePruningParams(RelevancePruning("name", 0f)),
          )(isEmptyString)
        },
        test("threshold is negative") {
          assert(
            uriComposer.relevancePruningParams(RelevancePruning("name", -1f)),
          )(isEmptyString)
        },
        test("positive scenario") {
          assert(
            uriComposer
              .relevancePruningParams(RelevancePruning("formula_name", 5.5f)),
          )(equalTo(";filter=formula_name;filter_border=5.5"))
        },
      ),
      suite("sorting")(
        test("by attr") {
          assert(
            uriComposer.sortParam(
              Sorting
                .SortBy(ByAttributeSort("i_price", ByAttributeSort.Order.ASC)),
              Nil,
              ExtFactors.defaultInstance,
              calcAllFactors = false,
            ),
          )(equalTo(Map("how" -> "i_price", "asc" -> "da"))) &&
          assert(
            uriComposer.sortParam(
              Sorting
                .SortBy(ByAttributeSort("i_price", ByAttributeSort.Order.DESC)),
              Nil,
              ExtFactors.defaultInstance,
              calcAllFactors = false,
            ),
          )(equalTo(Map("how" -> "i_price")))
        },
        test("without sort") {
          assert(
            uriComposer.sortParam(
              Sorting.Empty,
              Nil,
              ExtFactors.defaultInstance,
              calcAllFactors = false,
            ),
          )(equalTo(Map("how" -> "docid")))
        },
        test("random") {
          assert(
            uriComposer
              .sortParam(
                Sorting.Random(Random()),
                Nil,
                ExtFactors.defaultInstance,
                calcAllFactors = false,
              )
              .get("how"),
          )(isSome(containsString("shuffle"))) &&
          assert(
            uriComposer
              .sortParam(
                Sorting.Random(Random(67)),
                Nil,
                ExtFactors.defaultInstance,
                calcAllFactors = false,
              )
              .get("relev"),
          )(
            isSome(
              containsString(
                s"calc=$seedRandomSortFactor:fnvhash_f32(67,zdocid_i64());formula=${UriComposer.ShufflePolynom}",
              ),
            ),
          )
        },
        test("by relevance") {
          assert(
            uriComposer
              .sortParam(
                Sorting.Relevance(QueryMetadata.Relevance()),
                Nil,
                ExtFactors.defaultInstance,
                calcAllFactors = true,
              )
              .get("relev"),
          )(isSome(containsString(s"formula=$DefaultRelevancePolynomName")))
        },
        test("by formula") {
          assert(
            uriComposer
              .sortParam(
                Sorting.Relevance(
                  QueryMetadata.Relevance(formulaName = "click_relevance"),
                ),
                Nil,
                ExtFactors.defaultInstance,
                calcAllFactors = true,
              )
              .get("relev"),
          )(isSome(containsString("formula=click_relevance")))
        },
      ),
      suite("experiment")(
        test("turn on annotation") {
          assertZIO(
            uriComposer
              .composeSearchUri(
                requestID = "",
                simpleText = Text.defaultInstance.withQuery("панель"),
                filters = FilterQuery("", "", Nil),
                sorting = Sorting.SortBy(
                  ByAttributeSort("i_price", ByAttributeSort.Order.ASC),
                ),
                refineFactors = Seq(),
                rankingVectors = Nil,
                userFactors = ExtFactors.defaultInstance,
                pruneLimit = None,
                relevancePruning = RelevancePruning.defaultInstance,
                softness = None,
                debugMode = Debug.defaultInstance,
                experiments = Set(AnnSearchExp),
              )
              .map(_.toString)
              .run,
          )(
            succeeds(
              containsString(
                s"${URLEncoder.encode("relevgeo=255", Charset.defaultCharset())}&pron=use_factorann",
              ),
            ),
          )
        },
        test("turn off annotation") {
          assertZIO(
            uriComposer
              .composeSearchUri(
                requestID = "",
                simpleText = Text.defaultInstance.withQuery("панель"),
                filters = FilterQuery("", "", Nil),
                sorting = Sorting.SortBy(
                  ByAttributeSort("i_price", ByAttributeSort.Order.ASC),
                ),
                refineFactors = Seq(),
                rankingVectors = Nil,
                userFactors = ExtFactors.defaultInstance,
                pruneLimit = None,
                relevancePruning = RelevancePruning.defaultInstance,
                softness = None,
                debugMode = Debug.defaultInstance,
                experiments = Set(),
              )
              .map(_.toString)
              .run,
          )(
            succeeds(
              not(
                containsString(
                  s"${URLEncoder.encode("relevgeo=255", Charset.defaultCharset())}&pron=use_factorann",
                ),
              ),
            ),
          )
        },
        test("turn on highlight") {
          assertZIO(
            uriComposer
              .composeSearchUri(
                requestID = "",
                simpleText = Text
                  .defaultInstance
                  .withQuery("панель")
                  .withEnableHighlight(true),
                filters = FilterQuery("", "", Nil),
                sorting = Sorting.SortBy(
                  ByAttributeSort("i_price", ByAttributeSort.Order.ASC),
                ),
                refineFactors = Seq(),
                rankingVectors = Nil,
                userFactors = ExtFactors.defaultInstance,
                pruneLimit = None,
                relevancePruning = RelevancePruning.defaultInstance,
                softness = None,
                debugMode = Debug.defaultInstance,
                experiments = Set(AnnSearchExp),
              )
              .map(_.toString)
              .run,
          )(
            succeeds(
              containsString(
                s"&gta=_snippet_g&gta=$AllZoneNames&gta=$AllTextZonesTogether&fsgta=$Highlights&rty_hits_detail=da&qi=rty_hits_count&qi=rty_hits_count_full",
              ),
            ),
          )
        },
      ),
      suite("relev")(
        test("without vectors") {
          assert(
            uriComposer
              .relevanceExpr(
                "some_poly",
                Nil,
                ExtFactors.defaultInstance.addAssign(SetFactor("fac_age", 26f)),
                calcAllFactors = true,
              )
              .split(";")
              .toSeq,
          )(
            constAssert &&
              hasSubset(Seq("formula=some_poly", "calc=fac_age:26.0")),
          )

        },
        test("with vectors") {
          assert(
            uriComposer
              .relevanceExpr(
                "some_poly",
                Seq(
                  EmbeddedVector(
                    "q1",
                    1,
                    Some(FloatVector(Seq(1.7f, 3.6f, 7.8f, 6.1f))),
                    "dssm_dot_product_q1",
                  ),
                  EmbeddedVector(
                    "q2",
                    2,
                    Some(FloatVector(Seq(7.8f, 6.1f, 1.7f, 3.6f))),
                    "dssm_dot_product_q2",
                  ),
                ),
                ExtFactors.defaultInstance.addAssign(SetFactor("fac_age", 26f)),
                calcAllFactors = true,
              )
              .split(";")
              .toSeq,
          )(
            constAssert &&
              hasSubset(
                Seq(
                  "dssm_dot_product_q1_qv=mpnZP2ZmZkCamflAMzPDQA==",
                  """calc=dssm_dot_product_q1:dot_product(doc_dssm_decompress("q1","Float32","1"),dssm_decode(base64_decode(get_relev("dssm_dot_product_q1_qv")),"Float32"))""",
                  "dssm_dot_product_q2_qv=mpn5QDMzw0Camdk/ZmZmQA==",
                  """calc=dssm_dot_product_q2:dot_product(doc_dssm_decompress("q2","Float32","2"),dssm_decode(base64_decode(get_relev("dssm_dot_product_q2_qv")),"Float32"))""",
                  "formula=some_poly",
                  "calc=fac_age:26.0",
                ),
              ),
          )

        },
      ),
      suite("user factors")(
        test("userFactorsParam") {
          val input: QueryMetadata.ExtFactors = QueryMetadata
            .ExtFactors(assign =
              Seq(SetFactor("f_age", 23), SetFactor("f_location", 5.77887f)),
            )
          val invalid: QueryMetadata.ExtFactors = QueryMetadata
            .ExtFactors(assign = Seq())
          assert(uriComposer.userFactorsParam(input))(
            equalTo("calc=f_age:23.0;calc=f_location:5.77887"),
          ) &&
          assert(uriComposer.userFactorsParam(ExtFactors.defaultInstance))(
            equalTo(""),
          ) && assert(uriComposer.userFactorsParam(invalid))(equalTo(""))
        },
        test("convertInset") {
          assert(
            uriComposer.convertInset(
              Inset(
                Some(SetFactor("", 15)),
                "attr_name",
                Seq(Raw.i64(1), Raw.i64(2), Raw.i64(-3)),
              ),
            ),
          )(isEmptyString) &&
          assert(
            uriComposer.convertInset(
              Inset(
                Some(SetFactor("factor_name", 15)),
                "",
                Seq(Raw.i64(1), Raw.i64(2), Raw.i64(-3)),
              ),
            ),
          )(isEmptyString) &&
          assert(
            uriComposer.convertInset(
              Inset(Some(SetFactor("factor_name", 15)), "attr_name", Nil),
            ),
          )(isEmptyString) &&
          assert(
            uriComposer.convertInset(
              Inset(
                Some(SetFactor("factor_name", 15)),
                "attr_name",
                Seq(Raw.i64(1), Raw.i64(2), Raw.i64(-3)),
              ),
            ),
          )(
            equalTo(
              s"calc=factor_name:mul(insetany(#group_attr_name,1,2,9223372036854775805), 15.0)",
            ),
          )
        },
      ),
      test("paging") {
        assert(uriComposer.pagingCondition(Paging(3, 4)))(
          equalTo(Map("p" -> "3", "numdoc" -> "4")),
        )
      },
      test("refineFactorsExpr") {
        val input = Seq(
          ExtendedRefine(
            BoostFactor("f_score", 0.5f),
            FilterQuery("""i_price:"1000"""", ""),
          ),
          ExtendedRefine(
            BoostFactor("f_user_score", 0.3f),
            FilterQuery("""i_date:"16786543"""", ""),
          ),
        )
        assert(uriComposer.refineFactorsExpr(input))(
          equalTo(
            """ <- refinefactor:f_score=0.5 i_price:"1000"""" +
              """ <- refinefactor:f_user_score=0.3 i_date:"16786543"""",
          ),
        )
      },
      test("statistics") {
        val input = ExtendedStatParams(
          FacetParam(AttrNameMapping("price", "i_price"), 10),
          Seq(
            AttrNameMapping("price", "i_price"),
            AttrNameMapping("date", "i_date"),
          ),
        )
        assert(uriComposer.borderCondition(input.boundedAttrs))(
          equalTo(
            Seq(
              "borders" -> "i_price,i_date",
              "qi"      -> "rty_min_i_price",
              "qi"      -> "rty_max_i_price",
              "qi"      -> "rty_min_i_date",
              "qi"      -> "rty_max_i_date",
            ),
          ),
        ) &&
        assert(uriComposer.facetCondition(input.facetParam))(
          equalTo(Map("gafacets" -> "i_price:10", "qi" -> "facet_i_price")),
        )
      },
      suite("search text")(
        test("search one word") {
          assertZIO(
            uriComposer
              .packTextParams(Text(query = "плейстейшн"), "", Nil, None)
              .run,
          )(succeeds(equalTo(("плейстейшн", "%request%"))))
        },
        test("search several word") {
          assertZIO(
            uriComposer
              .packTextParams(
                Text(query = "sony плейстейшн 4", exclude = "nintendo"),
                "",
                Nil,
                None,
              )
              .run,
          )(succeeds(equalTo(("sony плейстейшн 4 ~~ nintendo", "%request%"))))
        },
        test("search with text, softness, refine") {
          assertZIO(
            uriComposer
              .packTextParams(
                Text(query = "sony плейстейшн 4"),
                """i_epoch:>="8"""",
                Seq(
                  ExtendedRefine(
                    BoostFactor("f_score", 0.5f),
                    FilterQuery("""i_price:"1000"""", ""),
                  ),
                ),
                Some(55),
              )
              .run,
          )(
            succeeds(
              equalTo(
                (
                  """sony плейстейшн 4""",
                  """%request% << i_epoch:>="8" softness:55 <- refinefactor:f_score=0.5 i_price:"1000"""",
                ),
              ),
            ),
          )
        },
      ),
      suite("fulfillment check")(
        test("sort by other polynom") {
          val result = uriComposer
            .composeSearchUri(
              requestID = "test-123",
              simpleText = Text(query = "панель"),
              filters = FilterQuery(
                s"""$epochPrefix i_offer_price_price_in_currency:>"5" << i_offer_price_price_in_currency:<"10"""",
                "",
                Nil,
              ),
              sorting = Sorting.Polynom(UsingPolynom("other_polynom")),
              refineFactors = Seq(
                ExtendedRefine(
                  BoostFactor("f_refine_category", 0.9999984f),
                  FilterQuery(
                    """offer.category.all:"stroitelstvo-i-remont_h7HBY6"""",
                    "",
                  ),
                ),
                ExtendedRefine(
                  BoostFactor("f_refine_category", 1.6350776e-6f),
                  FilterQuery(
                    """offer.category.all:"listovoy-prokat_vYPldQ"""",
                    "",
                  ),
                ),
              ),
              rankingVectors = Seq(
                EmbeddedVector(
                  "q1",
                  1,
                  Some(FloatVector(Seq(1.7f, 3.6f, 7.8f, 6.1f))),
                  "dssm_dot_product_q1",
                ),
                EmbeddedVector(
                  "q2",
                  1,
                  Some(FloatVector(Seq(1.7f, 3.6f, 7.8f, 6.1f))),
                  "dssm_dot_product_q2",
                ),
              ),
              userFactors = ExtFactors.defaultInstance,
              pruneLimit = None,
              relevancePruning = RelevancePruning.defaultInstance,
              softness = None,
              debugMode = Debug(false, true),
              experiments = Set.empty,
            )
            .map(_.toString().replaceAll("1\\d{9}", "NOW_STUB"))
          assertZIO(result.run)(
            succeeds(
              equalTo(
                s"""${testSaasConfig.baseUrl}?service=${testSaasConfig.service}&
                   |dbgrlv=da&fsgta=_JsonFactors&snip=diqm%3D1&reqid=test-123&timeout=1000000&ms=proto&kps=1&
                   |relev=all_factors;calc%3Dsort_hash:fnvhash_f32(zdocid_i64());
                   |calc%3Doffer_time_diff:clamp(diff(NOW_STUB,%23f_f_offer_publish_date_g),0,2592000);
                   |calc%3Df_publish_date_diff:diff(NOW_STUB,%23f_f_offer_publish_date_g);
                   |calc%3Df_activate_date_diff:diff(NOW_STUB,%23f_f_offer_activate_date_g);
                   |calc%3Df_publish_date_normed:mul(%23f_publish_date_diff,+0.0000003858);
                   |calc%3Df_activate_date_normed:clamp(%23f_activate_date_diff,+0,+2592000);
                   |dssm_dot_product_q1_qv%3DmpnZP2ZmZkCamflAMzPDQA%3D%3D;
                   |calc%3Ddssm_dot_product_q1:dot_product(doc_dssm_decompress(%22q1%22,%22Float32%22,%221%22),dssm_decode(base64_decode(get_relev(%22dssm_dot_product_q1_qv%22)),%22Float32%22));
                   |dssm_dot_product_q2_qv%3DmpnZP2ZmZkCamflAMzPDQA%3D%3D;
                   |calc%3Ddssm_dot_product_q2:dot_product(doc_dssm_decompress(%22q2%22,%22Float32%22,%221%22),dssm_decode(base64_decode(get_relev(%22dssm_dot_product_q2_qv%22)),%22Float32%22));
                   |formula%3Dother_polynom&gta=_snippet_g&relev=enable_softness;attr_limit%3D100000000&
                   |text=%D0%BF%D0%B0%D0%BD%D0%B5%D0%BB%D1%8C&template=%25request%25+%3C%3C+i_epoch:%3E%3D%228%22+%3C%3C+i_offer_price_price_in_currency:%3E%225%22+%3C%3C+i_offer_price_price_in_currency:%3C%2210%22+%3C-+
                   |refinefactor:f_refine_category%3D0.9999984+offer.category.all:%22stroitelstvo-i-remont_h7HBY6%22+%3C-+refinefactor:f_refine_category%3D1.6350776E-6+offer.category.all:%22listovoy-prokat_vYPldQ%22"""
                  .stripMargin
                  .replaceAll("\n", ""),
              ),
            ),
          )
        },
        test("sort by price") {
          assertZIO(
            (
              for {
                uri <- uriComposer.composeSearchUri(
                  requestID = "",
                  simpleText = Text(query = "панель"),
                  filters = FilterQuery(
                    s"""$epochPrefix i_offer_price_price_in_currency:>"5" << i_offer_price_price_in_currency:<"10"""",
                    "",
                    Nil,
                  ),
                  sorting = Sorting.SortBy(
                    ByAttributeSort("i_price", ByAttributeSort.Order.ASC),
                  ),
                  refineFactors = Seq(
                    ExtendedRefine(
                      BoostFactor("f_refine_category", 0.9999984f),
                      FilterQuery(
                        """offer.category.all:"stroitelstvo-i-remont_h7HBY6"""",
                        "",
                      ),
                    ),
                    ExtendedRefine(
                      BoostFactor("f_refine_category", 1.6350776e-6f),
                      FilterQuery(
                        """offer.category.all:"listovoy-prokat_vYPldQ"""",
                        "",
                      ),
                    ),
                  ),
                  rankingVectors = Nil,
                  userFactors = ExtFactors.defaultInstance,
                  pruneLimit = None,
                  relevancePruning = RelevancePruning.defaultInstance,
                  softness = None,
                  debugMode = Debug.defaultInstance,
                  experiments = Set.empty,
                )
              } yield uri.toString()
            ).run,
          )(
            succeeds(
              equalTo(
                s"""${testSaasConfig.baseUrl}?service=${testSaasConfig.service}
                   |&dbgrlv=da&fsgta=_JsonFactors&kps=1&asc=da&how=i_price&snip=diqm%3D1&timeout=1000000
                   |&ms=proto&gta=_snippet_g&relev=enable_softness;attr_limit%3D100000000&text=%D0%BF%D0%B0%D0%BD%D0%B5%D0%BB%D1%8C
                   |&template=%25request%25+%3C%3C+i_epoch:%3E%3D%228%22+%3C%3C+i_offer_price_price_in_currency:%3E%225%22+%3C%3C+i_offer_price_price_in_currency:%3C%2210%22+%3C-+
                   |refinefactor:f_refine_category%3D0.9999984+offer.category.all:%22stroitelstvo-i-remont_h7HBY6%22+%3C-+refinefactor:f_refine_category%3D1.6350776E-6+offer.category.all:%22listovoy-prokat_vYPldQ%22"""
                  .stripMargin
                  .replaceAll("\n", ""),
              ),
            ),
          )
        },
        test("debug mode") {
          assertZIO(
            (
              for {
                uri <- uriComposer.composeSearchUri(
                  requestID = "",
                  simpleText = Text(query = "iphone"),
                  filters = FilterQuery("", "", Nil),
                  sorting = Sorting.Empty,
                  refineFactors = Nil,
                  rankingVectors = Nil,
                  userFactors = ExtFactors.defaultInstance,
                  pruneLimit = None,
                  relevancePruning = RelevancePruning.defaultInstance,
                  softness = None,
                  debugMode = Debug(true, showFactors = false),
                  experiments = Set.empty,
                )
              } yield uri.toString()
            ).run,
          )(
            succeeds(
              equalTo(
                s"""${testSaasConfig.baseUrl}?service=${testSaasConfig.service}
                   |&dbgrlv=da&fsgta=_JsonFactors&kps=1&dump=eventlog&how=docid&snip=diqm%3D1&timeout=1000000
                   |&ms=proto&gta=_snippet_g&relev=enable_softness;attr_limit%3D100000000&text=iphone&template=%25request%25"""
                  .stripMargin
                  .replaceAll("\n", ""),
              ),
            ),
          )
        },
      ),
      suite("pron")(
        test("empty") {
          assertZIO(
            (
              for {
                uri <- uriComposer.composeSearchUri(
                  requestID = "",
                  simpleText = Text(query = "iphone"),
                  filters = FilterQuery("", "", Nil),
                  sorting = Sorting.Empty,
                  refineFactors = Nil,
                  rankingVectors = Nil,
                  userFactors = ExtFactors.defaultInstance,
                  pruneLimit = None,
                  relevancePruning = RelevancePruning.defaultInstance,
                  softness = None,
                  debugMode = Debug(true, showFactors = false),
                  experiments = Set.empty,
                )
              } yield uri.toString()
            ).run,
          )(
            succeeds(
              equalTo(
                s"""${testSaasConfig.baseUrl}?service=${testSaasConfig.service}
                   |&dbgrlv=da&fsgta=_JsonFactors&kps=1&dump=eventlog&how=docid&snip=diqm%3D1&timeout=1000000
                   |&ms=proto&gta=_snippet_g&relev=enable_softness;attr_limit%3D100000000&text=iphone&template=%25request%25"""
                  .stripMargin
                  .replaceAll("\n", ""),
              ),
            ),
          )
        },
        test("pron without geo") {
          assertZIO(
            (
              for {
                uri <- uriComposer.composeSearchUri(
                  requestID = "",
                  simpleText = Text(query = "iphone"),
                  filters = FilterQuery("", "", Nil),
                  sorting = Sorting.Empty,
                  refineFactors = Nil,
                  rankingVectors = Nil,
                  userFactors = ExtFactors.defaultInstance,
                  pruneLimit = Some(500),
                  relevancePruning = RelevancePruning.defaultInstance,
                  softness = None,
                  debugMode = Debug(true, showFactors = false),
                  experiments = Set.empty,
                )
              } yield uri.toString()
            ).run,
          )(succeeds(containsString("&pron=pruncount500")))
        },
        test("pron with geo") {
          assertZIO(
            (
              for {
                uri <- uriComposer.composeSearchUri(
                  requestID = "",
                  simpleText = Text(query = "iphone"),
                  filters = FilterQuery("", "test_geo_filter", Nil),
                  sorting = Sorting.Empty,
                  refineFactors = Nil,
                  rankingVectors = Nil,
                  userFactors = ExtFactors.defaultInstance,
                  pruneLimit = Some(500),
                  relevancePruning = RelevancePruning.defaultInstance,
                  softness = None,
                  debugMode = Debug(true, showFactors = false),
                  experiments = Set.empty,
                )
              } yield uri.toString()
            ).run,
          )(
            succeeds(
              containsString(
                "&pron=pruncount500;allow_pruning_with_docid_filter%3D1",
              ),
            ),
          )
        },
      ),
    )
  }

  private object uriComposer extends UriComposer(testSaasConfig)

}
