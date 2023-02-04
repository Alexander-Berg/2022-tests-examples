package ru.yandex.vertis.baker

import org.joda.time.DateTime
import ru.yandex.vertis.baker.components.zookeeper.node.{ZookeeperNode, ZookeeperNodeImpl}
import ru.yandex.vertis.baker.components.zookeeper.serializer.StringNodeSerializer
import ru.yandex.vertis.baker.components.zookeeper.zkinterface.{TracedZookeeperInterface, ZookeeperWrapper}
import ru.yandex.vertis.baker.lifecycle.DefaultApplication
import ru.yandex.vertis.tracing.Traced

object ZookeeperRealTest extends DefaultApplication {
  private val components = new TestComponents(this)

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

  afterStart {
    implicit val trace: Traced = components.traceCreator.trace

    val zkClient = new ZookeeperWrapper(components.zkServiceClient) with TracedZookeeperInterface

    val node = new ZookeeperNodeImpl[String]("test-node", zkClient, "") with StringNodeSerializer

    println(DateTime.now())
    val moment = DateTime.now().plusSeconds(5).withMillisOfSecond(0)
    println(moment)

    node.updateAndGet(_ => Some("test0"))

    val threads = (1 to 5).map { _ =>
      thread(node, moment)
    }

    threads.foreach(_.join())

    println(node.get)
    println(trace.requestId)
    trace.finish()
    Thread.sleep(5000)
    sys.exit(0)
  }
}
