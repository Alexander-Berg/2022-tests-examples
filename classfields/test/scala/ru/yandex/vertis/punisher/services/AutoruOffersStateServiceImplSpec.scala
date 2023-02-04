package ru.yandex.vertis.punisher.services

import java.time.Instant

import cats.syntax.applicative._
import org.mockito.Mockito.when
import ru.yandex.vertis.punisher.Generators._
import ru.yandex.vertis.punisher.dao.AutoruOffersStateDao
import ru.yandex.vertis.punisher.dao.AutoruOffersStateDao.Filter
import ru.yandex.vertis.punisher.model.OfferState
import ru.yandex.vertis.punisher.services.impl.AutoruOffersStateServiceImpl
import ru.yandex.vertis.punisher.{BaseSpec, Generators}
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.quality.lang_utils.Use

/**
  * @author mpoplavkov
  */
class AutoruOffersStateServiceImplSpec extends BaseSpec {

  val dao: AutoruOffersStateDao[F] = mock[AutoruOffersStateDao[F]]
  val service = new AutoruOffersStateServiceImpl(dao)

  "AutoruOffersStateServiceImpl" should {
    "correctly find user cluster based on VIN numbers" in {
      val since = generate[Instant]
      val userOffer1 = Generators.generateSuchThat[OfferState.Autoru](_.vin.nonEmpty)
      val userId = userOffer1.userId
      val userOffer2 =
        Generators
          .generateSuchThat[OfferState.Autoru](_.vin.nonEmpty)
          .copy(userId = userId)
      val vin1 = userOffer1.vin.get
      val vin2 = userOffer2.vin.get

      val anotherUserOffer1 = Generators.generateSuchThat[OfferState.Autoru](_.userId != userId)
      val anotherUserOffer2 = Generators.generateSuchThat[OfferState.Autoru](_.userId != userId)

      when(dao.getStatesLimitless(Filter(userIds = Use(Set(userId)), wasActiveSince = Use(since))))
        .thenReturn(Seq(userOffer1, userOffer2).pure)
      when(dao.getStatesLimitless(Filter(vins = Use(Set(vin1, vin2)), wasActiveSince = Use(since))))
        .thenReturn(Seq(userOffer1, userOffer2, anotherUserOffer1, anotherUserOffer2).pure)

      val actualCluster = service.getUserClusterByVin(userId, since).await
      actualCluster shouldBe Set(userId, anotherUserOffer1.userId, anotherUserOffer2.userId)
    }
  }
}
