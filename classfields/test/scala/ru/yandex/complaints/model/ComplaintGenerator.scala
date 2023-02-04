package ru.yandex.complaints.model

import org.scalacheck.Gen
import ru.yandex.complaints.api.handlers.api.complaint.model.{RequestInfo, UpdateComplaintRequest}
import ru.yandex.complaints.api.handlers.api.user.model.UserInfo
import ru.yandex.complaints.model.complaint.CommonGen

/**
  * Created by s-reznick on 01.08.16.
  */
object ComplaintGenerator {
  val NumberOfUsers = 4321
  val NumberOfOffers = 2

  val requestInfo = new RequestInfo(
    ip = Some("192.168.0.1"),
    proxyIp = Some("127.0.0.1"),
    userAgent = Some("tank"),
    flashCookie = Some("tymzsviqZvcPvNdSzktgekexbg")
  )

  val ComplaintTypeGen = Gen.oneOf(ComplaintType.Instances).map(_.toString)

  val IdSizeMin = 11
  val IdSizeMax= 12

  val availableUIDs =
    Gen.listOfN(NumberOfUsers,
      CommonGen.limitedStr(IdSizeMin, IdSizeMax)).sample.get.map(
        "uid_" + _ + "2345")

  val availableOIDs =
    Gen.listOfN(NumberOfOffers,
      CommonGen.limitedStr(IdSizeMin, IdSizeMax)).sample.get.map(
        "oid_" + _ + "1234")

  //val OfferIdGen = Gen.choose(0, availableOIDs.size-1).map(availableOIDs(_))

  val OfferIdGen = Gen.choose(1, availableOIDs.size).map(_.toString)

  val UserDomainSize = 5

  val UserInfoGen = for {
    userIndex ← Gen.choose(0, availableUIDs.size-1)
    domain <- CommonGen.limitedStr(UserDomainSize)
    userType <- Gen.oneOf(UserType.values.toSeq)
    userTypeOpt <- Gen.oneOf(Some(userType), None)
  } yield new UserInfo(domain, availableUIDs(userIndex), userTypeOpt)

  val DescrSizeMin = 100
  val DescrSizeMax = 2048

  val NewUpdateComplaintRequestGen = for {
    complaintType ← ComplaintTypeGen
    userInfo <- UserInfoGen
    descr <- CommonGen.limitedStr(DescrSizeMin, DescrSizeMax)
    source <- Gen.option(CommonGen.limitedStr())
  } yield new UpdateComplaintRequest(userInfo,
      requestInfo, Some(descr), complaintType, source)
}