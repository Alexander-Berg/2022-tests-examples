package ru.yandex.vos2.autoru.api.v1.offer

import akka.http.scaladsl.model._
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuiteLike
import ru.auto.api.CommonModel
import ru.auto.api.CommonModel.{PriceInfo, RecallReason}
import ru.auto.api.RequestModel.{AttributeUpdateRequest, OfferHideRequest, PriceAttribute}
import ru.auto.api.TrucksModel.TruckCategory
import ru.yandex.vertis.baker.util.Protobuf.RichDateTime
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.dao.old.AutoruSalesDaoCommon
import ru.yandex.vos2.autoru.model.AutoruModelUtils.AutoruModelRichOffer
import ru.yandex.vos2.autoru.model._
import ru.yandex.vos2.autoru.utils.ApiFormUtils.RichPriceInfoOrBuilder
import ru.yandex.vos2.autoru.utils.Vos2ApiHandlerResponses._
import ru.yandex.vos2.autoru.utils.testforms.TestFormParams
import ru.yandex.vos2.autoru.utils.{ErrorResponse, RecallUtils, Vos2ApiSuite}
import ru.yandex.vos2.dao.offers.OfferUpdate
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder
import ru.yandex.vos2.model.{OfferRef, UserRef}
import ru.yandex.vos2.util.{ExternalAutoruUserRef, Protobuf, RandomUtil}

import java.sql.Timestamp
import java.util.concurrent.ThreadLocalRandom
import scala.jdk.CollectionConverters._
import scala.collection.mutable.ArrayBuffer

/**
  * Created by andrey on 11/21/16.
  */
//scalastyle:off number.of.methods
@RunWith(classOf[JUnitRunner])
class OfferHandlerStatusesTest extends AnyFunSuiteLike with Vos2ApiSuite with OptionValues {

  private val featuresManager = components.featuresManager
  private val autoruOfferDao = components.getOfferDao()

  private val now = new DateTime

  private val createDate = now.minusDays(1)
  private val recallDate = now.minusHours(1)
  // истекает через месяц минус два дня
  private val expireDate = now.plusMonths(1).minusDays(2)

  implicit private val t: Traced = Traced.empty

  for {
    category <- Seq("cars", "trucks", "moto")
    isDealer <- Seq(false, true)
  } test(s"statuses (category = $category, inVos = false, isDealer = $isDealer)") {
    testSet(category, isDealer)
  }

