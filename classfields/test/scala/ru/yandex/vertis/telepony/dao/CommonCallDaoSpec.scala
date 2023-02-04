package ru.yandex.vertis.telepony

package dao

import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import ru.yandex.vertis.telepony.dao.jdbc.api._
import ru.yandex.vertis.telepony.generator.AppBackCallGenerator.AppBackCallGen
import ru.yandex.vertis.telepony.generator.AppCallGenerator._
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.CallbackGenerator._
import ru.yandex.vertis.telepony.model.{ActualRedirect, AppBackCall, AppCall, CallV2, Callback, CallbackOrder, ObjectId, OperatorNumber, TeleponyCall, TypedDomain}
import ru.yandex.vertis.telepony.service.CallHistoryService.{CallRequest, OrderByFields, Orders, SortOrder}
import ru.yandex.vertis.telepony.service.{CallbackCallService, CallbackOrderService}
import ru.yandex.vertis.telepony.time._
import ru.yandex.vertis.telepony.util.CallFilters.{DurationFilter, TimeFilter}
import ru.yandex.vertis.telepony.util.{Page, Range, Threads}

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * @author neron
  */
trait CommonCallDaoSpec extends SpecBase with BeforeAndAfterEach with BeforeAndAfterAll with DatabaseSpec {

  import Threads.lightWeightTasksEc
  import ru.yandex.vertis.telepony.model.{Phone, Username}

  def domain: TypedDomain

  def dao: CommonCallDao

  def numberDao: OperatorNumberDaoV2

  def redirectDao: RedirectDaoV2

  def callDao: CallDaoV2

  def appCallDao: AppCallDao

  def callbackOrderDao: CallbackOrderDao

  def callbackDao: CallbackCallDao

  def appBackCallDao: AppBackCallDao

  def genObjectId: ObjectId = ShortStr.map(ObjectId.apply).next

  def genRedirect: ActualRedirect = ActualRedirectGen.next

  def genAppCall: AppCall = AppCallGen.next

  def genAppBackCall: AppBackCall = AppBackCallGen.next

  def genCall: CallV2 = CallV2Gen.next

  def genCallbackOrderCreateRequest: CallbackOrderService.CreateRequest = CallbackOrderRequestGen.next

  def genCallbackCallCreateRequest: CallbackCallService.CreateRequest = CallbackCreateRequestGen.next

  def genCallbackOrder: CallbackOrder = CallbackOrderGen.next

  def genCallback: Callback = CallbackGen.next

  def genPhone: Phone = PhoneGen.next

  def genUsername: Username = ShortStr.next

  def createNumbers(item: OperatorNumber, items: OperatorNumber*): Unit = {
    DBIO.sequence((item +: items).map(numberDao.create)).databaseValue.futureValue
  }

  def createRedirects(item: ActualRedirect, items: ActualRedirect*): Unit = {
    DBIO.sequence((item +: items).map(redirectDao.create)).databaseValue.futureValue
  }

  def createAppCalls(item: AppCall, items: AppCall*): Unit = {
    Future.sequence((item +: items).map(appCallDao.createOrUpdate)).futureValue
  }

  def createAppBackCalls(item: AppBackCall, items: AppBackCall*): Unit = {
    Future.sequence((item +: items).map(appBackCallDao.createOrUpdate)).futureValue
  }

  def createCalls(item: CallV2, items: CallV2*): Unit = {
    DBIO.sequence((item +: items).map(callDao.create)).databaseValue.futureValue
  }

