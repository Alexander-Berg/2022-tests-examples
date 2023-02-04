package ru.yandex.vertis.telepony.service

import org.joda.time.DateTime
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.exception.CantParsePhone
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.model.classifier._
import ru.yandex.vertis.telepony.service.impl.classifier.{Classifiers, SourceClassifierClient}
import ru.yandex.vertis.telepony.service.impl.{PhoneServiceImpl, SourceClassifierServiceImpl}
import ru.yandex.vertis.telepony.util._
import ru.yandex.vertis.telepony.util.sliced.SlicedResult

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class SourceClassifierServiceSpec extends SpecBase with MockitoSupport {

  implicit val rc: RequestContext = AutomatedContext(id = "source-classifier-service")

  val phoneInfo: PhoneInfo =
    PhoneInfo(Phone("+78005553535"), 0, PhoneTypes.Unknown, 0, Operators.Beeline.toString)

  val source: RefinedSource = RefinedSource("+78005553535")

  val troubleSource: RefinedSource = RefinedSource("+37777777777")

  val phoneNumber = Phone("+79999999999")

  val targetNumber = Phone("+79998887766")

  lazy val redirect: HistoryRedirect = HistoryRedirect(
    id = RedirectId("id"),
    key = RedirectKey(ObjectId("id"), Phone(source.callerId.value), Tag.Empty),
    createTime = DateTime.now(),
    endTime = Some(DateTime.now()),
    number = phoneNumber,
    geoId = 0,
    operator = Operators.Vox,
    originOperator = Some(Operators.Vox),
    account = OperatorAccounts.VoxShared,
    antiFraud = Set(AntiFraudOptions.AonBlacklist),
    options = None
  )

  lazy val callV2: CallV2 = CallV2(
    id = "test",
    DateTime.now(),
    DateTime.now(),
    externalId = "test",
    source = Some(source),
    redirect = redirect,
    time = DateTime.now(),
    duration = 90.seconds,
    talkDuration = 90.seconds,
    hasRecord = false,
    callResult = CallResults.Unknown,
    fallbackCall = None,
    whitelistOwnerId = None
  )

  lazy val bannedCall = BannedCall(
    id = "test",
    createTime = DateTime.now(),
    updateTime = DateTime.now(),
    externalId = "test",
    source = Some(source),
    redirect = redirect,
    time = DateTime.now(),
    duration = 90.seconds
  )

  lazy val rawCall = RawCall(
    externalId = "test",
    source = Some(source),
    startTime = DateTime.now(),
    proxy = phoneNumber,
    target = Some(targetNumber),
    duration = 90.seconds,
    recUrl = None,
    talkDuration = 90.seconds,
    callResult = CallResults.Unknown,
    origin = RawCall.Origins.Offline,
    operator = Operators.Mts
  )

  val request: SourceClassifierRequest =
    SourceClassifierRequest(
      source = source,
      calls = List(Call.create(callV2, Some(phoneInfo), phoneInfo)),
      blockedCalls = List(BlockedCall.create(bannedCall, Some(phoneInfo), phoneInfo)),
      unmatchedCalls = List(UnmatchedCall.create(rawCall, Some(phoneInfo), phoneInfo))
    )

  val troubleRequest: SourceClassifierRequest =
    SourceClassifierRequest(
      source = troubleSource,
      calls = List(Call.create(callV2, None, phoneInfo)),
      blockedCalls = List(BlockedCall.create(bannedCall, None, phoneInfo)),
      unmatchedCalls = List(UnmatchedCall.create(rawCall, None, phoneInfo))
    )

  val sliceMock: Slice = MockitoSupport.mock[Slice]

  val badResponse: Future[SourceClassifierResponse] = Future {
    SourceClassifierResponse(SourceClasses.Bad, 0.75)
  }

  val goodResponse: Future[SourceClassifierResponse] = Future {
    SourceClassifierResponse(SourceClasses.Good, 0.25)
  }

  val middleResponse: Future[SourceClassifierResponse] = Future {
    SourceClassifierResponse(SourceClasses.Unknown, 0.5)
  }

  val clientMock: SourceClassifierClient = {
    val mock = MockitoSupport.mock[SourceClassifierClient]
    when(mock.checkSource(MockitoSupport.eq(request), MockitoSupport.eq(Classifiers.GoodRecallClassifier)))
      .thenReturn(badResponse)
    when(mock.checkSource(MockitoSupport.eq(request), MockitoSupport.eq(Classifiers.GoodPrecisionClassifier)))
      .thenReturn(goodResponse)
    when(mock.checkSource(MockitoSupport.eq(troubleRequest), any[Classifiers.Value]()))
      .thenReturn(middleResponse)
    mock
  }

  val callV2ListMock: Future[SlicedResult[CallV2]] =
    Future {
      SlicedResult(List(callV2), 1, sliceMock)
    }

  val callServiceV2Mock: ActualCallService = {
    val serviceMock = MockitoSupport.mock[ActualCallService]
    when(serviceMock.list(MockitoSupport.any(), any[Int]())(?)).thenReturn(
      callV2ListMock
    )
    serviceMock
  }

  val bannedCallListMock: Future[SlicedResult[BannedCall]] =
    Future {
      SlicedResult(List(bannedCall), 1, sliceMock)
    }

  val blockedCallServiceV2Mock: BlockedCallService = {
    val serviceMock = MockitoSupport.mock[BlockedCallService]
    when(serviceMock.list(MockitoSupport.any(), any[Int]())(?)).thenReturn(
      bannedCallListMock
    )
    serviceMock
  }

  val rawCallListMock: Future[SlicedResult[RawCall]] =
    Future {
      SlicedResult(List(rawCall), 1, sliceMock)
    }

  val unmatchedCallServiceV2Mock: UnmatchedCallService = {
    val serviceMock = MockitoSupport.mock[UnmatchedCallService]
    when(serviceMock.list(MockitoSupport.any(), any[Int]())(?)).thenReturn(
      rawCallListMock
    )
    serviceMock
  }

  val phoneServiceMock: PhoneService = {
    val serviceMock = MockitoSupport.mock[PhoneService]
    when(serviceMock.provideInfo(MockitoSupport.any())).thenReturn(
      Future.successful(Some(phoneInfo))
    )
    serviceMock
  }

  val troublePhoneServiceMock: PhoneService = {
    val serviceMock = MockitoSupport.mock[PhoneServiceImpl]
    when(serviceMock.provideInfo(troubleSource.callerId.value))
      .thenReturn(Future.failed(CantParsePhone(troubleSource.callerId.value)))
    when(serviceMock.provideInfo(phoneNumber.value)).thenReturn(
      Future.successful(Some(phoneInfo))
    )
    serviceMock
  }

  "SourceClassifierService" should {
    "predict forecast \"bad\"" in {
      val sourceClassifierService: SourceClassifierService = new SourceClassifierServiceImpl(
        callServiceV2Mock,
        blockedCallServiceV2Mock,
        unmatchedCallServiceV2Mock,
        clientMock,
        phoneServiceMock
      )

      val goodRecallClassifierResponse = sourceClassifierService
        .classify(source, Classifiers.GoodRecallClassifier)
        .futureValue

      goodRecallClassifierResponse.badProbability should equal(0.75)
      goodRecallClassifierResponse.sourceClass should equal(SourceClasses.Bad)
    }

    "predict forecast \"good\"" in {
      val sourceClassifierService: SourceClassifierService = new SourceClassifierServiceImpl(
        callServiceV2Mock,
        blockedCallServiceV2Mock,
        unmatchedCallServiceV2Mock,
        clientMock,
        phoneServiceMock
      )

      val goodPrecisionClassifierResponse = sourceClassifierService
        .classify(source, Classifiers.GoodPrecisionClassifier)
        .futureValue

      goodPrecisionClassifierResponse.badProbability should equal(0.25)
      goodPrecisionClassifierResponse.sourceClass should equal(SourceClasses.Good)
    }

    "handle PhoneService exception" in {
      val sourceClassifierService: SourceClassifierService = new SourceClassifierServiceImpl(
        callServiceV2Mock,
        blockedCallServiceV2Mock,
        unmatchedCallServiceV2Mock,
        clientMock,
        troublePhoneServiceMock
      )

      val goodRecallClassifierResponse = sourceClassifierService
        .classify(troubleSource, Classifiers.GoodRecallClassifier)
        .futureValue
      goodRecallClassifierResponse.sourceClass should equal(SourceClasses.Unknown)
      goodRecallClassifierResponse.badProbability should equal(0.5)

      val goodPrecisionClassifierResponse = sourceClassifierService
        .classify(troubleSource, Classifiers.GoodRecallClassifier)
        .futureValue
      goodPrecisionClassifierResponse.sourceClass should equal(SourceClasses.Unknown)
      goodPrecisionClassifierResponse.badProbability should equal(0.5)
    }
  }
}
