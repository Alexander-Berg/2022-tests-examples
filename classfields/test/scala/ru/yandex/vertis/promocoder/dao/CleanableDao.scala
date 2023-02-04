package ru.yandex.vertis.promocoder.dao

import ru.yandex.vertis.promocoder.dao.impl.jdbc.{
  OrmAutoProlongFeatureDao,
  OrmFeatureInstanceDaoBase,
  OrmPromocodeInstanceDao
}
import ru.yandex.vertis.promocoder.dao.impl.jdbc.OrmFeatureInstanceDaoBase.FeaturesTableBase
import ru.yandex.vertis.promocoder.dao.impl.jvm
import ru.yandex.vertis.promocoder.dao.impl.jvm.{JvmFeatureInstanceDaoBase, JvmPromocodeInstanceDao}
import ru.yandex.vertis.promocoder.model.{PromocodeInstance, PromocodeInstanceId}

import scala.concurrent.{ExecutionContext, Future}

/** Allows to make clean-up
  *
  * @author alex-kovalenko
  */
trait CleanableDao {
  def clean(implicit ec: ExecutionContext): Future[Unit]
}

trait CleanableJvmFeatureInstanceDao extends JvmFeatureInstanceDaoBase with CleanableDao {

  def clean(implicit ec: ExecutionContext): Future[Unit] = Future {
    jvm.withLock(lock.writeLock()) {
      featureStorage = Map.empty
    }
  }
}

trait CleanableOrmFeatureInstanceDao[A <: FeaturesTableBase] extends OrmFeatureInstanceDaoBase[A] with CleanableDao {

  def clean(implicit ec: ExecutionContext): Future[Unit] = {
    import ru.yandex.vertis.promocoder.dao.impl.jdbc.api.queryDeleteActionExtensionMethods
    database.runOnMaster_(table.delete)
  }
}

trait CleanableJvmPromocodeInstanceDso extends JvmPromocodeInstanceDao with CleanableDao {

  override def clean(implicit ec: ExecutionContext): Future[Unit] = Future {
    jvm.withLock(lock.writeLock()) {
      storage = Map.empty[PromocodeInstanceId, PromocodeInstance]
    }
  }
}

trait CleanableOrmPromocodeInstanceDao extends OrmPromocodeInstanceDao with CleanableDao {

  override def clean(implicit ec: ExecutionContext): Future[Unit] = {
    import ru.yandex.vertis.promocoder.dao.impl.jdbc.api.{jdbcActionExtensionMethods, queryDeleteActionExtensionMethods}
    val action = promocodeInstanceTbl.delete
    database.run(action.transactionally).map(_ => ())
  }

}

trait CleanableOrmAutoProlongFeatureDao extends OrmAutoProlongFeatureDao with CleanableDao {

  override def clean(implicit ec: ExecutionContext): Future[Unit] = {
    import ru.yandex.vertis.promocoder.dao.impl.jdbc.api.{jdbcActionExtensionMethods, queryDeleteActionExtensionMethods}
    val action = table.delete
    database.run(action.transactionally).map(_ => ())
  }
}
