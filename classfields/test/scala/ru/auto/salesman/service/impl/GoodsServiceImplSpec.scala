package ru.auto.salesman.service.impl

import cats.data.NonEmptySet
import cats.implicits._
import com.github.nscala_time.time.Imports._
import org.joda.time.{DateTime, LocalDate, LocalTime}
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Args, BeforeAndAfter, Status, Suite}
import ru.auto.api.ApiOfferModel.Category
import ru.auto.salesman
import ru.auto.salesman.billing.BootstrapClient
import ru.auto.salesman.client.PromocoderClient
import ru.auto.salesman.dao.GoodsDao.{Condition, Record}
import ru.auto.salesman.dao.OfferDao.Condition.OfferIdCategory
import ru.auto.salesman.dao.OfferDao.OfferPatch
import ru.auto.salesman.dao.user.ProductScheduleDao
import ru.auto.salesman.dao.{BadgeDao, GoodsDao, OfferDao}
import ru.auto.salesman.environment
import ru.auto.salesman.environment._
import ru.auto.salesman.model.OfferStatuses.Expired
import ru.auto.salesman.model._
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Boost
import ru.auto.salesman.model.user.schedule.AllowMultipleRescheduleUpsert.SameOrTrue
import ru.auto.salesman.model.user.schedule.ScheduleParameters.OnceAtDates
import ru.auto.salesman.model.user.schedule.{IsVisible, ScheduleSource}
import ru.auto.salesman.service.GoodsDecider.Action._
import ru.auto.salesman.service.GoodsDecider.DeactivateReason.InactiveClient
import ru.auto.salesman.service.GoodsDecider.NoActionReason.GetAdsRequestError
import ru.auto.salesman.service.GoodsDecider._
import ru.auto.salesman.service._
import ru.auto.salesman.service.impl.GoodsServiceImplSpec._
import ru.auto.salesman.tasks.instrumented.InstrumentedGoodsService
import ru.auto.salesman.test.dao.gens._
import ru.auto.salesman.test.model.gens
import ru.auto.salesman.test.model.gens.OfferModelGenerators._
import ru.auto.salesman.test.{BaseSpec, TestException}
import ru.auto.salesman.util.JodaCatsInstances._
import ru.auto.salesman.util.offer.RichOffer
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.billing.Model.OfferBilling
import ru.yandex.vertis.billing.Model.OfferBilling.KnownCampaign
import ru.yandex.vertis.billing.model.Versions
import ru.yandex.vertis.generators.ProducerProvider.asProducer
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.scalamock.util.RichTryCallHandler
import zio.{Task, ZIO}

import java.util
import scala.util.{Success, Try}

class GoodsServiceImplSpec extends BaseSpec with Mocking {
  implicit private val rc: RequestContext = AutomatedContext("test")

