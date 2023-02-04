package ru.yandex.vertis.telepony.service

import org.joda.time.DateTime
import org.scalactic.anyvals.{PosInt, PosZInt}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.{Assertion, BeforeAndAfterEach, OptionValues}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SampleHelper._
import ru.yandex.vertis.telepony.component.{MtsComponent, MttComponent}
import ru.yandex.vertis.telepony.exception.NoReadyNumbersException
import ru.yandex.vertis.telepony.generator.Generator
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.geo.{RegionGeneralizerService, RegionTreeService}
import ru.yandex.vertis.telepony.model.AntiFraud.{All, AonAndBlAndCounter, Disabled}
import ru.yandex.vertis.telepony.model.RedirectOptions.RedirectCallbackInfo
import ru.yandex.vertis.telepony.model.Status.Busy
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.RedirectServiceV2.AvailableRequest.PhoneAvailableRequest
import ru.yandex.vertis.telepony.service.RedirectServiceV2.RedirectKeyFilter.ByKey
import ru.yandex.vertis.telepony.service.RedirectServiceV2.{
  Clear,
  ComplainRequest,
  CreateFromExistingResponse,
  CreateRequest,
  Filter,
  ListRedirectRequest,
  RedirectKeyFilter,
  Update,
  UpdateOptionsRequest
}
import ru.yandex.vertis.telepony.service.mts.InMemoryMtsClient
import ru.yandex.vertis.telepony.service.mtt.InMemoryMttClient
import ru.yandex.vertis.telepony.util.Range._
import ru.yandex.vertis.telepony.{time, SpecBase}

import scala.annotation.nowarn
import scala.collection.mutable
import scala.concurrent.{Await, Future}

/**
  * @author evans
  */
