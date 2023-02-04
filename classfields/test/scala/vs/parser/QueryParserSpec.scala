package vs.parser

import vsql.query
import vsql.query.*
import vsql.query.Condition.Is.FilterType.Null
import vsql.query.Condition.{Cond, Equals, In, Is, Less, More, NotEquals}
import vsql.query.Function.Func.Aggr
import vsql.query.Rank.Field
import vsql.query.Type.TInt
import vsql.query.Value.Val
import zio.test.*
import zio.test.Assertion.*
import zio.test.ZIOSpecDefault

object QueryParserSpec extends ZIOSpecDefault {

  private def wrapEqCondition(
    field: String,
    value: vsql.query.Value,
  ): Condition = {
    Condition(cond = Cond.Filter(Equals(field, Some(value))))
  }

  private def wrapNotEqCondition(
    field: String,
    value: vsql.query.Value,
  ): Condition = {
    Condition(cond = Cond.Filter(NotEquals(field, Some(value))))
  }

  private def wrapGtCondition(
    field: String,
    value: vsql.query.Value,
    orEq: Boolean,
  ): Condition = {
    Condition(cond = Cond.Filter(More(field, Some(value), orEq)))
  }

  private def wrapLtCondition(
    field: String,
    value: vsql.query.Value,
    orEq: Boolean,
  ): Condition = {
    Condition(cond = Cond.Filter(Less(field, Some(value), orEq)))
  }

  private def wrapInCondition(
    field: String,
    values: Seq[vsql.query.Value],
  ): Condition = {
    Condition(cond = Cond.Filter(In(field, values)))
  }

  private def wrapFunction(funcName: String, fieldName: String): Function = {
    val f =
      funcName match {
        case "count" =>
          Count(fieldName)
        case "min" =>
          Min(fieldName)
        case "max" =>
          Max(fieldName)
        case "sum" =>
          Sum(fieldName)
        case _ =>
          AggregateFunction.Empty
      }
    vsql.query.Function.of(f.getClass.getSimpleName.toUpperCase, Aggr(f))
  }

  private def wrapAndCondition(seq: Seq[Condition]): Condition = {
    Condition(cond = vsql.query.Condition.Cond.And(Condition.And(seq)))
  }

  private def wrapOrCondition(seq: Seq[Condition]): Condition = {
    Condition(cond = vsql.query.Condition.Cond.Or(Condition.Or(seq)))
  }

  private def wrapIsNullCondition(field: String): Condition = {
    Condition(cond = Cond.Filter(Is(field, Null(Condition.Is.Null()))))
  }

