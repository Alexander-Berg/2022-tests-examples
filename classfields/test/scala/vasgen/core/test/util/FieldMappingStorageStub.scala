package vasgen.core.test.util

import scala.collection.mutable

class FieldMappingStorageStub extends FieldMappingStorage.Service with Logging {

  private val cache: mutable.Map[Int, mutable.Map[String, FieldMapping]] =
    new mutable.HashMap[Int, mutable.Map[String, FieldMapping]]()

  def clear(): Unit = cache.clear()

  def put(fields: FieldMapping*): Unit = {
    fields
      .groupBy(_.epoch)
      .foreach { case (epoch, metas) =>
        val ecache = cache
          .getOrElseUpdate(epoch, mutable.HashMap.empty[String, FieldMapping])
        metas.foreach(meta => ecache.put(meta.name, meta))
      }
  }

  override def store(
    fields: Seq[FieldMapping],
  ): ZIO[Clock, VasgenStatus, Unit] = {
    val grouped = fields.groupBy(_.epoch)
    if (
      grouped.forall { case (epoch, metas) =>
        val ecache = cache.getOrElse(epoch, Map.empty[String, FieldMapping])
        metas.forall(meta => !ecache.contains(meta.name))
      }
    )
      ZIO.succeed {
        grouped.foreach { case (epoch, metas) =>
          val ecache = cache
            .getOrElseUpdate(epoch, mutable.HashMap.empty[String, FieldMapping])
          metas.foreach(meta => ecache.put(meta.name, meta))
        }
      }
    else
      ZIO.fail(VasgenErrorContainer(TxError.dieMessage("stubbed")))

  }

  override def retrieve(
    epoch: Int,
    fields: Seq[String],
  ): ZIO[Clock, VasgenStatus, List[FieldMapping]] =
    ZIO.succeed {
      val ecache = cache.getOrElse(epoch, Map.empty[String, FieldMapping])
      fields.flatMap(ecache.get).toList
    }

  override def createOrMigrate: IO[VasgenStatus, Unit] = ZIO.succeed(())
}
