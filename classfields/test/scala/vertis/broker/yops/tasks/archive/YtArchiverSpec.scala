package vertis.broker.yops.tasks.archive

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.broker.model.common.PartitionPeriods
import ru.yandex.vertis.broker.model.common.PartitionPeriods.PartitionPeriod
import vertis.broker.yops.tasks.archive.YtArchiverSpec.{getDates, Test}

import java.time.LocalDate
import scala.jdk.CollectionConverters._

/** @author kusaeva
  */
class YtArchiverSpec extends AnyWordSpec with Matchers {
  private val now = LocalDate.now
  private val start = LocalDate.of(now.getYear - 2, 10, 1)

  private val tests = Seq(
    Test(PartitionPeriods.byMonth, 1),
    Test(PartitionPeriods.byDay, 20),
    Test(PartitionPeriods.byDay, 0),
    Test(PartitionPeriods.byDay, 200)
  )

  "YtArchiver" should {
    tests.foreach { case Test(partitioning, delay) =>
      s"filter $partitioning with delay $delay" in {
        val months = getDates(start, now, partitioning)
        val expected = getDates(start, now.minus(partitioning.periodOf(delay)), partitioning)

        YtArchiver
          .filterDaysToArchive(
            months.map(_ -> ()),
            delay,
            partitioning
          )
          .map(_._1) should contain theSameElementsAs expected
      }
    }
  }
}

object YtArchiverSpec {
  case class Test(partitioning: PartitionPeriod, delay: Int)

  def getDates(from: LocalDate, to: LocalDate, partitioning: PartitionPeriod): List[LocalDate] = {
    val step = partitioning.period
    val until = partitioning.round(to).plus(step)
    val dates = from.datesUntil(until, step)
    dates.iterator().asScala.toList
  }
}
