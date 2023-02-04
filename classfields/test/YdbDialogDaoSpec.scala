package ru.yandex.vertis.etc.dust.storage.ydb.test

import cats.data.NonEmptyList
import common.zio.ydb.Ydb
import ru.yandex.vertis.etc.dust.model.{Dialog, DialogId}
import zio.clock.Clock
import zio.test.Assertion.{contains, equalTo, fails, hasSameElements, isNone, isSubtype}
import zio.test._
import zio.test.TestAspect.{before, sequential, shrinks}
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, ZSpec}
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.etc.dust.model.Dialog.Phrase
import ru.yandex.vertis.etc.dust.storage.DialogDao
import ru.yandex.vertis.etc.dust.storage.ydb.YdbDialogDao

import java.time.Instant

object YdbDialogDaoSpec extends DefaultRunnableSpec {

  val teleponyCallDomain = "telepony_call"
  val autoruChatDomain = "autoru_chat"

  val dialog = Dialog(
    id = DialogId("1"),
    domain = teleponyCallDomain,
    startTime = Instant.now,
    phrases = NonEmptyList.one(Phrase(Instant.now, "text", Dialog.Buyer)),
    Dialog.CallPayload("+79999999999", "autoru_def")
  )

  override def spec: ZSpec[TestEnvironment, Any] = {
    (suite("YdbDialogDao")(
      testM("get nothing when empty") {
        for {
          stored <- runTx(DialogDao.get(dialog.domain, dialog.id))
        } yield assert(stored)(isNone)
      },
      testM("upsert and get") {
        for {
          _ <- runTx(DialogDao.upsert(dialog))
          stored <- runTx(DialogDao.get(dialog.domain, dialog.id))
        } yield assert(stored.get)(equalTo(dialog))
      },
      testM("upsert dialogs with same id but different domains") {
        for {
          _ <- runTx(DialogDao.upsert(dialog))
          secondDialog = dialog.copy(domain = autoruChatDomain)
          _ <- runTx(DialogDao.upsert(secondDialog))
          firstStored <- runTx(DialogDao.get(dialog.domain, dialog.id))
          secondStored <- runTx(DialogDao.get(secondDialog.domain, secondDialog.id))
        } yield assert(firstStored.get)(equalTo(dialog)) && assert(secondStored.get)(equalTo(secondDialog))
      },
      testM("Upsert dialogs and get them") {
        for {
          _ <- runTx(DialogDao.upsert(dialog))
          secondDialog = dialog.copy(domain = autoruChatDomain)
          _ <- runTx(DialogDao.upsert(secondDialog))
          keys = List(dialog.domain -> dialog.id, secondDialog.domain -> secondDialog.id)
          dialogs <- runTx(DialogDao.getDialogs(keys))
        } yield assert(dialogs.keySet)(hasSameElements(keys.toSet)) && assert(dialogs.values)(
          contains(dialog)
        ) && assert(dialogs.values)(contains(secondDialog))
      }
    ) @@ sequential @@ before(runTx(YdbDialogDao.clean)))
      .provideCustomLayerShared {
        (TestYdb.ydb ++ Clock.live) >+> (YdbDialogDao.live ++ Ydb.txRunner)
      }
  }

}