@nowarn
trait RedirectServiceV2Spec
  extends SpecBase
  with BeforeAndAfterEach
  with OptionValues
  with ScalaCheckPropertyChecks
  with MockitoSupport { self: MtsComponent with MttComponent =>

  implicit def propertyCheckConfiguration: PropertyCheckConfiguration =
    PropertyCheckConfiguration(sizeRange = PosZInt(5), minSuccessful = PosInt(5))

  def clean(): Unit

  def operatorNumberServiceV2: OperatorNumberServiceV2

  def redirectServiceV2: RedirectServiceV2

  def touchRedirectRequests: mutable.Buffer[TouchRedirectRequest]

  def defaultMtsAccount: OperatorAccount

  def otherMtsAccount: OperatorAccount

  def operatorLabelService: OperatorLabelService

  def regionTree: RegionTreeService

  def phoneAnalyzer: PhoneAnalyzerService

  def regionGeneralizerService: RegionGeneralizerService

  def sharedRedirectService: SharedRedirectService

  def typedDomain: TypedDomain

  override protected def beforeEach(): Unit = {
    try {
      super.beforeEach()
    } finally {
      clean()
    }
  }

  import scala.concurrent.duration._

  val SpbCreateRedirect = createRequestV2Gen(SpbPhoneGen).map(_.copy(geoId = None))

  val MoscowCreateRedirect =
    createRequestV2Gen(MoscowPhoneGen)
      .map(_.copy(geoId = None, phoneType = None, preferredOperator = None))
  val DowntimeGen = Generator.TtlGen

  private def createRedirect(target: Phone) =
    createRequestV2Gen(MoscowPhoneGen).map { cr =>
      cr.copy(key = cr.key.copy(target = target), geoId = None, phoneType = None)
    }

  private def prepareOperatorNumber(
      phone: Phone,
      status: Status = Status.Ready(None),
      account: OperatorAccount = defaultMtsAccount,
      originOperator: Operator = Operators.Mts,
      geoId: Option[GeoId] = None) = {
    val createRequest = createNumberRequest(phone, account, originOperator).copy(status = Some(status))
    val opn = operatorNumberServiceV2.create(createRequest).futureValue
    account.operator match {
      case Operators.Mts =>
        mtsClients(account).clientV4.asInstanceOf[InMemoryMtsClient].registerNewUniversalNumber(phone)
      case Operators.Mtt =>
        mttClients(account).asInstanceOf[InMemoryMttClient].registerNewPhone(phone)
      case Operators.Vox =>
      // do nothing
    }
    opn
  }

  implicit private class RichPhone(val phone: Phone) {
    def region = phoneAnalyzer.getRegion(phone).futureValue
    def phoneType = phoneAnalyzer.getPhoneType(phone).futureValue
    def generalRegion = regionGeneralizerService.generalize(region, phoneType).futureValue
  }

  private val invalidOptions = RedirectOptions.Empty.copy(
    callerIdMode = Some(true),
    callbackInfo = Some(RedirectCallbackInfo(CallPeriodsGen.next, Some("some name")))
  )

  "Redirect service" should {
    "create redirect" in {
      forAll(MoscowPhoneGen, MoscowCreateRedirect) { (phone, redirectCreate) =>
        prepareOperatorNumber(phone)
        val redirect = redirectServiceV2.create(redirectCreate).futureValue
        redirect.key shouldEqual redirectCreate.key
        redirect.source.number shouldEqual phone
        clean()
      }
    }
    "not create redirect with invalid options set" in {
      val (phone, redirectRequest) = (MoscowPhoneGen.next, MoscowCreateRedirect.next.copy(ttl = None))
      prepareOperatorNumber(phone)
      val options = RedirectOptionsGen.map { options =>
        options.copy(
          callerIdMode = Some(true),
          callbackInfo = Some(RedirectCallbackInfoGen.next)
        )
      }.next
      val modifiedRequest = redirectRequest.copy(options = Some(options))
      redirectServiceV2.create(modifiedRequest).failed.futureValue shouldBe an[RedirectOptions.InvalidOptionsException]
    }
    "create redirect without ttl when not exist" in {
      val (phone, redirectCreate) = (MoscowPhoneGen.next, MoscowCreateRedirect.next.copy(ttl = None))
      prepareOperatorNumber(phone)
      val redirect = redirectServiceV2.getOrCreate(redirectCreate).futureValue
      val found = redirectServiceV2.getOrCreate(redirectCreate).futureValue
      redirect.key shouldEqual redirectCreate.key
      redirect.source.number shouldEqual phone
      found shouldEqual redirect
    }
    "create redirect without ttl when old one is expired" in {
      val (phone, redirectCreate) = (MoscowPhoneGen.next, MoscowCreateRedirect.next.copy(ttl = Some(50.millis)))
      prepareOperatorNumber(phone)
      redirectServiceV2.getOrCreate(redirectCreate).futureValue
      Thread.sleep(1000)
      val found1 = redirectServiceV2.getOrCreate(redirectCreate)
      found1.failed.futureValue shouldBe an[NoReadyNumbersException]
    }
    "emit TouchRedirectRequest" in {
      val (phone, redirectCreate) = (MoscowPhoneGen.next, MoscowCreateRedirect.next.copy(ttl = Some(1.day)))
      prepareOperatorNumber(phone)
      redirectServiceV2.getOrCreate(redirectCreate).futureValue
      val redirect = redirectServiceV2.getOrCreate(redirectCreate).futureValue
      val requests = touchRedirectRequests.filter(_.redirectId == redirect.id)
      requests should have size 1
      val touchRedirectRequest = touchRedirectRequests.head
      touchRedirectRequest.ttl shouldEqual redirectCreate.ttl
      touchRedirectRequest.antiFraud shouldEqual redirect.antiFraud
    }
    "update antiFraud" in {
      val (phone, redirectCreate) = (MoscowPhoneGen.next, MoscowCreateRedirect.next.copy(ttl = Some(1.day)))
      prepareOperatorNumber(phone)
      redirectServiceV2.getOrCreate(redirectCreate.copy(antiFraudOptions = Disabled)).futureValue
      val redirect = redirectServiceV2
        .getOrCreate(redirectCreate.copy(antiFraudOptions = All))
        .futureValue
      val requests = touchRedirectRequests.filter(_.redirectId == redirect.id)
      requests should have size 1
      val touchRedirectRequest = touchRedirectRequests.head
      touchRedirectRequest.antiFraud shouldEqual All
    }
    "update to blacklist antiFraud" in {
      val (phone, redirectCreate) = (MoscowPhoneGen.next, MoscowCreateRedirect.next.copy(ttl = Some(1.day)))
      prepareOperatorNumber(phone)
      redirectServiceV2.getOrCreate(redirectCreate.copy(antiFraudOptions = Disabled)).futureValue
      val redirect = redirectServiceV2
        .getOrCreate(redirectCreate.copy(antiFraudOptions = AonAndBlAndCounter))
        .futureValue
      val requests = touchRedirectRequests.filter(_.redirectId == redirect.id)
      requests should have size 1
      val touchRedirectRequest = touchRedirectRequests.head
      touchRedirectRequest.antiFraud shouldEqual AonAndBlAndCounter
    }
    "not update antiFraud" in {
      val (phone, redirectCreate) = (MoscowPhoneGen.next, MoscowCreateRedirect.next.copy(ttl = Some(1.day)))
      prepareOperatorNumber(phone)
      redirectServiceV2.getOrCreate(redirectCreate.copy(antiFraudOptions = All)).futureValue
      val found1 = redirectServiceV2
        .getOrCreate(redirectCreate.copy(antiFraudOptions = Disabled))
        .futureValue
      found1.antiFraud shouldEqual All
    }
    "not update antiFraud to blacklist" in {
      val (phone, redirectCreate) = (MoscowPhoneGen.next, MoscowCreateRedirect.next.copy(ttl = Some(1.day)))
      prepareOperatorNumber(phone)
      redirectServiceV2.getOrCreate(redirectCreate.copy(antiFraudOptions = All)).futureValue
      val found1 = redirectServiceV2
        .getOrCreate(redirectCreate.copy(antiFraudOptions = AonAndBlAndCounter))
        .futureValue
      found1.antiFraud shouldEqual All
    }
    "create redirect when not exist" in {
      forAll(MoscowPhoneGen, MoscowCreateRedirect) { (phone, redirectCreate) =>
        prepareOperatorNumber(phone)
        val redirect = redirectServiceV2.getOrCreate(redirectCreate).futureValue
        redirect.key shouldEqual redirectCreate.key
        redirect.source.number shouldEqual phone
        clean()
      }
    }

    "create redirect with new parameters when exists diff redirect" in {
      forAll(MoscowPhoneGen, SpbPhoneGen, MoscowCreateRedirect) { (moscowPhone, spbPhone, redirectCreateTemplate) =>
        val moscowRedirectCreate = redirectCreateTemplate.copy(geoId = Some(213))
        val spbRedirectCreate = redirectCreateTemplate.copy(geoId = Some(2))
        prepareOperatorNumber(moscowPhone)
        prepareOperatorNumber(spbPhone)
        val moscowRedirect = redirectServiceV2.getOrCreate(moscowRedirectCreate).futureValue
        val spbRedirect = redirectServiceV2.getOrCreate(spbRedirectCreate).futureValue
        (moscowRedirect.source.number should not).equal(spbRedirect.source.number)
        clean()
      }
    }
    def withEmptyDeadline(status: Status): Status =
      Status(status.value, None, status.updateTime)

    "get redirect when exist" in {
      forAll(MoscowPhoneGen, MoscowCreateRedirect.suchThat(_.ttl.exists(_.toSeconds > 5))) { (phone, redirectCreate) =>
        prepareOperatorNumber(phone)
        val redirect = redirectServiceV2.create(redirectCreate).futureValue
        val found = redirectServiceV2.getOrCreate(redirectCreate).futureValue
        redirect.key shouldEqual redirectCreate.key
        redirect.source.number shouldEqual phone
        found.copy(
          source = found.source.copy(status = withEmptyDeadline(found.source.status))
        ) shouldEqual redirect.copy(
          source = redirect.source.copy(status = withEmptyDeadline(redirect.source.status))
        )
        clean()
      }
    }

    "get redirect when exist (crazy case/ geo tree)" in {
      val MskOblPhone1 = Phone("+79199695618") // geo-id Msk obl
      val MskOblPhone2 = Phone("+79854702450") // geo-id Msk obl

      prepareOperatorNumber(MskOblPhone1)
      prepareOperatorNumber(MskOblPhone2)

      val targetPhone = Phone("+74994290941") // geo-id Msk

      val redirectCreate = {
        val cr = Generator.createRequestV2Gen(MoscowPhoneGen).next
        cr.copy(key = cr.key.copy(target = targetPhone), geoId = None)
      }

      val x = redirectServiceV2.getOrCreate(redirectCreate).futureValue
      val y = redirectServiceV2.getOrCreate(redirectCreate).futureValue
      x.source.number shouldEqual y.source.number
    }

    "get actual redirect" in {
      forAll(MoscowPhoneGen, MoscowCreateRedirect) { (phone, redirectCreate) =>
        prepareOperatorNumber(phone)
        val value1: Option[ActualRedirect] = redirectServiceV2.get(phone).futureValue
        value1 shouldEqual None
        val r1 = redirectServiceV2.create(redirectCreate).futureValue
        val r2 = redirectServiceV2.get(phone).futureValue.get
        r2.id shouldEqual r1.id
        r2.source shouldEqual r1.source
        r2.target shouldEqual r1.target
        clean()
      }
    }
    "get by redirect id" in {
      forAll(MoscowPhoneGen, MoscowCreateRedirect) { (phone, redirectCreate) =>
        prepareOperatorNumber(phone)
        val value1: Option[ActualRedirect] = redirectServiceV2.get(phone).futureValue
        value1 shouldEqual None
        val r1 = redirectServiceV2.create(redirectCreate).futureValue
        val r2 = redirectServiceV2.get(r1.id).futureValue.get
        r2.id shouldEqual r1.id
        r2.source shouldEqual r1.source
        r2.target shouldEqual r1.target
        clean()
      }
    }

    "delete redirect by id" in {
      forAll(MoscowPhoneGen, MoscowCreateRedirect) { (phone, redirectCreate) =>
        prepareOperatorNumber(phone)
        val redirect = redirectServiceV2.create(redirectCreate).futureValue
        redirectServiceV2.delete(redirect.id, None, false).futureValue
        redirectServiceV2.get(phone).futureValue shouldEqual None
        clean()
      }
    }

    "delete several redirects by objectId" in {
      forAll(MoscowPhoneGen, MoscowCreateRedirect) { (phone, redirectCreate) =>
        prepareOperatorNumber(phone)
        val redirect = redirectServiceV2.create(redirectCreate).futureValue
        redirectServiceV2.delete(redirect.objectId, None, false).futureValue
        redirectServiceV2.get(phone).futureValue shouldEqual None
        clean()
      }
    }

    "list actual redirects" in {
      val phone = MoscowPhoneGen.next
      val redirectCreate = MoscowCreateRedirect.next
      prepareOperatorNumber(phone)
      redirectServiceV2.create(redirectCreate).futureValue
      val r1 = redirectServiceV2.get(phone).futureValue.get
      val r2 = redirectServiceV2.list(RedirectServiceV2.Filter.Empty, Full).futureValue.head
      r2 shouldEqual r1
    }

    "list actual and outdated redirects" in {
      val phone1 = MoscowPhoneGen.next
      val redirectCreate1 = MoscowCreateRedirect.next
      prepareOperatorNumber(phone1)
      redirectServiceV2.create(redirectCreate1).futureValue
      val redirect1 = redirectServiceV2.get(phone1).futureValue.get

      val phone2 = MoscowPhoneGen.next
      val redirectCreate2 = MoscowCreateRedirect.next
      prepareOperatorNumber(phone2)
      redirectServiceV2.create(redirectCreate2).futureValue
      val redirect2 = redirectServiceV2.get(phone2).futureValue.get
      redirectServiceV2.delete(redirect2.id, downtime = None, false).futureValue

      val request = ListRedirectRequest(Full)
      val redirects = redirectServiceV2.listHistory(request).futureValue
      redirects.size shouldEqual 2
      redirects.map(_.id) should contain theSameElementsAs Seq(redirect1.id, redirect2.id)
    }

    "delete redirect and put operator number in downtime state" in {
      forAll(MoscowPhoneGen, MoscowCreateRedirect, DowntimeGen) { (phone, redirectCreate, downtime) =>
        prepareOperatorNumber(phone)
        val redirect = redirectServiceV2.create(redirectCreate).futureValue
        redirectServiceV2.delete(redirect.id, downtime, false).futureValue
        val nextOpn = operatorNumberServiceV2.get(phone).futureValue
        nextOpn.status.value shouldEqual StatusValues.Downtimed
        val deadline = nextOpn.status.deadline.get
        val ttl = downtime.getOrElse(7.days)
        val gap = 1.minute
        deadline.isBefore(DateTime.now.plusMillis((ttl + gap).toMillis.toInt)) shouldEqual true
        deadline.isAfter(DateTime.now.plusMillis((ttl - gap).toMillis.toInt)) shouldEqual true
        clean()
      }
    }

    "fail create redirect if no available ready numbers without redirects" in {
      val phone = MoscowPhoneGen.next
      val redirectCreate = MoscowCreateRedirect.next
      prepareOperatorNumber(phone)

      redirectServiceV2.create(redirectCreate).futureValue
      prepareOperatorNumber(MoscowPhoneGen.next, Status.New(None))
      val res = redirectServiceV2.create(MoscowCreateRedirect.next)
      res.failed.futureValue shouldBe an[NoReadyNumbersException]
    }

    "fail redirect, if no number with proper geo-id" in {
      forAll(MoscowPhoneGen, SpbCreateRedirect) { (phone, redirectCreate) =>
        prepareOperatorNumber(phone)
        val f = redirectServiceV2.create(redirectCreate).failed.futureValue
        f shouldBe an[NoReadyNumbersException]
        clean()
      }
    }

    "find proper geo-id" in {
      val phone1 = Phone("+74952766501") // 1,Москва и Московская область

      prepareOperatorNumber(phone1)

      val target = Phone("+74952768902") // 98599,Одинцовский район
      val createRequest = createRedirect(target).next

      val redirect = redirectServiceV2.create(createRequest).futureValue

      redirect.source.number shouldEqual phone1
    }

    "find parent geo" in {
      val phone1 = Phone("+74963220000") // 1,Москва и Московская область
      prepareOperatorNumber(phone1)
      val target = Phone("+74952768902") // 98599,Одинцовский район
      val createRequest = createRedirect(target).next
      import scala.concurrent.duration._
      val redirect = Await.result(redirectServiceV2.create(createRequest), 1.hour)

      redirect.source.number shouldEqual phone1
    }

    "fail if nothing matches" in {
      val phone2 = Phone("+78123836303") // 2 SPb

      prepareOperatorNumber(phone2)
      val target = Phone("+74952770001") // 213, moscow
      val createRequest = createRedirect(target).next

      redirectServiceV2.create(createRequest).failed.futureValue
    }

    "get available" in {
      val phone = Phone("+74952768901") // 98599,Одинцовский район
      prepareOperatorNumber(phone)
      val req = PhoneAvailableRequest(phone, None)
      redirectServiceV2.countAvailable(req).futureValue shouldEqual 1
    }

    "get available with fallback regions" in {
      val phone1 = Phone("+79034470001") // 10995,Краснодарский край
      val phone2 = Phone("+78772571593") // 1093,Майкоп - Республика адыгея
      prepareOperatorNumber(phone1)
      val req = PhoneAvailableRequest(phone2, Some(PhoneTypes.Mobile))
      redirectServiceV2.countAvailable(req).futureValue shouldEqual 1
    }

    "countAvailable and create are consistent" when {
      "there are no available numbers" in {
        val phone1 = Phone("+74952770001") // 213, moscow
        val phone2 = Phone("+79213395291") // 2, SPb
        phone1.region should !==(phone2.region)
        prepareOperatorNumber(phone1)
        val req = PhoneAvailableRequest(phone2, None)
        redirectServiceV2.countAvailable(req).futureValue shouldEqual 0
        val createRequest = createRedirect(phone2).next
        redirectServiceV2.create(createRequest).failed.futureValue
      }
      "source and target are mobile" in {
        val source = Phone("+79213395292")
        val target = Phone("+79213395293")
        source.region should ===(target.region)
        val geoId = target.generalRegion
        prepareOperatorNumber(source, geoId = Some(geoId))
        val req = PhoneAvailableRequest(target, None)
        redirectServiceV2.countAvailable(req).futureValue should ===(1)
        val createRequest = createRedirect(target).next
        redirectServiceV2.create(createRequest).futureValue.source.number should ===(source)
      }
      "source is mobile and target is local" in {
        val source = Phone("+79213395294") // 2 SPb
        val target = Phone("+78123836302") // 2 SPb
        source.region should ===(target.region)
        prepareOperatorNumber(source)
        val req = PhoneAvailableRequest(target, None)
        redirectServiceV2.countAvailable(req).futureValue shouldEqual 1
        val createRequest = createRedirect(target).next
        redirectServiceV2.create(createRequest).futureValue.source.number should ===(source)
      }
      "source is local and target is mobile" in {
        val source = Phone("+78123836303") // 2 SPb
        val target = Phone("+79213395295") // 2 SPb
        source.region should ===(target.region)
        prepareOperatorNumber(source)
        val req = PhoneAvailableRequest(target, Some(PhoneTypes.Local))
        redirectServiceV2.countAvailable(req).futureValue shouldEqual 1
        val createRequest = createRedirect(target).next.copy(phoneType = Some(PhoneTypes.Local))
        redirectServiceV2.create(createRequest).futureValue.source.number should ===(source)
      }
    }

    "cache successfully" in {
      val phone = Phone("+78123836303") // 2 SPb
      prepareOperatorNumber(phone)
      val req = PhoneAvailableRequest(phone, None)
      redirectServiceV2.countAvailable(req).futureValue shouldEqual 1
      val target = Phone("+74952770001") // 213, moscow
      val createRequest = createRedirect(target).next
      redirectServiceV2.create(createRequest).failed.futureValue
      redirectServiceV2.countAvailable(req).futureValue shouldEqual 1
    }

    "create redirect when preferred operator exist" in {
      val mtsPhone = MoscowPhoneGen.next
      val mttPhone = MoscowPhoneGen.next
      val mttRedirectCreate = MoscowCreateRedirect.next
        .copy(ttl = None, preferredOperator = Some(Operators.Mtt))
      prepareOperatorNumber(mtsPhone, account = OperatorAccounts.MtsShared, originOperator = Operators.Mts)
      prepareOperatorNumber(mttPhone, account = OperatorAccounts.MttShared, originOperator = Operators.Mtt)
      val redirect = redirectServiceV2.getOrCreate(mttRedirectCreate).futureValue
      redirect.key shouldEqual mttRedirectCreate.key
      redirect.source.number shouldEqual mttPhone
      redirect.source.operator shouldEqual Operators.Mtt
    }

    "do not create redirect when preferred operator does not exist" in {
      val mtsPhone = MoscowPhoneGen.next
      val mttRedirectCreate = MoscowCreateRedirect.next
        .copy(ttl = None, preferredOperator = Some(Operators.Mtt))
      prepareOperatorNumber(mtsPhone, account = OperatorAccounts.MtsShared, originOperator = Operators.Mts)
      val f = redirectServiceV2.getOrCreate(mttRedirectCreate).failed.futureValue
      f shouldBe an[NoReadyNumbersException]
    }

    "create redirects using different accounts" in {
      forAll(MoscowPhoneGen, MoscowPhoneGen, MoscowCreateRedirect, MoscowCreateRedirect) {
        (phone1, phone2, request1, request2) =>
          val phones = Set(phone1, phone2)
          prepareOperatorNumber(phone1, account = defaultMtsAccount, originOperator = Operators.Mts)
          prepareOperatorNumber(phone2, account = otherMtsAccount, originOperator = Operators.Mts)

          def createTestRedirect(request: CreateRequest): ActualRedirect = {
            val redirect = redirectServiceV2.create(request).futureValue
            redirect.key shouldBe request.key
            phones(redirect.source.number) shouldBe true
            redirect
          }

          val redirect1 = createTestRedirect(request1)
          val redirect2 = createTestRedirect(request2)
          redirect1.source.number should not be redirect2.source.number
          redirect1.source.account should not be redirect2.source.account
          clean()
      }
    }

    "create new redirect and delete old when replaceIfPresent" in {
      val (oldPhone, redirectCreate) = (MoscowPhoneGen.next, MoscowCreateRedirect.next.copy(ttl = None))
      prepareOperatorNumber(oldPhone)
      val oldRedirect = redirectServiceV2.getOrCreate(redirectCreate).futureValue
      // oldPhone now is obsolete
      val newPhone = MoscowPhoneGen.next
      prepareOperatorNumber(newPhone)
      val maybeNewRedirect = redirectServiceV2.replace(oldRedirect).futureValue
      maybeNewRedirect shouldBe defined
      maybeNewRedirect.foreach { newRedirect =>
        newRedirect.source.status.value shouldEqual oldRedirect.source.status.value
        newRedirect shouldEqual oldRedirect.copy(
          id = newRedirect.id,
          createTime = newRedirect.createTime,
          source = oldRedirect.source.copy(
            number = newRedirect.source.number,
            status = newRedirect.source.status
          )
        )
      }
      val maybeOldRedirect = redirectServiceV2.get(oldPhone).futureValue
      maybeOldRedirect should not be defined
      val getMaybeNewRedirect = redirectServiceV2.get(newPhone).futureValue
      getMaybeNewRedirect shouldEqual maybeNewRedirect
    }

    "do nothing when old redirect expired" in {
      val expiredOpn = OperatorNumberGen.next.copy(status = Busy.until(time.now().minusMinutes(1)))
      val phone = MoscowPhoneGen.next
      val oldRedirect =
        ActualRedirect(
          RedirectId("1"),
          RedirectKey(ObjectId("1"), phone, Tag.Empty),
          time.now().minusMinutes(2),
          expiredOpn,
          Set(),
          None
        )
      val res = redirectServiceV2.replace(oldRedirect).futureValue
      res shouldEqual None
    }

    "delete redirect by operator number and move number to garbage" in {
      val (phone, redirectCreate) = (MoscowPhoneGen.next, MoscowCreateRedirect.next.copy(ttl = None))
      prepareOperatorNumber(phone)
      val redirect = redirectServiceV2.getOrCreate(redirectCreate).futureValue
      val source = redirect.source.number
      redirectServiceV2.complain(source, ComplainRequest.WithTtl(Some(10.seconds))).futureValue
      redirectServiceV2.get(source).futureValue should be(empty)
      operatorNumberServiceV2.get(source).futureValue.status.value should ===(StatusValues.Garbage)
    }

    "move number to garbage even there is no redirect for it" in {
      val source = PhoneGen.next
      val request = createNumberRequest(source, defaultMtsAccount)
        .copy(status = Some(Status.Ready.forDuration(1.day)))
      operatorNumberServiceV2.create(request).futureValue
      redirectServiceV2.get(source).futureValue should be(empty)
      redirectServiceV2.complain(source, ComplainRequest.WithTtl(None)).futureValue
      val opn = operatorNumberServiceV2.get(source).futureValue
      opn.status.value should ===(StatusValues.Garbage)
      opn.status.deadline should ===(None)
    }

    "update options" should {

      val voxUsername = ShortStr.next
      val voxUsername2 = ShortStr.next
      val doubleRedirectNumber = PhoneGen.next
      val callPassRules = CallPassRules(Set(RefinedSourceGen.next))
      val redirectCallbackInfo = RedirectCallbackInfoGen.next

      def prepareRedirect: (CreateRequest, ActualRedirect) = {
        val (phone, redirectCreate) = (MoscowPhoneGen.next, MoscowCreateRedirect.next.copy(ttl = None))
        prepareOperatorNumber(phone)
        (redirectCreate, redirectServiceV2.getOrCreate(redirectCreate).futureValue)
      }

      def enableAllValidOptions(redirect: ActualRedirect): Unit =
        redirectServiceV2
          .updateOptions(
            redirect.objectId,
            redirect.target,
            UpdateOptionsRequest(
              callerIdModeOp = Update(false),
              needAnswerOp = Update(true),
              recordEnabledOp = Update(true),
              doubleRedirectOp = Update(doubleRedirectNumber),
              allowRedirectUnsuccessfulOp = Update(true),
              callPassRulesOp = Update(callPassRules),
              voxUsernameOp = Update(voxUsername),
              redirectCallbackInfoOp = Update(redirectCallbackInfo)
            )
          )
          .futureValue

      def checkInvalidUpdate(updateFn: (ActualRedirect, UpdateOptionsRequest) => Future[Unit]): Assertion = {
        val redirect = prepareRedirect._2
        val rq = UpdateOptionsRequest(
          callerIdModeOp = Update(true),
          redirectCallbackInfoOp = Update(RedirectCallbackInfoGen.next)
        )
        val future = updateFn(redirect, rq)
        future.failed.futureValue shouldBe an[RedirectOptions.InvalidOptionsException]
      }

      "not be able to update to invalid options set via redirectId" in {
        checkInvalidUpdate((redirect, rq) => redirectServiceV2.updateOptions(redirect.id, rq))
      }

      "not be able to update to invalid options set via objectId and target" in {
        checkInvalidUpdate((redirect, rq) => redirectServiceV2.updateOptions(redirect.objectId, redirect.target, rq))
      }

      "be able to set all valid options" in {
        val (redirectCreate, redirect) = prepareRedirect
        enableAllValidOptions(redirect)
        val updated = redirectServiceV2.getOrCreate(redirectCreate).futureValue
        updated.options shouldEqual Some(
          RedirectOptions.Empty.copy(
            callerIdMode = Some(false),
            needAnswer = Some(true),
            recordEnabled = Some(true),
            doubleRedirectNumber = Some(doubleRedirectNumber),
            allowRedirectUnsuccessful = Some(true),
            callPassRules = Some(callPassRules),
            voxUsername = Some(voxUsername),
            callbackInfo = Some(redirectCallbackInfo)
          )
        )
      }

      "not affect non-present in query options" in {
        val (redirectCreate, redirect) = prepareRedirect
        enableAllValidOptions(redirect)
        redirectServiceV2
          .updateOptions(
            redirect.objectId,
            redirect.target,
            UpdateOptionsRequest(callerIdModeOp = Update(false))
          )
          .futureValue
        val updated = redirectServiceV2.getOrCreate(redirectCreate).futureValue
        updated.options shouldEqual Some(
          RedirectOptions.Empty.copy(
            callerIdMode = Some(false),
            needAnswer = Some(true),
            recordEnabled = Some(true),
            doubleRedirectNumber = Some(doubleRedirectNumber),
            allowRedirectUnsuccessful = Some(true),
            callPassRules = Some(callPassRules),
            voxUsername = Some(voxUsername),
            callbackInfo = Some(redirectCallbackInfo)
          )
        )
      }

      Map[UpdateOptionsRequest, Option[RedirectOptions] => Unit](
        UpdateOptionsRequest(callerIdModeOp = Update(true), redirectCallbackInfoOp = Clear) ->
          (_.exists(_.callerIdMode.contains(true)) should ===(true)),
        UpdateOptionsRequest(needAnswerOp = Update(true)) ->
          (_.exists(_.needAnswer.contains(true)) should ===(true)),
        UpdateOptionsRequest(recordEnabledOp = Update(true)) ->
          (_.exists(_.recordEnabled.contains(true)) should ===(true)),
        UpdateOptionsRequest(voxUsernameOp = Update(voxUsername2)) ->
          (_.exists(_.voxUsername.contains(voxUsername2)) should ===(true))
      ).foreach { case (updateOptionsReq, check) =>
        s"change one option: $updateOptionsReq" in {
          val (redirectCreate, redirect) = prepareRedirect
          enableAllValidOptions(redirect)
          redirectServiceV2.updateOptions(redirect.objectId, redirect.target, updateOptionsReq).futureValue
          val updated = redirectServiceV2.getOrCreate(redirectCreate).futureValue
          check(updated.options)
        }
      }
    }

    "create redirects for different tags" in {
      val tagPairGen = for {
        tag1 <- TagGen
        tag2 <- TagGen.suchThat(_ != tag1)
      } yield (tag1, tag2)
      forAll(tagPairGen, MoscowPhoneGen, MoscowPhoneGen) { case ((tag1, tag2), phone1, phone2) =>
        prepareOperatorNumber(phone1)
        prepareOperatorNumber(phone2)
        val redirectCreate = MoscowCreateRedirect.next.copy(ttl = None)
        val k = redirectCreate.key
        val redirectCreate1 = redirectCreate.copy(key = k.copy(tag = tag1))
        val redirectCreate2 = redirectCreate.copy(key = k.copy(tag = tag2))
        val redirect1 = redirectServiceV2.getOrCreate(redirectCreate1).futureValue
        val redirect2 = redirectServiceV2.getOrCreate(redirectCreate2).futureValue
        redirect1.objectId shouldEqual redirect2.objectId
        redirect1.target shouldEqual redirect2.target
        redirect1.key.tag shouldEqual tag1
        redirect2.key.tag shouldEqual tag2
        (redirect1.source.number should not).equal(redirect2.source.number)
        clean()
      }
    }
    "delete one redirect by object-id and target" in {
      forAll(MoscowPhoneGen, MoscowCreateRedirect) { (phone, redirectCreate) =>
        import RedirectKeyFilter._

        prepareOperatorNumber(phone)
        val redirect = redirectServiceV2.create(redirectCreate).futureValue
        val deleted = redirectServiceV2
          .delete(
            ByObjectIdAndTarget(redirect.key.objectId, redirect.key.target),
            None,
            false
          )
          .futureValue
        deleted should have size 1
        deleted.head shouldEqual redirect
        redirectServiceV2.get(phone).futureValue shouldEqual None
        clean()
      }
    }

    "delete all redirects by object-id and target" in {
      forAll(MoscowPhoneGen, SpbPhoneGen, PhoneGen, QualifierGen) { (mskPhone, spbPhone, target, objectId) =>
        import RedirectKeyFilter._
        val mskRedirectCreate = CreateRequest(
          key = RedirectKey(objectId, target, Tag.Empty),
          geoId = Some(2),
          phoneType = None,
          ttl = None,
          antiFraudOptions = Disabled,
          preferredOperator = Some(Operators.Mts),
          operatorNumber = None,
          options = None
        )
        val spbRedirectCreate = mskRedirectCreate.copy(geoId = Some(213))
        Seq(mskPhone, spbPhone).foreach(p => prepareOperatorNumber(p))
        Seq(mskRedirectCreate, spbRedirectCreate).foreach(redirectServiceV2.create(_).futureValue)
        val filter = ByObjectIdAndTarget(objectId, target)
        val redirects = redirectServiceV2.list(filter, Full).futureValue
        redirects.total shouldEqual 2
        val deleted = redirectServiceV2.delete(filter, None, false).futureValue
        deleted should have size 2
        redirectServiceV2.get(mskPhone).futureValue shouldEqual None
        redirectServiceV2.get(spbPhone).futureValue shouldEqual None
        val redirectsAfterDelete = redirectServiceV2.list(filter, Full).futureValue
        redirectsAfterDelete.total shouldEqual 0
        clean()
      }
    }

    "delete all redirects by object-id, target and tag" in {
      forAll(MoscowPhoneGen, SpbPhoneGen, PhoneGen, QualifierGen, TagGen) {
        (mskPhone, spbPhone, target, objectId, tag) =>
          import RedirectKeyFilter._
          val redirectKey = RedirectKey(objectId, target, tag)
          val mskRedirectCreate = CreateRequest(
            key = redirectKey,
            geoId = Some(2),
            phoneType = None,
            ttl = None,
            antiFraudOptions = Disabled,
            preferredOperator = Some(Operators.Mts),
            operatorNumber = None,
            options = None
          )
          val spbRedirectCreate = mskRedirectCreate.copy(geoId = Some(213))
          Seq(mskPhone, spbPhone).foreach(p => prepareOperatorNumber(p))
          Seq(mskRedirectCreate, spbRedirectCreate).foreach(redirectServiceV2.create(_).futureValue)
          val filter = ByKey(redirectKey)
          val redirects = redirectServiceV2.list(filter, Full).futureValue
          redirects.total shouldEqual 2
          val deleted = redirectServiceV2.delete(filter, None, false).futureValue
          deleted should have size 2
          redirectServiceV2.get(mskPhone).futureValue shouldEqual None
          redirectServiceV2.get(spbPhone).futureValue shouldEqual None
          val redirectsAfterDelete = redirectServiceV2.list(filter, Full).futureValue
          redirectsAfterDelete.total shouldEqual 0
          clean()
      }
    }

    "delete both parts of double redirect" in {
      val mtsPhone = MoscowPhoneGen.next
      val voxPhoneEmulator = MoscowPhoneGen.next
      val redirectMain = MoscowCreateRedirect
        .suchThat(r => r.key.objectId.value.length < 100 - DoubleRedirectObjectIdPrefix.length)
        .next
        .copy(ttl = None)
      val doubleRedirectKey = redirectMain.key.copy(objectId = redirectMain.key.objectId.toDoubleRedirect)
      val redirectSecondary = MoscowCreateRedirect.next.copy(
        key = doubleRedirectKey,
        ttl = None,
        preferredOperator = Some(Operators.Vox)
      )
      prepareOperatorNumber(mtsPhone, account = OperatorAccounts.MtsShared, originOperator = Operators.Mts)
      prepareOperatorNumber(
        voxPhoneEmulator,
        account = OperatorAccounts.VoxShared,
        originOperator = Operators.Vox,
        geoId = Some(1)
      )
      val mtsRedirect = redirectServiceV2.getOrCreate(redirectMain).futureValue
      val voxRedirect = redirectServiceV2.getOrCreate(redirectSecondary).futureValue
      redirectServiceV2
        .updateOptions(
          mtsRedirect.objectId,
          mtsRedirect.target,
          UpdateOptionsRequest(doubleRedirectOp = Update(voxRedirect.source.number))
        )
        .futureValue
      val redirectsBeforeDelete = redirectServiceV2.list(Filter.Empty, Full).futureValue
      redirectsBeforeDelete should have size 2
      redirectServiceV2.delete(ByKey(redirectMain.key), None, false).futureValue
      val redirectsAfterDelete = redirectServiceV2.list(Filter.Empty, Full).futureValue
      redirectsAfterDelete should have size 0
    }

    "create healthy redirect when no redirects before" in {
      val mtsPhone = MoscowPhoneGen.next
      val mttPhone = MoscowPhoneGen.next
      val redirectCreate = MoscowCreateRedirect.next.copy(ttl = None)
      prepareOperatorNumber(mtsPhone, account = OperatorAccounts.MtsShared, originOperator = Operators.Mts)
      prepareOperatorNumber(mttPhone, account = OperatorAccounts.MttShared, originOperator = Operators.Mtt)

      when(operatorLabelService.getUnhealthy).thenReturn(Set(Operators.Mtt))

      val mtsRedirect = redirectServiceV2.getOrCreate(redirectCreate).futureValue
      mtsRedirect.source.operator shouldEqual Operators.Mts
      mtsRedirect.options shouldEqual redirectCreate.options
      mtsRedirect.isTemporary shouldEqual false
      mtsRedirect.source.status.deadline
        .exists(_.isBefore(DateTime.now().plusHours(3))) shouldEqual false
    }

    "create unhealthy redirect when cannot create healthy and no redirects before" in {
      val mttPhone = MoscowPhoneGen.next
      val redirectCreate = MoscowCreateRedirect.next.copy(ttl = None)
      prepareOperatorNumber(mttPhone, account = OperatorAccounts.MttShared, originOperator = Operators.Mtt)

      when(operatorLabelService.getUnhealthy).thenReturn(Set(Operators.Mtt))

      val mttRedirect = redirectServiceV2.getOrCreate(redirectCreate).futureValue
      mttRedirect.source.operator shouldEqual Operators.Mtt
      mttRedirect.options shouldEqual redirectCreate.options
      mttRedirect.isTemporary shouldEqual false
      mttRedirect.source.status.deadline
        .exists(_.isBefore(DateTime.now().plusHours(3))) shouldEqual false
    }

    "create healthy when exist only unhealthy" in {
      val mttPhone = MoscowPhoneGen.next
      val redirectCreate = MoscowCreateRedirect.next.copy(ttl = None)
      prepareOperatorNumber(mttPhone, account = OperatorAccounts.MttShared, originOperator = Operators.Mtt)
      redirectServiceV2.getOrCreate(redirectCreate).futureValue
      val mtsPhone = MoscowPhoneGen.next
      prepareOperatorNumber(mtsPhone, account = OperatorAccounts.MtsShared, originOperator = Operators.Mts)
      when(operatorLabelService.getUnhealthy).thenReturn(Set(Operators.Mtt))
      val healthyMtsRedirect = redirectServiceV2.getOrCreate(redirectCreate).futureValue
      healthyMtsRedirect.source.operator shouldEqual Operators.Mts
      val expectedOptions = redirectCreate.options
        .map(_.copy(temporary = Some(true)))
        .orElse(Some(RedirectOptions.Empty.copy(temporary = Some(true))))
      healthyMtsRedirect.options shouldEqual expectedOptions
      healthyMtsRedirect.isTemporary shouldEqual true
      healthyMtsRedirect.source.status.deadline
        .exists(_.isBefore(DateTime.now().plusHours(3))) shouldEqual true
    }

    "return existing unhealthy when cannot create healthy" in {
      val mttPhone = MoscowPhoneGen.next
      val redirectCreate = MoscowCreateRedirect.next.copy(ttl = None)
      prepareOperatorNumber(mttPhone, account = OperatorAccounts.MttShared, originOperator = Operators.Mtt)
      redirectServiceV2.getOrCreate(redirectCreate).futureValue
      when(operatorLabelService.getUnhealthy).thenReturn(Set(Operators.Mtt))
      val unhealthyMttRedirect2 = redirectServiceV2.getOrCreate(redirectCreate).futureValue
      unhealthyMttRedirect2.source.operator shouldEqual Operators.Mtt
      unhealthyMttRedirect2.options shouldEqual redirectCreate.options
      unhealthyMttRedirect2.isTemporary shouldEqual false
      unhealthyMttRedirect2.source.status.deadline
        .exists(_.isBefore(DateTime.now().plusHours(3))) shouldEqual false
    }

    "return original redirect when failure is over" in {
      val mttPhone = MoscowPhoneGen.next
      val redirectCreate = MoscowCreateRedirect.next.copy(ttl = None)
      prepareOperatorNumber(mttPhone, account = OperatorAccounts.MttShared, originOperator = Operators.Mtt)
      val unhealthyMttRedirect = redirectServiceV2.getOrCreate(redirectCreate).futureValue
      unhealthyMttRedirect.source.operator shouldEqual Operators.Mtt
      unhealthyMttRedirect.options shouldEqual redirectCreate.options
      unhealthyMttRedirect.isTemporary shouldEqual false

      when(operatorLabelService.getUnhealthy).thenReturn(Set(Operators.Mtt))

      val mtsPhone = MoscowPhoneGen.next
      prepareOperatorNumber(mtsPhone, account = OperatorAccounts.MtsShared, originOperator = Operators.Mts)
      val healthyMtsRedirect = redirectServiceV2.getOrCreate(redirectCreate).futureValue
      healthyMtsRedirect.source.operator shouldEqual Operators.Mts
      val expectedOptions = redirectCreate.options
        .map(_.copy(temporary = Some(true)))
        .orElse(Some(RedirectOptions.Empty.copy(temporary = Some(true))))
      healthyMtsRedirect.options shouldEqual expectedOptions
      healthyMtsRedirect.isTemporary shouldEqual true

      when(operatorLabelService.getUnhealthy).thenReturn(Set.empty[Operator])

      val originalMttRedirect = redirectServiceV2.getOrCreate(redirectCreate).futureValue

      originalMttRedirect.source.operator shouldEqual Operators.Mtt
      originalMttRedirect.options shouldEqual redirectCreate.options
      originalMttRedirect.isTemporary shouldEqual false
      originalMttRedirect.source.status.deadline
        .exists(_.isBefore(DateTime.now().plusHours(3))) shouldEqual false
    }

    "getOrCreate returns latest redirect if there is more than one" in {
      for (_ <- 1 to 3) {
        val mtsPhone = MoscowPhoneGen.next
        prepareOperatorNumber(mtsPhone, account = OperatorAccounts.MtsShared, originOperator = Operators.Mts)
      }
      val create = MoscowCreateRedirect.next.copy(ttl = None)

      redirectServiceV2.create(create).futureValue
      Thread.sleep(50)
      redirectServiceV2.create(create).futureValue
      Thread.sleep(50)
      val redirect3 = redirectServiceV2.create(create).futureValue

      redirectServiceV2.getOrCreate(create).futureValue.id should ===(redirect3.id)
    }

    "updateAntiFraud can extend" in {
      val (phone, redirectCreate) = (MoscowPhoneGen.next, MoscowCreateRedirect.next.copy(ttl = Some(1.day)))
      prepareOperatorNumber(phone)
      val r = redirectServiceV2.getOrCreate(redirectCreate.copy(antiFraudOptions = AonAndBlAndCounter)).futureValue
      val isUpdated =
        redirectServiceV2.updateAntiFraud(r.id, All, canWeaken = false).futureValue
      isUpdated should ===(true)
      val found = redirectServiceV2.get(r.id).futureValue.get
      found.antiFraud should ===(All)
    }

    "updateAntiFraud can not weaken" in {
      val (phone, redirectCreate) = (MoscowPhoneGen.next, MoscowCreateRedirect.next.copy(ttl = Some(1.day)))
      prepareOperatorNumber(phone)
      val r = redirectServiceV2.getOrCreate(redirectCreate.copy(antiFraudOptions = All)).futureValue
      val isUpdated =
        redirectServiceV2.updateAntiFraud(r.id, Disabled, canWeaken = false).futureValue
      isUpdated should ===(false)
      val found = redirectServiceV2.get(r.id).futureValue.get
      found.antiFraud should ===(All)
    }

    "updateAntiFraud can weaken if required" in {
      val (phone, redirectCreate) = (MoscowPhoneGen.next, MoscowCreateRedirect.next.copy(ttl = Some(1.day)))
      prepareOperatorNumber(phone)
      val r = redirectServiceV2.getOrCreate(redirectCreate.copy(antiFraudOptions = All)).futureValue
      val isUpdated =
        redirectServiceV2.updateAntiFraud(r.id, Disabled, canWeaken = true).futureValue
      isUpdated should ===(true)
      val found = redirectServiceV2.get(r.id).futureValue.get
      found.antiFraud should ===(Disabled)
    }

    "create from existing redirect" in {
      val operatorNumber = MoscowPhoneGen.next
      val createRedirectRequest = MoscowCreateRedirect.next.copy(ttl = None)
      prepareOperatorNumber(operatorNumber)
      var currentRedirect = redirectServiceV2.getOrCreate(createRedirectRequest).futureValue
      forAll(UpdateRedirectRequestGen) { update =>
        val CreateFromExistingResponse(deleted, created) =
          redirectServiceV2.createFromExisting(currentRedirect.id, update).futureValue

        import update._
        objectId.foreach(_ should ===(created.objectId))
        target.foreach(_ should ===(created.target))
        tag.foreach(_ should ===(created.tag))
        // ttl
        antiFraud.foreach(_ should ===(created.antiFraud))
        options.foreach(_ should ===(created.options.value))

        deleted should ===(currentRedirect)

        currentRedirect = created
      }
    }

    "fail to create from existing redirect if invalid options" in {
      val operatorNumber = MoscowPhoneGen.next
      val createRedirectRequest = MoscowCreateRedirect.next.copy(ttl = None)
      prepareOperatorNumber(operatorNumber)
      val currentRedirect = redirectServiceV2.getOrCreate(createRedirectRequest).futureValue
      forAll(
        UpdateRedirectRequestGen.map(_.copy(options = Some(invalidOptions)))
      ) { update =>
        redirectServiceV2
          .createFromExisting(currentRedirect.id, update)
          .failed
          .futureValue shouldBe an[RedirectOptions.InvalidOptionsException]
      }
    }

    "create with last used target" in {
      val opn1 = MoscowPhoneGen.next
      val opn2 = MoscowPhoneGen.next
      val request = MoscowCreateRedirect.next
      prepareOperatorNumber(opn1)
      prepareOperatorNumber(opn2)
      val r1 = redirectServiceV2.getOrCreate(request).futureValue
      redirectServiceV2.delete(request.key.objectId, downtime = Some(3.minutes), false).futureValue
      val r2 = redirectServiceV2.getOrCreate(request).futureValue
      r2.source.number should ===(r1.source.number)
    }

    "Shared redirect service" should {

      "get empty App2AppRedirectResult for non-existing redirect" in {
        val actualResult = sharedRedirectService.getAppRedirectInfo(RedirectIdGen.next).futureValue

        actualResult.voxUsername should ===(None)
        actualResult.target should ===(None)
      }

      "get filled App2AppRedirectResult for existing redirect" in {
        val voxUsername = ShortStr.next
        val opn = MoscowPhoneGen.next
        val request = MoscowCreateRedirect.next.copy(
          options = Some(RedirectOptionsGen.next.copy(voxUsername = Some(voxUsername)))
        )

        prepareOperatorNumber(opn)
        val r = redirectServiceV2.getOrCreate(request).futureValue
        val actualResult = sharedRedirectService.getAppRedirectInfo(r.id).futureValue

        actualResult.voxUsername should ===(Some(voxUsername))
        actualResult.target should ===(Some(request.key.target))
      }

      "get domain" in {
        val opn = MoscowPhoneGen.next
        val request = MoscowCreateRedirect.next
        prepareOperatorNumber(opn)
        val r = redirectServiceV2.getOrCreate(request).futureValue
        redirectServiceV2.delete(r.id, None, false).futureValue

        val domainResult = sharedRedirectService.getDomain(r.id).futureValue
        domainResult should ===(typedDomain)
      }
    }
  }
}