  "GoodsService" should {
    "process empty records" in {
      val result =
        goodsService.prolong(
          testOffer,
          List.empty[Record]
        )
      result.success.value shouldBe (())
      (goodsDao.update _).verify(*, *).never
      (offerDao.update _).verify(*, *).never
      (decider
        .apply(_: Request)(_: RequestContext))
        .verify(*, *)
        .never
    }

    "process Activate action on Placement" in {
      val record = newRecord(ProductId.Placement)
      stubDeciderApply(
        record,
        activateDate,
        List.empty,
        offerBilling,
        deciderActivate
      )
      stubAlwaysUpdateGood()
      goodsService
        .prolong(testOffer, List(record))
        .success
        .value shouldBe (())
      (decider
        .apply(_: Request)(_: RequestContext))
        .verify(
          argAssert { request: Request =>
            verifyDeciderRequest(request, record)
          },
          *
        )
      (goodsDao.update _).verify(
        Condition.WithGoodsId(record.primaryKeyId),
        argAssert { goodPatch: GoodsDao.Patch =>
          verifyActivate(goodPatch)
        }
      )
      (offerDao.update _).verify(
        OfferIdCategory(record.offerId, record.category),
        argAssert { offerPatch: OfferDao.OfferPatch =>
          verifyActivate(offerPatch)
        }
      )
    }

    "process Activate action on Placement with unknown price" in {
      val record = newRecord(ProductId.Placement)
      stubDeciderApply(
        record,
        activateDate,
        List.empty,
        offerBilling,
        deciderActivate
      )
      stubAlwaysUpdateGood()
      goodsService
        .prolong(testOffer, List(record))
        .success
        .value shouldBe (())
      (decider
        .apply(_: Request)(_: RequestContext))
        .verify(
          argAssert { request: Request =>
            verifyDeciderRequest(request, record)
          },
          *
        )
      (goodsDao.update _).verify(
        Condition.WithGoodsId(record.primaryKeyId),
        argAssert { goodPatch: GoodsDao.Patch =>
          verifyActivate(goodPatch)
        }
      )
      (offerDao.update _).verify(
        OfferIdCategory(record.offerId, record.category),
        argAssert { offerPatch: OfferDao.OfferPatch =>
          verifyActivate(offerPatch)
        }
      )
    }

    "process Activate action on Badge" in {
      val record = newRecord(ProductId.Badge)
      stubDeciderApplyBadge(
        record,
        activateDate,
        List.empty,
        offerBilling,
        deciderActivate
      )
      stubAlwaysUpdateGood()
      goodsService
        .prolong(testOffer, List(record))
        .success
        .value shouldBe (())
      (decider
        .apply(_: Request)(_: RequestContext))
        .verify(
          argAssert { request: Request =>
            verifyDeciderRequest(request, record)
          },
          *
        )
      (goodsDao.update _).verify(
        Condition.WithGoodsId(record.primaryKeyId),
        argAssert { goodPatch: GoodsDao.Patch =>
          verifyActivate(goodPatch)
        }
      )
      (badgeDao.updateStatus _).verify(
        record.primaryKeyId,
        record.offerId,
        record.category,
        GoodStatuses.Active
      )
    }

    "process Activate action on Fresh" in {
      val record = newRecord(ProductId.Fresh)
      stubDeciderApply(
        record,
        activateDate,
        List.empty,
        offerBilling,
        deciderActivate
      )
      stubAlwaysUpdateGood()
      goodsService
        .prolong(testOffer, List(record))
        .success
        .value shouldBe (())
      (decider
        .apply(_: Request)(_: RequestContext))
        .verify(
          argAssert { request: Request =>
            verifyDeciderRequest(request, record)
          },
          *
        )
      (offerDao.update _).verify(
        OfferIdCategory(record.offerId, record.category),
        argAssert { offerPatch: OfferDao.OfferPatch =>
          verifyFresh(offerPatch)
        }
      )
    }

    "process Activate on other goods" in {
      val record = newRecord(ProductId.Top)
      stubDeciderApply(
        record,
        activateDate,
        List.empty,
        offerBilling,
        deciderActivate
      )
      stubAlwaysUpdateGood()
      goodsService
        .prolong(testOffer, List(record))
        .success
        .value shouldBe (())
      (decider
        .apply(_: Request)(_: RequestContext))
        .verify(
          argAssert { request: Request =>
            verifyDeciderRequest(request, record)
          },
          *
        )
      (goodsDao.update _).verify(
        Condition.WithGoodsId(record.primaryKeyId),
        argAssert { goodPatch: GoodsDao.Patch =>
          verifyActivate(goodPatch)
        }
      )
      (offerDao.update _).verify(
        OfferIdCategory(record.offerId, record.category),
        argAssert { offerPatch: OfferDao.OfferPatch =>
          offerPatch should matchPattern { case OfferPatch(None, None, Some(_), None) =>
          }
        }
      )
    }

    "process Activate with promocodes" in {
      val record = newRecord(ProductId.Top)
      val featureId = s"${record.product}:promo_salesman-test:96eb92e69602f216"
      val featureTag = record.product.toString
      val user = PromocoderUser(record.clientId, UserTypes.ClientUser)
      val featureCount = FeatureCount(10L, FeatureUnits.Items)
      val featurePayload = FeaturePayload(FeatureUnits.Items)
      val featureOrigin = FeatureOrigin("origin")
      val feature = FeatureInstance(
        featureId,
        featureOrigin,
        featureTag,
        user.toString,
        featureCount,
        now(),
        now().plusDays(2),
        featurePayload
      )
      val feature2 = feature.copy(id = featureId + "2")

      stubDeciderApply(
        record,
        activateDate,
        List(
          PriceModifierFeature(
            feature,
            FeatureCount(1L, FeatureUnits.Items),
            testGoodPrice
          ),
          PriceModifierFeature(
            feature2,
            FeatureCount(2L, FeatureUnits.Money),
            testGoodPrice
          )
        ),
        offerBillingByPromocode,
        deciderActivate
      )
      stubAlwaysUpdateGood()
      whenPromocodeDecrease(
        feature.id,
        FeatureCount(1L, FeatureUnits.Items),
        ZIO.succeed(feature)
      )
      whenPromocodeDecrease(
        feature2.id,
        FeatureCount(2L, FeatureUnits.Money),
        ZIO.succeed(feature)
      )

      goodsService
        .prolong(testOffer, List(record))
        .success
        .value shouldBe (())
      (decider
        .apply(_: Request)(_: RequestContext))
        .verify(
          argAssert { request: Request =>
            verifyDeciderRequest(request, record)
          },
          *
        )
      (goodsDao.update _).verify(
        Condition.WithGoodsId(record.primaryKeyId),
        argAssert { goodPatch: GoodsDao.Patch =>
          verifyActivateByPromocode(goodPatch)
        }
      )
      (offerDao.update _).verify(
        OfferIdCategory(record.offerId, record.category),
        argAssert { offerPatch: OfferDao.OfferPatch =>
          offerPatch should matchPattern { case OfferPatch(None, None, Some(_), None) =>
          }
        }
      )
    }

    "process Activate with failed promocode dec" in {
      val record = newRecord(ProductId.Top)
      val featureId = s"${record.product}:promo_salesman-test:96eb92e69602f216"
      val featureTag = record.product.toString
      val user = PromocoderUser(record.clientId, UserTypes.ClientUser)
      val featureCount = FeatureCount(10L, FeatureUnits.Items)
      val featurePayload = FeaturePayload(FeatureUnits.Items)
      val featureOrigin = FeatureOrigin("origin")
      val feature = FeatureInstance(
        featureId,
        featureOrigin,
        featureTag,
        user.toString,
        featureCount,
        now(),
        now().plusDays(2),
        featurePayload
      )

      stubDeciderApply(
        record,
        activateDate,
        List(
          PriceModifierFeature(
            feature,
            FeatureCount(1L, FeatureUnits.Items),
            testGoodPrice
          )
        ),
        offerBillingByPromocode,
        deciderActivate
      )
      stubAlwaysUpdateGood()
      whenPromocodeDecrease(
        feature.id,
        FeatureCount(1L, FeatureUnits.Items),
        ZIO.fail(new Exception)
      )

      goodsService
        .prolong(testOffer, List(record))
        .success
        .value shouldBe (())
      (decider
        .apply(_: Request)(_: RequestContext))
        .verify(
          argAssert { request: Request =>
            verifyDeciderRequest(request, record)
          },
          *
        )
      (goodsDao.update _).verify(
        Condition.WithGoodsId(record.primaryKeyId),
        argAssert { goodPatch: GoodsDao.Patch =>
          verifyActivateByPromocode(goodPatch)
        }
      )
      (offerDao.update _).verify(
        OfferIdCategory(record.offerId, record.category),
        argAssert { offerPatch: OfferDao.OfferPatch =>
          offerPatch should matchPattern { case OfferPatch(None, None, Some(_), None) =>
          }
        }
      )
    }

    "process Activate on Turbo" in {
      val activateDate =
        FirstActivateDate(
          DateTime
            .parse("2019-02-12T15:28:00+03:00")
            .withZone(salesman.environment.TimeZone)
        )
      val record =
        newRecord(ProductId.Turbo).copy(firstActivateDate = activateDate)
      val offerBillingAtActivateDate =
        offerBilling.toBuilder.setTimestamp(activateDate.getMillis).build()
      stubDeciderApply(
        record,
        ActivateDate(activateDate.asDateTime),
        List.empty,
        offerBillingAtActivateDate,
        deciderActivate
      )
      stubAlwaysUpdateGood()
      (productScheduleDao.insertIfAbsent _).when(*).returningT(())
      goodsService
        .prolong(testOffer, List(record))
        .success
        .value shouldBe (())
      (decider
        .apply(_: Request)(_: RequestContext))
        .verify(
          argAssert { request: Request =>
            verifyDeciderRequest(request, record)
          },
          *
        )
      (goodsDao.update _).verify(Condition.WithGoodsId(record.primaryKeyId), *)
      (productScheduleDao.insertIfAbsent _).verify(
        ScheduleSource(
          AutoruOfferId(
            record.offerId,
            Some(record.offerHash).filter(_.nonEmpty)
          ),
          record.owner,
          Boost,
          OnceAtDates(
            NonEmptySet
              .of(
                LocalDate.parse("2019-02-12"),
                LocalDate.parse("2019-02-14"),
                LocalDate.parse("2019-02-16")
              ),
            LocalTime.parse("16:28:00"),
            environment.TimeZone
          ),
          IsVisible(false),
          expireDate = Some(
            DateTime
              .parse("2019-02-17T16:28:00+03:00")
              .withZone(environment.TimeZone)
          ),
          customPrice = Some(0L),
          allowMultipleReschedule = SameOrTrue,
          prevScheduleId = None
        )
      )
    }

    "process Activate on Turbo with boost schedule expire date more than all schedule dates" in {
      val activateDate =
        FirstActivateDate(DateTime.parse("2019-02-12T15:28:00+03:00"))
      val record =
        newRecord(ProductId.Turbo).copy(firstActivateDate = activateDate)
      stubDeciderApply(
        record,
        ActivateDate(activateDate.asDateTime),
        List.empty,
        offerBilling,
        deciderActivate
      )
      stubAlwaysUpdateGood()
      (productScheduleDao.insertIfAbsent _).when(*).returningT(())
      goodsService
        .prolong(testOffer, List(record))
        .success
        .value shouldBe (())
      (productScheduleDao.insertIfAbsent _).verify {
        argThat { schedule: ScheduleSource =>
          schedule.scheduleParameters match {
            case OnceAtDates(dates, time, timezone) =>
              schedule.expireDate.value > dates.maximum
                .toDateTime(time, timezone)
            case _ =>
              false
          }
        }
      }
    }

    "not process Activate on Turbo when creating schedule failed" in {
      val record = newRecord(ProductId.Turbo)
      stubDeciderApply(
        record,
        activateDate,
        List.empty,
        offerBilling,
        deciderActivate
      )
      (productScheduleDao.insertIfAbsent _).when(*).throwingT(new TestException)
      goodsService
        .prolong(testOffer, List(record))
        .failure
        .exception shouldBe a[TestException]
    }

    "process Activate on Turbo and update offer set_date" in {
      val record = newRecord(ProductId.Turbo)
      val before = environment.now()
      (productScheduleDao.insertIfAbsent _).when(*).returningT(())
      stubDeciderApply(
        record,
        activateDate,
        List.empty,
        offerBilling,
        deciderActivate
      )
      stubAlwaysUpdateGood()
      goodsService
        .prolong(testOffer, List(record))
        .success
        .value shouldBe (())
      (offerDao.update _).verify(
        OfferIdCategory(record.offerId, record.category),
        argAssert { offerPatch: OfferDao.OfferPatch =>
          val setDate = offerPatch.setDate.value
          setDate should be >= before
          setDate should be < before.plusSeconds(10)
          offerPatch.expireDate shouldBe empty
          offerPatch.status shouldBe empty
          offerPatch.freshDate shouldBe empty
        }
      )
    }

    "process Deactivate on Placement" in {
      val record = newRecord(ProductId.Placement)
      stubDeciderApply(
        record,
        activateDate,
        List.empty,
        offerBilling,
        deciderDeactivate
      )
      stubAlwaysUpdateGood()
      (goodsDao.get _).when(*).returningT(Iterable(record))
      goodsService
        .prolong(testOffer, List(record))
        .success
        .value shouldBe (())
      (decider
        .apply(_: Request)(_: RequestContext))
        .verify(
          argAssert { request: Request =>
            verifyDeciderRequest(request, record)
          },
          *
        )
      (goodsDao.update _).verify(
        Condition.WithGoodsId(record.primaryKeyId),
        argAssert { goodPatch: GoodsDao.Patch =>
          verifyDeactivate(goodPatch)
        }
      )
      (offerDao.update _).verify(
        OfferIdCategory(record.offerId, record.category),
        argAssert { offerPatch: OfferDao.OfferPatch =>
          verifyDeactivate(offerPatch)
        }
      )
    }

    "process Deactivate action on Badge" in {
      val record = newRecord(ProductId.Badge)
      stubDeciderApplyBadge(
        record,
        activateDate,
        List.empty,
        offerBilling,
        deciderDeactivate
      )
      stubAlwaysUpdateGood()
      (goodsDao.get _).when(*).returningT(Iterable(record))
      goodsService
        .prolong(testOffer, List(record))
        .success
        .value shouldBe (())
      (decider
        .apply(_: Request)(_: RequestContext))
        .verify(
          argAssert { request: Request =>
            verifyDeciderRequest(request, record)
          },
          *
        )
      (goodsDao.update _).verify(
        Condition.WithGoodsId(record.primaryKeyId),
        argAssert { goodPatch: GoodsDao.Patch =>
          verifyDeactivate(goodPatch)
        }
      )
      (badgeDao.delete _).verify(
        record.primaryKeyId,
        record.offerId,
        record.category
      )
    }

    "process Deactivate on other goods" in {
      val record = newRecord(ProductId.Top)
      stubDeciderApply(
        record,
        activateDate,
        List.empty,
        offerBilling,
        deciderDeactivate
      )
      stubAlwaysUpdateGood()
      goodsService
        .prolong(testOffer, List(record))
        .success
        .value shouldBe (())
      (decider
        .apply(_: Request)(_: RequestContext))
        .verify(
          argAssert { request: Request =>
            verifyDeciderRequest(request, record)
          },
          *
        )
      (goodsDao.update _).verify(
        Condition.WithGoodsId(record.primaryKeyId),
        argAssert { goodPatch: GoodsDao.Patch =>
          verifyDeactivate(goodPatch)
        }
      )
      (offerDao.update _).verify(
        OfferIdCategory(record.offerId, record.category),
        argAssert { offerPatch: OfferDao.OfferPatch =>
          offerPatch should matchPattern { case OfferPatch(None, None, Some(_), None) =>
          }
        }
      )
    }

    "skip NoAction" in {
      val record = newRecord(ProductId.Placement)
      stubDeciderApply(
        record,
        activateDate,
        List.empty,
        offerBilling,
        // don't care about exact reason here for now
        (_, _, _) => Success(response(NoAction(GetAdsRequestError(new TestException))))
      )
      goodsService
        .prolong(testOffer, List(record))
        .success
        .value shouldBe (())
      (decider
        .apply(_: Request)(_: RequestContext))
        .verify(
          argAssert { request: Request =>
            verifyDeciderRequest(request, record)
          },
          *
        )
      (goodsDao.update _).verify(*, *).never
      (offerDao.update _).verify(*, *).never
    }

    "deactivate all if found Deactivate Placement" in {
      val placementRecord = newRecord(ProductId.Placement)
      val otherRecord = newRecord(ProductId.Top)
      val forLoading = newRecord(ProductId.Premium)

      val offerRecords = Iterable(placementRecord, forLoading)
      val records = List(otherRecord, placementRecord)

      (goodsDao.get _).when(*).returningT(offerRecords)
      stubDeciderApply(
        placementRecord,
        activateDate,
        List.empty,
        offerBilling,
        deciderDeactivate
      )
      stubAlwaysUpdateGood()

      goodsService
        .prolong(testOffer, records)
        .success
        .value shouldBe (())

      (decider
        .apply(_: Request)(_: RequestContext))
        .verify(
          argAssert { request: Request =>
            verifyDeciderRequest(request, placementRecord)
          },
          *
        )
      (goodsDao.get _).verify(argAssert { goodFilter: GoodsDao.Filter =>
        goodFilter should matchPattern {
          case GoodsDao.Filter
                .ForOfferCategory(`testOfferId`, `testOfferCategory`) =>
        }
      })
      var uncheckedGoodsIds = offerRecords.map(_.primaryKeyId).toIndexedSeq
      (goodsDao.update _)
        .verify(
          argAssert { condition: GoodsDao.Condition =>
            condition match {
              case GoodsDao.Condition.WithGoodsId(goodsId) =>
                uncheckedGoodsIds should contain(goodsId)
                val indexToRemove = uncheckedGoodsIds.indexOf(goodsId)
                uncheckedGoodsIds = uncheckedGoodsIds
                  .take(indexToRemove) ++ uncheckedGoodsIds.drop(
                  indexToRemove + 1
                )
              case other => fail(s"expected WithGoodsId, got $other")
            }
          },
          argAssert { patch: GoodsDao.Patch =>
            verifyDeactivate(patch)
          }
        )
        .repeat(offerRecords.size)
      (offerDao.update _)
        .verify(OfferIdCategory(testOfferId, testOfferCategory), *)
        .twice()
    }

    "not deactivate placement if found Deactivate Placement, but deactivation of other service failed" in {
      val placementRecord = newRecord(ProductId.Placement)
      val otherRecord = newRecord(ProductId.Top)
      // без +1 тест флапает, когда placementId и premiumId совпадают. в таком
      // случае дёргается goodsDao.update(premiumId), и проверка на отсутствие
      // запусков goodsDao.update(placementId) фейлится
      val forLoading =
        newRecord(ProductId.Premium)
          .copy(primaryKeyId = placementRecord.primaryKeyId + 1)

      val offerRecords = Iterable(forLoading, placementRecord)
      val records = List(otherRecord, placementRecord)

      (goodsDao.get _).when(*).returningT(offerRecords)
      val e = new TestException
      (goodsDao.update _)
        .when(Condition.WithGoodsId(forLoading.primaryKeyId), *)
        .throwingT(e)
      stubDeciderApply(
        placementRecord,
        activateDate,
        List.empty,
        offerBilling,
        deciderDeactivate
      )

      goodsService
        .prolong(testOffer, records)
        .failure
        .exception shouldBe e

      (goodsDao.update _)
        .verify(Condition.WithGoodsId(placementRecord.primaryKeyId), *)
        .never
    }

    "process different actions when no Deactivate Placement occurs" in {
      val records = List.fill(50)(newRecord(ProductId.Placement))
      stubDeciderApply
      stubAlwaysUpdateGood()
      goodsService
        .prolong(testOffer, records)
        .success
        .value shouldBe (())
      val placementsCount = records.count(_.product == ProductId.Placement)

      (decider
        .apply(_: Request)(_: RequestContext))
        .verify(*, *)
        .repeat(records.size)
      (goodsDao.update _).verify(*, *).repeat(placementsCount to records.size)
      (offerDao.update _).verify(*, *).repeat(0 to records.size)
      (goodsDao.get _).verify(*).never
    }

    "fail if at least one decider's Failure detected" in {
      val record1 = newRecord(ProductId.Placement)
      val record2 = newRecord(ProductId.Placement)
      val ex = new Exception("artificial")
      (decider
        .apply(_: Request)(_: RequestContext))
        .when(
          argThat { r: Request =>
            r.offer.nonHashedId == record1.offerId &&
            r.product == record1.product &&
            r.clientId == record1.clientId
          },
          *
        )
        .throwingT(ex)
      (decider
        .apply(_: Request)(_: RequestContext))
        .when(
          argThat { r: Request =>
            r.offer.nonHashedId == record2.offerId &&
            r.product == record2.product &&
            r.clientId == record2.clientId
          },
          *
        )
        .returning(deciderActivate(activateDate, offerBilling, List.empty))
      goodsService
        .prolong(testOffer, List(record1, record2))
        .failure
        .exception shouldBe ex
      (goodsDao.update _).verify(*, *).never
      (offerDao.update _).verify(*, *).never
    }
  }

