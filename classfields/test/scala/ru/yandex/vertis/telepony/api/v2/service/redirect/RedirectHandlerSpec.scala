package ru.yandex.vertis.telepony.api.v2.service.redirect

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{ContentTypes, StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import org.mockito.Mockito
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eql}
import ru.yandex.vertis.telepony.api.DomainExceptionHandler._
import ru.yandex.vertis.telepony.api.RouteTest
import ru.yandex.vertis.telepony.api.v2.service.redirect.RedirectHandlerSpec._
import ru.yandex.vertis.telepony.api.v2.view.json.redirect.{DeletedRedirectsView, PhoneStatsView, UpdateOptionsRequestView}
import ru.yandex.vertis.telepony.exception.MtsException
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.json.redirect.{ActualRedirectView, CreateRequestV2View}
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.RedirectServiceV2.AvailableRequest.{GeoIdAvailableRequest, PhoneAvailableRequest}
import ru.yandex.vertis.telepony.service.RedirectServiceV2._
import ru.yandex.vertis.telepony.service.impl.RedirectCreationServiceImpl
import ru.yandex.vertis.telepony.service.{OneTimeRedirectService, RedirectCreationService, RedirectServiceV2}
import ru.yandex.vertis.telepony.util.Page
import ru.yandex.vertis.telepony.util.sliced.SlicedResult

import scala.concurrent.Future

/**
  * @author evans
  */
class RedirectHandlerSpec extends RouteTest with MockitoSupport with ScalaCheckDrivenPropertyChecks {

  val redirect: ActualRedirect = ActualRedirectGen.next

  def createHandler(redirectServiceV2: RedirectServiceV2, creationService: RedirectCreationService): Route = seal(
    new RedirectHandler {
      override def redirectService: RedirectServiceV2 = redirectServiceV2
      override def redirectCreationService: RedirectCreationService = creationService
      override def oneTimeRedirectService: OneTimeRedirectService = mock[OneTimeRedirectService]
    }.route
  )