  def createCallback(inCallback: Callback): Callback = {

    val callbackOrderCreateRequest: CallbackOrderService.CreateRequest = genCallbackOrderCreateRequest.copy(
      objectId = inCallback.order.objectId,
      tag = inCallback.order.tag,
      source = inCallback.order.source,
      target = inCallback.order.target,
      payload = inCallback.order.payload
    )

    val callbackOrder = callbackOrderDao
      .create(inCallback.order.domain, callbackOrderCreateRequest, inCallback.order.callbackOrderSource)
      .futureValue

    val callbackCallCreateRequest: CallbackCallService.CreateRequest = genCallbackCallCreateRequest.copy(
      orderId = callbackOrder.id,
      time = inCallback.time,
      sourceCallerId = inCallback.sourceCallerId,
      sourceCallResult = inCallback.sourceCallResult,
      targetCallerId = inCallback.targetCallerId,
      targetCallResult = inCallback.targetCallResult,
      duration = inCallback.duration,
      talkDuration = inCallback.talkDuration,
      hasRecord = inCallback.hasRecord,
      stage = inCallback.stage,
      endCause = inCallback.endCause,
      callingTargetDuration = inCallback.callingTargetDuration,
      confirmationDuration = inCallback.confirmationDuration,
      confirmationType = inCallback.confirmationType
    )

    val insertedCallback = callbackDao.create(inCallback.id, callbackCallCreateRequest).futureValue
    require(insertedCallback.nonEmpty)

    val outCallback = inCallback.copy(
      order = inCallback.order.copy(id = callbackOrder.id),
      updateTime = insertedCallback.get.updateTime
    )

    outCallback
  }

  trait DefaultEnvironment {
    val expectedObjectId = genObjectId
    val unexpectedObjectId = genObjectId

    val expectedRedirect = genRedirect.withObjectId(expectedObjectId)
    val unexpectedRedirect = genRedirect.withObjectId(unexpectedObjectId)

    val expectedAppCall = genAppCall.withUuid(None).withPayloadJson(None).withRedirect(expectedRedirect)
    val unexpectedAppCall = genAppCall.withUuid(None).withPayloadJson(None).withRedirect(unexpectedRedirect)

    val expectedAppBackCall = genAppBackCall.withUuid(None).withPayloadJson(None).withRedirect(expectedRedirect)
    val unexpectedAppBackCall = genAppBackCall.withUuid(None).withPayloadJson(None).withRedirect(unexpectedRedirect)

    val expectedCall = genCall.withRedirect(expectedRedirect)
    val unexpectedCall = genCall.withRedirect(unexpectedRedirect)

    val expectedCallback: Callback = genCallback.withObjectId(expectedObjectId).withDomain(domain) // raw
    val unexpectedCallback: Callback = genCallback.withObjectId(unexpectedObjectId).withDomain(domain) // raw

    createNumbers(expectedRedirect.source, unexpectedRedirect.source)
    createRedirects(expectedRedirect, unexpectedRedirect)
    createAppCalls(expectedAppCall, unexpectedAppCall)
    createAppBackCalls(expectedAppBackCall, unexpectedAppBackCall)
    createCalls(expectedCall, unexpectedCall)

    val expectedCallback2 = createCallback(expectedCallback) // prepared
    val unexpectedCallback2 = createCallback(unexpectedCallback) // prepared

    val expectedAppCalls = Seq(expectedAppCall).map(TeleponyCall.from(_, domain))
    val expectedAppBackCalls = Seq(expectedAppBackCall).map(TeleponyCall.from(_, domain))
    val expectedCalls = Seq(expectedCall).map(call => TeleponyCall.from(call, domain, call.hasRecord))

    val expectedCallbacks =
      Seq(expectedCallback2).map(call => TeleponyCall.from(call, call.order.domain, call.hasRecord))

    val byObjectIdSeq = (expectedAppCalls ++ expectedAppBackCalls ++ expectedCalls ++ expectedCallbacks)
      .sortBy(_.time)(Ordering[DateTime].reverse)

    val byRedirectIdSeq = (expectedAppCalls ++ expectedAppBackCalls ++ expectedCalls).sortBy(_.time)
  }

  trait TagFilterEnvironment {

    import ru.yandex.vertis.telepony.model.Tag

    val objectId = genObjectId

    val emptyTag = Tag.Empty
    val fullTag = Tag(Some("123456789"))
    val partTag = Tag(Some("456"))

    val emptyTagRedirect = genRedirect.withObjectId(objectId).withTag(emptyTag)
    val fullTagRedirect = genRedirect.withObjectId(objectId).withTag(fullTag)
    val partTagRedirect = genRedirect.withObjectId(objectId).withTag(partTag)

