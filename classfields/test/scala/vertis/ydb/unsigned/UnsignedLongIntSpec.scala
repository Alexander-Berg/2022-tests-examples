package vertis.ydb.unsigned

import vertis.ydb.test.YdbConsistencyTest
import vertis.zio.test.ZioSpecBase
import zio.ZIO

import scala.util.Random

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class UnsignedLongIntSpec extends ZioSpecBase with YdbConsistencyTest {

  private val tableName = this.getClass.getSimpleName
  private val column = "u64"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    runSync(
      oneColumnTable(tableName, column, UnsignedLong.YdbUnsignedLongParam.yType)
    )
    ()
  }

  "UnsignedLong" should {
    "have consistent write/read" in ydbTest {
      ZIO.foreach(
        Seq(
          UnsignedLong(0),
          UnsignedLong(Long.MinValue),
          UnsignedLong(Long.MaxValue)
        ) ++ Iterator.continually(Random.nextLong()).map(UnsignedLong(_)).take(10)
      ) { i =>
        checkConsistent(tableName, column)(i)
      }
    }
  }
}
