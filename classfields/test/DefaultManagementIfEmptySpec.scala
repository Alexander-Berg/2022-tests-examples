package ru.yandex.vertis.shark.client.bank.converter.impl.rosgosstrah

import baker.common.client.dadata.model.{Management, Organization, Suggestion}
import cats.implicits.catsSyntaxOptionId
import zio.test.Assertion.{equalTo, isNone, isTrue}
import zio.test._

object DefaultManagementIfEmptySpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val someOrg = Suggestion(
      value = "value",
      unrestrictedValue = "value",
      Organization(
        inn = None,
        kpp = None,
        ogrn = None,
        ogrnDate = None,
        hid = None,
        `type` = None,
        name = None,
        okato = None,
        oktmo = None,
        okpo = None,
        okogu = None,
        okfs = None,
        okved = None,
        okvedType = None,
        okveds = None,
        opf = None,
        management = None,
        branchCount = None,
        branchType = None,
        address = None,
        state = None,
        employeeCount = None,
        authorities = None,
        citizenship = None,
        source = None,
        qc = None,
        finance = None,
        founders = None,
        managers = None,
        predecessors = None,
        successors = None,
        licenses = None,
        capital = None,
        documents = None,
        phones = None
      ).some
    ).some

    val defaultManagement = Management("нет данных".some, "нет данных".some, None).some

    val step3 = new ConvertStep3 {}

    suite("DefaultManagementIfEmpty")(
      test("should set default management for organization if empty") {
        val res = step3.defaultManagementIfEmpty(someOrg)
        println(res)
        assert(res.exists(_.data.exists(_.management == defaultManagement)))(isTrue)
      },
      test("shouldn't set default management if management is defined") {
        val definedManagement = someOrg.map { org =>
          org.copy(data = org.data.map { data =>
            data.copy(management = Management("Murat".some, "director".some, None).some)
          })
        }
        val res = step3.defaultManagementIfEmpty(definedManagement)
        println(res)
        assert(res)(equalTo(definedManagement))
      },
      test("should stay empty suggestion if suggestion is emtpy") {
        val res = step3.defaultManagementIfEmpty(None)
        println(res)
        assert(res)(isNone)
      },
      test("should stay empty org if org is emtpy") {
        val res = step3.defaultManagementIfEmpty(Suggestion[Organization]("asd", "asd", None).some)
        println(res)
        assert(res.exists(_.data.isEmpty))(isTrue)
      }
    )
  }
}
