package ru.yandex.vos2.testing.meta

trait TestingMetaLoadingList {
  def isUidAllowed(uid: String): Boolean
}

class TestingMetaLoadingListStorage(uids: Set[String]) extends TestingMetaLoadingList {
  override def isUidAllowed(uid: String): Boolean = uids.contains(uid)
}
