package vertis.spamalot.dao.convert

import java.time.Instant

import com.yandex.ydb.table.values.{PrimitiveValue, StructValue, Value}
import org.scalatest.Assertion
import vertis.spamalot.dao.YdbConstants.{IdColumn, UserIdColumn}
import vertis.spamalot.dao.model.{Pagination, UserNotificationId}
import vertis.spamalot.convert.instances._
import vertis.zio.test.ZioSpecBase

/** @author kusaeva
  */
class YdbConverterInstancesSpec extends ZioSpecBase {

  "YdbConverterInstances" should {
    "convert pagination to value" in {
      val x = Pagination(lastId = "id", lastTs = Instant.now, limit = 10)

      val result: Value[_] = paginationConvert.write(x)

      val expected = StructValue.of(
        "last_id",
        PrimitiveValue.utf8(x.lastId),
        "last_ts",
        PrimitiveValue.timestamp(x.lastTs),
        "limit",
        PrimitiveValue.int32(x.limit)
      )

      test(result, expected)

    }

    "convert user notification to value" in {
      val x = UserNotificationId(userId = "some_user", id = "some_id")

      val result: Value[_] = userNotificationConvert.write(x)

      val expected = StructValue.of(
        UserIdColumn,
        PrimitiveValue.utf8(x.userId),
        IdColumn,
        PrimitiveValue.utf8(x.id)
      )

      test(result, expected)
    }
  }

  private def test(result: Value[_], expected: Value[_]): Assertion = {
    result.getType shouldBe expected.getType
    result shouldBe expected
  }

}
