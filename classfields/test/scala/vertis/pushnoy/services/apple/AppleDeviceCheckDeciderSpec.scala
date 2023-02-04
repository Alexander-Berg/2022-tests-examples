package vertis.pushnoy.services.apple

import java.time.YearMonth

import vertis.pushnoy.PushnoySpecBase
import vertis.pushnoy.model.request.DeviceInfo
import vertis.pushnoy.services.apple.AppleDeviceCheckDecider.Decision

class AppleDeviceCheckDeciderSpec extends PushnoySpecBase {

  private case class TestCase(
      description: String,
      current: Option[DeviceInfo.IosDeviceCheckBits],
      apple: Option[DeviceInfo.IosDeviceCheckBits],
      yearMonth: YearMonth,
      expected: Decision)

  private val nowYearMonth = YearMonth.now()
  private val actualYearMonth = nowYearMonth.minusMonths(6)
  private val notActualYearMonth = nowYearMonth.minusMonths(7)

  private val testCases: Seq[TestCase] = Seq(
    TestCase(
      description = "Empty current & apple",
      current = None,
      apple = None,
      yearMonth = YearMonth.now(),
      Decision(None, update = false)
    ),
    TestCase(
      description = "Empty current & with apple",
      current = None,
      apple = Some(DeviceInfo.IosDeviceCheckBits(bit0 = true, bit1 = true, lastUpdateTime = Some(nowYearMonth))),
      yearMonth = nowYearMonth,
      Decision(
        Some(DeviceInfo.IosDeviceCheckBits(bit0 = true, bit1 = true, lastUpdateTime = Some(nowYearMonth))),
        update = false
      )
    ),
    TestCase(
      description = "With current & empty apple",
      current = Some(DeviceInfo.IosDeviceCheckBits(bit0 = true, bit1 = true, lastUpdateTime = Some(nowYearMonth))),
      apple = None,
      yearMonth = nowYearMonth,
      Decision(
        Some(DeviceInfo.IosDeviceCheckBits(bit0 = true, bit1 = true, lastUpdateTime = Some(nowYearMonth))),
        update = true
      )
    ),
    TestCase(
      description = "With current < apple",
      current = Some(DeviceInfo.IosDeviceCheckBits(bit0 = true, bit1 = false, lastUpdateTime = Some(nowYearMonth))),
      apple = Some(DeviceInfo.IosDeviceCheckBits(bit0 = false, bit1 = true, lastUpdateTime = Some(actualYearMonth))),
      yearMonth = nowYearMonth,
      Decision(
        Some(DeviceInfo.IosDeviceCheckBits(bit0 = false, bit1 = true, lastUpdateTime = Some(actualYearMonth))),
        update = false
      )
    ),
    TestCase(
      description = "With current < apple & lastUpdateTime not actual",
      current = Some(DeviceInfo.IosDeviceCheckBits(bit0 = true, bit1 = false, lastUpdateTime = Some(nowYearMonth))),
      apple = Some(DeviceInfo.IosDeviceCheckBits(bit0 = false, bit1 = true, lastUpdateTime = Some(notActualYearMonth))),
      yearMonth = nowYearMonth,
      Decision(
        Some(DeviceInfo.IosDeviceCheckBits(bit0 = true, bit1 = false, lastUpdateTime = Some(nowYearMonth))),
        update = true
      )
    ),
    TestCase(
      description = "With current > apple",
      current = Some(DeviceInfo.IosDeviceCheckBits(bit0 = false, bit1 = true, lastUpdateTime = Some(nowYearMonth))),
      apple = Some(DeviceInfo.IosDeviceCheckBits(bit0 = true, bit1 = false, lastUpdateTime = Some(actualYearMonth))),
      yearMonth = nowYearMonth,
      Decision(
        Some(DeviceInfo.IosDeviceCheckBits(bit0 = false, bit1 = true, lastUpdateTime = Some(nowYearMonth))),
        update = true
      )
    ),
    TestCase(
      description = "With current == apple",
      current = Some(DeviceInfo.IosDeviceCheckBits(bit0 = false, bit1 = true, lastUpdateTime = Some(nowYearMonth))),
      apple = Some(DeviceInfo.IosDeviceCheckBits(bit0 = false, bit1 = true, lastUpdateTime = Some(actualYearMonth))),
      yearMonth = nowYearMonth,
      Decision(
        Some(DeviceInfo.IosDeviceCheckBits(bit0 = false, bit1 = true, lastUpdateTime = Some(nowYearMonth))),
        update = true
      )
    ),
    TestCase(
      description = "With current == apple & equals lastUpdateTime",
      current = Some(DeviceInfo.IosDeviceCheckBits(bit0 = false, bit1 = true, lastUpdateTime = Some(nowYearMonth))),
      apple = Some(DeviceInfo.IosDeviceCheckBits(bit0 = false, bit1 = true, lastUpdateTime = Some(nowYearMonth))),
      yearMonth = nowYearMonth,
      Decision(
        Some(DeviceInfo.IosDeviceCheckBits(bit0 = false, bit1 = true, lastUpdateTime = Some(nowYearMonth))),
        update = false
      )
    )
  )

  "AppleDeviceCheckDecider" should {
    testCases.foreach { case TestCase(description, current, apple, yearMonth, expected) =>
      description in {
        val actual = AppleDeviceCheckDecider.decide(current, apple, yearMonth)
        actual shouldBe expected
      }
    }
  }
}