  private def verifyActivate(goodsPatch: GoodsDao.Patch): Unit = {
    import GoodsDao._
    import goodsPatch._
    offerBillingDeadline.value shouldBe Update(deadlineTs)
    offerBilling.value should matchPattern {
      case Update(obb: Array[Byte]) if util.Arrays.equals(obb, offerBillingBytes) =>
    }
    status.value shouldBe GoodStatuses.Active
    firstActivateDate shouldBe None
    holdId.value shouldBe Update(hold)
  }

  private def verifyActivateByPromocode(goodsPatch: GoodsDao.Patch): Unit = {
    import GoodsDao._
    import goodsPatch._
    offerBillingDeadline.value shouldBe Update(deadlineTs)
    offerBilling.value should matchPattern {
      case Update(obb: Array[Byte])
          if util.Arrays.equals(obb, offerBillingByPromocodeBytes) =>
    }
    status.value shouldBe GoodStatuses.Active
    firstActivateDate shouldBe None
    holdId.value shouldBe Update("")
  }

  private def verifyDeactivate(goodsPatch: GoodsDao.Patch): Unit = {
    import goodsPatch._
    status.value shouldBe GoodStatuses.Inactive
  }

  private def verifyActivate(offerPatch: OfferDao.OfferPatch): Unit = {
    import offerPatch._
    expireDate.value shouldBe deadlineTs
    status.value shouldBe OfferStatuses.Show
    setDate should matchPattern { case Some(_: DateTime) => }
  }