    val emptyTagAppCall = genAppCall.withUuid(None).withPayloadJson(None).withRedirect(emptyTagRedirect)
    val fullTagAppCall = genAppCall.withUuid(None).withPayloadJson(None).withRedirect(fullTagRedirect)
    val partTagAppCall = genAppCall.withUuid(None).withPayloadJson(None).withRedirect(partTagRedirect)

    val emptyTagAppBackCall = genAppBackCall.withUuid(None).withPayloadJson(None).withRedirect(emptyTagRedirect)
    val fullTagAppBackCall = genAppBackCall.withUuid(None).withPayloadJson(None).withRedirect(fullTagRedirect)
    val partTagAppBackCall = genAppBackCall.withUuid(None).withPayloadJson(None).withRedirect(partTagRedirect)

    val emptyTagCall = genCall.withRedirect(emptyTagRedirect)
    val fullTagCall = genCall.withRedirect(fullTagRedirect)
    val partTagCall = genCall.withRedirect(partTagRedirect)

    val emptyTagCallback = genCallback.withObjectId(objectId).withDomain(domain).withTag(emptyTag) // raw
    val fullTagCallback = genCallback.withObjectId(objectId).withDomain(domain).withTag(fullTag) // raw
    val partTagCallback = genCallback.withObjectId(objectId).withDomain(domain).withTag(partTag) // raw

    createNumbers(emptyTagRedirect.source, fullTagRedirect.source, partTagRedirect.source)
    createRedirects(emptyTagRedirect, fullTagRedirect, partTagRedirect)
    createAppCalls(emptyTagAppCall, fullTagAppCall, partTagAppCall)
    createAppBackCalls(emptyTagAppBackCall, fullTagAppBackCall, partTagAppBackCall)
    createCalls(emptyTagCall, fullTagCall, partTagCall)

    val emptyTagCallback2 = createCallback(emptyTagCallback) // prepared
    val fullTagCallback2 = createCallback(fullTagCallback) // prepared
    val partTagCallback2 = createCallback(partTagCallback) // prepared

    val calls =
      (
        Seq(emptyTagAppCall, fullTagAppCall, partTagAppCall).map(TeleponyCall.from(_, domain)) ++
          Seq(emptyTagAppBackCall, fullTagAppBackCall, partTagAppBackCall).map(TeleponyCall.from(_, domain)) ++
          Seq(emptyTagCall, fullTagCall, partTagCall).map(call => TeleponyCall.from(call, domain, call.hasRecord)) ++
          Seq(emptyTagCallback2, fullTagCallback2, partTagCallback2)
            .map(call => TeleponyCall.from(call, domain, call.hasRecord))
      ).sortBy(_.time)(Ordering[DateTime].reverse)

  }

  trait SourceFilterEnvironment {

    val redirect = genRedirect

    val expectedSource = genPhone
    val unexpectedSource = genPhone

    val expectedAppCall =
      genAppCall.withUuid(None).withPayloadJson(None).withRedirect(redirect).withSource(expectedSource)

    val unexpectedAppCall =
      genAppCall.withUuid(None).withPayloadJson(None).withRedirect(redirect).withSource(unexpectedSource)

    val expectedCall = genCall.withRedirect(redirect).withSource(expectedSource)
    val unexpectedCall = genCall.withRedirect(redirect).withSource(unexpectedSource)

    val expectedCallback = genCallback.withDomain(domain).withSource(expectedSource) // raw
    val unexpectedCallback = genCallback.withDomain(domain).withSource(unexpectedSource) // raw

    createNumbers(redirect.source)
    createRedirects(redirect)
    createAppCalls(expectedAppCall, unexpectedAppCall)
    createCalls(expectedCall, unexpectedCall)

    val expectedCallback2 = createCallback(expectedCallback) // prepared
    val unexpectedCallback2 = createCallback(unexpectedCallback) // prepared

    val calls =
      (
        Seq(expectedAppCall, unexpectedAppCall).map(TeleponyCall.from(_, domain)) ++
          Seq(expectedCall, unexpectedCall).map(call => TeleponyCall.from(call, domain, call.hasRecord)) ++
          Seq(expectedCallback2, unexpectedCallback2).map(call => TeleponyCall.from(call, domain, call.hasRecord))
      ).sortBy(_.time)(Ordering[DateTime].reverse)

  }

