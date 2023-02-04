package ru.yandex.vertis.general.common.resources.ban_reasons.testkit

import general.gost.palma.BanReason.OfferBlockStatus._
import general.gost.palma.{BanReason, Sender}
import ru.yandex.vertis.general.common.resources.ban_reasons.BanReasonSnapshot

object Data {

  val anotherReason: BanReason = BanReason(
    code = "ANOTHER",
    titleLk = "titleLk_of_ANOTHER",
    offerEditable = NON_EDITABLE,
    sender = Some(Sender(templateGost = "anotherGost", templatePassport = "anotherPassport"))
  )

  val offerUnban: BanReason = BanReason(
    code = "OFFER_UNBAN",
    titleLk = "titleLk_of_OFFER_UNBAN",
    offerEditable = NON_EDITABLE,
    sender = Some(Sender(templateGost = "offerUnban"))
  )

  val userUnban: BanReason = BanReason(
    code = "USER_UNBAN",
    titleLk = "titleLk_of_USER_UNBAN",
    offerEditable = NON_EDITABLE,
    sender = Some(Sender(templateGost = "anotherGost", templatePassport = "anotherPassport"))
  )

  val wrongPhoto: BanReason = BanReason(
    code = "WRONG_PHOTO",
    titleLk = "titleLk_of_WRONG_PHOTO",
    offerEditable = NON_EDITABLE,
    sender = Some(Sender(templateGost = "wrong_photo_gost", templatePassport = "wrong_photo_passport"))
  )

  val sold: BanReason = BanReason(
    code = "SOLD",
    titleLk = "titleLk_of_SOLD",
    offerEditable = EXCLUSION,
    sender = Some(Sender(templateGost = "sold"))
  )

  val offerExpired: BanReason = BanReason(
    code = "OFFER_EXPIRED",
    titleLk = "titleLk_of_OFFER_EXPIRED",
    offerEditable = EXCLUSION,
    sender = Some(Sender(templateGost = "templateGost_of_OFFER_EXPIRED"))
  )

  val editableReason: BanReason = BanReason(
    code = "EDITABLE",
    titleLk = "titleLk_of_EDITABLE",
    offerEditable = EDITABLE,
    sender = Some(Sender(templateGost = "editable gost"))
  )

  val TestSnapshot: BanReasonSnapshot = BanReasonSnapshot(
    Seq(anotherReason, offerUnban, userUnban, wrongPhoto, sold, editableReason, offerExpired)
  )
}