  private def verifyFresh(offerPatch: OfferDao.OfferPatch): Unit = {
    import offerPatch._
    setDate should matchPattern { case Some(_: DateTime) => }
    freshDate should matchPattern { case Some(_: DateTime) => }
  }

  private def verifyDeactivate(offerPatch: OfferDao.OfferPatch): Unit = {
    import offerPatch._
    expireDate.value should not be expireDate
    status.value shouldBe Expired
    setDate should matchPattern { case Some(_: DateTime) => }
  }

  private def verifyDeciderRequest(
      request: GoodsDecider.Request,
      record: GoodsDao.Record
  ): Unit = {
    request.offer.nonHashedId should be(record.offerId)
    request.product should be(record.product)
    request.clientId should be(record.clientId)

  }
}

//noinspection TypeAnnotation
object GoodsServiceImplSpec {

  val testOfferId = 1L
  val testOfferCategory = OfferCategories.Cars

  val testOffer = offerGen(
    offerIdGen = gens.AutoruOfferIdGen.map(_.copy(id = testOfferId)),
    offerCategoryGen = Category.CARS
  ).next

  private val testGoodPrice: Funds = 2000

  private def newRecord(product: ProductId, price: Funds = testGoodPrice) =
    GoodRecordGen.next
      .copy(
        offerId = testOfferId,
        category = testOfferCategory,
        product = product
      )