  trait SourcePhoneFilterEnvironment {

    val objectId = genObjectId

    val redirect = genRedirect.withObjectId(objectId)

    val expectedSource = genPhone
    val unexpectedSource = genPhone

    val expectedAppCall =
      genAppCall.withUuid(None).withPayloadJson(None).withRedirect(redirect).withSource(expectedSource)

    val unexpectedAppCall =
      genAppCall.withUuid(None).withPayloadJson(None).withRedirect(redirect).withSource(unexpectedSource)

    val expectedCall = genCall.withRedirect(redirect).withSource(expectedSource)
    val unexpectedCall = genCall.withRedirect(redirect).withSource(unexpectedSource)

    val expectedCallback = genCallback.withDomain(domain).withObjectId(objectId).withSource(expectedSource) // raw
    val unexpectedCallback = genCallback.withDomain(domain).withObjectId(objectId).withSource(unexpectedSource) // raw

    createNumbers(redirect.source)
    createRedirects(redirect)
    createAppCalls(expectedAppCall, unexpectedAppCall)
    createCalls(expectedCall, unexpectedCall)

    val expectedCallback2 = createCallback(expectedCallback) // prepared
    val unexpectedCallback2 = createCallback(unexpectedCallback) // prepared

    val calls =
      (
        Seq(expectedAppCall, unexpectedAppCall).map(TeleponyCall.from(_, domain)) ++
          Seq(expectedCall, unexpectedCall).map(call => TeleponyCall.from(call, domain, call.hasRecord)) ++
          Seq(expectedCallback2, unexpectedCallback2).map(call => TeleponyCall.from(call, domain, call.hasRecord))
      ).sortBy(_.time)(Ordering[DateTime].reverse)

  }

  trait TargetPhoneFilterEnvironment {

    val objectId = genObjectId

    val expectedTarget = genPhone
    val unexpectedTarget = genPhone

    val expectedRedirect = genRedirect.withObjectId(objectId).withTarget(expectedTarget)
    val unexpectedRedirect = genRedirect.withObjectId(objectId).withTarget(unexpectedTarget)

    val expectedAppCall = genAppCall.withUuid(None).withPayloadJson(None).withRedirect(expectedRedirect)
    val unexpectedAppCall = genAppCall.withUuid(None).withPayloadJson(None).withRedirect(unexpectedRedirect)

    val expectedCall = genCall.withRedirect(expectedRedirect)
    val unexpectedCall = genCall.withRedirect(unexpectedRedirect)

    val expectedCallback = genCallback.withDomain(domain).withObjectId(objectId).withTarget(expectedTarget) // raw
    val unexpectedCallback = genCallback.withDomain(domain).withTarget(unexpectedTarget) // raw

    createNumbers(expectedRedirect.source, unexpectedRedirect.source)
    createRedirects(expectedRedirect, unexpectedRedirect)
    createAppCalls(expectedAppCall, unexpectedAppCall)
    createCalls(expectedCall, unexpectedCall)

    val expectedCallback2 = createCallback(expectedCallback) // prepared
    val unexpectedCallback2 = createCallback(unexpectedCallback) // prepared

    val calls =
      (
        Seq(expectedAppCall, unexpectedAppCall).map(TeleponyCall.from(_, domain)) ++
          Seq(expectedCall, unexpectedCall).map(call => TeleponyCall.from(call, domain, call.hasRecord)) ++
          Seq(expectedCallback2, unexpectedCallback2).map(call => TeleponyCall.from(call, domain, call.hasRecord))
      ).sortBy(_.time)(Ordering[DateTime].reverse)

  }

  trait SourceUsernameFilterEnvironment {

    val objectId = genObjectId

    val redirect = genRedirect.withObjectId(objectId)

    val expectedUsername = genUsername
    val unexpectedUsername = genUsername

