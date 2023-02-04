package ru.auto.salesman.service.impl.user.notify.impl

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.salesman.dao.notification.ProlongationFailedNotificationDao
import ru.auto.salesman.model.notification.ProlongationFailedNotification
import ru.auto.salesman.model.notification.ProlongationFailedNotification.ProlongationFailedNotificationStatus
import ru.auto.salesman.model.user.ProductType
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}

import java.time.Duration

class FailedProlongationNotificationPersistServiceImplSpec
    extends BaseSpec
    with UserModelGenerators
    with OfferModelGenerators
    with IntegrationPropertyCheckConfig {

  val prolongationFailedNotificationDao =
    mock[ProlongationFailedNotificationDao]

  val failedProlongationNotificationPersistService =
    new FailedProlongationNotificationPersistServiceImpl(
      prolongationFailedNotificationDao
    )
  "FailedProlongationNotificationPersistServiceImpl" should {

    "call insert in dao" in {
      (prolongationFailedNotificationDao
        .insert(_: ProductType, _: String))
        .expects(ProductType.Goods, "1244")
        .returningZ(())

      failedProlongationNotificationPersistService
        .insert(ProductType.Goods, "1244")
        .success
    }

    "call markStatusAsProcessed in dao" in {

      (prolongationFailedNotificationDao
        .markStatusAsProcessed(_: Long, _: Option[String]))
        .expects(10L, None)
        .returningZ(42)

      failedProlongationNotificationPersistService
        .markStatusAsProcessed(10L, None)
        .success
        .value shouldBe 42
    }

    "call getUnProcesse in dao" in {
      val result = recordGeneration().next(4)

      (prolongationFailedNotificationDao
        .getUnProcessed(_: Int, _: Duration))
        .expects(10, Duration.ofSeconds(10))
        .returningZ(result)

      failedProlongationNotificationPersistService
        .getUnProcessed(10, Duration.ofSeconds(10))
        .success
        .value shouldBe result
    }
  }

  private def recordGeneration(): Gen[ProlongationFailedNotification] =
    for {
      productType <- Gen.oneOf(
        ProductType.values
      )
      productId <- Gen.uuid
      details <- Gen.option(Gen.alphaStr)
      epoch <- Gen.chooseNum(10000L, 1000000L)
      lastUpdate <- Gen.chooseNum(10000L, 1000000L)
    } yield
      ProlongationFailedNotification(
        id = 0L,
        productType = productType,
        productId = productId.toString.replace("-", ""),
        status = ProlongationFailedNotificationStatus.Processed,
        details = details,
        epoch = new DateTime(epoch),
        lastUpdate = new DateTime(lastUpdate)
      )

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