  val deadlineTs = now().plusHours(3)
  val activateDate = ActivateDate(now())
  val hold = "1"

  val offerBilling: OfferBilling = {
    val knownCampaignBuilder =
      KnownCampaign
        .newBuilder()
        .setActiveStart(activateDate.getMillis)
        .setActiveDeadline(deadlineTs.getMillis)
        .setHold(hold)
    OfferBilling
      .newBuilder()
      .setVersion(Versions.OFFER_BILLING)
      .setKnownCampaign(knownCampaignBuilder)
      .build()
  }

  val offerBillingByPromocode: OfferBilling = {
    val knownCampaignBuilder =
      KnownCampaign
        .newBuilder()
        .setActiveStart(activateDate.getMillis)
        .setActiveDeadline(deadlineTs.getMillis)
    OfferBilling
      .newBuilder()
      .setVersion(Versions.OFFER_BILLING)
      .setKnownCampaign(knownCampaignBuilder)
      .build()
  }

  val offerBillingBytes = offerBilling.toByteArray
  val offerBillingByPromocodeBytes = offerBillingByPromocode.toByteArray

  private def deciderActivate(
      activateDate: ActivateDate,
      ob: OfferBilling,
      features: List[PriceModifierFeature]
  ) =
    Success(
      Response(
        Activate(activateDate, ob, features),
        None,
        None
      )
    )

