package ru.yandex.vertis.promocoder.service

import org.junit.runner.RunWith

import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.promocoder.WordSpecBase
import ru.yandex.vertis.promocoder.dao.impl.jvm.{JvmPromocodeAliasDao, JvmPromocodeDao}
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.model.{Promocode, PromocodeParameters, PromocodeSource}
import ru.yandex.vertis.promocoder.service.PromocodeService.Filter.{ByAlias, ByCode}
import ru.yandex.vertis.promocoder.service.PromocodeService.Options
import ru.yandex.vertis.promocoder.service.impl.PromocodeServiceImpl
import ru.yandex.vertis.promocoder.util.{AutomatedContext, CharsGenerator, DefaultPromocodeGenerator, RequestContext}

/** Runnable specs on [[LowerCaseAliasesPromocodeService]]
  *
  * @author alex-kovalenko
  */
class LowerCaseAliasesPromocodeServiceSpec extends WordSpecBase with MockitoSupport with ModelGenerators {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val rc: RequestContext =
    AutomatedContext("LowerCaseAliasesPromocodeServiceSpec")

  val service: PromocodeService =
    new PromocodeServiceImpl(
      new JvmPromocodeDao,
      new JvmPromocodeAliasDao,
      new DefaultPromocodeGenerator(CharsGenerator.LowerCase)
    ) with LowerCaseAliasesPromocodeService

  "LowerCaseAliasesPromocodeService" should {
    "make incoming code/aliases to lower-case" in {
      def toLowerCase(p: Promocode): Promocode =
        p.copy(code = p.code.toLowerCase, aliases = p.aliases.map(_.toLowerCase))

      val promocode = PromocodeGen.next.copy(code = "CODE", aliases = Set("a_lower", "a_UPPER"))

      val withAliases = Options(true)

      service.create(promocode).futureValue

      service.get(ByCode("CODE"), withAliases).futureValue shouldBe toLowerCase(promocode)
      service.get(ByCode("code"), withAliases).futureValue shouldBe toLowerCase(promocode)
      service.get(ByAlias("a_lower"), withAliases).futureValue shouldBe toLowerCase(promocode)
      service.get(ByAlias("A_Lower"), withAliases).futureValue shouldBe toLowerCase(promocode)
      service.get(ByAlias("a_UPPER"), withAliases).futureValue shouldBe toLowerCase(promocode)
      service.get(ByAlias("A_upper"), withAliases).futureValue shouldBe toLowerCase(promocode)

      val promocode2 = promocode.copy(code = "code")

      service.create(promocode2).futureValue

      service.get(ByCode("CODE"), withAliases).futureValue shouldBe toLowerCase(promocode2)
    }

    "generate with lowercase prefix and codes" in {
      val source = PromocodeSource(
        FeatureGen.next(2),
        ConstraintsGen.next,
        PromocodeParameters(count = Some(1), prefix = Some("UPPER"), Set("ProVidEd"))
      )
      val generated = service.generate(source).futureValue
      val codes = generated.otherCodes + generated.promocode.code
      codes.size shouldBe 2
      codes.count(_.startsWith("upper")) shouldBe 1
      codes should contain("provided")
    }
  }
}
