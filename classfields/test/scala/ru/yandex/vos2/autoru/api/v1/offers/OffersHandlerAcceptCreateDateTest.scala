package ru.yandex.vos2.autoru.api.v1.offers

import akka.http.scaladsl.model._
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.autoru.model.AutoruCommonLogic
import ru.yandex.vos2.autoru.utils.Vos2ApiSuite
import ru.yandex.vos2.autoru.utils.testforms._

/**
  * Created by andrey on 3/6/17.
  */
@RunWith(classOf[JUnitRunner])
class OffersHandlerAcceptCreateDateTest extends AnyFunSuite with Vos2ApiSuite {

  private val now = DateTime.now()

  for {
    isDealer <- Seq(true, false)
    category <- Seq("cars", "trucks", "moto")
  } test(s"accept create date (inVos = false, isDealer = $isDealer, category = $category)") {
    acceptCreateDateTests(isDealer, category)
  }

  //scalastyle:off method.length
  private def acceptCreateDateTests(isDealer: Boolean, category: String): Unit = {
    val formInfo1 = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))
    val userId1: String = formInfo1.form.getUserRef
    val now1 = ru.yandex.vos2.getNow

    // сохраняем новое с acceptCreateDate = false
    val req = Post(s"/api/v1/offers/$category/$userId1?accept_create_date=0&insert_new=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo1.json))
    val offerId = checkAcceptCreateDate(req, category, now1)

    // апдейтим с acceptCreateDate = false
    val formInfo2 = testFormGenerator.updateForm(formInfo1, TestFormParams(now = now))
    val req2 = Put(s"/api/v1/offers/$category/$userId1/$offerId?accept_create_date=0")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo2.json))
    checkAcceptCreateDate(req2, category, now1)

    // апдейтим с acceptCreateDate = true, но не передав createDate
    val req3 = Put(s"/api/v1/offers/$category/$userId1/$offerId?accept_create_date=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo2.json))
    checkAcceptCreateDate(req3, category, now1)

    // апдейтим с acceptCreateDate = true, передав createDate
    val creationDate = new DateTime(2017, 2, 28, 0, 0, 0)
    val formInfo4 =
      testFormGenerator.updateForm(formInfo2, TestFormParams(optCreateDate = Some(creationDate), now = now))
    val req4 = Put(s"/api/v1/offers/$category/$userId1/$offerId?accept_create_date=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo4.json))

    checkAcceptCreateDate(req4, category, now1, Some(creationDate))

    // сохраняем новое с acceptCreateDate = true, но не передав createDate
    val formInfo5 =
      testFormGenerator.createForm(category, TestFormParams(isDealer, now = now, excludeUserRefs = Seq(userId1)))
    val userId5: String = formInfo5.form.getUserRef
    val now2 = ru.yandex.vos2.getNow
    val req5 = Post(s"/api/v1/offers/$category/$userId5?accept_create_date=1&insert_new=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo5.json))
    checkAcceptCreateDate(req5, category, now2)

    // сохраняем новое с acceptCreateDate = true, передав createDate
    val formInfo6 = testFormGenerator.createForm(
      category,
      TestFormParams(isDealer, now = now, optCreateDate = Some(creationDate), excludeUserRefs = Seq(userId1, userId5))
    )
    val userId6: String = formInfo6.form.getUserRef
    val req6 = Post(s"/api/v1/offers/$category/$userId6?accept_create_date=1&insert_new=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo6.json))
    checkAcceptCreateDate(req6, category, now1, Some(creationDate))

    // сохраняем из черновика
    val formInfo7 = testFormGenerator.createForm(
      category,
      TestFormParams(
        isDealer,
        now = now,
        optCreateDate = Some(creationDate),
        excludeUserRefs = Seq(userId1, userId5, userId6)
      )
    )
    val userId7: String = formInfo7.form.getUserRef
    val draftId = checkSuccessRequest(
      Post(s"/api/v1/draft/$category/$userId7?accept_create_date=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo7.json))
    )
    val req8 = Post(s"/api/v1/draft/$category/$draftId/publish/$userId7?accept_create_date=1&insert_new=1")
    checkAcceptCreateDate(req8, category, now1, Some(creationDate))

  }

  private def checkAcceptCreateDate(req: HttpRequest,
                                    category: String,
                                    now: Long,
                                    optCreateDate: Option[DateTime] = None): String = {
    val offerId = checkSuccessRequest(req)
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    optCreateDate match {
      case Some(createDate) =>
        readForm.getAdditionalInfo.getCreationDate shouldBe createDate.getMillis
        readForm.getAdditionalInfo.getExpireDate shouldBe AutoruCommonLogic
          .expireDate(createDate)
          .getMillis
      case None =>
        readForm.getAdditionalInfo.getCreationDate shouldBe now +- 2000
        readForm.getAdditionalInfo.getExpireDate shouldBe AutoruCommonLogic
          .expireDate(now)
          .getMillis +- 2000
    }
    offerId
  }
}