  private def deciderDeactivate(
      activateDate: ActivateDate,
      ob: OfferBilling,
      features: List[PriceModifierFeature]
  ) =
    // don't care about exact reason here for now
    Success(
      Response(
        Deactivate(InactiveClient, offerStatusPatch = Some(Expired)),
        None,
        None
      )
    )

  trait Mocking extends BeforeAndAfter with MockFactory {

    this: Suite =>

    abstract override protected def runTest(
        testName: String,
        args: Args
    ): Status =
      super.runTest(testName, args)

    abstract override def run(testName: Option[String], args: Args): Status =
      super.run(testName, args)

    val campaignSource = stub[CampaignService]
    val billingBootstrapClient = stub[BootstrapClient]

    val campaignCreationService = new CampaignCreationServiceImpl(
      billingBootstrapClient
    )
    val goodsDao = stub[GoodsDao]
    val offerDao = stub[OfferDao]
    val badgeDao = stub[BadgeDao]
    val decider = stub[GoodsDecider]
    val promocoder = stub[PromocoderClient]
    val productScheduleDao = stub[ProductScheduleDao]

    val goodsDaoProvider = new GoodsDaoProvider {
      def chooseDao(category: Category): GoodsDao = goodsDao
      def getAll(): List[GoodsDao] = List(goodsDao)
    }