    val expectedAppCall =
      genAppCall.withUuid(None).withPayloadJson(None).withRedirect(redirect).withSourceUsername(expectedUsername)

    val unexpectedAppCall =
      genAppCall.withUuid(None).withPayloadJson(None).withRedirect(redirect).withSourceUsername(unexpectedUsername)

    val expectedAppBackCall =
      genAppBackCall.withUuid(None).withPayloadJson(None).withRedirect(redirect).withSourceUsername(expectedUsername)

    val unexpectedAppBackCall =
      genAppBackCall.withUuid(None).withPayloadJson(None).withRedirect(redirect).withSourceUsername(unexpectedUsername)

    createNumbers(redirect.source)
    createRedirects(redirect)
    createAppCalls(expectedAppCall, unexpectedAppCall)
    createAppBackCalls(expectedAppBackCall, unexpectedAppBackCall)

    val expectedSeq =
      (
        Seq(expectedAppCall).map(TeleponyCall.from(_, domain)) ++
          Seq(expectedAppBackCall).map(TeleponyCall.from(_, domain))
      ).sortBy(_.time)(Ordering[DateTime].reverse)

  }

  trait TargetUsernameFilterEnvironment {

    val objectId = genObjectId

    val redirect = genRedirect.withObjectId(objectId)

    val expectedUsername = genUsername
    val unexpectedUsername = genUsername

    val expectedAppCall =
      genAppCall.withUuid(None).withPayloadJson(None).withRedirect(redirect).withTargetUsername(expectedUsername)

    val unexpectedAppCall =
      genAppCall.withUuid(None).withPayloadJson(None).withRedirect(redirect).withTargetUsername(unexpectedUsername)

    val expectedAppBackCall =
      genAppBackCall.withUuid(None).withPayloadJson(None).withRedirect(redirect).withTargetUsername(expectedUsername)

    val unexpectedAppBackCall =
      genAppBackCall.withUuid(None).withPayloadJson(None).withRedirect(redirect).withTargetUsername(unexpectedUsername)

    createNumbers(redirect.source)
    createRedirects(redirect)
    createAppCalls(expectedAppCall, unexpectedAppCall)
    createAppBackCalls(expectedAppBackCall, unexpectedAppBackCall)

    val expectedSeq =
      (
        Seq(expectedAppCall).map(TeleponyCall.from(_, domain)) ++
          Seq(expectedAppBackCall).map(TeleponyCall.from(_, domain))
      ).sortBy(_.time)(Ordering[DateTime].reverse)

  }

  def defaultEnvironment(): DefaultEnvironment = new DefaultEnvironment {}

  def tagFilterEnvironment(): TagFilterEnvironment = new TagFilterEnvironment {}

  def sourceFilterEnvironment(): SourceFilterEnvironment = new SourceFilterEnvironment {}

  def sourcePhoneFilterEnvironment(): SourcePhoneFilterEnvironment = new SourcePhoneFilterEnvironment {}

  def targetPhoneFilterEnvironment(): TargetPhoneFilterEnvironment = new TargetPhoneFilterEnvironment {}

  def sourceUsernameFilterEnvironment(): SourceUsernameFilterEnvironment = new SourceUsernameFilterEnvironment {}

  def targetUsernameFilterEnvironment(): TargetUsernameFilterEnvironment = new TargetUsernameFilterEnvironment {}

  def query(request: CallRequest): Seq[TeleponyCall] = {
    dao.list(request).futureValue
  }

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected: Boolean = true

  override protected def afterAll(): Unit = {
    callbackDao.clear().futureValue
    callbackOrderDao.clear().futureValue
    callDao.clear().databaseValue.futureValue
    appBackCallDao.clear().futureValue
    appCallDao.clear().futureValue
    redirectDao.clear().databaseValue.futureValue
    numberDao.clear().databaseValue.futureValue
    super.afterAll()
  }

