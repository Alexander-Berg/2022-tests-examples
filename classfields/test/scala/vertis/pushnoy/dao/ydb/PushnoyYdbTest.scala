package vertis.pushnoy.dao.ydb

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import vertis.pushnoy.MockedCtx
import vertis.ydb.test.YdbTest
import vertis.zio.test.ZioSpecBase
import zio.ZIO

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/** @author kusaeva
  */
trait PushnoyYdbTest extends YdbTest with ScalaFutures with MockedCtx with IntegrationPatience {
  this: org.scalatest.Suite with ZioSpecBase =>

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  lazy val devicesStorage = new DevicesYdbStorage(ydbWrapper)
  lazy val usersStorage = new UsersYdbStorage(ydbWrapper)
  lazy val dao = new YdbDao(ydbWrapper, zioRuntime)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    runSync(ZIO.foreachPar(Seq(devicesStorage, usersStorage))(_.createTable))
    ()
  }
}