    val offerService = new OfferServiceImpl(offerDao)

    val goodsActivateService = new GoodsActivateServiceImpl(
      offerService,
      badgeDao,
      promocoder,
      productScheduleDao,
      goodsDaoProvider
    )

    val goodsDeactivateService =
      new GoodsDeactivateServiceImpl(offerService, goodsDao, badgeDao)

    val goodsService = new GoodsServiceImpl(
      goodsActivateService,
      goodsDeactivateService,
      goodsDao,
      decider
    ) with InstrumentedGoodsService {
      override def serviceName: String = "test-service"
      override def ops: OperationalSupport = TestOperationalSupport
    }

    before {
      (badgeDao.updateStatus _).when(*, *, *, *).returningT(())
      (badgeDao.delete _).when(*, *, *).returningT(())
      (offerDao.update _).when(*, *).returningT(())
    }

    protected def stubDeciderApply(
        record: GoodsDao.Record,
        activateDate: ActivateDate,
        features: List[PriceModifierFeature],
        ob: OfferBilling,
        r: (
            ActivateDate,
            OfferBilling,
            List[PriceModifierFeature]
        ) => Try[GoodsDecider.Response]
    ) =
      (decider
        .apply(_: Request)(_: RequestContext))
        .when(
          argThat { request: Request =>
            request match {
              case Request(clientId, offer, ProductContext(product), _, _, _) =>
                clientId == record.clientId &&
                  offer.nonHashedId == record.offerId &&
                  product == record.product
              case Request(clientId, offer, BadgeContext(goodId), _, _, _) =>
                clientId == record.clientId &&
                  offer.nonHashedId == record.offerId &&
                  record.product == ProductId.Badge &&
                  goodId == record.primaryKeyId
              case _ => false
            }
          },
          *
        )
        .returning(r(activateDate, ob, features))

