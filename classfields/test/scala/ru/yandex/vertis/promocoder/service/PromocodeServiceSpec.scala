package ru.yandex.vertis.promocoder.service

import org.scalatest.GivenWhenThen
import ru.yandex.vertis.promocoder.FeatureSpecBase
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.model.{Promocode, PromocodeId, PromocodeParameters, PromocodeSource}
import ru.yandex.vertis.promocoder.service.PromocodeService.Filter.{ByAlias, ByCode}
import ru.yandex.vertis.promocoder.service.PromocodeService.Options
import ru.yandex.vertis.promocoder.service.PromocodeServiceSpec.{
  alias1,
  aliases1,
  aliases2,
  code0,
  code1,
  code2,
  promocode0,
  promocode1,
  promocode2,
  withAliases
}
import ru.yandex.vertis.promocoder.util.{AutomatedContext, RequestContext}

/** Specs on [[PromocodeService]]
  *
  * @author alex-kovalenko
  */
trait PromocodeServiceSpec extends FeatureSpecBase with GivenWhenThen with ModelGenerators {

  def getService(promocodes: Iterable[Promocode]): PromocodeService

  implicit val rc: RequestContext = AutomatedContext("PromocodeServiceSpec")

  Feature("AsyncPromocodeService") {
    Scenario("create promocodes") {
      Given("empty service")
      val service = getService(Iterable.empty)

      When("create promocode with no aliases")
      service.create(promocode0).futureValue

      Then("get it by code")
      service.get(ByCode(code0), Options(false)).futureValue shouldBe promocode0

      When("create promocode with aliases")
      service.create(withAliases(promocode1, Set(alias1))).futureValue
      service.create(withAliases(promocode2, aliases2)).futureValue

      Then("get them by alias")
      service.get(ByAlias(alias1), Options(false)).futureValue shouldBe promocode1

      And("by code")
      service.get(ByCode(code2), Options(false)).futureValue shouldBe promocode2
    }

    Scenario("get promocodes with unknown identifiers") {
      Given("empty service")
      val service = getService(Iterable.empty)

      When("get promocode by unknown identifier")
      Then("throw NoSuchElementException")
      shouldFailWith[NoSuchElementException] {
        service.get(ByCode(""), Options(false))
      }
      shouldFailWith[NoSuchElementException] {
        service.get(ByAlias(""), Options(false))
      }
    }

    Scenario("get promocodes") {
      Given("service with data")
      val service = getService(
        Iterable(withAliases(promocode0), withAliases(promocode1, Set(alias1)), withAliases(promocode2, aliases2))
      )

      When("requested by code")
      Then("return promocode")
      service.get(ByCode(code0), Options(false)).futureValue shouldBe promocode0
      service.get(ByCode(code1), Options(false)).futureValue shouldBe promocode1
      service.get(ByCode(code2), Options(false)).futureValue shouldBe promocode2

      When("requested by alias")
      Then("return promocode")
      service.get(ByAlias(alias1), Options(false)).futureValue shouldBe promocode1
      service.get(ByAlias(aliases2.head), Options(false)).futureValue shouldBe promocode2
      service.get(ByAlias(aliases2.drop(1).head), Options(false)).futureValue shouldBe promocode2
    }

    Scenario("update promocodes") {
      Given("service with promocode with one alias")
      val service = getService(Iterable(withAliases(promocode1, Set(alias1))))

      When("asked to create the same promocode")
      service.create(withAliases(promocode1, aliases1)).futureValue

      Then("replace aliases")
      service.get(ByAlias(aliases1.head), Options(false)).futureValue shouldBe promocode1

      When("requested promocode by previous alias")
      Then("should fail")
      shouldFailWith[NoSuchElementException] {
        service.get(ByCode(alias1), Options(false))
      }
    }

    Scenario("enriches promocodes with aliases") {
      Given("service with data")
      val service = getService(
        Iterable(withAliases(promocode0), withAliases(promocode1, Set(alias1)), withAliases(promocode2, aliases2))
      )

      When("requested")
      Then("return promocode with correct aliases")
      service.get(ByCode(code0), Options(true)).futureValue shouldBe withAliases(promocode0, Set.empty)
      service.get(ByCode(code1), Options(true)).futureValue shouldBe withAliases(promocode1, Set(alias1))
      service.get(ByCode(code2), Options(true)).futureValue shouldBe withAliases(promocode2, aliases2)

      When("requested by alias")
      Then("return promocode with correct aliases")
      service.get(ByAlias(alias1), Options(true)).futureValue shouldBe withAliases(promocode1, Set(alias1))
      service.get(ByAlias(aliases2.head), Options(true)).futureValue shouldBe withAliases(promocode2, aliases2)
    }

    Scenario("generate promocodes") {
      Given("service with data")
      val service = getService(
        Iterable(
          withAliases(promocode0),
          withAliases(promocode1, Set(alias1))
        )
      )

      When("asked to generate with existent code")
      val source1 =
        PromocodeSource(FeatureGen.next(1), ConstraintsGen.next, PromocodeParameters(None, None, Set(code0)))
      Then("fail")
      shouldFailWith[IllegalArgumentException] {
        service.generate(source1)
      }

      When("asked to generate with existent alias")
      val source2 = source1.copy(parameters = source1.parameters.copy(codes = Set(alias1)))
      Then("fail")
      shouldFailWith[IllegalArgumentException] {
        service.generate(source2)
      }

      When("asked to generate with new codes")
      val source3 = source1.copy(parameters = source1.parameters.copy(codes = aliases2))
      Then("store generated promocode")

      val generatedBatch = service.generate(source3).futureValue
      generatedBatch.otherCodes should have size 1
      (generatedBatch.otherCodes + generatedBatch.promocode.code) should contain theSameElementsAs aliases2
      generatedBatch.promocode.features should contain theSameElementsAs source3.features
      generatedBatch.promocode.constraints shouldBe source3.constraints

      val first = service.get(ByCode(generatedBatch.promocode.code), Options(withAliases = true)).futureValue
      val second = service.get(ByCode(generatedBatch.otherCodes.head), Options(withAliases = true)).futureValue
      generatedBatch.promocode should (be(first).or(be(second)))
      first.features should contain theSameElementsAs second.features
      first.constraints shouldBe second.constraints
    }
  }
}

object PromocodeServiceSpec extends ModelGenerators {

  private val code0 = "code0"

  private val (code1, alias1, aliases1) = ("code1", "alias1.1", Set("alias1.2", "alias1.3"))

  private val (code2, aliases2) = ("code2", Set("alias2.1", "alias2.2"))

  private val promocode0 = PromocodeGen.next.copy(code = code0, aliases = Set.empty)

  private val promocode1 = PromocodeGen.next.copy(code = code1, aliases = Set.empty)

  private val promocode2 = PromocodeGen.next.copy(code = code2, aliases = Set.empty)

  private def withAliases(p: Promocode, as: Set[PromocodeId] = Set.empty) =
    p.copy(aliases = as)
}