  "CommonCallDao" should {

    "testFilterByObjectId" in {
      val environment = defaultEnvironment()
      val expectedSeq = environment.byObjectIdSeq.sortBy(_.time.getMillis)
      val actualSeq = query(
        CallRequest(
          slice = Range.Full,
          objectId = Some(environment.expectedObjectId),
          sortOrder = SortOrder(OrderByFields.CallTime, Orders.Asc)
        )
      )
      actualSeq shouldBe expectedSeq
    }

    "testSlice" in {
      val environment = defaultEnvironment()
      // full
      val expectedSeq1 = environment.byObjectIdSeq
      val actualSeq1 =
        query(
          CallRequest(
            slice = Range.Full,
            objectId = Some(environment.expectedObjectId)
          )
        )
      actualSeq1 shouldBe expectedSeq1
      // page 2 with 3 elements per page
      val expectedSeq2 = environment.byObjectIdSeq.slice(3, 6)
      val actualSeq2 = query(
        CallRequest(
          slice = Page(1, 3), // zero-based
          objectId = Some(environment.expectedObjectId)
        )
      )
      actualSeq2 shouldBe expectedSeq2
    }

    "testSortOrder" in {
      val environment = defaultEnvironment()
      val ascCalls = query(
        CallRequest(
          slice = Range.Full,
          objectId = Some(environment.expectedObjectId),
          sortOrder = SortOrder(OrderByFields.CallTime, Orders.Asc)
        )
      )
      val descCalls = query(
        CallRequest(
          slice = Range.Full,
          objectId = Some(environment.expectedObjectId),
          sortOrder = SortOrder(OrderByFields.CallTime, Orders.Desc)
        )
      )
      (ascCalls should contain).theSameElementsInOrderAs(descCalls.reverse)
    }

    "testTimeFilter" in {
      val environment = defaultEnvironment()
      val from = environment.byObjectIdSeq.minBy(_.time).time
      val to = environment.byObjectIdSeq.maxBy(_.time).time
      val expectedSeq = environment.byObjectIdSeq.filter(item => item.time.isAfter(from) && item.time.isBefore(to))
      val actualSeq = query(
        CallRequest(
          slice = Range.Full,
          objectId = Some(environment.expectedObjectId),
          callTime = Some(TimeFilter(from.plusSeconds(1), to.minusSeconds(1)))
        )
      )
      actualSeq shouldBe expectedSeq
    }

    "testTagFilterEmpty" in {
      import ru.yandex.vertis.telepony.model.Tag
      import ru.yandex.vertis.telepony.util.CallFilters

      val environment = tagFilterEnvironment()
      val expectedSeq = environment.calls.filter(_.tag == Tag.Empty)
      val actualSeq = query(
        CallRequest(
          slice = Range.Full,
          objectId = Some(environment.objectId),
          tag = Some(CallFilters.TagFilter.Full(Tag.Empty))
        )
      )
      actualSeq.shouldBe(expectedSeq)
    }

    "testTagFilterFull" in {
      import ru.yandex.vertis.telepony.util.CallFilters

      val environment = tagFilterEnvironment()
      val expectedSeq = environment.calls.filter(_.tag == environment.fullTag)
      val actualSeq = query(
        CallRequest(
          slice = Range.Full,
          objectId = Some(environment.objectId),
          tag = Some(CallFilters.TagFilter.Full(environment.fullTag))
        )
      )
      actualSeq.shouldBe(expectedSeq)
    }

    "testTagFilterPart" in {
      import ru.yandex.vertis.telepony.util.CallFilters

      val environment = tagFilterEnvironment()
      val tagStr = environment.partTag.asOption.get
      val expectedSeq = environment.calls.filter(_.tag.asOption.exists(_.contains(tagStr)))
      require(expectedSeq.size == 2 * 4) // два звонка каждого типа
      val actualSeq = query(
        CallRequest(
          slice = Range.Full,
          objectId = Some(environment.objectId),
          tag = Some(CallFilters.TagFilter.Part(tagStr))
        )
      )
      actualSeq.shouldBe(expectedSeq)
    }

    "testTargetPhoneFilter" in {
      import ru.yandex.vertis.telepony.util.CallFilters

      val environment = targetPhoneFilterEnvironment()
      val expectedSeq = environment.calls.filter(_.target.contains(environment.expectedTarget))
      val actualSeq = query(
        CallRequest(
          slice = Range.Full,
          objectId = Some(environment.objectId),
          targetPhones = Some(CallFilters.PhoneFilter(Set(environment.expectedTarget)))
        )
      )
      actualSeq shouldBe expectedSeq
    }

    "testSourcePhoneFilter" in {
      import ru.yandex.vertis.telepony.model.RefinedSource
      import ru.yandex.vertis.telepony.util.CallFilters

      val environment = sourcePhoneFilterEnvironment()
      val expectedSeq = environment.calls.filter(_.source.contains(RefinedSource(environment.expectedSource)))
      val actualSeq = query(
        CallRequest(
          slice = Range.Full,
          objectId = Some(environment.objectId),
          sourcePhones = Some(CallFilters.SourcePhoneFilter(Set(RefinedSource(environment.expectedSource))))
        )
      )
      actualSeq shouldBe expectedSeq
    }

    "testDurationFilter" in {
      val environment = defaultEnvironment()
      val from = environment.byObjectIdSeq.minBy(_.duration.length).duration.plus(1.second)
      val to = environment.byObjectIdSeq.maxBy(_.duration.length).duration
      val expectedSeq = environment.byObjectIdSeq.filter(item => item.duration.gteq(from) && item.duration.lt(to))
      val actualSeq = query(
        CallRequest(
          slice = Range.Full,
          objectId = Some(environment.expectedObjectId),
          duration = Some(DurationFilter(from, to))
        )
      )
      actualSeq shouldBe expectedSeq
    }

    "testTalkDurationFilter" in {
      val environment = defaultEnvironment()
      val from = environment.byObjectIdSeq.minBy(_.talkDuration.length).talkDuration.plus(1.second)
      val to = environment.byObjectIdSeq.maxBy(_.talkDuration.length).talkDuration
      val expectedSeq =
        environment.byObjectIdSeq.filter(item => item.talkDuration.gteq(from) && item.talkDuration.lt(to))
      val actualSeq = query(
        CallRequest(
          slice = Range.Full,
          objectId = Some(environment.expectedObjectId),
          talkDuration = Some(DurationFilter(from, to))
        )
      )
      actualSeq shouldBe expectedSeq
    }

    "testRedirectIdFilter" in {
      val environment = defaultEnvironment()
      val expectedSeq = environment.byRedirectIdSeq.sortBy(_.time)
      val actualSeq = query(
        CallRequest(
          slice = Range.Full,
          redirectId = Some(environment.expectedRedirect.id),
          sortOrder = SortOrder(OrderByFields.CallTime, Orders.Asc)
        )
      )
      actualSeq shouldBe expectedSeq
    }

    "testSourceUsernameFilter" in {
      import ru.yandex.vertis.telepony.util.CallFilters

      val environment = sourceUsernameFilterEnvironment()
      val expectedSeq = environment.expectedSeq
      val actualSeq = query(
        CallRequest(
          slice = Range.Full,
          redirectId = Some(environment.redirect.id),
          sourceVoxUsernames = Some(CallFilters.VoxUsernameFilter(Set(environment.expectedUsername)))
        )
      )
      actualSeq shouldBe expectedSeq
    }

    "testTargetUsernameFilter" in {
      import ru.yandex.vertis.telepony.util.CallFilters

      val environment = targetUsernameFilterEnvironment()
      val expectedSeq = environment.expectedSeq
      val actualSeq = query(
        CallRequest(
          slice = Range.Full,
          redirectId = Some(environment.redirect.id),
          targetVoxUsernames = Some(CallFilters.VoxUsernameFilter(Set(environment.expectedUsername)))
        )
      )
      actualSeq shouldBe expectedSeq
    }

    "testSourceFilter" in {
      import ru.yandex.vertis.telepony.model.RefinedSource

      val environment = sourceFilterEnvironment()
      val expectedSeq = environment.calls.filter(_.source.contains(RefinedSource(environment.expectedSource)))
      val actualSeq = query(
        CallRequest(
          slice = Range.Full,
          source = Some(RefinedSource(environment.expectedSource))
        )
      )
      actualSeq shouldBe expectedSeq
    }

  }

}
