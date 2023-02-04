package ru.yandex.vertis.chat.util.uuid

import java.text.SimpleDateFormat
import java.util.concurrent.ThreadLocalRandom

import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.util.uuid.TimeIdGenerator.TimeSource
import ru.yandex.vertis.generators.BasicGenerators.readableString
import ru.yandex.vertis.generators.NetGenerators._

class TimeIdGeneratorSpec extends SpecBase {

  private val instanceIdGen = readableString

  object SameTimeSource extends TimeSource {

    override val currentTimeMillis: Long = System.currentTimeMillis()
  }

  "Time-based UUID generator" should {

    "generate unique UUIDs at the same moment of time" in {
      val now = System.currentTimeMillis()
      val generator = new TimeIdGenerator(instanceIdGen.next, () => now)
      val uuids = (1 to 100).map(_ => generator.generate())
      uuids should be(uuids.distinct)
    }

    "generate unique UUIDs at the different moments of time" in {
      val random = ThreadLocalRandom.current()
      val generator = new TimeIdGenerator(instanceIdGen.next)
      val uuids = (1 to 10).map(_ => {
        Thread.sleep(random.nextInt(1, 100))
        generator.generate()
      })
      uuids should be(uuids.distinct)
    }

    "generate ordered presentation of UUIDs even if canonical representation is unordered" in {
      val dates = Seq("2017-07-19 17:38:04.422", "2017-07-19 17:38:20.569", "2017-07-19 17:51:52.484")
      val uuids = dates.map(date => {
        val canonical = createConstGenerator(date).generateUUID().toString
        val ordered = createConstGenerator(date).generate()
        (date, canonical, ordered)
      })
      uuids.sortBy(_._3) shouldBe uuids.sortBy(_._1)
      uuids.sortBy(_._2) should not be uuids.sortBy(_._1)
    }

    "generate different UUIDs for different hosts at the same moment" in {
      val instanceId = instanceIdGen.next
      val generator1 = new TimeIdGenerator(instanceId + "1", SameTimeSource)
      val generator2 = new TimeIdGenerator(instanceId + "2", SameTimeSource)
      generator1.generate() shouldNot be(generator2.generate())
    }

    "generate ordered Ids by same generator" in {
      val generator = new TimeIdGenerator("localhost")
      val ids = (1 to 100).map(_ => generator.generate())
      ids should be(ids.distinct)
      ids should be(ids.sorted)
    }

    "generate unordered Ids by different generators" in {
      val generators = (1 to 10).map(_ => new TimeIdGenerator("localhost")).toArray
      val ids = (1 to 100).map(_ => generators((math.random() * generators.length).toInt).generate())
      ids should be(ids.distinct)
      ids should not be ids.sorted
    }

    "generate ordered Ids by different generators if there is more then 1 ms pause between each generation" in {
      val generators = (1 to 10).map(_ => new TimeIdGenerator("localhost")).toArray
      val ids = (1 to 100).map(_ => {
        Thread.sleep(1)
        generators((math.random() * generators.length).toInt).generate()
      })
      ids should be(ids.distinct)
      ids should be(ids.sorted)
    }
  }

  private def createConstGenerator(date: String): TimeIdGenerator = {
    val parsed = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS").parse(date)
    new TimeIdGenerator("aaa", () => parsed.getTime)
  }

}
