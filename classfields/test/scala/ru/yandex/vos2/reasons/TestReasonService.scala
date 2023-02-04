package ru.yandex.vos2.reasons

class TestReasonService extends BanReasonService {

  override def getReason(code: Int): Option[BanReason] = {
    Some(BanReason(Some(code), Some(s"Error $code"), None, isEditable = true))
  }
}
