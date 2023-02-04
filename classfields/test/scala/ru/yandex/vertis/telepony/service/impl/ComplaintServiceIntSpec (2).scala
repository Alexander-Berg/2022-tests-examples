package ru.yandex.vertis.telepony.service.impl

import org.scalacheck.Gen
import org.scalatest.Inside
import ru.yandex.vertis.telepony.SampleHelper.createNumberRequest
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.Complaint.{ComplaintPayload, ComplaintStatusValues}
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.RedirectInspectService.OperatorNumberAttributes
import ru.yandex.vertis.telepony.service.SharedComplaintService.ComplaintDuplicate
import ru.yandex.vertis.telepony.service.mts.InMemoryMtsClient
import ru.yandex.vertis.telepony.util.AuthorizedContext
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}
import ru.yandex.vertis.tracing.Traced

class ComplaintServiceIntSpec extends SpecBase with IntegrationSpecTemplate with Inside {

  implicit val rc: AuthorizedContext = AuthorizedContext(id = "1", login = "test", trace = Traced.empty)

  private def prepareOperatorNumber(
      phone: Phone,
      status: Status = Status.Ready(None),
      account: OperatorAccount = defaultMtsAccount) = {
    val createRequest = createNumberRequest(phone).copy(status = Some(status), account = account)
    val opn = operatorNumberServiceV2.create(createRequest).futureValue
    account.operator match {
      case Operators.Mts =>
        mtsClients(account).clientV4.asInstanceOf[InMemoryMtsClient].registerNewUniversalNumber(phone)
      case Operators.Mtt =>
        mttClients(account).registerNewPhone(phone)
      case Operators.Vox =>
      // do nothing
    }
    opn
  }

  private def prepareRedirect(targetGen: Gen[Phone] = MoscowPhoneGen): ActualRedirect = {
    val MoscowCreateRedirect =
      createRequestV2Gen(targetGen)
        .map(_.copy(geoId = None, phoneType = None, preferredOperator = None))
    val redirectCreate = MoscowCreateRedirect.next
    val phone = MoscowPhoneGen.next
    prepareOperatorNumber(phone)
    sharedOperatorNumberDao.upsert(SharedOperatorNumberGen.next.copy(number = phone)).futureValue
    redirectServiceV2.create(redirectCreate).futureValue
  }

  "ComplaintDomainService" should {
    "create complaint" in {
      val redirect = prepareRedirect()
      val complaintRequest = ComplaintCreateRequestGen.next
      val complaint = complaintService.create(redirect.id, complaintRequest).futureValue
      inside(complaint) {
        case Complaint(
              id,
              redirectId,
              operatorNumber,
              targetNumber,
              author,
              complaintStatus,
              createTime,
              updateTime,
              domain,
              complaintPayload,
              operator,
              originOperator
            ) =>
          createTime should ===(updateTime)
          redirect.id should ===(redirectId)
          operatorNumber should ===(redirect.source.number)
          targetNumber should ===(redirect.target)
          author should ===(rc.login)
          domain should ===(typedDomain)
          complaintStatus should ===(ComplaintStatusValues.Open)
          inside(complaintPayload) { case ComplaintPayload(complaintCause, complaintCallInfo, commentsList) =>
            complaintCause should ===(complaintRequest.complaintCause)
            complaintCallInfo should ===(complaintRequest.complaintCallInfo)
            commentsList should be(empty)
          }
      }
    }
    "fetch redirect and create complaint" in {
      val redirect = prepareRedirect()
      val complaintRequest = ComplaintCreateRequestGen.next
      val complaint = complaintService.create(redirect.id, complaintRequest).futureValue
      complaint shouldBe sharedComplaintService.getById(complaint.id).futureValue
    }
    "fail on duplicate redirect id" in {
      val redirect = prepareRedirect()
      val complaintRequest1 = ComplaintCreateRequestGen.next
      val complaintRequest2 = ComplaintCreateRequestGen.next
      complaintService.create(redirect.id, complaintRequest1).futureValue
      val th = complaintService.create(redirect.id, complaintRequest2).failed.futureValue
      th should be(a[ComplaintDuplicate])
    }

    "get complained redirects" in {
      val redirect = prepareRedirect()
      val complaintRequest = ComplaintCreateRequestGen.next
      complaintService.create(redirect.id, complaintRequest).futureValue
      val redirects = complaintService.getComplainedRedirects(redirect.target).futureValue
      redirects should have size 1
      redirects.headOption.get should ===(redirect)
    }

    "get active complaint" in {
      val redirect = prepareRedirect()
      val complaintRequest = ComplaintCreateRequestGen.next
      val complaint = complaintService.create(redirect.id, complaintRequest).futureValue
      val complaintOpt = complaintService.getActiveComplaint(redirect.id).futureValue
      complaintOpt.get should ===(complaint)
    }

    "get empty complainedAttributes if single complaint" in {
      val redirect = prepareRedirect()
      val complaintRequest = ComplaintCreateRequestGen.next
      complaintService.create(redirect.id, complaintRequest).futureValue
      val attributeSet = complaintService.getComplainedAttributes(redirect.target).futureValue
      attributeSet should be(empty)
    }

    "get non empty complainedAttributes" in {
      val target = MoscowPhoneGen.next
      val constTargetGen = Gen.const(target)
      val redirect1 = prepareRedirect(constTargetGen)
      val complaintRequest1 = ComplaintCreateRequestGen.next
      complaintService.create(redirect1.id, complaintRequest1).futureValue
      val redirect2 = prepareRedirect(constTargetGen)
      val complaintRequest2 = ComplaintCreateRequestGen.next
      complaintService.create(redirect2.id, complaintRequest2).futureValue
      val attributeSet = complaintService.getComplainedAttributes(target).futureValue
      attributeSet should not be empty
      attributeSet should ===(Set(redirect1, redirect2).map(r => OperatorNumberAttributes.from(r.source)))
    }
  }
}