  //scalastyle:off method.length cyclomatic.complexity
  private def testSet(category: String, isDealer: Boolean): Unit = {
    // сохраняем новое объявление
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, optOwnerId = Some(24813)))
    val userRef = formInfo.userRef
    val userId = formInfo.optUser.map(_.id).getOrElse(0L)
    val extOwnerId: String = formInfo.extUserId
    val fullForm = formInfo.form
    val offerId = checkSuccessRequest(createRequest(Post(s"/api/v1/offers/$category/$extOwnerId?insert_new=1")) {
      _.mergeFrom(fullForm)
    })
    val newAutoruOfferID = AutoruOfferID.parse(offerId)

    // проверяем историю статусов
    val activeStatus: String = "CS_NEED_ACTIVATION"
    checkStatusHistory(category, offerId, (activeStatus, "Offer create from api"))

    // скрываем
    okHideRequest(category, newAutoruOfferID, Some(userRef))
    checkStatusHistory(
      category,
      offerId,
      (activeStatus, "Offer create from api"),
      ("CS_INACTIVE", "Offer hide from api")
    )

    // сначала объявление в скрытом состоянии
    // проверим сначала всякие ошибки

    // TODO: если без хеша, то ошибка

    // послали GET, вместо POST
    checkErrorRequest(
      Get(s"/api/v1/offer/$category/$offerId/activate"),
      StatusCodes.BadRequest,
      unsupportedMethod("POST")
    )

    // невалидная категория
    checkErrorRequest(Post(s"/api/v1/offer/blabla/$offerId/activate"), StatusCodes.NotFound, unknownHandler)

    // другая категория
    val wrongCategory: String = if (category == "cars") "trucks" else "cars"
    checkErrorRequest(Post(s"/api/v1/offer/$wrongCategory/$offerId/activate"), StatusCodes.NotFound, unknownOfferError)

    // сделаем объявление старше одного года по дате создания
    setCreateDate(category, now.minusDays(366), newAutoruOfferID)
    // и удалим recall reason
    removeRecallReason(newAutoruOfferID)
    // объявление с датой создания старше одного года можно активировать
    okActivateRequest(category, newAutoruOfferID, isDealer)
    // ставим дату отзыва на полгода назад
    setRecallDate(now.minusDays(181), newAutoruOfferID, userId)
    // объявление с датой снятия старше полугода можно активировать
    okActivateRequest(category, newAutoruOfferID, isDealer)
    // возвращаем recall_date
    setRecallDate(recallDate, newAutoruOfferID, userId)

    // пытаемся активировать, передав левого пользователя
    notOkPostRequest(
      category,
      "activate",
      newAutoruOfferID,
      Some(UserRef.refAid(1)),
      StatusCodes.NotFound,
      unknownOfferError
    )

    // активируем объявление, хотя у него и дата создания старше года,
    // но дата размещения меньше полугода, так что все ок
    okActivateRequest(category, newAutoruOfferID, isDealer)
    val activateStatus: String = if (isDealer) "CS_NEED_ACTIVATION" else "CS_ACTIVE"
    val statusHistory = ArrayBuffer(
      (activeStatus, "Offer create from api"),
      ("CS_INACTIVE", "Offer hide from api"),
      (activateStatus, "Offer activate from api")
    )
    checkStatusHistory(category, offerId, statusHistory.toSeq: _*)

    setCreateDate(category, createDate, newAutoruOfferID)

    // повторная активация НЕ возвращает ошибку
    okActivateRequest(category, newAutoruOfferID, isDealer)
    checkStatusHistory(category, offerId, statusHistory.toSeq: _*)

    // актуализация с левой категорией
    actualizeWrongCategoryRequest(wrongCategory, newAutoruOfferID)

    // после активации из скрытого состояния объявление в состоянии active,
    // и у него уже продлен expire_date, такое не продлится на три дня
    actualizeRequest(category, restoreExpireDate = true, prolong = false, newAutoruOfferID)

    // если до expire больше месяца - не продлеваем
    actualizeRequest(category, restoreExpireDate = false, prolong = false, newAutoruOfferID)

    // скрываем объявление, но не передали POST Data - скрываем все равно, с неизвестной причиной
    okHideRequest(category, newAutoruOfferID, postData = OfferHideRequest.newBuilder().build())
    statusHistory += (("CS_INACTIVE", "Offer hide from api"))
    checkStatusHistory(category, offerId, statusHistory.toSeq: _*)

    // успешно скрываем объявление еще разок. Повторное скрытие НЕ возвращает ошибку
    okHideRequest(category, newAutoruOfferID)
    checkStatusHistory(category, offerId, statusHistory.toSeq: _*)

    // скрытое объявление актуализируется, но не продлевается на три дня
    actualizeRequest(category, restoreExpireDate = true, prolong = false, newAutoruOfferID)

    // снова активируем объявление
    okActivateRequest(category, newAutoruOfferID, isDealer)
    statusHistory += ((activateStatus, "Offer activate from api"))
    checkStatusHistory(category, offerId, statusHistory.toSeq: _*)

    makeNeedActivation(category, newAutoruOfferID)
    if (activateStatus != "CS_NEED_ACTIVATION") statusHistory += (("CS_NEED_ACTIVATION", "test"))
    checkStatusHistory(category, offerId, statusHistory.toSeq: _*)

    // статус need_activation, в этом статусе также успешно можем скрыть
    okHideRequest(category, newAutoruOfferID)
    statusHistory += (("CS_INACTIVE", "Offer hide from api"))
    checkStatusHistory(category, offerId, statusHistory.toSeq: _*)

    // делаем активным для дальнейших тестов
    okActivateRequest(category, newAutoruOfferID, isDealer)
    statusHistory += ((activateStatus, "Offer activate from api"))
    checkStatusHistory(category, offerId, statusHistory.toSeq: _*)

    // попробуем скрыть с проверкой пользователя. Сначала передаем левого
    notOkHideRequest(category, newAutoruOfferID, Some(UserRef.refAid(1)), StatusCodes.NotFound, unknownSaleError)

    // теперь нормального
    okHideRequest(category, newAutoruOfferID, Some(userRef))
    statusHistory += (("CS_INACTIVE", "Offer hide from api"))
    checkStatusHistory(category, offerId, statusHistory.toSeq: _*)

    // пытаемся удалить, передав левого пользователя
    notOkDeleteRequest(category, "", newAutoruOfferID, Some(UserRef.refAid(1)), StatusCodes.NotFound, unknownSaleError)

    // удалим объявление
    okDeleteRequest(
      category,
      req = "",
      newAutoruOfferID,
      needStatus = if (isDealer) {
        Seq(OfferFlag.OF_INACTIVE, OfferFlag.OF_NEED_ACTIVATION, OfferFlag.OF_DELETED)
      } else {
        Seq(OfferFlag.OF_INACTIVE, OfferFlag.OF_DELETED)
      },
      needExternalStatus = CompositeStatus.CS_REMOVED
    )

    // повторное удаление не проходит
    notOkDeleteRequest(category, req = "", newAutoruOfferID)

    // удаленное объявление нельзя скрыть, забанить, разбанить
    notOkHideRequest(category, newAutoruOfferID, None, StatusCodes.Conflict, updateStatusError)
    // удаленное в vos объявление не актуализируется и не продлевается на три дня
    actualizeDeletedRequest(category, newAutoruOfferID)

    // но вот активировать удаленное таки можно
    okActivateRequest(category, newAutoruOfferID, isDealer)

    // проверим апи /skip-need-activation
    makeNeedActivation(category, newAutoruOfferID)
    okPostRequest(category, "skip-need-activation", newAutoruOfferID, Seq(), CompositeStatus.CS_ACTIVE)
    // если старая база, проверяем, что статус AutoruSaleStatus.STATUS_SHOW (0)
    assert(statusFromOldDb(category, newAutoruOfferID) == AutoruSaleStatus.STATUS_SHOW)

    // активируем объявление в состоянии need_activation.
    // Оно остается в статусе need_activation и expire_date не продлевается
    makeNeedActivation(category, newAutoruOfferID)
    okActivateRequestOnNeedActivation(category, newAutoruOfferID)
  }

  private def makeNeedActivation(category: String, offerId: AutoruOfferID): Unit = {
    autoruOfferDao.useOfferID(offerId, comment = "test") { offer =>
      OfferUpdate.visitNow(offer.toBuilder.clearFlag().addFlag(OfferFlag.OF_NEED_ACTIVATION).build())
    }
    getDao(category).setStatus(
      offerId.id,
      expectedStatuses = Seq.empty,
      newStatus = AutoruSaleStatus.STATUS_WAITING_ACTIVATION
    )
  }

  private def getDao(category: String): AutoruSalesDaoCommon[_ <: MigratableOffer[_]] = {
    category match {
      case "cars" => components.autoruSalesDao
      case "trucks" => components.autoruTrucksDao
      case "moto" => components.autoruMotoDao
      case _ => sys.error("unexpected category")
    }
  }

  private def setCreateDate(category: String, date: DateTime, offerId: AutoruOfferID): Unit = {
    autoruOfferDao.useOfferID(offerId) { offer =>
      OfferUpdate.visitNow(offer.toBuilder.setTimestampCreate(date.getMillis).build())
    }
    val dao = getDao(category)
    dao.shard.master.jdbc.update(
      s"update ${dao.salesTable} set create_date = ? where id = ?",
      new Timestamp(date.getMillis),
      Long.box(offerId.id)
    )
  }

  private def removeRecallReason(offerId: AutoruOfferID): Unit = {
    autoruOfferDao.useOfferID(offerId) { offer =>
      val builder: Offer.Builder = offer.toBuilder
      builder.getOfferAutoruBuilder.clearRecallInfo()
      OfferUpdate.visitNow(builder.build())
    }
    components.oldOfficeDatabase.master.jdbc
      .update("delete from users.reason_archive_users where sale_id = ?", Long.box(offerId.id))
  }

  private def setRecallDate(date: DateTime, offerId: AutoruOfferID, userId: Long): Unit = {
    autoruOfferDao.useOfferID(offerId) { offer =>
      val builder: Offer.Builder = offer.toBuilder
      builder.getOfferAutoruBuilder.getRecallInfoBuilder
        .setReason(RecallReason.SOLD_ON_AUTORU)
        .setManyCalls(false)
        .setRecallTimestamp(date.getMillis)
      OfferUpdate.visitNow(builder.build())
    }
    components.oldOfficeDatabase.master.jdbc.update(
      "replace into users.reason_archive_users " +
        "(sale_id, user_id, reason_id, many_calls, date_update) VALUES (?, ?, ?, ?, ?)",
      Long.box(offerId.id),
      Long.box(userId),
      Int.box(1),
      Int.box(0),
      new Timestamp(date.getMillis)
    )
  }

  private def randomRecallReason: OfferHideRequest = {
    val r = ThreadLocalRandom.current()
    OfferHideRequest
      .newBuilder()
      .setReason(CommonModel.RecallReason.forNumber(r.nextInt(2, 7)))
      .setManySpamCalls(r.nextBoolean())
      .setSoldPrice(r.nextInt(1000, 1000000))
      .build()
  }

  private def resetSetDate(category: String, offerId: AutoruOfferID): Unit = {
    // делаем set_date очень старым, чтобы убедиться, что она обновляется при обновлениях статуса
    val dao = getDao(category)
    dao.shard.master.jdbc
      .update(s"update ${dao.salesTable} set set_date = '2000-01-01 00:00:00' where id = ?", Long.box(offerId.id))
  }

  private def setExpireDate(category: String, date: DateTime, offerId: AutoruOfferID): Unit = {
    val dao = getDao(category)
    dao.shard.master.jdbc.update(
      s"update ${dao.salesTable} set expire_date = ? where id = ?",
      new Timestamp(date.getMillis),
      Long.box(offerId.id)
    )
    autoruOfferDao.useOfferID(offerId, includeRemoved = true) { offer =>
      OfferUpdate.visitNow(offer.toBuilder.setTimestampWillExpire(date.getMillis).build())
    }
  }

  private def getOffer(category: String, offerId: AutoruOfferID): Offer = {
    checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/${offerId.toPlain}/proto?include_removed=1"))
  }

  private def getOfferFromOldDb(category: String, offerId: AutoruOfferID): Offer = {
    category match {
      case "cars" =>
        val sale = components.autoruSalesDao.getOffer(offerId.id).value
        components.carOfferConverter.convertStrict(sale, None).converted.value
      case "trucks" =>
        val sale = components.autoruTrucksDao.getOffer(offerId.id).value
        components.truckOfferConverter.convertStrict(sale, None).converted.value
      case "moto" =>
        val sale = components.autoruMotoDao.getOffer(offerId.id).value
        components.motoOfferConverter.convertStrict(sale, None).converted.value
      case _ => sys.error("unexpected category")
    }
  }

  private def getOfferFromVos(offerId: AutoruOfferID): Offer = {
    autoruOfferDao.findById(offerId.toPlain, includeRemoved = true).value
  }

  private def statusFromOldDb(category: String, offerId: AutoruOfferID): AutoruSaleStatus = {
    val dao = getDao(category)
    dao.getOffer(offerId.id).value.status
  }

  private def getAutoruOffer(category: String, offerId: AutoruOfferID): AutoruOffer = {
    getOffer(category, offerId).getOfferAutoru
  }

  private def okActivateRequest(category: String,
                                offerId: AutoruOfferID,
                                isDealer: Boolean,
                                checkStatusHistory: Boolean = false,
                                needProlong: Boolean = true): Unit = {
    val (needStatus, needExternalStatus, autoruSaleStatus) = if (isDealer) {
      (
        Seq(OfferFlag.OF_NEED_ACTIVATION),
        CompositeStatus.CS_NEED_ACTIVATION,
        AutoruSaleStatus.STATUS_WAITING_ACTIVATION
      )
    } else {
      (Seq(), CompositeStatus.CS_ACTIVE, AutoruSaleStatus.STATUS_SHOW)
    }
    val offer0 = getOffer(category, offerId)
    if (isDealer) {
      // не меняем expire_date если статус == WAITING_ACTIVATION, который ставится для дилеров в легковых
      okPostRequest(category, "activate", offerId, needStatus, needExternalStatus, checkStatusHistory)
      val offer = getOffer(category, offerId)
      assert(offer0.getTimestampWillExpire == offer.getTimestampWillExpire)
    } else {
      okPostRequest(category, "activate", offerId, needStatus, needExternalStatus, checkStatusHistory)
      val offer = getOffer(category, offerId)
      if (needProlong) {
        offer.getTimestampWillExpire > DateTime.now().plusMonths(1).getMillis
      } else {
        withClue("Unexpected offer expire date (should not be prolonged)") {
          assert(offer.getTimestampWillExpire == offer0.getTimestampWillExpire)
        }
      }
    }
    // если старая база, проверяем, что статус AutoruSaleStatus.STATUS_SHOW (0)
    assert(statusFromOldDb(category, offerId) == autoruSaleStatus)
  }

  private def okActivateRequestOnNeedActivation(category: String,
                                                offerId: AutoruOfferID,
                                                checkStatusHistory: Boolean = false): Unit = {
    val oldOffer = getOffer(category, offerId)
    okPostRequest(
      category,
      "activate",
      offerId,
      Seq(OfferFlag.OF_NEED_ACTIVATION),
      CompositeStatus.CS_NEED_ACTIVATION,
      checkStatusHistory
    )
    val offer = getOffer(category, offerId)
    assert(offer.getTimestampWillExpire == oldOffer.getTimestampWillExpire, "expire_date не должно измениться")
  }

  private def actualizeRequest(category: String,
                               restoreExpireDate: Boolean,
                               prolong: Boolean,
                               offerId: AutoruOfferID): Unit = {
    actualizeRequestOldDb(category, restoreExpireDate, prolong, offerId)
  }

  private def actualizeRequestOldDb(category: String,
                                    restoreExpireDate: Boolean,
                                    prolong: Boolean,
                                    offerId: AutoruOfferID): Unit = {
    if (restoreExpireDate) setExpireDate(category, expireDate, offerId)

    val oldSaleOffer0 = getOfferFromOldDb(category, offerId)
    val vosOffer0 = getOfferFromVos(offerId)

    checkSimpleSuccessRequest(Post(s"/api/v1/offer/$category/${offerId.toPlain}/actualize"))

    val oldSaleOffer = getOfferFromOldDb(category, offerId)
    val vosOffer = getOfferFromVos(offerId)
    // дата акутализации - всегда в базе vos, продлевается у НЕУДАЛЕННОГО объявления в любом статусе
    assert(System.currentTimeMillis() - vosOffer.getTimestampTtlStart < 10000)

    if (prolong) {
      // если старая база, продлеваем также в ней, в новой продлеваем всегда
      assert(System.currentTimeMillis() - oldSaleOffer.getTimestampAnyUpdate < 10000)
      assert(
        oldSaleOffer.getTimestampWillExpire ==
          new DateTime(oldSaleOffer0.getTimestampWillExpire).plusDays(3).getMillis
      )
      assert(System.currentTimeMillis() - vosOffer.getTimestampAnyUpdate < 10000)
      assert(
        vosOffer.getTimestampWillExpire ==
          new DateTime(vosOffer0.getTimestampWillExpire).plusDays(3).getMillis
      )
    }
  }

  private def actualizeRequestVos(category: String,
                                  restoreExpireDate: Boolean,
                                  prolong: Boolean,
                                  offerId: AutoruOfferID): Unit = {
    if (restoreExpireDate) setExpireDate(category, expireDate, offerId)

    val vosOffer0 = getOfferFromVos(offerId)

    checkSimpleSuccessRequest(Post(s"/api/v1/offer/$category/${offerId.toPlain}/actualize"))

    val vosOffer = getOfferFromVos(offerId)
    // дата акутализации - всегда в базе vos, продлевается у НЕУДАЛЕННОГО объявления в любом статусе
    assert(System.currentTimeMillis() - vosOffer.getTimestampTtlStart < 10000)

    if (prolong) {
      assert(System.currentTimeMillis() - vosOffer.getTimestampAnyUpdate < 10000)
      assert(
        vosOffer.getTimestampWillExpire ==
          new DateTime(vosOffer0.getTimestampWillExpire).plusDays(3).getMillis
      )
    }
  }

  private def actualizeDeletedRequest(category: String, offerId: AutoruOfferID): Unit = {
    setExpireDate(category, expireDate, offerId)

    val req: HttpRequest = Post(s"/api/v1/offer/$category/${offerId.toPlain}/actualize")
    checkErrorRequest(req, StatusCodes.NotFound, unknownOfferError)

    val vosOffer = getOfferFromVos(offerId)

    // дата акутализации - всегда в базе vos, продлевается у НЕУДАЛЕННОГО объявления в любом статусе
    assert(System.currentTimeMillis() - vosOffer.getTimestampTtlStart < 10000)
    assert(System.currentTimeMillis() - vosOffer.getTimestampAnyUpdate < 10000)
  }

  private def actualizeWrongCategoryRequest(category: String, offerId: AutoruOfferID): Unit = {
    setExpireDate(category, expireDate, offerId)

    val req: HttpRequest = Post(s"/api/v1/offer/$category/${offerId.toPlain}/actualize")
    checkErrorRequest(req, StatusCodes.NotFound, unknownOfferError)

    val vosOffer = getOfferFromVos(offerId)

    // дата акутализации - всегда в базе vos, продлевается у НЕУДАЛЕННОГО объявления в любом статусе
    assert(System.currentTimeMillis() - vosOffer.getTimestampTtlStart < 10000)
    assert(System.currentTimeMillis() - vosOffer.getTimestampAnyUpdate < 10000)
  }

  private def okHideRequest(category: String,
                            offerId: AutoruOfferID,
                            optUserRef: Option[UserRef] = None,
                            postData: OfferHideRequest = randomRecallReason,
                            doNotCheckRecallInfo: Boolean = false,
                            checkStatusHistory: Boolean = true): Unit = {
    resetSetDate(category, offerId)

    val postDataStr = Protobuf.toJson(postData)

    val request = createRequest(category, "hide", optUserRef, offerId)

    val req = Post(request).withEntity(HttpEntity(ContentTypes.`application/json`, postDataStr))
    checkSimpleSuccessRequest(req)

    val offer0 = getOffer(category, offerId)
    val needFlags = (offer0.getFlagList.asScala.toSet + OfferFlag.OF_INACTIVE).toSeq

    checkOfferUpdated(category, offerId, needFlags, CompositeStatus.CS_INACTIVE, checkStatusHistory)

    val autoruOffer = getAutoruOffer(category, offerId)
    if (!doNotCheckRecallInfo) {
      assert(
        RecallUtils.getIdByReason(autoruOffer.getRecallInfo.getReason) == RecallUtils.getIdByReason(postData.getReason)
      )
      assert(autoruOffer.getRecallInfo.getManyCalls == postData.getManySpamCalls)
      assert(autoruOffer.getRecallInfo.getSoldPrice == postData.getSoldPrice)
      assert(System.currentTimeMillis() - autoruOffer.getRecallInfo.getRecallTimestamp < 20000)
    }
  }

  private def notOkHideRequest(category: String,
                               offerId: AutoruOfferID,
                               optUserRef: Option[UserRef] = None,
                               status: StatusCode = StatusCodes.Conflict,
                               errorResponse: ErrorResponse = updateStatusError): Unit = {
    resetSetDate(category, offerId)
    val offer0 = getOffer(category, offerId)
    val offer0ExternalStatus = offer0.getAutoruCompositeStatus
    val offer0TimestampAnyUpdate: Long = offer0.getTimestampAnyUpdate
    val postData = randomRecallReason

    val postDataStr = Protobuf.toJson(postData)

    val request = Post(createRequest(category, "hide", optUserRef, offerId))
      .withEntity(HttpEntity(ContentTypes.`application/json`, postDataStr))

    checkErrorRequest(request, status, errorResponse)

    checkOfferNotUpdated(
      category,
      offerId,
      offer0.getFlagList.asScala.toSeq,
      offer0ExternalStatus,
      offer0TimestampAnyUpdate
    )

    val offer = getAutoruOffer(category, offerId)
    assert(offer.getRecallInfo == offer0.getOfferAutoru.getRecallInfo)
  }

  private def okDeleteRequest(category: String,
                              req: String,
                              offerId: AutoruOfferID,
                              needStatus: Seq[OfferFlag],
                              needExternalStatus: CompositeStatus,
                              optUserRef: Option[UserRef] = None,
                              checkStatusHistory: Boolean = true): Unit = {
    // обнулим set_date перед запросом чтобы проще было его проверить
    resetSetDate(category, offerId)

    val request = createRequest(category, req, optUserRef, offerId)

    checkSimpleSuccessRequest(Delete(request))

    checkOfferUpdated(category, offerId, needStatus, needExternalStatus, checkStatusHistory)
  }

  private def notOkDeleteRequest(category: String,
                                 req: String = "",
                                 offerId: AutoruOfferID,
                                 optUserRef: Option[UserRef] = None,
                                 status: StatusCode = StatusCodes.Conflict,
                                 errorResponse: ErrorResponse = updateStatusError): Unit = {
    // обнулим set_date перед запросом чтобы проще было его проверить
    resetSetDate(category, offerId)
    val offer0 = getOffer(category, offerId)
    val offer0ExternalStatus = offer0.getAutoruCompositeStatus
    val offer0TimestampAnyUpdate: Long = offer0.getTimestampAnyUpdate

    val request = createRequest(category, req, optUserRef, offerId)

    checkErrorRequest(Delete(request), status, errorResponse)

    checkOfferNotUpdated(
      category,
      offerId,
      offer0.getFlagList.asScala.toSeq,
      offer0ExternalStatus,
      offer0TimestampAnyUpdate
    )
  }

  private def createRequest(category: String,
                            req: String,
                            optUserRef: Option[UserRef],
                            offerId: AutoruOfferID): String = {
    optUserRef match {
      case Some(userRef) =>
        s"/api/v1/offer/$category/${ExternalAutoruUserRef.fromUserRef(userRef).get}/${offerId.toPlain}/$req"
      case None =>
        s"/api/v1/offer/$category/${offerId.toPlain}/$req"
    }
  }

  private def okPostRequest(category: String,
                            req: String,
                            offerId: AutoruOfferID,
                            needStatus: Seq[OfferFlag],
                            needExternalStatus: CompositeStatus,
                            checkStatusHistory: Boolean = true): Unit = {
    // обнулим set_date перед запросом чтобы проще было его проверить
    resetSetDate(category, offerId)

    checkSimpleSuccessRequest(Post(s"/api/v1/offer/$category/${offerId.toPlain}/$req"))

    checkOfferUpdated(category, offerId, needStatus, needExternalStatus, checkStatusHistory)
  }

  private def notOkPostRequest(category: String,
                               req: String,
                               offerId: AutoruOfferID,
                               optUserRef: Option[UserRef] = None,
                               status: StatusCode = StatusCodes.Conflict,
                               errorResponse: ErrorResponse = updateStatusError): Unit = {
    resetSetDate(category, offerId)
    val offer0 = getOffer(category, offerId)
    val offer0ExternalStatus = offer0.getAutoruCompositeStatus

    val offer0TimestampAnyUpdate: Long = offer0.getTimestampAnyUpdate

    val request = createRequest(category, req, optUserRef, offerId)

    checkErrorRequest(Post(request), status, errorResponse)

    checkOfferNotUpdated(
      category,
      offerId,
      offer0.getFlagList.asScala.toSeq,
      offer0ExternalStatus,
      offer0TimestampAnyUpdate
    )
  }

  private def checkOfferUpdated(category: String,
                                offerId: AutoruOfferID,
                                needStatus: Seq[OfferFlag],
                                needExternalStatus: CompositeStatus,
                                checkStatusHistory: Boolean): Unit = {
    val offer = getOffer(category, offerId)
    val offerExternalStatus = offer.getAutoruCompositeStatus
    val offerFlags = offer.getFlagList.asScala.filterNot(_ == OfferFlag.OF_MIGRATED).sortBy(_.getNumber)
    val needFlags = needStatus.filterNot(_ == OfferFlag.OF_MIGRATED).sortBy(_.getNumber)
    assert(offerFlags == needFlags)
    assert(offerExternalStatus == needExternalStatus)
    assert(System.currentTimeMillis() - offer.getTimestampAnyUpdate < 10000)
    if (checkStatusHistory) {
      assert(offer.getStatusHistoryCount > 0)
    }
  }

  private def checkOfferNotUpdated(category: String,
                                   offerId: AutoruOfferID,
                                   needStatus: Seq[OfferFlag],
                                   needExternalStatus: CompositeStatus,
                                   needTimestampAnyUpdate: Long): Unit = {
    val offer = getOffer(category, offerId)
    val offerExternalStatus = offer.getAutoruCompositeStatus
    val offerFlags = offer.getFlagList.asScala.filterNot(_ == OfferFlag.OF_MIGRATED).sortBy(_.getNumber)
    val needFlags = needStatus.filterNot(_ == OfferFlag.OF_MIGRATED).sortBy(_.getNumber)
    assert(offerFlags == needFlags)
    assert(offerExternalStatus == needExternalStatus)
    assert(offer.getTimestampAnyUpdate == needTimestampAnyUpdate)
  }

  private def updateDiscountOptionsAttribute(category: String): Unit = {
    // создаем объявление
    val formBuilder = testFormGenerator
      .createForm(category, TestFormParams(now = now, excludeTruckCategories = Some(Seq(TruckCategory.LCV))))
      .form
      .toBuilder
    // задаем скидку
    val tradeInDiscount1 = RandomUtil.nextInt(50000, 150000)
    val tradeInDiscount2 = RandomUtil.nextInt(200000, 300000)
    formBuilder.getDiscountOptionsBuilder.setTradein(tradeInDiscount1)
    val form = formBuilder.build()
    val extUserId: String = form.getUserRef
    // передаем параметр пытаемся сохранить. Должен быть успех
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )
    // попробуем прочитать и проверить скидки
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId"))

    // для комтранса сбрасываем скидку
    if (category == "trucks") assert(readForm.getDiscountOptions.getTradein == 0)
    else assert(readForm.getDiscountOptions.getTradein == tradeInDiscount1)

    //пробуем изменить скидку в trade-in
    val attrReq = AttributeUpdateRequest.newBuilder()
    attrReq.getDiscountOptionsBuilder.setTradein(tradeInDiscount2)
    if (category == "trucks") {
      checkValidationErrorRequest(
        Put(s"/api/v1/offer/$category/$extUserId/$offerId/attribute")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(attrReq.build()))),
        invalidDiscountOptionsError
      )
    } else {
      checkSimpleSuccessRequest(
        Put(s"/api/v1/offer/$category/$extUserId/$offerId/attribute")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(attrReq.build())))
      )
    }

    //читаем повторно и проверяем
    val readForm2 = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId"))
    if (category == "trucks") assert(readForm2.getDiscountOptions.getTradein == 0)
    else assert(readForm2.getDiscountOptions.getTradein == tradeInDiscount2)
  }

  private def updatePriceAttribute(category: String, inVos: Boolean): Unit = {
    // создаем объявление
    val formBuilder = testFormGenerator.createForm(category, TestFormParams(now = now)).form.toBuilder
    // задаем цены
    formBuilder.setPriceInfo(PriceInfo.newBuilder().setCurrency("RUR").setPrice(200000))
    val form = formBuilder.build()
    val extUserId: String = form.getUserRef
    // передаем параметр пытаемся сохранить. Должен быть успех
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )
    // попробуем прочитать и проверить цены
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId"))
    assert(readForm.getPriceInfo.getCurrency == "RUR")
    assert(readForm.getPriceInfo.selectPrice == 200000)

    // пробуем поменять цену - ошибка валидации
    val attrReq0 = AttributeUpdateRequest
      .newBuilder()
      .setPrice(
        PriceAttribute
          .newBuilder()
          .setCurrency("RUR")
          .setPrice(1000)
      )
      .build()
    checkValidationErrorRequest(
      Put(s"/api/v1/offer/$category/$extUserId/$offerId/attribute")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(attrReq0))),
      invalidOfferPriceError
    )

    //пробуем изменить цену
    val attrReq = AttributeUpdateRequest
      .newBuilder()
      .setPrice(
        PriceAttribute
          .newBuilder()
          .setCurrency("USD")
          .setPrice(300000)
      )
      .build()
    checkSimpleSuccessRequest(
      Put(s"/api/v1/offer/$category/$extUserId/$offerId/attribute")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(attrReq)))
    )

    //читаем повторно и проверяем
    val readForm2 = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId"))
    assert(readForm2.getPriceInfo.getCurrency == "USD")
    assert(readForm2.getPriceInfo.selectPrice == 300000)
    assert(readForm2.getPriceHistoryCount == 2)
    assert(readForm2.getPriceHistory(0).selectPrice == 200000)
    assert(readForm2.getPriceHistory(0).getCurrency == "RUR")
    assert(readForm2.getPriceHistory(1).selectPrice == 300000)
    assert(readForm2.getPriceHistory(1).getCurrency == "USD")
    //пробуем записать уже установленную цену повторно
    val attrReq2 = AttributeUpdateRequest
      .newBuilder()
      .setPrice(
        PriceAttribute
          .newBuilder()
          .setCurrency("USD")
          .setPrice(300000)
      )
      .build()
    val req2 = Put(s"/api/v1/offer/$category/$extUserId/$offerId/attribute")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(attrReq2)))

    val error = if (inVos) failedToUpdatePriceError else failedToUpdatePriceInOldDbError
    checkErrorRequest(req2, StatusCode.int2StatusCode(409), error)

    //ничего не должно изменится
    val readForm3 = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId"))
    assert(readForm3.getPriceInfo.getCurrency == "USD")
    assert(readForm3.getPriceInfo.selectPrice == 300000)
    assert(readForm3.getPriceHistoryCount == 2)
    assert(readForm3.getPriceHistory(0).selectPrice == 200000)
    assert(readForm3.getPriceHistory(0).getCurrency == "RUR")
    assert(readForm3.getPriceHistory(1).selectPrice == 300000)
    assert(readForm3.getPriceHistory(1).getCurrency == "USD")
  }

  test("activate offer without phone") {
    // создаем объявление
    val formInfo = testFormGenerator.createForm("cars", TestFormParams(now = now))
    val formBuilder = formInfo.form.toBuilder
    val form = formBuilder.build()
    val extUserId: String = form.getUserRef
    // передаем параметр пытаемся сохранить. Должен быть успех
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/cars/$extUserId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )
    val ref = OfferRef.ref(formInfo.userRef, offerId)
    components.getOfferDao().useRef(ref) { offer =>
      val offerBuilder = offer.toBuilder
      offerBuilder.getOfferAutoruBuilder.getSellerBuilder.clearPhone()
      offerBuilder.putFlag(OfferFlag.OF_INACTIVE)
      OfferUpdate.visitNow(offerBuilder.build())
    }

    val req = Post(s"/api/v1/offer/cars/$offerId/activate")
    checkErrorRequest(req, StatusCode.int2StatusCode(409), noPhoneOfferError)

  }

  test("check activation status on offer without phone") {
    // создаем объявление
    val formInfo = testFormGenerator.createForm("cars", TestFormParams(now = now))
    val formBuilder = formInfo.form.toBuilder
    val form = formBuilder.build()
    val extUserId: String = form.getUserRef
    // передаем параметр пытаемся сохранить. Должен быть успех
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/cars/$extUserId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )
    val ref = OfferRef.ref(formInfo.userRef, offerId)
    components.getOfferDao().useRef(ref) { offer =>
      val offerBuilder = offer.toBuilder
      offerBuilder.getOfferAutoruBuilder.getSellerBuilder.clearPhone()
      offerBuilder.putFlag(OfferFlag.OF_INACTIVE)
      OfferUpdate.visitNow(offerBuilder.build())
    }

    val req = Post(s"/api/v1/offer/cars/$offerId/activate/check")
    checkErrorRequest(req, StatusCode.int2StatusCode(409), noPhoneOfferError)

  }

  test(s"do not prolong paid offer on activation (invos = false)") {
    val category = "cars"
    // создаем объявление как бы неделю назад
    val formInfo = testFormGenerator.createForm(category, TestFormParams(now = now, isDealer = false))
    val formBuilder = formInfo.form.toBuilder
    formBuilder.getAdditionalInfoBuilder.setCreationDate(DateTime.now.minusDays(7).getMillis)
    val form = formBuilder.build()
    val extUserId: String = form.getUserRef

    info("Create")
    val offer = checkSuccessRequestWithOffer(
      Post(
        s"/api/v1/offers/$category/$extUserId?" +
          s"insert_new=1&accept_create_date=1"
      ).withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )

    val offerId: String = offer.getId
    val newAutoruOfferID = AutoruOfferID.parse(offerId)

    // объявление платное
    val req1 = createAddServicesRequest("all_sale_add", active = true)
    checkSimpleSuccessRequest(
      Put(s"/api/v1/offer/$category/$extUserId/$offerId/services?source=ios")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(req1)))
    )

    info("Hide")
    // скрываем
    okHideRequest(category, newAutoruOfferID)

    info("Activate")
    // пытаемся активировать
    okActivateRequest(category, newAutoruOfferID, isDealer = false, needProlong = false)
  }

  private def bookingAllowedRequest(category: String, ownerId: String, offerId: String, allowed: Boolean) =
    Put(s"/api/v1/offer/$category/$ownerId/$offerId/booking_allowed?allowed=$allowed")

  test("booking allowed handler with dealer offer") {
    val isDealer = true
    val category = "cars"
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer, optOwnerId = Some(24813)))
    val extOwnerId: String = formInfo.extUserId
    val fullForm = formInfo.form
    val request = createRequest(Post(s"/api/v1/offers/$category/$extOwnerId?insert_new=1"))(_.mergeFrom(fullForm))
    val offerId = checkSuccessRequest(request)
    val autoruOfferID = AutoruOfferID.parse(offerId)
    okActivateRequest(category, autoruOfferID, isDealer)

    // Enable booking
    checkSimpleSuccessRequest(bookingAllowedRequest(category, extOwnerId, offerId, allowed = true))
    assert(getOfferFromVos(autoruOfferID).getOfferAutoru.getBooking.getAllowed)

    // Disable booking
    checkSimpleSuccessRequest(bookingAllowedRequest(category, extOwnerId, offerId, allowed = false))
    assert(!getOfferFromVos(autoruOfferID).getOfferAutoru.getBooking.getAllowed)
  }

  test("booking allowed handler with private owners offer") {
    val isDealer = false
    val category = "cars"
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer, optOwnerId = Some(24813)))
    val extOwnerId: String = formInfo.extUserId
    val fullForm = formInfo.form
    val request = createRequest(Post(s"/api/v1/offers/$category/$extOwnerId?insert_new=1"))(_.mergeFrom(fullForm))
    val offerId = checkSuccessRequest(request)
    checkErrorRequest(
      bookingAllowedRequest(category, extOwnerId, offerId, allowed = true),
      StatusCodes.Forbidden,
      forbiddenBooking
    )
  }

  test("booking allowed handler with not founded offer") {
    checkErrorRequest(
      bookingAllowedRequest("cars", "dealer:123", "123-hash123", allowed = true),
      StatusCodes.NotFound,
      unknownOfferError
    )
  }

  test("hide with reactivation delay") {
    // создаем объявление
    val formInfo = testFormGenerator.createForm("cars", TestFormParams(now = now))
    val formBuilder = formInfo.form.toBuilder
    val form = formBuilder.build()
    val extUserId: String = form.getUserRef
    // передаем параметр пытаемся сохранить. Должен быть успех
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/cars/$extUserId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )
    val ref = OfferRef.ref(formInfo.userRef, offerId)
    components.getOfferDao().useRef(ref) { offer =>
      val offerBuilder = offer.toBuilder
      offerBuilder.getOfferAutoruBuilder.getSellerBuilder.clearPhone()
      offerBuilder.putFlag(OfferFlag.OF_INACTIVE)
      OfferUpdate.visitNow(offerBuilder.build())
    }

    val dt = DateTime.now().toProtobufTimestamp
    val postData = OfferHideRequest.newBuilder()
    postData.setReactivateAt(dt)

    okHideRequest(category = "cars", AutoruOfferID.parse(offerId), postData = postData.build())

    val offer = components.getOfferDao().findById(offerId)
    assert(offer.get.getOfferAutoru.getReactivationData.getReactivateAt == dt)
  }
}