  "RedirectHandler" should {
    "list redirects" in {
      val mockRS = mock[RedirectServiceV2]
      val mockRCS = mock[RedirectCreationService]
      val handler = createHandler(mockRS, mockRCS)
      val page = Page(0, 10)
      val target = PhoneGen.next
      when(mockRS.list(eq(Filter.ByTarget(target)), eq(page))(?))
        .thenReturn(Future.successful(SlicedResult(Iterable(redirect), 1, page)))

      val uri = Uri./.withQuery(Query("target" -> target.value))
      Get(uri) ~> handler ~> check {
        import ActualRedirectView._
        responseAs[SlicedResult[ActualRedirectView]].head shouldEqual asView(redirect)
        status shouldEqual StatusCodes.OK
      }
      Mockito.verify(mockRS).list(eq(Filter.ByTarget(target)), eq(page))(?)
    }

    "list redirects by object id" in {
      val mockRS = mock[RedirectServiceV2]
      val mockRCS = mock[RedirectCreationService]
      val handler = createHandler(mockRS, mockRCS)
      val page = Page(0, 10)
      val objectId = ObjectId("1234")
      val targetPhonePrefix = PhonePrefix("+798")
      val proxyPhonePrefix = PhonePrefix("+79876")
      val filter = Filter.ByObjectIdAndPhonePrefixes(objectId, Some(targetPhonePrefix), Some(proxyPhonePrefix))
      when(mockRS.list(eq(filter), eq(page))(?))
        .thenReturn(Future.successful(SlicedResult(Iterable(redirect), 1, page)))

      val uri = Uri(s"/${objectId.value}").withQuery(
        Query(
          "targetPhone" -> targetPhonePrefix.value,
          "proxyPhone" -> proxyPhonePrefix.value
        )
      )
      Get(uri) ~> handler ~> check {
        import ActualRedirectView._
        responseAs[SlicedResult[ActualRedirectView]].head shouldEqual asView(redirect)
        status shouldEqual StatusCodes.OK
      }
      Mockito.verify(mockRS).list(eq(filter), eq(page))(?)
    }

    "delete redirect by id" in {
      val objectId = ObjectId("2")
      forAll(RedirectIdGen, TtlGen) {
        case (redirectId, downtime) =>
          val mockRS = mock[RedirectServiceV2]
          val mockRCS = mock[RedirectCreationService]
          val handler = createHandler(mockRS, mockRCS)
          when(mockRS.delete(eq.apply(redirectId), eq.apply(downtime), eq.apply(false))(?))
            .thenReturn(Future.successful(true))

          val qs = downtime.map(fd => "downtime" -> fd.toSeconds.toString).toMap
          val uri = Uri(s"/${objectId.value}/${redirectId.value}").withQuery(Query(qs))
          Delete(uri) ~> handler ~> check {
            status shouldEqual StatusCodes.OK
          }
          Mockito.verify(mockRS).delete(eq.apply(redirectId), eq.apply(downtime), eq.apply(false))(?)
      }
    }

    //TODO: trailing slash is mandatory. Don't forget to document
    "delete several redirects by qualifier" in {
      forAll(QualifierGen, TtlGen) {
        case (objectId, downtime) =>
          val mockRS = mock[RedirectServiceV2]
          val mockRCS = mock[RedirectCreationService]
          val handler = createHandler(mockRS, mockRCS)
          when(mockRS.delete(eq.apply(objectId), eq.apply(downtime), eq.apply(false))(?))
            .thenReturn(Future.successful(true))

          val qs = downtime.map(fd => "downtime" -> fd.toSeconds.toString).toMap
          val uri = Uri(s"/${objectId.value}/").withQuery(Query(qs))
          Delete(uri) ~> handler ~> check {
            status shouldEqual StatusCodes.OK
          }
          Mockito.verify(mockRS).delete(eq.apply(objectId), eq.apply(downtime), eq.apply(false))(?)
      }
    }

    "fail during deleting redirect by id" in {
      val mockRS = mock[RedirectServiceV2]
      val mockRCS = mock[RedirectCreationService]
      val handler = createHandler(mockRS, mockRCS)
      val redirectId = RedirectId("1")
      val objectId = ObjectId("2")
      when(mockRS.delete(eq.apply(redirectId), eq.apply(None), eq.apply(false))(?))
        .thenReturn(Future.successful(false))

      Delete(s"/${objectId.value}/${redirectId.value}") ~> handler ~> check {
        status shouldEqual StatusCodes.NotFound
      }
      Mockito.verify(mockRS).delete(eq.apply(redirectId), eq.apply(None), eq.apply(false))(?)
    }

    "create redirect" in {
      forAll(createRequestV2Gen(MoscowPhoneGen)) { req =>
        val mockRS = mock[RedirectServiceV2]
        val mockRCS = mock[RedirectCreationService]
        val handler = createHandler(mockRS, mockRCS)
        when(mockRS.create(eq(req))(?)).thenReturn(Future.successful(redirect))
        import CreateRequestV2View.modelMarshaller
        val objectId: ObjectId = req.key.objectId

        Post(s"/${objectId.value}/", req) ~> handler ~> check {
          status shouldEqual StatusCodes.OK
          import ActualRedirectView._
          responseAs[ActualRedirectView] shouldEqual ActualRedirectView.asView(redirect)
        }
        Mockito.verify(mockRS).create(eq(req))(?)
      }
    }

    "getOrCreate redirect" in {
      forAll(createRequestV2Gen(MoscowPhoneGen)) { req =>
        val redirectView = ActualRedirectView.asView(redirect)
        val mockRS = mock[RedirectServiceV2]
        val mockRCS = mock[RedirectCreationService]
        val handler = createHandler(mockRS, mockRCS)
        when(mockRCS.getOrCreate(eq(req))(?)).thenReturn(Future.successful(Right(redirectView)))

        import CreateRequestV2View.modelMarshaller
        val objectId: ObjectId = req.key.objectId
        Post(s"/getOrCreate/${objectId.value}/", req) ~> handler ~> check {
          status shouldEqual StatusCodes.OK
          import ActualRedirectView._
          responseAs[ActualRedirectView] shouldEqual redirectView
        }
        Mockito.verify(mockRCS).getOrCreate(eq(req))(?)
      }
    }

    "getOrCreate redirect without slash" in {
      val redirectView = ActualRedirectView.asView(redirect)
      val mockRS = mock[RedirectServiceV2]
      val mockRCS = mock[RedirectCreationService]
      val handler = createHandler(mockRS, mockRCS)
      val req = createRequestV2Gen(MoscowPhoneGen).next
      when(mockRCS.getOrCreate(eq(req))(?)).thenReturn(Future.successful(Right(redirectView)))

      import CreateRequestV2View.modelMarshaller
      val objectId: ObjectId = req.key.objectId
      Post(s"/getOrCreate/${objectId.value}", req) ~> handler ~> check {
        status shouldEqual StatusCodes.OK
        import ActualRedirectView._
        responseAs[ActualRedirectView] shouldEqual redirectView
      }
      Mockito.verify(mockRCS).getOrCreate(eq(req))(?)
    }

    "count available phones for domain and geo id" in {
      val count = 3
      forAll(AvailableRequestGen) { req =>
        val mockRS = mock[RedirectServiceV2]
        val mockRCS = mock[RedirectCreationService]
        val handler = createHandler(mockRS, mockRCS)
        when(mockRS.countAvailable(eq(req))(?)).thenReturn(Future.successful(count))
        val qs = {
          val checkOpAvQuery = "operator-availability" -> req.checkOperatorAvailability.toString
          def phoneTypeQ(opt: Option[PhoneType]) = opt.map(v => "phone-type" -> v.toString)
          req match {
            case GeoIdAvailableRequest(geoId, phoneTypeOpt, _, _) =>
              Seq("geo-id" -> geoId.toString, checkOpAvQuery) ++ phoneTypeQ(phoneTypeOpt)
            case PhoneAvailableRequest(phone, phoneTypeOpt, _, _) =>
              Seq("phone" -> phone.value, checkOpAvQuery) ++ phoneTypeQ(phoneTypeOpt)
          }
        }
        val uri = Uri(s"/available").withQuery(Query(qs.toMap))

        Get(uri) ~> handler ~> check {
          status shouldEqual StatusCodes.OK
          responseAs[PhoneStatsView] shouldEqual PhoneStatsView(count)
        }
        Mockito.verify(mockRS).countAvailable(eq(req))(?)
      }
    }

    "return bad request for count available" in {
      val mockRS = mock[RedirectServiceV2]
      val mockRCS = mock[RedirectCreationService]
      val handler = createHandler(mockRS, mockRCS)
      val invalidRawPhone = "+7"
      val uri = Uri(s"/available").withQuery(Query("phone" -> invalidRawPhone)) // invalid phone
      Get(uri) ~> handler ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] should include(s"Can't parse valid phone number from $invalidRawPhone")
      }
    }

    "complete with gateway timeout on mts exception" in {
      val mockRS = mock[RedirectServiceV2]
      val mockRCS = mock[RedirectCreationServiceImpl]
      val handler = createHandler(mockRS, mockRCS)
      val req = createRequestV2Gen(MoscowPhoneGen).next

      when(mockRCS.getOrCreate(eq(req))(?))
        .thenReturn(Future.successful(Left(MtsException("oops", "req", None, None))))

      val objectId: ObjectId = req.key.objectId
      import CreateRequestV2View.modelMarshaller
      Post(s"/getOrCreate/${objectId.value}", req) ~> handler ~> check {
        status shouldEqual StatusCodes.GatewayTimeout
        contentType shouldEqual ContentTypes.`application/json`
      }
      Mockito.verify(mockRCS).getOrCreate(eq(req))(?)
    }

    "delete redirect by object-id and target" in {
      import ru.yandex.vertis.telepony.service.RedirectServiceV2.RedirectKeyFilter
      val numberDeleted = 5
      val deletedRedirects = 1.to(5).map(_ => ActualRedirectGen.next)
      forAll(QualifierGen, PhoneGen, TtlGen) {
        case (objectId, target, downtime) =>
          val mockRS = mock[RedirectServiceV2]
          val mockRCS = mock[RedirectCreationService]
          val handler = createHandler(mockRS, mockRCS)
          val filter = RedirectKeyFilter.ByObjectIdAndTarget(objectId, target)
          when(mockRS.delete(eq.apply(filter), eq.apply(downtime), eq.apply(false))(?))
            .thenReturn(Future.successful(deletedRedirects))

          val qs = downtime.map(fd => "downtime" -> fd.toSeconds.toString).toMap
          val uri = Uri(s"/object-id/${objectId.value}/target/${target.value}").withQuery(Query(qs))
          Delete(uri) ~> handler ~> check {
            status shouldEqual StatusCodes.OK
            responseAs[DeletedRedirectsView] shouldEqual DeletedRedirectsView(numberDeleted)
          }
          Mockito.verify(mockRS).delete(eq.apply(filter), eq.apply(downtime), eq.apply(false))(?)
      }
    }

    s"delete redirect by object-id, target and tag" in {
      import ru.yandex.vertis.telepony.service.RedirectServiceV2.RedirectKeyFilter
      val numberDeleted = 5
      val deletedRedirects = 1.to(5).map(_ => ActualRedirectGen.next)
      forAll(QualifierGen, PhoneGen, TagGen, TtlGen) {
        case (objectId, target, tag, downtime) =>
          val mockRS = mock[RedirectServiceV2]
          val mockRCS = mock[RedirectCreationService]
          val handler = createHandler(mockRS, mockRCS)
          val filter = RedirectKeyFilter.ByKey(RedirectKey(objectId, target, tag))
          when(mockRS.delete(eq.apply(filter), eq.apply(downtime), eq.apply(false))(?))
            .thenReturn(Future.successful(deletedRedirects))

          val qs = downtime.map(fd => "downtime" -> fd.toSeconds.toString).toMap
          val tagValue = tag.asOption.getOrElse("")
          val uri = Uri(s"/object-id/${objectId.value}/target/${target.value}/tag/$tagValue")
            .withQuery(Query(qs))
          Delete(uri) ~> handler ~> check {
            status shouldEqual StatusCodes.OK
            responseAs[DeletedRedirectsView] shouldEqual DeletedRedirectsView(numberDeleted)
          }
          Mockito.verify(mockRS).delete(eq.apply(filter), eq.apply(downtime), eq.apply(false))(?)
      }
    }

    "get redirect by redirectId" in {
      val mockRS = mock[RedirectServiceV2]
      val mockRCS = mock[RedirectCreationService]
      val handler = createHandler(mockRS, mockRCS)
      val redirectId = RedirectIdGen.next
      when(mockRS.get(eq.apply(redirectId))(?)).thenReturn(Future.successful(Some(redirect)))

      val uri = Uri(s"/redirect-id/${redirectId.value}")
      Get(uri) ~> handler ~> check {
        import ActualRedirectView._
        responseAs[Option[ActualRedirectView]].head shouldEqual asView(redirect)
        status shouldEqual StatusCodes.OK
      }
      Mockito.verify(mockRS).get(eq.apply(redirectId))(?)
    }

    "return 404 when get non-existing redirect" in {
      val mockRS = mock[RedirectServiceV2]
      val mockRCS = mock[RedirectCreationService]
      val handler = createHandler(mockRS, mockRCS)
      val redirectId = RedirectIdGen.next
      when(mockRS.get(eq.apply(redirectId))(?)).thenReturn(Future.successful(None))

      val uri = Uri(s"/redirect-id/${redirectId.value}")
      Get(uri) ~> handler ~> check {
        status shouldEqual StatusCodes.NotFound
      }
      Mockito.verify(mockRS).get(eq.apply(redirectId))(?)
    }

    "update options" in {
      forAll(UpdateOptionsRequestGen) { req =>
        val objectId = QualifierGen.next
        val target = PhoneGen.next
        val tag = Tag.Empty
        val redirectKey: RedirectKey = RedirectKey(objectId, target, tag)
        val mockRS = mock[RedirectServiceV2]
        val mockRCS = mock[RedirectCreationService]
        val handler = createHandler(mockRS, mockRCS)
        when(mockRS.updateOptions(eq.apply(redirectKey), eql(req))(?))
          .thenReturn(Future.unit)

        val uri = Uri(s"/options/object-id/${objectId.value}/target/${target.value}")
        import UpdateOptionsRequestView.modelMarshaller
        Put(uri, req) ~> handler ~> check {
          status should ===(StatusCodes.OK)
        }
        Mockito.verify(mockRS).updateOptions(eq.apply(redirectKey), eql(req))(?)
      }
    }

    import RedirectOptions.Names._
    Seq(
      CallerIdMode -> UpdateOptionsRequest(callerIdModeOp = Clear),
      NeedAnswer -> UpdateOptionsRequest(needAnswerOp = Clear),
      RecordEnabled -> UpdateOptionsRequest(recordEnabledOp = Clear),
      DoubleRedirectNumber -> UpdateOptionsRequest(doubleRedirectOp = Clear)
    ).foreach {
      case (optionName, expectedReq) =>
        s"delete option $optionName" in {
          val objectId = QualifierGen.next
          val target = PhoneGen.next
          val mockRS = mock[RedirectServiceV2]
          val mockRCS = mock[RedirectCreationService]
          val handler = createHandler(mockRS, mockRCS)
          when(mockRS.updateOptions(eql(objectId), eql(target), eql(expectedReq))(?))
            .thenReturn(Future.unit)

          val uri = Uri(s"/options/object-id/${objectId.value}/target/${target.value}/option/$optionName")
          Delete(uri) ~> handler ~> check {
            status should ===(StatusCodes.OK)
          }
          Mockito.verify(mockRS).updateOptions(eql(objectId), eql(target), eql(expectedReq))(?)
        }
    }

  }
}

object RedirectHandlerSpec {

  // Clear operation is not allowed here
  def operationGen[T](innerGen: Gen[T]): Gen[Operation[T]] =
    Gen.oneOf(Gen.const(Ignore), innerGen.map(Update.apply))

  val UpdateOptionsRequestGen: Gen[UpdateOptionsRequest] = for {
    callerIdModeOp <- operationGen(BooleanGen)
    needAnswerOp <- operationGen(BooleanGen)
    allPrevAreEmpty = Seq(callerIdModeOp, needAnswerOp).forall(_ == Ignore)
    recordEnabledOp <- operationGen(BooleanGen)
      .suchThat(v => !allPrevAreEmpty || v != Ignore) // at least one option must be defined
  } yield UpdateOptionsRequest(
    callerIdModeOp = callerIdModeOp,
    needAnswerOp = needAnswerOp,
    recordEnabledOp = recordEnabledOp
  )
}
