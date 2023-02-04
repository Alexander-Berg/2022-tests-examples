package ru.yandex.vertis.promocoder.dao

import ru.yandex.vertis.promocoder.dao.PromocodeDao.Filter
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.util.time.DateTimeUtil

/** Specs on [[PromocodeDao]]
  *
  * @author alex-kovalenko
  */
trait PromocodeDaoSpec extends DaoSpecBase with ModelGenerators {

  def dao: PromocodeDao

  val (promocode1 :: promocode2 :: Nil) =
    uniquePromocodesGen(2).next.map(_.copy(aliases = Set.empty))

  "PromocodeDao" should {
    "correctly upsert promocodes" when {
      "got unique promocode" in {
        dao.upsert(promocode1).futureValue
        dao.get(Filter.ByCodes(Set(promocode1.code))).futureValue.toList match {
          case `promocode1` :: Nil =>
          case other => fail(s"Unexpected $other\nexpected: $promocode1")
        }
      }

      "got promocode with existent code" in {
        dao.upsert(promocode2).futureValue
        val updated = PromocodeGen.next.copy(code = promocode2.code, aliases = Set.empty)
        dao.upsert(updated).futureValue
        dao.get(Filter.ByCodes(Set(updated.code))).futureValue.toList match {
          case `updated` :: Nil =>
          case other => fail(s"Unexpected $other")
        }
      }
    }

    "get promocode" when {
      "by one code" in {
        dao.get(Filter.ByCodes(Set(promocode1.code))).futureValue.toList match {
          case `promocode1` :: Nil =>
          case other => fail(s"Unexpected $other")
        }
      }

      "by few codes" in {
        val promocodes = uniquePromocodesGen(3).next.map(_.copy(aliases = Set.empty))
        dao.insert(promocodes).futureValue
        val filter = Filter.ByCodes(promocodes.map(_.code).toSet)
        dao.get(filter).futureValue should contain theSameElementsAs promocodes
      }

      "for owner" in {
        val owner1 = "o1"
        val owner2 = "02"
        dao.get(Filter.ByOwner(owner1)).futureValue shouldBe empty
        dao.get(Filter.ByOwner(owner2)).futureValue shouldBe empty
        val owner1Promos = uniquePromocodesGen(2).next.map(_.copy(owner = Some(owner1), aliases = Set.empty))
        val owner2Promos = uniquePromocodesGen(3).next.map(_.copy(owner = Some(owner2), aliases = Set.empty))
        dao.insert(owner1Promos ++ owner2Promos).futureValue
        dao.get(Filter.ByOwner(owner1)).futureValue should contain theSameElementsAs owner1Promos
        dao.get(Filter.ByOwner(owner2)).futureValue should contain theSameElementsAs owner2Promos
      }

      "by codes and owner" in {
        val owner1 = "owner1"
        val owner2 = "owner2"
        val promocodes1 = uniquePromocodesGen(3).next.map(_.copy(owner = Some(owner1), aliases = Set.empty))
        val promocodes2 = uniquePromocodesGen(3).next.map(_.copy(owner = Some(owner2), aliases = Set.empty))

        val filter1: Seq[Filter] = Seq(Filter.ByCodes(promocodes1.map(_.code).toSet), Filter.ByOwner(owner1))
        val filter2 = Seq(Filter.ByCodes(promocodes2.map(_.code).toSet), Filter.ByOwner(owner2))
        val wrongFilter = Seq(Filter.ByCodes(promocodes1.map(_.code).toSet), Filter.ByOwner(owner2))

        dao.insert(promocodes1 ++ promocodes2).futureValue

        dao.get(filter1).futureValue should contain theSameElementsAs promocodes1
        dao.get(filter2).futureValue should contain theSameElementsAs promocodes2
        dao.get(wrongFilter).futureValue shouldBe empty
      }
    }

    "update deadline if promocode with expected deadline and code present" in {
      val promocode = uniquePromocodesGen(1).next.head.copy(aliases = Set.empty)
      val now = DateTimeUtil.now()
      val promocodeUpdated = promocode.copy(constraints = promocode.constraints.copy(deadline = now))
      dao.upsert(promocode).futureValue
      dao
        .updateDeadline(promocode.code, newDeadline = now, currentDeadline = promocode.constraints.deadline)
        .futureValue
      dao.get(Filter.ByCodes(Set(promocode.code))).futureValue should contain theSameElementsAs Seq(promocodeUpdated)
    }

    "throw exception if promocode found by code but has not expected deadline" in {
      val now = DateTimeUtil.now()
      val promocode = uniquePromocodesGen(1).next.head.copy(aliases = Set.empty)
      dao.upsert(promocode).futureValue
      dao
        .updateDeadline(
          promocode.code,
          newDeadline = now,
          currentDeadline = promocode.constraints.deadline.minusDays(1)
        )
        .failed
        .futureValue shouldBe an[IllegalArgumentException]
    }

    "throw exception if no promocode by code found" in {
      val now = DateTimeUtil.now()
      val promocode = uniquePromocodesGen(1).next.head.copy(aliases = Set.empty)
      dao.upsert(promocode).futureValue
      dao
        .updateDeadline(
          promocode.code + "asd",
          newDeadline = now,
          currentDeadline = promocode.constraints.deadline
        )
        .failed
        .futureValue shouldBe an[IllegalArgumentException]
    }
  }
}
