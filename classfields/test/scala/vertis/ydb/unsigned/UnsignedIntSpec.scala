package vertis.ydb.unsigned

import vertis.ydb.test.YdbConsistencyTest
import vertis.zio.test.ZioSpecBase
import zio.ZIO

import scala.util.Random

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class UnsignedIntSpec extends ZioSpecBase with YdbConsistencyTest {

  private val tableName = this.getClass.getSimpleName
  private val column = "u32"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    runSync(
      oneColumnTable(tableName, column, UnsignedInt.YdbUnsignedIntParam.yType)
    )
    ()
  }

  "UnsignedInt" should {
    "have consistent write/read" in ydbTest {
      ZIO.foreach(
        Seq(
          UnsignedInt(0),
          UnsignedInt(Int.MaxValue),
          UnsignedInt(1L + Int.MaxValue),
          UnsignedInt(UnsignedInt.MaxValue)
        ) ++
          Iterator
            .continually(Random.nextLong(Int.MaxValue) + Int.MaxValue)
            .map(UnsignedInt(_))
            .take(10)
      ) { i =>
        checkConsistent(tableName, column)(i)
      }
    }
  }

}
