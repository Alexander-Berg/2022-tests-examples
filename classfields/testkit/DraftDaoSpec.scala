package ru.yandex.vertis.general.gost.storage.testkit

import common.zio.ydb.Ydb.HasTxRunner
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.common.model.user.testkit.OwnerIdGen
import ru.yandex.vertis.general.gost.model.testkit.DraftGen
import ru.yandex.vertis.general.gost.storage.DraftDao
import ru.yandex.vertis.general.gost.storage.DraftDao.DraftDao
import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion._
import zio.test._

object DraftDaoSpec {

  def spec(
      label: String): Spec[DraftDao with Clock with HasTxRunner with Random with Sized with TestConfig, TestFailure[Nothing], TestSuccess] = {
    suite(label)(
      testM("create draft") {
        checkNM(1)(DraftGen.anyDraft, OwnerIdGen.anyOwnerId) { (draft, ownerId) =>
          for {
            _ <- runTx(DraftDao.createOrUpdateDraft(ownerId, draft))
            saved <- runTx(DraftDao.getDraft(ownerId, draft.id))
          } yield assert(saved)(isSome(equalTo(draft)))
        }
      },
      testM("return None if draft does not exist") {
        checkNM(1)(DraftGen.anyDraftId, OwnerIdGen.anyOwnerId) { (draftId, ownerId) =>
          for {
            saved <- runTx(DraftDao.getDraft(ownerId, draftId))
          } yield assert(saved)(isNone)
        }
      },
      testM("delete draft") {
        checkNM(1)(DraftGen.anyDraft, OwnerIdGen.anyOwnerId) { (draft, ownerId) =>
          for {
            _ <- runTx(DraftDao.createOrUpdateDraft(ownerId, draft))
            _ <- runTx(DraftDao.deleteDraft(ownerId, draft.id))
            saved <- runTx(DraftDao.getDraft(ownerId, draft.id))
          } yield assert(saved)(isNone)
        }
      },
      testM("set and delete current draft") {
        checkNM(1)(DraftGen.anyDraftId, OwnerIdGen.anyUserId()) { (draftId, userId) =>
          for {
            _ <- runTx(DraftDao.setCurrentDraft(userId, draftId))
            afterSet <- runTx(DraftDao.currentDraft(userId))
            _ <- runTx(DraftDao.resetCurrentDraft(userId))
            afterReset <- runTx(DraftDao.currentDraft(userId))
          } yield assert(afterSet.user)(isSome(equalTo(draftId))) && assert(afterReset.user)(isNone)
        }
      }
    )
  }
}