    protected def stubDeciderApplyBadge(
        record: GoodsDao.Record,
        activateDate: ActivateDate,
        features: List[PriceModifierFeature],
        ob: OfferBilling,
        r: (
            ActivateDate,
            OfferBilling,
            List[PriceModifierFeature]
        ) => Try[GoodsDecider.Response]
    ) =
      (decider
        .apply(_: Request)(_: RequestContext))
        .when(
          argThat { request: Request =>
            request match {
              case Request(clientId, offer, BadgeContext(_), _, _, _) =>
                clientId == record.clientId &&
                  offer.nonHashedId == record.offerId
              case _ => false
            }
          },
          *
        )
        .returning(r(activateDate, ob, features))

    protected def stubDeciderApply = {
      (decider
        .apply(_: Request)(_: RequestContext))
        .when(
          argThat { request: Request =>
            request match {
              case Request(
                    _,
                    _,
                    ProductContext(ProductId.Placement),
                    _,
                    _,
                    _
                  ) =>
                true
              case _ => false
            }
          },
          *
        )
        .returningT(response(Activate(activateDate, offerBilling, List.empty)))
      (decider
        .apply(_: Request)(_: RequestContext))
        .when(
          argThat { request: Request =>
            request match {
              case Request(
                    _,
                    _,
                    ProductContext(ProductId.Placement),
                    _,
                    _,
                    _
                  ) =>
                false
              case _ => true
            }
          },
          *
        )
        .onCall { (request, _) =>
          deciderRandomResponse(request)
        }
    }

    protected def stubAlwaysUpdateGood(): Unit =
      (goodsDao.update _).when(*, *).returningT(())

    protected def deciderRandomResponse(request: GoodsDecider.Request) =
      // @TODO: TESTS
      Gen
        .oneOf(
          Gen.const(
            Success(response(Activate(activateDate, offerBilling, List.empty)))
          ),
          // don't care about exact reason here for now
          Gen.const(
            Success(
              response(
                Deactivate(InactiveClient, offerStatusPatch = Some(Expired))
              )
            )
          ),
          Gen.const(
            Success(response(NoAction(GetAdsRequestError(new TestException))))
          )
        )
        .next

    protected def response(action: GoodsDecider.Action) =
      Response(action, None, None)

    protected def whenPromocodeDecrease(
        featureInstanceId: FeatureInstanceId,
        featureCount: FeatureCount,
        response: Task[FeatureInstance]
    ) =
      (promocoder.changeFeatureCount _)
        .when(featureInstanceId, featureCount)
        .returning(response)
  }
}
