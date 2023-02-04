package auto.dealers.dealer_pony.api.test.dealer_pony

import com.google.protobuf.wrappers.{BoolValue, Int32Value, Int64Value}
import ru.auto.api.dealer.proto.dealer_pony_model.{
  PhoneNumberResult,
  PhonesResultList,
  ResponseStatus,
  SimplePhonesList
}
import auto.dealers.dealer_pony.storage.dao.DealerPhonesDao.InsertedExceededCounterException
import auto.dealers.dealer_pony.api.dealer_pony.DealerPonyService
import auto.dealers.dealer_pony.api.dealer_pony.DealerPonyService.{
  RequestShouldContainPhoneNumbers,
  TooManyNumbersInOneRequest,
  WhiteListUnavailable
}
import auto.dealers.dealer_pony.model.{DealerId, PhoneNumber}
import ru.auto.dealer_pony.proto.api_model.{AddPhoneNumbersRequest, DeletePhoneNumbersRequest}
import auto.dealers.dealer_pony.storage.testkit.{DealerPhonesDaoMock, DealerStatusDaoMock}
import common.zio.logging.Logging
import zio.test.mock.Expectation._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import auto.dealers.dealer_pony.storage.dao.DealerPhonesDao

object DealerPonyServiceSpec extends DefaultRunnableSpec {
  val dealerId: DealerId = 1L

