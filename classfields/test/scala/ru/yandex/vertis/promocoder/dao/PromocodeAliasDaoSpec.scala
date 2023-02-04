package ru.yandex.vertis.promocoder.dao

import ru.yandex.vertis.promocoder.dao.PromocodeAliasDao.{Filter, Record}
import ru.yandex.vertis.promocoder.dao.PromocodeAliasDaoSpec._

/** Specs on [[PromocodeAliasDao]]
  *
  * @author alex-kovalenko
  */
trait PromocodeAliasDaoSpec extends DaoSpecBase {

  def dao: PromocodeAliasDao

  "PromocodeAliasDao" should {
    "upsert" when {
      "get code only" in {
        dao.upsert(Record(code0, Set.empty)).futureValue
        dao.get(Filter.Direct(code0)).futureValue.toList match {
          case Record(`code0`, aliases) :: Nil if aliases.isEmpty =>
          case other => fail(s"Unexpected $other")
        }
      }
      "get code with one alias" in {
        dao.upsert(Record(code1, Set(alias1))).futureValue
        dao.get(Filter.Direct(code1)).futureValue.toList match {
          case Record(`code1`, aliases) :: Nil if aliases.size == 1 && aliases.head == alias1 =>
          case other => fail(s"Unexpected $other")
        }
      }
      "get same code with other aliases" in {
        dao.upsert(Record(code1, aliases1)).futureValue
        dao.get(Filter.Direct(code1)).futureValue.toList match {
          case Record(`code1`, `aliases1`) :: Nil =>
          case other => fail(s"Unexpected $other")
        }
      }
      "get code with two aliases" in {
        dao.upsert(Record(code2, aliases2)).futureValue
        dao.get(Filter.Direct(code2)).futureValue.toList match {
          case Record(`code2`, `aliases2`) :: Nil =>
          case other => fail(s"Unexpected $other")
        }
      }
      "get same code with the same aliases" in {
        dao.upsert(Record(code2, aliases2)).futureValue
        dao.get(Filter.Direct(code2)).futureValue.toList match {
          case Record(`code2`, `aliases2`) :: Nil =>
          case other => fail(s"Unexpected $other")
        }
      }
      "get code with aliases and then with code only" in {
        dao.upsert(Record(code3, aliases3)).futureValue
        dao.get(Filter.Direct(code3)).futureValue.toList match {
          case Record(`code3`, `aliases3`) :: Nil =>
          case other => fail(s"Unexpected $other")
        }
        dao.upsert(Record(code3, Set.empty)).futureValue
        dao.get(Filter.Direct(code3)).futureValue.toList match {
          case Record(`code3`, as) :: Nil if as.isEmpty =>
          case other => fail(s"Unexpected $other")
        }
      }
    }

    "fail to upsert" when {
      "get existent alias with other code" in {
        shouldFailWith[IllegalArgumentException] {
          dao.upsert(Record(code1, aliases2))
        }
      }
      "get existent alias as a code" in {
        shouldFailWith[IllegalArgumentException] {
          dao.upsert(Record(aliases1.head, Set.empty))
        }
      }
      "get existent code as an alias" in {
        shouldFailWith[IllegalArgumentException] {
          dao.upsert(Record(code1, Set(code2)))
        }
      }
    }

    "fail to insert" when {

      "at least one of codes or aliases exist" in {

        val records = List(
          Record(generatedCode1, generatedSet1),
          Record(generatedCode2, generatedSet2)
        )

        dao.insert(records).futureValue

        shouldFailWith[IllegalArgumentException] {
          dao.insert(List(Record(generatedCode1, Set.empty)))
        }

        shouldFailWith[IllegalArgumentException] {
          dao.insert(List(Record(generatedCode3, Set(generatedCode4, generatedCode2))))
        }
      }
    }

    "return code->aliases with by-code filter" in {
      dao.get(Filter.Direct(code1)).futureValue.toList match {
        case Record(`code1`, `aliases1`) :: Nil =>
        case other => fail(s"Unexpected $other")
      }
    }

    "return code->`alias` with by-`alias` filter" in {
      val alias = aliases2.head
      dao.get(Filter.Reverse(alias)).futureValue.toList match {
        case Record(`code2`, as) :: Nil if as.size == 1 && as.head == alias =>
        case other => fail(s"Unexpected $other")
      }
    }

    "return code->empty pair when got `code` in by-alias filter" in {
      dao.get(Filter.Reverse(code1)).futureValue.toList match {
        case Record(`code1`, as) :: Nil if as.isEmpty =>
        case other => fail(s"Unexpected $other")
      }
    }

    "return all code->aliases with All filter" in {
      val codeToAliases = dao
        .get(Filter.All)
        .futureValue
        .map { case Record(head, aliases) =>
          head -> aliases
        }
        .toMap
      codeToAliases should (((((((have size 6 and
        contain).key(code0) and
        contain).key(code1) and
        contain).key(code2) and
        contain).key(code3) and
        contain).key(generatedCode1) and
        contain).key(generatedCode2))

      codeToAliases(code0) shouldBe Set.empty
      codeToAliases(code1) shouldBe aliases1
      codeToAliases(code2) shouldBe aliases2
    }
  }
}

object PromocodeAliasDaoSpec {

  val code0 = "code0"

  val (code1, alias1, aliases1) = ("code1", "alias1.1", Set("alias1.2", "alias1.3"))

  val (code2, aliases2) = ("code2", Set("alias2.1", "alias2.2"))

  val (code3, aliases3) = ("code3", Set("alias3.1"))

  val (generatedCode1, generatedSet1) = ("generatedCode1", Set("gen_alias1", "gen_alias1.1"))
  val (generatedCode2, generatedSet2) = ("generatedCode2", Set("gen_alias2"))
  val (generatedCode3, generatedCode4) = ("generatedCode3", "generatedCode4")
}
