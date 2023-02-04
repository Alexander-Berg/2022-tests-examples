package ru.yandex.realty2.extdataloader.loaders.graph

import ru.yandex.realty.graph.NodeId
import ru.yandex.realty.graph.dao.RegionGraphDao
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty2.extdataloader.loaders.graph.RegionGraphDaoTest.{allNodeIds, deletedIds}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class RegionGraphDaoTest(implicit ec: ExecutionContext) extends RegionGraphDao {
  private val insertedIds = mutable.ArrayBuffer[NodeId]()
  override def getAllDeletedIds(implicit traced: Traced): Future[Seq[NodeId]] = Future.successful(deletedIds)

  override def getAllNodeIds(implicit traced: Traced): Future[Seq[NodeId]] =
    Future.successful(allNodeIds ++ insertedIds)

  override def insert(geoId: Int)(implicit traced: Traced): Future[Long] = Future {
    val rgid = nextRgid()
    insertedIds += NodeId(rgid, geoId)
    rgid
  }

  private def nextRgid(): Long = {
    (deletedIds ++ allNodeIds ++ insertedIds).map(_.rgid).max + 1
  }
}

object RegionGraphDaoTest {

  val deletedIds = Seq(
    NodeId(199441, 1),
    NodeId(394250, 1),
    NodeId(587654, 1),
    NodeId(751749, 1)
  )

  val allNodeIds = Seq(
    NodeId(0, 0),
    NodeId(143, 225),
    NodeId(17431294, 3),
    NodeId(741964, 1),
    NodeId(587795, 213),
    NodeId(587663, 98580),
    NodeId(12425, 116995),
    NodeId(183312, 20358),
    NodeId(274311, 20402),
    NodeId(758620, 17),
    NodeId(741965, 10174),
    NodeId(417899, 2)
  )
}
