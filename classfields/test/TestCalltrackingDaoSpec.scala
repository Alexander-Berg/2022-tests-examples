package auto.dealers.calltracking.storage.test

import auto.common.pagination.RequestPagination
import auto.dealers.calltracking.model.{CallId, ClientId, Filters}
import auto.dealers.calltracking.model.testkit.CallGen._
import ru.auto.calltracking.proto.filters_model.Sorting
import auto.dealers.calltracking.storage.CalltrackingDao
import auto.dealers.calltracking.storage.CalltrackingDao.CallNotFound
import auto.dealers.calltracking.storage.testkit.TestCalltrackingDao
import zio.test.Assertion._
import zio.test._

object TestCalltrackingDaoSpec extends DefaultRunnableSpec {

  def spec =
    suite("TestCalltrackingDao")(
      testM("Upsert some calls and then get them") {
        checkM(Gen.listOfBounded(0, 100)(anyCall), Gen.anyLong) { case (calls, clientId) =>
          for {
            _ <- CalltrackingDao.upsertCalls(calls.map(_.copy(clientId = clientId)))
            calls <- CalltrackingDao.getCalls(
              ClientId(clientId),
              Filters(),
              new RequestPagination(1, 100),
              new Sorting()
            )
          } yield assert(calls)(hasSize(equalTo(calls.size)))
        }
      },
      testM("Upsert call and get it by external id`") {
        checkM(anyCall) { call =>
          for {
            _ <- CalltrackingDao.upsertCalls(Seq(call))
            call <- CalltrackingDao.getCallByExternalId(call.externalId)
          } yield assertCompletes
        }
      },
      testM("Fail when trying to get non-existing call by external id") {
        checkM(anyExternalId) { externalId =>
          for {
            call <- CalltrackingDao.getCallByExternalId(externalId).either
          } yield assert(call)(isLeft(isSubtype[CallNotFound](anything)))
        }
      },
      testM("Upsert existing call") {
        checkM(anyCall, anyTelepony()) { (call, telepony) =>
          for {
            _ <- CalltrackingDao.upsertCalls(Seq(call))
            _ <- CalltrackingDao.upsertCalls(Seq(call.copy(telepony = Some(telepony))))
            updated <- CalltrackingDao.getCallByExternalId(call.externalId)
          } yield assert(updated.telepony)(isSome(equalTo(telepony)))
        }
      },
      testM("Get call by callId") {
        checkM(anyCall) { call =>
          for {
            _ <- CalltrackingDao.upsertCalls(Seq(call))
            upserted <- CalltrackingDao.getCallByExternalId(call.externalId)
            byCallId <- CalltrackingDao.getCall(ClientId(upserted.clientId), CallId(upserted.id))
          } yield assert(upserted)(equalTo(byCallId))
        }
      }
    ).provideCustomLayer(TestCalltrackingDao.live)
}
