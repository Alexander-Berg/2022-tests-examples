package vertis.broker.yops.tasks.repartition.targeting

import vertis.broker.yops.tasks.repartition.RepartitionSpec
import vertis.broker.yops.tasks.repartition.model.RepartitionJob
import vertis.core.time.DateTimeUtils.toLocalDate
import vertis.yt.config.KeepaliveConfig
import vertis.yt.util.transactions.YtTxWrapper
import vertis.yt.zio.wrappers.YtZio
import zio.duration.{durationInt, Duration}

import java.time.{Instant, LocalDate}

class RepartitionMrTargetingIntSpec extends RepartitionSpec {

  override protected val ioTestTimeout: Duration = 10.minutes

  "RepartitionMrTargeting" should {

    "work without cache" in smokeTest { yt =>
      new RepartitionMrTargeting(yt, tmpPath, tmpPath)
    }

    "work with cache" in smokeTest { yt =>
      new RepartitionMrTargeting(yt, tmpPath, tmpPath) with CachedRepartitionTargeting
    }

    "not touch old rows" in ioTest {
      val ts1 = Instant.now()
      val ts2 = Instant.now().minusSeconds(34.day.getSeconds)
      ytZio.use { yt =>
        YtTxWrapper.make(yt.transactions, KeepaliveConfig()).use { tx =>
          for {
            table <- createTable(yt, "bar", descriptor)
            messages = Seq(
              messageForTs(ts1),
              messageForTs(ts1),
              messageForTs(ts2)
            )
            _ <- appendToTable(yt, messages, table.path, descriptor)
            targeting = new RepartitionMrTargeting(yt, tmpPath, tmpPath)
            batches <- tx.withTx("test") {
              targeting.toBatches(
                RepartitionJob(
                  streamConfig,
                  table.path,
                  "timestamp",
                  LocalDate.now(),
                  messages.size.toLong
                ),
                1L,
                messages.size.toLong,
                100,
                10
              )
            }
          } yield {
            batches.size shouldBe 1
            val b = batches.head
            b shouldBe RepartitionDailyBatch(2, 3, Set(toLocalDate(ts2)).map(_.toString))
          }
        }
      }
    }

    // each write creates a chunk (i.e. a file)
    // mr targeting is a map job, which is run on every chunk (file)
    // thus it needs a merge of it's results
    "work with multichunk tables" in ioTest {
      val ts = Instant.now()
      ytZio.use { yt =>
        YtTxWrapper.make(yt.transactions, KeepaliveConfig()).use { tx =>
          for {
            n <- randomNatural(10).map(_ + 10)
            table <- createTable(yt, "baz", descriptor)
            _ <- appendToTable(yt, Seq(messageForTs(ts)), table.path, descriptor).repeatN(n)
            targeting = new RepartitionMrTargeting(yt, tmpPath, tmpPath)
            batches <- tx.withTx("test") {
              targeting.toBatches(
                RepartitionJob(
                  streamConfig,
                  table.path,
                  "timestamp",
                  LocalDate.now(),
                  n.toLong
                ),
                -1L,
                n.toLong,
                100,
                10
              )
            }
          } yield {
            batches.size shouldBe 1
            val b = batches.head
            b shouldBe RepartitionDailyBatch(0L, n.toLong, Set(toLocalDate(ts)).map(_.toString))
          }
        }
      }
    }
  }

  private def smokeTest(createTargeting: YtZio => RepartitionTargeting): Unit = ioTest {
    val ts1 = Instant.now()
    val ts2 = Instant.now().minusSeconds(3.day.getSeconds)
    val ts3 = Instant.now().minusSeconds(1.day.getSeconds)
    ytZio.use { yt =>
      YtTxWrapper.make(yt.transactions, KeepaliveConfig()).use { tx =>
        for {
          table <- createTable(yt, "foo", descriptor)
          messages = Seq(
            messageForTs(ts1),
            messageForTs(ts1),
            messageForTs(ts2),
            messageForTs(ts3)
          )
          _ <- appendToTable(yt, messages, table.path, descriptor)
          targeting = createTargeting(yt)
          batches <- tx.withTx("test") {
            targeting.toBatches(
              RepartitionJob(
                streamConfig,
                table.path,
                "timestamp",
                LocalDate.now(),
                messages.size.toLong
              ),
              -1L,
              messages.size.toLong,
              2,
              10
            )
          }
        } yield {
          batches.size shouldBe 2
          val Seq(b1, b2) = batches.sortBy(_.fromRow)
          b1 shouldBe RepartitionDailyBatch(0, 3, Set(toLocalDate(ts1), toLocalDate(ts2)).map(_.toString))
          b2 shouldBe RepartitionDailyBatch(3, 4, Set(toLocalDate(ts3)).map(_.toString))
        }
      }
    }
  }
}
