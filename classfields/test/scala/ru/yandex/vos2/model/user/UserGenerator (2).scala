package ru.yandex.vos2.model.user

import org.scalacheck.Gen
import ru.yandex.vos2.UserModel.User.AlternativeIds
import ru.yandex.vos2.UserModel._
import ru.yandex.vos2.model.CommonGen.{limitedStr, updateDateGen, CreateDateGen}
import ru.yandex.vos2.model.{UserRef, UserRefAutoru}

import scala.jdk.CollectionConverters._

/**
  * @author Leonid Nalchadzhi (nalchadzhi@yandex-team.ru)
  */
object UserGenerator {

  val AidRefGen: Gen[UserRefAutoru] =
    for {
      aid <- Gen.choose(10000, 100000)
    } yield UserRef.refAid(aid)

  val UserTypeGen = Gen.oneOf(UserType.values)

  val UserContactsGen =
    for {
      name <- limitedStr()
      phonesSize <- Gen.choose(1, 3)
      phones <- Gen.listOfN(phonesSize, limitedStr())
      email <- Gen.identifier
      callPeriod <- limitedStr()
    } yield UserContacts
      .newBuilder()
      .setName(name)
      .setEmail(s"$email@test.q")
      .addAllPhones(phones.map(p => UserPhone.newBuilder().setNumber(p).build()).asJava)
      .setCallPeriod(callPeriod.take(10))
      .build()

  val AlternativeIdsGen =
    for {
      uid <- Gen.choose(10000, 100000)
    } yield AlternativeIds.newBuilder().setExternal(uid).build()

  val UserGen = for {
    userType <- UserTypeGen
    contacts <- UserContactsGen
    timestampCreate <- CreateDateGen
    timestampUpdate <- updateDateGen(timestampCreate)
    licenseAccept <- Gen.oneOf(true, false)
    alternativeIds <- AlternativeIdsGen
    userRef = "uid_" + alternativeIds.getExternal
  } yield User
    .newBuilder()
    .setUserRef(userRef)
    .setUserType(userType)
    .setUserContacts(contacts)
    .setTimestampCreate(timestampCreate)
    .setTimestampUpdate(timestampUpdate)
    .setLicenseAccept(licenseAccept)
    .setAlternativeIds(alternativeIds)
    .build()

  val NewUserGen = for {
    userType <- UserTypeGen
    contacts <- UserContactsGen
    timestampCreate <- CreateDateGen
    timestampUpdate <- updateDateGen(timestampCreate)
    licenseAccept <- Gen.oneOf(true, false)
    alternativeIds <- AlternativeIdsGen
    userRef = "uid_" + alternativeIds.getExternal
  } yield User
    .newBuilder()
    .setUserRef(userRef)
    .setUserType(userType)
    .setUserContacts(contacts)
    .setTimestampCreate(timestampCreate)
    .setTimestampUpdate(timestampUpdate)
    .setLicenseAccept(licenseAccept)
    .setAlternativeIds(alternativeIds)
    .build()
}
