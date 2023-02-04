package ru.yandex.vertis.moderation

import org.scalacheck.Gen
import ru.yandex.vertis.moderation.model.{Domain, Opinion, Opinions}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators
import ru.yandex.vertis.moderation.model.instance.{Essentials, Instance}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.signal.SignalSet
import ru.yandex.vertis.moderation.opinion.OpinionCalculator

/**
  * @author semkagtn
  */
trait OpinionCalculatorSpecBase extends SpecBase {

  protected def service: Service

  protected def calculator: OpinionCalculator

  protected def nextInstance(signals: SignalSet): Instance =
    CoreGenerators.instanceGen(CoreGenerators.ExternalIdGen.next, essentialsGen).next.copy(signals = signals)

  protected def nextDomain(): Domain = domainGen.next

  protected def getExpected(domainOpinionList: (Domain, Opinion)*): Opinions =
    Opinions(Opinions.unknown(service) ++ domainOpinionList)

  private val essentialsGen: Gen[Essentials] =
    service match {
      case Service.REALTY             => CoreGenerators.RealtyEssentialsGen
      case Service.AUTORU             => CoreGenerators.AutoruEssentialsGen
      case Service.USERS_REALTY       => CoreGenerators.UserRealtyEssentialsGen
      case Service.USERS_AUTORU       => CoreGenerators.UserAutoruEssentialsGen
      case Service.AUTO_REVIEWS       => CoreGenerators.AutoReviewsEssentialsGen
      case Service.DEALERS_AUTORU     => CoreGenerators.DealersAutoruEssentialsGen
      case Service.AGENCY_CARD_REALTY => CoreGenerators.AgencyCardRealtyEssentialsGen
      case Service.FEEDS_REALTY       => CoreGenerators.FeedRealtyEssentialsGenerator
      case Service.TELEPHONES         => CoreGenerators.TelephonesEssentialsGenerator
      case Service.UNKNOWN_SERVICE    => ???
    }

  private val domainGen: Gen[Domain] =
    service match {
      case Service.REALTY             => CoreGenerators.DomainRealtyGen
      case Service.AUTORU             => CoreGenerators.DomainAutoruGen
      case Service.USERS_REALTY       => CoreGenerators.DomainUsersRealtyGen
      case Service.USERS_AUTORU       => CoreGenerators.DomainUsersAutoruGen
      case Service.AUTO_REVIEWS       => CoreGenerators.DomainAutoReviewsGen
      case Service.DEALERS_AUTORU     => CoreGenerators.DomainDealersAutoruGen
      case Service.AGENCY_CARD_REALTY => CoreGenerators.DomainAgencyCardRealtyGen
      case Service.FEEDS_REALTY       => CoreGenerators.DomainFeedsRealtyGen
      case Service.TELEPHONES         => CoreGenerators.DomainTelephonesGen
      case Service.UNKNOWN_SERVICE    => ???
    }
}
