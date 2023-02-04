package ru.yandex.vertis.subscriptions.core.matcher.qbd

import org.scalacheck.Gen
import ru.yandex.vertis.subscriptions.core.matcher.qbd.DSL._
import ru.yandex.vertis.subscriptions.core.matcher.qbd.Query.Builder._
import scala.util.Random

/** Generators to ScalaCheck
  */
object Generators {

  val genName: Gen[String] = Gen.oneOf(
    "all_region_codes",
    "state",
    "cluster_head",
    "mark_code",
    "model_code",
    "vendor",
    "price_rur",
    "year",
    "transmission",
    "run",
    "displacement",
    "body_type_not_empty",
    "cluster_id",
    "color",
    "body_type",
    "engine_type",
    "steering_wheel",
    "in_stock",
    "body_size",
    "acceleration",
    "seats",
    "consumption_mixed",
    "gear_type",
    "clearance",
    "power",
    "generation_id",
    "configuration_id"
  )

  def genPoint(names: Gen[String]): Gen[Term] =
    for {
      name <- names
      value <- Gen.alphaStr.filter(_ != PointTerm.AnyValue)
    } yield point(name, value)

  def genPoint(names: Iterable[String]): Gen[Term] = genPoint(Gen.oneOf(names.toSeq))

  val genPoint: Gen[Term] = genPoint(genName)

  def genIntRange(names: Gen[String]): Gen[Term] =
    for {
      name <- names
      from <- Gen.choose(1970, 2014)
      to <- Gen.choose(from, 2014)
    } yield range(name, from, to)

  def genIntRange(names: Iterable[String]): Gen[Term] = genIntRange(Gen.oneOf(names.toSeq))

  val genIntRange: Gen[Term] = genIntRange(genName)

  def genTerm(names: Gen[String]): Gen[Term] = Gen.oneOf(genPoint(names), genIntRange(names))

  val genTerm: Gen[Term] = Gen.oneOf(genPoint, genIntRange)

  def genTerms(names: Gen[String]): Gen[Iterable[Term]] =
    Gen.listOf(genTerm(names)).suchThat(_.nonEmpty)

  val genTerms: Gen[Iterable[Term]] = genTerms(genName)

  def genTermQuery(terms: Iterable[Term]): Gen[Query] =
    for {
      t <- Gen.oneOf(terms.toSeq)
    } yield term(t)

  def genAndQuery(terms: Iterable[Term], level: Int): Gen[Query] =
    for {
      size <- Gen.oneOf(2, 3, 4, 5)
      queries <- Gen.listOfN(size, genQuery(terms, level - 1))
    } yield AndQuery(queries)

  def genOrQuery(terms: Iterable[Term], level: Int): Gen[Query] =
    for {
      size <- Gen.oneOf(2, 3, 4, 5)
      queries <- Gen.listOfN(size, genQuery(terms, level - 1))
    } yield OrQuery(queries)

  def genNotQuery(queryGen: Gen[Query]): Gen[Query] =
    for {
      query <- queryGen
    } yield NotQuery(query)

  def genQuery(terms: Iterable[Term], level: Int): Gen[Query] = Gen.lzy(
    if (level <= 0) genTermQuery(terms)
    else
      Gen.oneOf(
        genTermQuery(terms),
        genAndQuery(terms, level),
        genOrQuery(terms, level),
        genNotQuery(genTermQuery(terms)),
        genNotQuery(genAndQuery(terms, level - 1)),
        genNotQuery(genOrQuery(terms, level - 1))
      )
  )

  val genConjunction: Gen[Set[QueryNormalizer.Var]] = for {
    terms <- Gen.nonEmptyListOf(genTerm)
    termsSet = terms.toSet
    conjunction = termsSet.map(t => QueryNormalizer.Var(t, Random.nextBoolean()))
  } yield conjunction

  val genDNF: Gen[QueryNormalizer.DNF] = for {
    conjuncts <- Gen.nonEmptyListOf(genConjunction)
  } yield conjuncts.toSet

}
