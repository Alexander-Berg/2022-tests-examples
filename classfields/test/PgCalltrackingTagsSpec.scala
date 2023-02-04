package auto.dealers.calltracking.storage.test

import common.zio.doobie.ConnManager
import auto.dealers.calltracking.model.testkit.CallGen
import auto.dealers.calltracking.model.{CallId, ClientId}
import ru.auto.calltracking.proto.model.Call
import auto.dealers.calltracking.storage.CalltrackingDao
import auto.dealers.calltracking.storage.postgresql.PgCalltrackingDao
import auto.dealers.calltracking.storage.testkit.TestPostgresql
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object PgCalltrackingTagsSpec extends DefaultRunnableSpec {
  private val callResult = Call.CallResult.SUCCESS

  def spec = {
    suite("PgCalltrackingDao tags")(
      testM("add tags") {
        checkM(CallGen.anyCall) { call =>
          for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.upsertCall(call.copy(tags = Set("a", "b"), callResult = callResult))
            inserted <- CalltrackingDao.getCallByExternalId(call.externalId)
            _ <- CalltrackingDao.addTags(ClientId(inserted.clientId), CallId(inserted.id), Seq("d", "e"))
            updated <- CalltrackingDao.getCallByExternalId(call.externalId)
          } yield assert(inserted.tags)(equalTo(Set("a", "b"))) &&
            assert(updated.tags)(equalTo(Set("a", "b", "d", "e")))
        }
      },
      testM("remove tags") {
        checkM(CallGen.anyCall) { call =>
          for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.upsertCall(call.copy(tags = Set("a", "b"), callResult = callResult))
            inserted <- CalltrackingDao.getCallByExternalId(call.externalId)
            _ <- CalltrackingDao.removeTags(ClientId(inserted.clientId), CallId(inserted.id), Seq("b"))
            _ <- CalltrackingDao.removeTags(ClientId(inserted.clientId), CallId(inserted.id), Seq("e"))
            updated <- CalltrackingDao.getCallByExternalId(call.externalId)
          } yield assert(inserted.tags)(equalTo(Set("a", "b"))) &&
            assert(updated.tags)(equalTo(Set("a")))
        }
      },
      testM("remove client tags") {
        checkM(CallGen.anyCall) { call =>
          for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.upsertCall(call.copy(tags = Set("a", "b"), callResult = callResult))
            inserted <- CalltrackingDao.getCallByExternalId(call.externalId)
            _ <- CalltrackingDao.removeClientTag(ClientId(inserted.clientId), "b")
            _ <- CalltrackingDao.removeClientTag(ClientId(inserted.clientId), "e")
            updated <- CalltrackingDao.getCallByExternalId(call.externalId)
          } yield assert(inserted.tags)(equalTo(Set("a", "b"))) &&
            assert(updated.tags)(equalTo(Set("a")))
        }
      },
      testM("list tags") {
        checkM(CallGen.anyCall) { call =>
          for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.upsertCall(
              call.copy(tags = Set("a", "b", "abba", "eda", "e%e"), callResult = callResult)
            )
            suggest0 <- CalltrackingDao.listTags(ClientId(call.clientId), "")
            suggest1 <- CalltrackingDao.listTags(ClientId(call.clientId), "a")
            suggest2 <- CalltrackingDao.listTags(ClientId(call.clientId), "abb")
            suggest3 <- CalltrackingDao.listTags(ClientId(call.clientId), "b")
            suggest4 <- CalltrackingDao.listTags(ClientId(call.clientId), "c")
            suggest5 <- CalltrackingDao.listTags(ClientId(call.clientId), "e%")
            _ <- CalltrackingDao.removeClientTag(ClientId(call.clientId), "b")
            suggest6 <- CalltrackingDao.listTags(ClientId(call.clientId), "")
          } yield assert(suggest0)(hasSameElements(Seq("a", "b", "abba", "eda", "e%e")) ?? "suggest0") &&
            assert(suggest1)(hasSameElements(Seq("a", "abba")) ?? "suggest1") &&
            assert(suggest2)(hasSameElements(Seq("abba")) ?? "suggest2") &&
            assert(suggest3)(hasSameElements(Seq("b")) ?? "suggest3") &&
            assert(suggest4)(hasSameElements(Seq.empty) ?? "suggest4") &&
            assert(suggest5)(hasSameElements(Seq("e%e")) ?? "suggest5") &&
            assert(suggest6)(hasSameElements(Seq("a", "abba", "eda", "e%e")) ?? "suggest6")
        }
      }
    ) @@ after(PgCalltrackingDao.clean) @@ beforeAll(PgCalltrackingDao.initSchema.orDie) @@ sequential
  }.provideCustomLayerShared(
    TestPostgresql.managedTransactor >+> ConnManager.fromTransactor >>> PgCalltrackingDao.live
  )
}
