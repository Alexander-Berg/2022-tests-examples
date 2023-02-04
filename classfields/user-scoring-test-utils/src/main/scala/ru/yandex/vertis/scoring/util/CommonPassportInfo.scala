package ru.yandex.vertis.scoring.util

import java.time.Instant

import eu.timepit.refined.auto._
import ru.yandex.vertis.scoring.model.passport.PassportResponseDataItem.PassportUserInfo
import ru.yandex.vertis.scoring.model.passport.info.{
  AccountAttributes,
  EmptyUid,
  KarmaInfo,
  KarmaStatusInfo,
  PhoneAttributes,
  PhoneInfo,
  UidInfo
}

object CommonPassportInfo {

  val uidInfo1 = UidInfo(3000000001L, false)
  val uidInfo2 = UidInfo(3000000002L, false)
  val uidInfo3 = UidInfo(3000000003L, false)

  val karmaInfo1 = KarmaInfo(85, Some(Instant.ofEpochSecond(1321965947L)))
  val karmaInfo2 = KarmaInfo(0, None)

  val karmaStatusInfo2 = KarmaStatusInfo(0)
  val karmaStatusInfo1 = KarmaStatusInfo(3085)

  val accountAttributes = AccountAttributes(Instant.ofEpochSecond(1294999198L))

  val phoneAttributes = PhoneAttributes(false, true)
  val phoneInfo = PhoneInfo(2, phoneAttributes)

  val passportUserInfo1 =
    PassportUserInfo(
      3000000001L,
      uidInfo1,
      karmaInfo1,
      karmaStatusInfo1,
      Some(accountAttributes),
      Some(List(phoneInfo))
    )
  val passportUserInfo2 =
    passportUserInfo1.copy(
      id = 3000000002L,
      uid = uidInfo2,
      phones = Some(List.empty)
    )
  val passportUserInfo3 =
    passportUserInfo1.copy(
      id = 3000000003L,
      uid = uidInfo3,
      attributes = None,
      phones = None
    )
  val passportUserInfo4 =
    PassportUserInfo(
      3000000005L,
      EmptyUid,
      karmaInfo2,
      karmaStatusInfo2,
      None,
      None
    )
}