  override def spec = {

    suite("QueryParserSpec")(
      suite("full script")(
        test("query 1") {
          val q =
            """DECLARE ext_int AS Int;
              |$mers = "Mercedes Бенц \"Зил\"";
              |$coef = 0.89;
              |$now = '2021-11-13 14:01:15';
              |$relevance_poly = 0.7 * publish_date +  1.5 * price + coef * MAX(price);
              |$vector = (1.0,8.9,7.5,9.1,6.0);
              |
              |SELECT seller, COUNT(in_stock), relevance
              |FROM auto
              |WHERE in_stock = true AND mark = $mers
              |ORDER BY $relevance_poly
              |GROUP BY seller""".stripMargin
          assert(QueryParser.parse(q))(
            isRight(
              equalTo(
                Query(
                  outputs = Seq(
                    SimpleField("seller", "seller"),
                    FunctionField(
                      Some(wrapFunction("count", "in_stock")),
                      "COUNT",
                    ),
                    SimpleField("relevance", "relevance"),
                  ),
                  source = "auto",
                  conditions = Some(
                    wrapAndCondition(
                      Seq(
                        wrapEqCondition(
                          "in_stock",
                          vsql.query.Value(Val.Bool(true)),
                        ),
                        wrapEqCondition(
                          "mark",
                          vsql.query.Value(Val.Pointer(Value.Pointer("mers"))),
                        ),
                      ),
                    ),
                  ),
                  rank = Some(
                    Rank(Rank.By.Pointer(Value.Pointer("relevance_poly"))),
                  ),
                  limit = None,
                  groupBy = Some(Grouping(Seq("seller"))),
                  predefinedPart = Seq(
                    ExternalVariable("ext_int", TInt),
                    Formula("mers", """Mercedes Бенц "Зил""""),
                    Formula("coef", "0.89"),
                    Formula("now", "2021-11-13T14:01:15Z"),
                    Formula(
                      "relevance_poly",
                      "0.7*publish_date+1.5*price+coef*MAX(price)",
                    ),
                    Formula("vector", "(1.0,8.9,7.5,9.1,6.0)"),
                  ),
                ),
              ),
            ),
          )
        },
        test("query 2") {
          val q =
            """$relevance_poly = 0.7 * publish_date +  1.5 * price + coef * MAX(price);
              |$by_price = 0.5 * price;
              |
              |SELECT MIN(price) AS minimum, TOP_N(5, snippet, id) as docs
              |FROM auto
              |WHERE year >= 2019 AND mark = "Mercedes"
              |ORDER BY $relevance_poly
              |GROUP BY seller""".stripMargin
          assert(QueryParser.parse(q))(
            isRight(
              equalTo(
                query.Query(
                  outputs = Seq(
                    FunctionField(
                      Some(wrapFunction("min", "price")),
                      "minimum",
                    ),
                    FunctionField(
                      Some(
                        vsql
                          .query
                          .Function
                          .of("TOPN", Aggr(TopN(5, Seq("snippet", "id")))),
                      ),
                      "docs",
                    ),
                  ),
                  source = "auto",
                  conditions = Some(
                    wrapAndCondition(
                      Seq(
                        wrapGtCondition(
                          "year",
                          Value(Val.Long(2019L)),
                          orEq = true,
                        ),
                        wrapEqCondition("mark", Value(Val.String("Mercedes"))),
                      ),
                    ),
                  ),
                  rank = Some(
                    Rank(Rank.By.Pointer(Value.Pointer("relevance_poly"))),
                  ),
                  limit = None,
                  groupBy = Some(Grouping(Seq("seller"))),
                  predefinedPart = Seq(
                    Formula(
                      "relevance_poly",
                      "0.7*publish_date+1.5*price+coef*MAX(price)",
                    ),
                    Formula("by_price", "0.5*price"),
                  ),
                ),
              ),
            ),
          )
        },
      ),
      suite("Filter")(
        test("simple") {
          val q = """SELECT id, docs FROM auto WHERE id=12345"""
          assert(QueryParser.parse(q).map(_.conditions))(
            isRight(
              isSome(equalTo(wrapEqCondition("id", Value(Val.Long(12345))))),
            ),
          )
        },
        test("using string declaration") {
          val q =
            """$mers = "Mercedes";
              |SELECT seller
              |FROM auto
              |WHERE mark = $mers""".stripMargin
          assert(QueryParser.parse(q))(
            isRight(
              equalTo(
                Query(
                  outputs = Seq(SimpleField("seller", "seller")),
                  source = "auto",
                  conditions = Some(
                    wrapEqCondition(
                      "mark",
                      Value(Val.Pointer(Value.Pointer("mers"))),
                    ),
                  ),
                  rank = None,
                  limit = None,
                  groupBy = None,
                  predefinedPart = Seq(Formula("mers", """Mercedes""")),
                ),
              ),
            ),
          )
        },
        test("fail: skip FROM") {
          val q = """SELECT id, docs WHERE id=12345"""
          assert(QueryParser.parse(q))(isLeft(containsString("Cannot parse")))
        },
        test("fail: incomplete condition") {
          val q = """SELECT id FROM auto WHERE id=9009 AND """
          assert(QueryParser.parse(q))(isLeft(containsString("Cannot parse")))
        },
        test("only and") {
          val q =
            """SELECT id, docs FROM auto WHERE id=12345 AND offer_id = "00CF4FC964FF"  AND user = 45 """
          assert(QueryParser.parse(q).map(_.conditions))(
            isRight(
              isSome(
                equalTo(
                  wrapAndCondition(
                    Seq(
                      wrapEqCondition("id", Value(Val.Long(12345))),
                      wrapEqCondition(
                        "offer_id",
                        Value(Val.String("00CF4FC964FF")),
                      ),
                      wrapEqCondition("user", Value(Val.Long(45))),
                    ),
                  ),
                ),
              ),
            ),
          )
        },
        test("only or") {
          val q =
            """SELECT id, docs FROM auto WHERE id=12345 OR offer_id = "00CF4FC964FF" OR user = 45 """
          assert(QueryParser.parse(q).map(_.conditions))(
            isRight(
              isSome(
                equalTo(
                  wrapOrCondition(
                    Seq(
                      wrapEqCondition("id", Value(Val.Long(12345))),
                      wrapEqCondition(
                        "offer_id",
                        Value(Val.String("00CF4FC964FF")),
                      ),
                      wrapEqCondition("user", Value(Val.Long(45))),
                    ),
                  ),
                ),
              ),
            ),
          )
        },
        test("mix or, and") {
          val q =
            """SELECT id, docs FROM auto WHERE (id=12345 OR y= 23 OR (id=23 AND offer_id=78787))"""
          assert(QueryParser.parse(q).map(_.conditions))(
            isRight(
              isSome(
                equalTo(
                  wrapOrCondition(
                    Seq(
                      wrapEqCondition("id", Value(Val.Long(12345))),
                      wrapEqCondition("y", Value(Val.Long(23))),
                      wrapAndCondition(
                        Seq(
                          wrapEqCondition("id", Value(Val.Long(23))),
                          wrapEqCondition("offer_id", Value(Val.Long(78787))),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          )
        },
        test("join ANDs by OR") {
          val q =
            """SELECT id, docs FROM auto WHERE (id=12345 AND y= 23) OR (id=23 AND offer_id=78787)"""
          assert(QueryParser.parse(q).map(_.conditions))(
            isRight(
              isSome(
                equalTo(
                  wrapOrCondition(
                    Seq(
                      wrapAndCondition(
                        Seq(
                          wrapEqCondition("id", Value(Val.Long(12345))),
                          wrapEqCondition("y", Value(Val.Long(23))),
                        ),
                      ),
                      wrapAndCondition(
                        Seq(
                          wrapEqCondition("id", Value(Val.Long(23))),
                          wrapEqCondition("offer_id", Value(Val.Long(78787))),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          )
        },
        test("AND vs OR") {
          val q =
            """SELECT id, docs FROM auto WHERE user = 45 OR (id=12345 AND offer_id = "00CF4FC964FF")"""
          assert(QueryParser.parse(q).map(_.conditions))(
            isRight(
              isSome(
                equalTo(
                  wrapOrCondition(
                    Seq(
                      wrapEqCondition("user", Value(Val.Long(45))),
                      wrapAndCondition(
                        Seq(
                          wrapEqCondition("id", Value(Val.Long(12345))),
                          wrapEqCondition(
                            "offer_id",
                            Value(Val.String("00CF4FC964FF")),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          )
        },
        test("AND vs OR other order") {
          val q =
            """SELECT id, docs FROM auto WHERE  (id=12345 AND offer_id = "00CF4FC964FF") OR (user = 45)"""
          assert(QueryParser.parse(q).map(_.conditions))(
            isRight(
              isSome(
                equalTo(
                  wrapOrCondition(
                    Seq(
                      wrapAndCondition(
                        Seq(
                          wrapEqCondition("id", Value(Val.Long(12345))),
                          wrapEqCondition(
                            "offer_id",
                            Value(Val.String("00CF4FC964FF")),
                          ),
                        ),
                      ),
                      wrapEqCondition("user", Value(Val.Long(45))),
                    ),
                  ),
                ),
              ),
            ),
          )
        },
        test("less, more and other") {
          val q =
            """SELECT id, docs FROM auto WHERE  (id >= 12345 AND offer_id != "00CF4FC964FF") OR (user < 45) OR (type in (12, 14))"""
          assert(QueryParser.parse(q).map(_.conditions))(
            isRight(
              isSome(
                equalTo(
                  wrapOrCondition(
                    Seq(
                      wrapAndCondition(
                        Seq(
                          wrapGtCondition(
                            "id",
                            Value(Val.Long(12345)),
                            orEq = true,
                          ),
                          wrapNotEqCondition(
                            "offer_id",
                            Value(Val.String("00CF4FC964FF")),
                          ),
                        ),
                      ),
                      wrapLtCondition(
                        "user",
                        Value(Val.Long(45)),
                        orEq = false,
                      ),
                      wrapInCondition(
                        "type",
                        Seq(Value(Val.Long(12)), Value(Val.Long(14))),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          )
        },
        test("IS NULL") {
          val q = """SELECT id, docs FROM auto WHERE user IS NULL """
          assert(QueryParser.parse(q).map(_.conditions))(
            isRight(isSome(equalTo(wrapIsNullCondition("user")))),
          )
        },
        test("IS NULL with 0 whitespaces") {
          val q = """SELECT id, docs FROM auto WHERE user ISNULL """
          assert(QueryParser.parse(q))(isLeft(containsString("Cannot parse")))
        },
        test("IS     NULL with multiple whitespaces") {
          val q = """SELECT id, docs FROM auto WHERE user IS        NULL"""
          assert(QueryParser.parse(q).map(_.conditions))(
            isRight(isSome(equalTo(wrapIsNullCondition("user")))),
          )
        },
      ),
      suite("Order by")(
        test("by name") {
          val q =
            """SELECT id, docs FROM auto WHERE id=12345 order by price DESC"""
          assert(QueryParser.parse(q).map(_.rank))(
            isRight(
              isSome(
                equalTo(
                  Rank(Rank.By.Field(Field(fieldName = "price", asc = false))),
                ),
              ),
            ),
          )
        },
        test("by declared formula") {
          val q =
            """DECLARE ext_int AS Int;
              |$relevance_poly = 0.7 * publish_date +  1.5 * price + coef * MAX(price);
              |SELECT seller
              |FROM auto
              |ORDER BY $relevance_poly""".stripMargin
          assert(QueryParser.parse(q).map(_.rank))(
            isRight(
              isSome(
                equalTo(Rank(Rank.By.Pointer(Value.Pointer("relevance_poly")))),
              ),
            ),
          )
        },
        test("error: attempt to sort by string") {
          val q =
            """SELECT id, docs FROM auto WHERE id=12345 order by "price" DESC"""
          assert(QueryParser.parse(q))(isLeft(containsString("Cannot parse")))
        },
      ),
      suite("Limit")(
        test("simple") {
          val q =
            """SELECT id, docs FROM auto WHERE id=12345 limit 35 offset 20"""
          assert(QueryParser.parse(q).map(_.limit))(
            isRight(isSome(equalTo(Limit(35L, 20L)))),
          )
        },
        test("wrong offset") {
          val q =
            """SELECT id, docs FROM vasgen_search_lb WHERE id=12345 limit 35 offset 20a"""
          assert(QueryParser.parse(q))(isLeft(containsString("Cannot parse")))
        },
      ),
    )
  }

}
