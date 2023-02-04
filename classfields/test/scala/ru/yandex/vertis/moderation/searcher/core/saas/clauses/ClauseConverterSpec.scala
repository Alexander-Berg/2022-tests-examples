package ru.yandex.vertis.moderation.searcher.core.saas.clauses

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.searcher.core.saas.clauses.ops._
import ru.yandex.vertis.moderation.searcher.core.saas.document.{AutoruFields, CommonFields}

@RunWith(classOf[JUnitRunner])
class ClauseConverterSpec extends SpecBase {

  private val cars = AutoruFields.Category === "CARS"
  private val notBus = AutoruFields.Category =/= "BUS"
  private val doors = AutoruFields.DoorsCount === 3
  private val notProvenOwner = AutoruFields.ProvenOwnerVerdict =/= "PROVEN_OWNER_OK"
  private val exactOwners: Clause = AutoruFields.PtsOwnersExactCount <= 3
  private val provenOwnerNotOk1 = AutoruFields.ProvenOwnerVerdict === "PROVEN_OWNER_BAD_PHOTOS"
  private val provenOwnerNotOk2 = AutoruFields.ProvenOwnerVerdict === "PROVEN_OWNER_FAILED"
  private val createdAfter = CommonFields.CreateDate > 1234567

  private case class TestCase(description: String, clause: Clause, saasQuery: String)

  private val testCases =
    Seq(
      TestCase(
        "converts single equality clause with string attribute",
        cars,
        "s_category:(CARS)"
      ),
      TestCase(
        "converts single non-equality clause with string attribute",
        notBus,
        "(i_create_date:>=(0) ~~ s_category:(BUS))"
      ),
      TestCase(
        "converts simple equality clause with numeric attribute",
        doors,
        "i_doors_count:(3)"
      ),
      TestCase(
        "converts simple comparison clause with numeric attribute",
        exactOwners,
        "i_pts_owners_exact_count:<=(3)"
      ),
      TestCase(
        "converts OR clause",
        provenOwnerNotOk1 || cars,
        "(s_proven_owner_verdict:(PROVEN_OWNER_BAD_PHOTOS) | s_category:(CARS))"
      ),
      TestCase(
        "converts OR clause with non-equality clause",
        provenOwnerNotOk1 || notProvenOwner,
        "(s_proven_owner_verdict:(PROVEN_OWNER_BAD_PHOTOS) | (i_create_date:>=(0) ~~ s_proven_owner_verdict:(PROVEN_OWNER_OK)))"
      ),
      TestCase(
        "converts AND clause",
        provenOwnerNotOk1 && cars,
        "(s_proven_owner_verdict:(PROVEN_OWNER_BAD_PHOTOS) && s_category:(CARS))"
      ),
      TestCase(
        "converts AND clause with non-equality clause",
        createdAfter && notProvenOwner,
        "(i_create_date:>(1234567) ~~ s_proven_owner_verdict:(PROVEN_OWNER_OK))"
      ),
      TestCase(
        "converts complex AND clause with inverted clauses and nested OR clause",
        AndClause(Iterable(cars, exactOwners, notProvenOwner, provenOwnerNotOk1 || provenOwnerNotOk2)),
        "((s_category:(CARS) && i_pts_owners_exact_count:<=(3) && (s_proven_owner_verdict:(PROVEN_OWNER_BAD_PHOTOS) |" +
          " s_proven_owner_verdict:(PROVEN_OWNER_FAILED))) ~~ s_proven_owner_verdict:(PROVEN_OWNER_OK))"
      )
    )

  "ClauseConverter.toSaasQuery" should {
    testCases.foreach { case TestCase(description, clause, saasQuery) =>
      description in {
        ClauseConverter.toSaasQuery(clause) shouldBe saasQuery
      }
    }
  }
}
