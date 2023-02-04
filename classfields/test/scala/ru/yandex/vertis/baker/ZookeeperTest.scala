package ru.yandex.vertis.baker

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.baker.components.zookeeper.ZookeeperSupport
import ru.yandex.vertis.baker.components.zookeeper.node.{ZookeeperNode, ZookeeperNodeImpl}
import ru.yandex.vertis.baker.components.zookeeper.serializer.StringNodeSerializer
import ru.yandex.vertis.baker.components.zookeeper.zkinterface.ZookeeperWrapper
import ru.yandex.vertis.baker.lifecycle.{Application, DefaultApplication}
import ru.yandex.vertis.baker.util.TracedUtils
import ru.yandex.vertis.tracing.Traced

class ZookeeperAppComponents(val app: Application) extends ZookeeperSupport

class ZookeeperTestApp extends DefaultApplication {
  val components = new ZookeeperAppComponents(this)
}

@RunWith(classOf[JUnitRunner])
class ZookeeperTest extends AnyWordSpec with Matchers {

  private def thread(node: ZookeeperNode[String], moment: DateTime)(implicit traced: Traced): Thread = {
    val t = new Thread(new Runnable {
      override def run(): Unit = {
        try {
          val millis = moment.getMillis - System.currentTimeMillis()
          println(s"${Thread.currentThread()} sleeping $millis ms")
          Thread.sleep(millis)
          node.updateAndGet { prev =>
            val i = prev.replace("test", "").toInt + 1
            Some("test" + i)
          }
        } catch {
          case e: Throwable =>
            println(e)
        }
      }
    })
    t.start()
    t
  }

  "Zookeeper App" should {
    "increase counter in zookeeper atomically" in {
      val app = new ZookeeperTestApp
      app.main(Array())
      val components = app.components
      implicit val trace: Traced = TracedUtils.empty
      val zkClient = new ZookeeperWrapper(components.zkServiceClient)
      val start = System.currentTimeMillis()
      val node = new ZookeeperNodeImpl[String](s"test-node-$start", zkClient, "") with StringNodeSerializer
      val moment = DateTime.now().plusSeconds(1).withMillisOfSecond(0)
      node.updateAndGet(_ => Some("test0"))
      val threads = (1 to 5).map { _ =>
        thread(node, moment)
      }
      threads.foreach(_.join())
      node.get shouldBe "test5"

      zkClient.remove(s"test-node-$start")
    }
  }
}
