package ru.yandex.vertis.parsing.util.zookeeper

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

import org.apache.zookeeper.KeeperException.BadVersionException
import org.joda.time.DateTime
import ru.yandex.vertis.parsing.components.zookeeper.zkinterface.{ZkNodeStat, ZookeeperInterface}

/**
  * Implementation stub for simple zookeeper client interface
  *
  * @author aborunov
  */
class ZookeeperWrapperStub extends ZookeeperInterface {
  private val current = new ConcurrentHashMap[String, AtomicReference[(Array[Byte], ZkNodeStat)]]()

  val path: String = "/"

  override def isExists(path: String): Boolean = {
    current.containsKey(path)
  }

  override def create(path: String, value: Array[Byte]): Unit = {
    val stat = ZkNodeStat(1, new DateTime(), new DateTime())
    current.putIfAbsent(path, new AtomicReference[(Array[Byte], ZkNodeStat)]((value, stat)))
  }

  override def get(path: String): Array[Byte] = {
    getDataAndVersion(path)._1
  }

  override def updateAndGet(path: String)(func: Array[Byte] => Option[Array[Byte]]): Array[Byte] = {
    try {
      val (data, stat) = getDataAndVersion(path)
      func(data) match {
        case Some(newData) =>
          setData(path, newData, stat.version)
          newData
        case None =>
          data
      }
    } catch {
      case _: BadVersionException =>
        updateAndGet(path)(func)
    }
  }

  def getDataAndVersion(path: String): (Array[Byte], ZkNodeStat) = {
    current.get(path).get()
  }

  def getChildren(path: String): Seq[String] = {
    Seq.empty
  }

  override def getChild(path: String): ZookeeperInterface = ???

  def remove(path: String): Unit = {
    current.remove(path)
  }

  private def setData(path: String, data: Array[Byte], version: Int): Unit = {
    val ref = current.get(path)
    val prev = ref.get()
    if (version != prev._2.version) {
      throw new BadVersionException
    } else {
      val res = ref.compareAndSet(prev, (data, prev._2.copy(version = version + 1, updated = new DateTime())))
      if (!res) {
        throw new BadVersionException
      }
    }
  }
}
