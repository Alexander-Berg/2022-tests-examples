package vasgen.grpc

object RequestParamsSpec extends ZIOSpecDefault {

  val initEpoch = 8L

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
      stype = model.u32,
    ),
    FieldMapping(
      epoch = 8,
      name = FieldName("offer.category.all"),
      searchName = SaasName("s_offer_category_all_g").some,
      groupName = SaasName("s_offer_category_all_g").some,
      stype = model.str,
    ),
    FieldMapping(
      epoch = 8,
      name = FieldName("offer.category.main"),
      searchName = SaasName("s_offer_category_main_g").some,
      groupName = SaasName("s_offer_category_main_g").some,
      stype = model.str,
    ),
  )

  val inputMeta: QueryMetadata = general
    .search
    .vasgen
    .vasgen_model
    .QueryMetadata(
      paging = Some(Paging(0, 64)),
      pruning = Some(Pruning(890)),
      sorting = Sorting.SortBy(
        QueryMetadata.ByAttributeSort(
          "offer.price.price_in_currency",
          QueryMetadata.ByAttributeSort.Order.ASC,
        ),
      ),
      statsParams = Some(
        StatsParams(
          facet = Some(Facet("offer.category.main", 100)),
          range = Seq(ValueRange("offer.price.price_in_currency")),
        ),
      ),
      refine = Seq(
        Refine(
          "f_refine_category",
          0.9999984f,
          "offer.category.all",
          Some(
            RawValue(
              RawValue.ValueTypeOneof.String("stroitelstvo-i-remont_h7HBY6"),
            ),
          ),
        ),
        Refine(
          "f_refine_category",
          1.6350776e-6f,
          "offer.category.all",
          Some(
            RawValue(RawValue.ValueTypeOneof.String("listovoy-prokat_vYPldQ")),
          ),
        ),
      ),
    )

  private val tests = {
    suite("request param spec")(
      suite("string meta to typed meta transformation")(
        test("check facet: real mapping") {
          val typedMetaRequest = ExecutionRequest(query =
            Some(
              Query(typedMetadata =
                Some(
                  inputMeta
                    .withStatsParams(
                      StatsParams(
                        facet = Some(Facet("offer.region", 100)),
                        range = Seq(ValueRange("offer.price.price_in_currency")),
                      ),
                    )
                    .toAny,
                ),
              ),
            ),
          )
          for {
            facet <- typedMetaRequest.statsParam.map(_.facetParam)
          } yield assert(facet.attr.saasName)(equalTo("s_offer_region_g"))
        },
        test("check facet: reserved field") {
          val typedMetaRequest = ExecutionRequest(query =
            Some(
              Query(typedMetadata =
                Some(
                  inputMeta
                    .withStatsParams(
                      StatsParams(
                        facet = Some(Facet("s_attributes", 100)),
                        range = Seq(ValueRange("offer.price.price_in_currency")),
                      ),
                    )
                    .toAny,
                ),
              ),
            ),
          )
          assertZIO(
            typedMetaRequest.statsParam.map(_.facetParam.attr.saasName),
          )(equalTo("s_attributes"))
        },
        test("check only min-max") {
          val i = ExtendedStatParams(
            EmptyFacetParam,
            Seq(
              AttrNameMapping(
                "offer.price.price_in_currency",
                "i_offer_price_price_in_currency",
              ),
            ),
          )

          val typedMetaRequest = ExecutionRequest(query =
            Some(
              Query(typedMetadata =
                Some(
                  inputMeta
                    .withStatsParams(
                      StatsParams(
                        facet = None,
                        range = Seq(ValueRange("offer.price.price_in_currency")),
                      ),
                    )
                    .toAny,
                ),
              ),
            ),
          )
          for {
            typed <- typedMetaRequest.statsParam
          } yield assert(typed)(equalTo(i))
        },
        test("check sorting: random sort") {
          val typedMetaRequest = ExecutionRequest(query =
            Some(
              Query(typedMetadata =
                Some(inputMeta.withRandom(QueryMetadata.Random()).toAny),
              ),
            ),
          )
          for {
            typed <- typedMetaRequest.sort
          } yield assert(typed.isRandom)(isTrue)
        },
        test("check complex sorting: sort by not exist attribute") {
          val complexMetaRequest = ExecutionRequest(query =
            Some(
              Query(typedMetadata =
                Some(
                  inputMeta
                    .withSortBy(
                      QueryMetadata.ByAttributeSort(
                        "not.exist.attribute",
                        QueryMetadata.ByAttributeSort.Order.ASC,
                      ),
                    )
                    .toAny,
                ),
              ),
            ),
          )
          assertZIO(complexMetaRequest.sort.run)(
            fails(equalTo(UnsupportedAttributeName("not.exist.attribute"))),
          )
        },
        test("check complex sorting: read simple meta") {
          val complexMetaRequest = ExecutionRequest(query =
            Some(
              Query(typedMetadata =
                Some(inputMeta.withSorting(QueryMetadata.Sorting.Empty).toAny),
              ),
            ),
          )

          for {
            complex <- complexMetaRequest.sort
          } yield assert(complex.isEmpty)(isTrue)
        },
        test("check complex") {
          val complexMetaRequest = ExecutionRequest(query =
            Some(
              Query(typedMetadata =
                Some(
                  inputMeta
                    .withSorting(QueryMetadata.Sorting.Empty)
                    .withPruning(Pruning.defaultInstance)
                    .toAny,
                ),
              ),
            ),
          )

          for {
            sort    <- complexMetaRequest.sort
            pruning <- complexMetaRequest.pruneLimit
            stats   <- complexMetaRequest.statsParam
          } yield assert(sort.isRelevance)(isFalse) &&
            assert(pruning)(isNone) &&
            assert(stats.facetParam.attr.externalName)(
              equalTo("offer.category.main"),
            ) && assert(stats.facetParam.limit)(equalTo(100))
        },
        test("invalid refine factors") {
          val request = ExecutionRequest(query =
            Some(
              Query(typedMetadata =
                Some(
                  inputMeta
                    .addRefine(
                      Refine(
                        "f_refine_category",
                        0.9999984f,
                        "not.exist.attribute",
                        Some(
                          RawValue(
                            RawValue
                              .ValueTypeOneof
                              .String("stroitelstvo-i-remont_h7HBY6"),
                          ),
                        ),
                      ),
                      Refine(
                        "f_refine_attr_from_mapping",
                        0.9999984f,
                        "seller_id.user_id",
                        Some(
                          RawValue(
                            RawValue
                              .ValueTypeOneof
                              .Integer(
                                IntegerValue(
                                  IntegerValue.Primitive.Sint64(1234L),
                                ),
                              ),
                          ),
                        ),
                      ),
                      Refine(
                        "f_refine_reserved_field",
                        0.9999984f,
                        "pk.s",
                        Some(
                          RawValue(
                            RawValue
                              .ValueTypeOneof
                              .Integer(
                                IntegerValue(
                                  IntegerValue.Primitive.Sint64(1234L),
                                ),
                              ),
                          ),
                        ),
                      ),
                    )
                    .toAny,
                ),
              ),
            ),
          )

          for {
            invalid <- request.refineFactors
          } yield assert(invalid.size)(equalTo(4)) &&
            assert(invalid.map(_.resolvedQuery.query))(
              hasSameElements(
                Seq(
                  """s_offer_category_all_g:"stroitelstvo-i-remont_h7HBY6"""",
                  """s_offer_category_all_g:"listovoy-prokat_vYPldQ"""",
                  """s_seller_id_user_id:"1234"""",
                  """s_pk:"1234"""",
                ),
              ),
            )
        },
        test("without refine factors") {
          val request = ExecutionRequest(query =
            Some(Query(typedMetadata = Some(inputMeta.clearRefine.toAny))),
          )
          assertZIO(request.refineFactors)(isEmpty)
        },
        test("refine factors with filters") {
          val request = ExecutionRequest(query =
            Some(
              Query(typedMetadata =
                Some(
                  inputMeta
                    .addRefine(
                      Refine(
                        "f_refine_category",
                        0.9999984f,
                        "",
                        None,
                        Some(
                          Filter(op =
                            more("offer.photos.has_photos", Raw.i32(6)),
                          ),
                        ),
                      ),
                    )
                    .toAny,
                ),
              ),
            ),
          )

          for {
            res <- request.refineFactors
          } yield assert(res.size)(equalTo(3)) &&
            assert(res.map(_.resolvedQuery.query))(
              hasSameElements(
                Seq(
                  """i_offer_photos_has_photos:>"1"""",
                  """s_offer_category_all_g:"listovoy-prokat_vYPldQ"""",
                  """s_offer_category_all_g:"stroitelstvo-i-remont_h7HBY6"""",
                ),
              ),
            )
        },
      ),
      suite("pruneLimit")(
        test("random sort") {
          for {
            randomSort <-
              req(
                Pruning.defaultInstance.withPruneLimit(10),
                Sorting.Random(QueryMetadata.Random.defaultInstance),
              ).pruneLimit
          } yield assert(randomSort)(isNone)
        },
        test("sort by attr") {
          for {
            attrSort <-
              req(
                Pruning.defaultInstance.withPruneLimit(10),
                Sorting.SortBy(
                  QueryMetadata.ByAttributeSort(
                    "offer.price.price_in_currency",
                    QueryMetadata.ByAttributeSort.Order.ASC,
                  ),
                ),
              ).pruneLimit
          } yield assert(attrSort)(isNone)
        },
        test("empty request") {
          for {
            emptyReq <- ExecutionRequest.defaultInstance.pruneLimit
          } yield assert(emptyReq)(isNone)
        },
        test("pruning not set") {
          for {
            emptyReq <-
              req(
                Pruning.defaultInstance,
                Sorting.Polynom(QueryMetadata.UsingPolynom("polynom")),
              ).pruneLimit
          } yield assert(emptyReq)(isNone)
        },
        test("negative pruning") {
          for {
            negativePruning <-
              req(
                Pruning.defaultInstance.withPruneLimit(-500),
                Sorting.Polynom(QueryMetadata.UsingPolynom("polynom")),
              ).pruneLimit
          } yield assert(negativePruning)(isNone)
        },
        test("positive pruning") {
          for {
            positivePruning <-
              req(
                Pruning.defaultInstance.withPruneLimit(100),
                Sorting.Polynom(QueryMetadata.UsingPolynom("polynom")),
              ).pruneLimit
          } yield assert(positivePruning)(equalTo(Some(100)))
        },
      ),
      suite("vectors")(
        test("one") {
          val result =
            ExecutionRequest(query =
              Some(
                Query(rankingVectors =
                  Seq(
                    EmbeddedVector(
                      "q1",
                      1,
                      Some(FloatVector(Seq(1.7f, 3.6f, 7.8f, 6.1f))),
                    ),
                  ),
                ),
              ),
            ).rankingVectors
          assert(result)(hasSize(equalTo(0)))
        },
        test("several") {
          val result =
            ExecutionRequest(query =
              Some(
                Query(rankingVectors =
                  Seq(
                    EmbeddedVector(
                      "q1",
                      1,
                      Some(FloatVector(Seq(1.7f, 3.6f, 7.8f, 6.1f))),
                      "dssm_q1",
                    ),
                    EmbeddedVector(
                      "q2",
                      4,
                      Some(FloatVector(Seq(1.7f, 3.6f, 7.8f, 6.1f))),
                      "dssm_q2",
                    ),
                    EmbeddedVector(
                      "q3",
                      7,
                      Some(FloatVector(Seq(1.7f, 3.6f, 7.8f, 6.1f))),
                      "dssm_q3",
                    ),
                  ),
                ),
              ),
            ).rankingVectors
          assert(result.size)(equalTo(3)) &&
          assert(result.map(_.productFactor))(
            hasSameElements(Seq("dssm_q1", "dssm_q2", "dssm_q3")),
          )
        },
        test(
          "skip one vector without factor (instead of assign to default factor)",
        ) {
          val result =
            ExecutionRequest(query =
              Some(
                Query(rankingVectors =
                  Seq(
                    EmbeddedVector(
                      "q1",
                      1,
                      Some(FloatVector(Seq(1.7f, 3.6f, 7.8f, 6.1f))),
                      "dssm_q1",
                    ),
                    EmbeddedVector(
                      "q2",
                      4,
                      Some(FloatVector(Seq(1.7f, 3.6f, 7.8f, 6.1f))),
                      "dssm_q2",
                    ),
                    EmbeddedVector(
                      "q3",
                      7,
                      Some(FloatVector(Seq(1.7f, 3.6f, 7.8f, 6.1f))),
                      " ",
                    ),
                  ),
                ),
              ),
            ).rankingVectors
          assert(result.size)(equalTo(2)) &&
          assert(result.map(_.productFactor))(
            hasSameElements(Seq("dssm_q1", "dssm_q2")),
          )
        },
        test("skip many vectors without factors") {
          val result =
            ExecutionRequest(query =
              Some(
                Query(rankingVectors =
                  Seq(
                    EmbeddedVector(
                      "q1",
                      1,
                      Some(FloatVector(Seq(1.7f, 3.6f, 7.8f, 6.1f))),
                      "dssm_q1",
                    ),
                    EmbeddedVector(
                      "q2",
                      4,
                      Some(FloatVector(Seq(1.7f, 3.6f, 7.8f, 6.1f))),
                    ),
                    EmbeddedVector(
                      "q3",
                      7,
                      Some(FloatVector(Seq(1.7f, 3.6f, 7.8f, 6.1f))),
                    ),
                  ),
                ),
              ),
            ).rankingVectors
          assert(result.size)(equalTo(1)) &&
          assert(result.map(_.productFactor))(hasSameElements(Seq("dssm_q1")))
        },
      ),
      suite("relevancePruning")(
        test("empty request") {
          for {
            rp <- ExecutionRequest.defaultInstance.relevancePruning
          } yield assert(rp)(equalTo(RelevancePruning.defaultInstance))
        },
        test("prune limit only") {
          for {
            rp <-
              req(Pruning.defaultInstance.withPruneLimit(100), Sorting.Empty)
                .relevancePruning
          } yield assert(rp)(equalTo(RelevancePruning.defaultInstance))
        },
        test("empty formula name") {
          assertZIO(
            (
              for {
                rp <-
                  req(
                    Pruning
                      .defaultInstance
                      .withRelevancePruning(RelevancePruning("", 10f)),
                    Sorting.Empty,
                  ).relevancePruning
              } yield rp
            ).run,
          )(fails(anything))
        },
        test("threshold is zero") {
          assertZIO(
            (
              for {
                rp <-
                  req(
                    Pruning
                      .defaultInstance
                      .withRelevancePruning(
                        RelevancePruning("formula_name", 0f),
                      ),
                    Sorting.Empty,
                  ).relevancePruning
              } yield rp
            ).run,
          )(fails(anything))
        },
        test("threshold is negative") {
          assertZIO(
            (
              for {
                rp <-
                  req(
                    Pruning
                      .defaultInstance
                      .withRelevancePruning(
                        RelevancePruning("formula_name", -1),
                      ),
                    Sorting.Empty,
                  ).relevancePruning
              } yield rp
            ).run,
          )(fails(anything))
        },
        test("threshold is positive") {
          assertZIO(
            (
              for {
                rp <-
                  req(
                    Pruning
                      .defaultInstance
                      .withRelevancePruning(
                        RelevancePruning("formula_name", 10f),
                      ),
                    Sorting.Empty,
                  ).relevancePruning
              } yield rp
            ).run,
          )(succeeds(equalTo(RelevancePruning("formula_name", 10f))))
        },
      ),
    )
  }

  override def spec
    : Spec[TestEnvironment, TestFailure[VasgenStatus], TestSuccess] =
    tests.provideCustomLayerShared(
      fieldConverterLayer(initMapping) ++ epoch(initEpoch) ++ Tracing.noop >+>
        RequestConverter.live,
    )

  def req(pruning: Pruning, sorting: Sorting): ExecutionRequest = {
    ExecutionRequest
      .defaultInstance
      .withQuery(
        Query
          .defaultInstance
          .withTypedMetadata(
            QueryMetadata
              .defaultInstance
              .withPruning(pruning)
              .withSorting(sorting)
              .toAny,
          ),
      )
  }

}
