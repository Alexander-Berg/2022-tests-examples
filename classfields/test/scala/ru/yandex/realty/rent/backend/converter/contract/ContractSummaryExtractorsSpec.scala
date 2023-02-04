package ru.yandex.realty.rent.backend.converter.contract

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.rent.gen.RentModelsGen

@RunWith(classOf[JUnitRunner])
class ContractSummaryExtractorsSpec extends SpecBase with RentModelsGen {

  import ContractSummaryExtractors._

  "ContractSummaryExtractors.extractSummary" should {
    "return fields for [Owner] when scope=[ContractScope.Owner]" in new Data {
      override val hasAuthority: Boolean = false

      val expected = Seq(
        Group(
          CommonGroup,
          Seq(
            Item("Адрес", "ул. Пушкина, дом Колотушкина"),
            Item("Арендная плата", "35 000,0 р/мес")
          )
        ),
        Group(
          OwnerGroup,
          Seq(
            Item("ФИО", "Василий Пирогов"),
            Item("Почта", "mail@yandex-team.ru")
          )
        )
      )

      val result: Seq[Group] = data.extractSummary(ContractScope.Owner, extractors)
      result should contain theSameElementsAs expected
    }

    "return fields for [Owner] when scope=[ContractScope.Owner] with authority" in new Data {
      override val hasAuthority: Boolean = true
      override val hasTenantCheckInDate: Boolean = true

      val expected = Seq(
        Group(
          CommonGroup,
          Seq(
            Item("Адрес", "ул. Пушкина, дом Колотушкина"),
            Item("Арендная плата", "35 000,0 р/мес")
          )
        ),
        Group(
          OwnerGroup,
          Seq(
            Item("ФИО", "Николай Потемкин")
          )
        ),
        Group(
          ConfidantGroup,
          Seq(
            Item("ФИО", "Василий Пирогов"),
            Item("Почта", "mail@yandex-team.ru")
          )
        ),
        Group(TenantGroup, Seq(Item("Дата заселения", "10.05.2022")))
      )

      val result: Seq[Group] = data.extractSummary(ContractScope.Owner, extractors)
      result should contain theSameElementsAs expected
    }

    "return fields for [Tenant] when scope=[ContractScope.Tenant]" in new Data {
      override val hasAuthority: Boolean = false
      override val hasTenantCheckInDate: Boolean = true

      val expected = Seq(
        Group(
          CommonGroup,
          Seq(
            Item("Адрес", "ул. Пушкина, дом Колотушкина"),
            Item("Сумма ежемесячного платежа", "36750.0 р/мес")
          )
        ),
        Group(
          OwnerGroup,
          Seq(
            Item("ФИО", "Василий Пирогов"),
            Item("Почта", "mail@yandex-team.ru")
          )
        ),
        Group(TenantGroup, Seq(Item("Дата заселения", "10.05.2022")))
      )

      val result: Seq[Group] = data.extractSummary(ContractScope.Tenant, extractors)
      result should contain theSameElementsAs expected
    }
  }

  trait Data {
    val hasAuthority: Boolean
    val hasTenantCheckInDate: Boolean = false

    val data: ContractSummaryData = mock[ContractSummaryData]

    val CommonGroup = "Основная информация"
    val OwnerGroup = "Собственник"
    val ConfidantGroup = "Представитель собственника"
    val TenantGroup = "Жилец"

    val extractors: Seq[ExtractorGroup] = Seq(
      extractorGroup(CommonGroup)(
        Extractor("Адрес", ContractScope.All, _ => "ул. Пушкина, дом Колотушкина"),
        Extractor("Арендная плата", ContractScope.Owner, _ => "35 000,0 р/мес"),
        Extractor("Сумма ежемесячного платежа", ContractScope.Tenant, _ => "36750.0 р/мес")
      ),
      extractorGroup(OwnerGroup, _ => !hasAuthority)(
        Extractor("ФИО", ContractScope.All, _ => "Василий Пирогов"),
        Extractor("Почта", ContractScope.All, _ => "mail@yandex-team.ru")
      ),
      extractorGroup(OwnerGroup, _ => hasAuthority)(
        Extractor("ФИО", ContractScope.All, _ => "Николай Потемкин")
      ),
      extractorGroup(ConfidantGroup, _ => hasAuthority)(
        Extractor("ФИО", ContractScope.All, _ => "Василий Пирогов"),
        Extractor("Почта", ContractScope.All, _ => "mail@yandex-team.ru")
      ),
      extractorGroup(TenantGroup, _ => hasTenantCheckInDate)(
        Extractor("Дата заселения", ContractScope.All, _ => "10.05.2022")
      )
    )
  }

}