  override val spec: ZSpec[TestEnvironment, Any] = suite("DefaultDealerPonyService")(
    testM("add phone numbers") {
      val phoneNumber1 = PhoneNumber.fromString("+78009990010").toOption.get
      val phoneNumber2 = PhoneNumber.fromString("+78009990011").toOption.get
      val insertionResult = Map(
        phoneNumber1 -> Right(()),
        phoneNumber2 -> Right(())
      )

      val mocks = DealerStatusDaoMock.WlAvailable(equalTo(dealerId), value(true)).toLayer ++
        (DealerPhonesDaoMock
          .Insert(equalTo((dealerId, Set(phoneNumber1, phoneNumber2))), value(insertionResult)))
          .toLayer ++
        Logging.live >>> DealerPonyService.live

      val phoneRequest =
        AddPhoneNumbersRequest.of(dealerId, Some(SimplePhonesList.of(Seq("+78009990010", "+78009990011"))))

      val phonesResult = Seq(
        PhoneNumberResult.of("+78009990010", ResponseStatus.SUCCESS, ""),
        PhoneNumberResult.of("+78009990011", ResponseStatus.SUCCESS, "")
      )

      assertM(DealerPonyService.addPhoneNumbers(phoneRequest).provideCustomLayer(mocks).map(_.result))(
        hasSameElements(phonesResult)
      )
    },
    testM("add phone numbers with empty phones") {
      val mocks = DealerStatusDaoMock.WlAvailable(equalTo(dealerId), value(true)).toLayer ++
        DealerPhonesDaoMock.empty ++
        Logging.live >>> DealerPonyService.live

      val phoneRequest =
        AddPhoneNumbersRequest.of(dealerId, Some(SimplePhonesList.of(Nil)))

      assertM(DealerPonyService.addPhoneNumbers(phoneRequest).provideCustomLayer(mocks).run)(
        fails(isSubtype[RequestShouldContainPhoneNumbers](anything))
      )
    },
    testM("add phone numbers with invalid phone") {
      val phoneNumber2 = PhoneNumber.fromString("+78009990011").toOption.get
      val insertionResult = Map(
        phoneNumber2 -> Right(())
      )

      val mocks = DealerStatusDaoMock.WlAvailable(equalTo(dealerId), value(true)).toLayer ++
        (DealerPhonesDaoMock
          .Insert(equalTo((dealerId, Set(phoneNumber2))), value(insertionResult)))
          .toLayer ++
        Logging.live >>> DealerPonyService.live

      val phoneRequest =
        AddPhoneNumbersRequest.of(dealerId, Some(SimplePhonesList.of(Seq("+78009990010sss", "+78009990011"))))

      val phonesResult = Seq(
        PhoneNumberResult.of("+78009990010sss", ResponseStatus.ERROR, "+78009990010sss is not a valid phone number"),
        PhoneNumberResult.of("+78009990011", ResponseStatus.SUCCESS, "")
      )

      assertM(DealerPonyService.addPhoneNumbers(phoneRequest).provideCustomLayer(mocks).map(_.result))(
        hasSameElements(phonesResult)
      )
    },
    testM("add phone numbers and some error happens on insert") {
      val phoneNumber1 = PhoneNumber.fromString("+78009990010").toOption.get
      val phoneNumber2 = PhoneNumber.fromString("+78009990011").toOption.get
      val insertionResult = Map(
        phoneNumber1 -> Right(()),
        phoneNumber2 -> Left(DealerPhonesDao.DuplicatePhoneException(phoneNumber2))
      )

      val mocks = DealerStatusDaoMock.WlAvailable(equalTo(dealerId), value(true)).toLayer ++
        (DealerPhonesDaoMock
          .Insert(equalTo((dealerId, Set(phoneNumber1, phoneNumber2))), value(insertionResult)))
          .toLayer ++
        Logging.live >>> DealerPonyService.live

      val phoneRequest =
        AddPhoneNumbersRequest.of(dealerId, Some(SimplePhonesList.of(Seq("+78009990010", "+78009990011"))))

      val phonesResult = Seq(
        PhoneNumberResult.of("+78009990010", ResponseStatus.SUCCESS, ""),
        PhoneNumberResult.of("+78009990011", ResponseStatus.ERROR, "+78009990011 already exists")
      )

      assertM(DealerPonyService.addPhoneNumbers(phoneRequest).provideCustomLayer(mocks).map(_.result))(
        hasSameElements(phonesResult)
      )
    },
    testM("add 2 phone numbers when 1 entry left") {
      val phoneNumber1 = PhoneNumber.fromString("+78009990010").toOption.get
      val phoneNumber2 = PhoneNumber.fromString("+78009990011").toOption.get

      val mocks = DealerStatusDaoMock.WlAvailable(equalTo(dealerId), value(true)).toLayer ++
        (DealerPhonesDaoMock
          .Insert(equalTo((dealerId, Set(phoneNumber1, phoneNumber2))), failure(InsertedExceededCounterException)) ++
          DealerPhonesDaoMock.EntriesLeftForDealer(equalTo(dealerId), value(1))).toLayer ++
        Logging.live >>> DealerPonyService.live

      val phoneRequest =
        AddPhoneNumbersRequest.of(dealerId, Some(SimplePhonesList.of(Seq("+78009990010", "+78009990011"))))

      assertM(DealerPonyService.addPhoneNumbers(phoneRequest).provideCustomLayer(mocks).run)(
        fails(isSubtype[TooManyNumbersInOneRequest](anything))
      )
    },
    testM("add phone numbers with not available status") {
      val mocks = DealerStatusDaoMock.WlAvailable(equalTo(dealerId), value(false)).toLayer ++
        DealerPhonesDaoMock.empty ++
        Logging.live >>> DealerPonyService.live

      val phoneRequest =
        AddPhoneNumbersRequest.of(dealerId, Some(SimplePhonesList.of(Seq("+78009990010", "+78009990011"))))

      assertM(DealerPonyService.addPhoneNumbers(phoneRequest).provideCustomLayer(mocks).run)(
        fails(isSubtype[WhiteListUnavailable](anything))
      )
    },
    testM("delete phone numbers") {
      val phoneNumber1 = PhoneNumber.fromString("+78009990010").toOption.get
      val phoneNumber2 = PhoneNumber.fromString("+78009990011").toOption.get
      val insertionResult = Map(
        phoneNumber1 -> Right(()),
        phoneNumber2 -> Right(())
      )

      val mocks = DealerStatusDaoMock.WlAvailable(equalTo(dealerId), value(true)).toLayer ++
        DealerPhonesDaoMock
          .MarkToBeDeleted(equalTo((dealerId, Set(phoneNumber1, phoneNumber2))), value(insertionResult))
          .toLayer ++
        Logging.live >>> DealerPonyService.live

      val phoneRequest =
        DeletePhoneNumbersRequest.of(dealerId, Some(SimplePhonesList.of(Seq("+78009990010", "+78009990011"))))

      val phonesResult = Seq(
        PhoneNumberResult.of("+78009990010", ResponseStatus.SUCCESS, ""),
        PhoneNumberResult.of("+78009990011", ResponseStatus.SUCCESS, "")
      )

      assertM(DealerPonyService.deletePhoneNumbers(phoneRequest).provideCustomLayer(mocks).map(_.result))(
        hasSameElements(phonesResult)
      )
    },
    testM("delete phone numbers with empty phones") {
      val mocks = DealerStatusDaoMock.WlAvailable(equalTo(dealerId), value(true)).toLayer ++
        DealerPhonesDaoMock.empty ++
        Logging.live >>> DealerPonyService.live

      val phoneRequest =
        DeletePhoneNumbersRequest.of(dealerId, Some(SimplePhonesList.of(Nil)))

      assertM(DealerPonyService.deletePhoneNumbers(phoneRequest).provideCustomLayer(mocks).run)(
        fails(isSubtype[RequestShouldContainPhoneNumbers](anything))
      )
    },
    testM("delete phone numbers with invalid phone") {
      val phoneNumber2 = PhoneNumber.fromString("+78009990011").toOption.get
      val insertionResult = Map(
        phoneNumber2 -> Right(())
      )

      val mocks = DealerStatusDaoMock.WlAvailable(equalTo(dealerId), value(true)).toLayer ++
        DealerPhonesDaoMock.MarkToBeDeleted(equalTo((dealerId, Set(phoneNumber2))), value(insertionResult)).toLayer ++
        Logging.live >>> DealerPonyService.live

      val phoneRequest =
        DeletePhoneNumbersRequest.of(dealerId, Some(SimplePhonesList.of(Seq("+78009990010sss", "+78009990011"))))

      val phonesResult = Seq(
        PhoneNumberResult.of("+78009990010sss", ResponseStatus.ERROR, "+78009990010sss is not a valid phone number"),
        PhoneNumberResult.of("+78009990011", ResponseStatus.SUCCESS, "")
      )

      assertM(DealerPonyService.deletePhoneNumbers(phoneRequest).provideCustomLayer(mocks).map(_.result))(
        hasSameElements(phonesResult)
      )
    },
    testM("delete phone numbers with not available status") {
      val mocks = DealerStatusDaoMock.WlAvailable(equalTo(dealerId), value(false)).toLayer ++
        DealerPhonesDaoMock.empty ++
        Logging.live >>> DealerPonyService.live

      val phoneRequest =
        DeletePhoneNumbersRequest.of(dealerId, Some(SimplePhonesList.of(Seq("+78009990010", "+78009990011"))))

      assertM(DealerPonyService.deletePhoneNumbers(phoneRequest).provideCustomLayer(mocks).run)(
        fails(isSubtype[WhiteListUnavailable](anything))
      )
    },
    testM("list phone numbers") {
      val phonesFromDao =
        Seq(PhoneNumber.fromString("+78009990010").toOption.get, PhoneNumber.fromString("+78009990011").toOption.get)

      val phonesResult = SimplePhonesList.of(Seq("+78009990010", "+78009990011"))

      val mocks = DealerStatusDaoMock.WlAvailable(equalTo(dealerId), value(true)).toLayer ++
        DealerPhonesDaoMock.Get(equalTo(dealerId), value(phonesFromDao)).toLayer ++
        Logging.live >>> DealerPonyService.live

      assertM(DealerPonyService.listPhoneNumbers(dealerId).provideCustomLayer(mocks))(
        equalTo(phonesResult)
      )
    },
    testM("list phone numbers not available") {
      val mocks = DealerStatusDaoMock.WlAvailable(equalTo(dealerId), value(false)).toLayer ++
        DealerPhonesDaoMock.empty ++
        Logging.live >>> DealerPonyService.live

      assertM(DealerPonyService.listPhoneNumbers(dealerId).provideCustomLayer(mocks).run)(
        fails(isSubtype[WhiteListUnavailable](anything))
      )
    },
    testM("phone entries left") {
      val mocks = DealerStatusDaoMock.empty ++
        DealerPhonesDaoMock.EntriesLeftForDealer(equalTo(dealerId), value(198)).toLayer ++
        Logging.live >>> DealerPonyService.live

      assertM(DealerPonyService.phoneEntriesLeft(dealerId).provideCustomLayer(mocks))(
        equalTo(198)
      )
    },
    testM("white list available") {
      val mocks = DealerStatusDaoMock.WlAvailable(equalTo(dealerId), value(true)).toLayer ++
        DealerPhonesDaoMock.empty ++
        Logging.live >>> DealerPonyService.live

      assertM(DealerPonyService.whiteListAvailable(dealerId).provideCustomLayer(mocks))(
        isTrue
      )
    }
  )
}
