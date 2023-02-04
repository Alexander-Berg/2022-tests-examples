package ru.yandex.vertis.general.wizard.scheduler.feed.filters

import ru.yandex.vertis.general.wizard.Generators.stockOfferWithCtrGen
import ru.yandex.vertis.general.wizard.model.StockOfferWithCtr
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, _}
import zio.test.environment.TestEnvironment

object ContactsFilterSpec extends DefaultRunnableSpec {
  private val nonNegativeIntGen = Gen.int(0, Integer.MAX_VALUE)

  private def propOfferNotFilteredWhenParamIsNotPresent(stockOfferWithCtr: StockOfferWithCtr) =
    for {
      actual <- ContactsFilter(None)(stockOfferWithCtr)
    } yield assert(actual)(equalTo(true))

  private def propTooManyContactsOfferFiltered(minNumContacts: Int, stockOfferWithCtr: StockOfferWithCtr) =
    for {
      actual <- ContactsFilter(Some(minNumContacts))(stockOfferWithCtr)
      expected = stockOfferWithCtr.contacts.forall(_ >= minNumContacts)
    } yield assert(actual)(equalTo(expected))

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ContactsFilter")(
      testM("When there is no maybeMinNumContacts no filtering should be performed") {
        checkM(stockOfferWithCtrGen)(propOfferNotFilteredWhenParamIsNotPresent)
      },
      testM(
        """When there is a positive maybeMinNumContacts, only offers which don't have contacts or have less contacts
          |than maybeMaxNumShows are left""".stripMargin
      ) {
        checkM(nonNegativeIntGen, stockOfferWithCtrGen)(propTooManyContactsOfferFiltered)
      }
    )
}
