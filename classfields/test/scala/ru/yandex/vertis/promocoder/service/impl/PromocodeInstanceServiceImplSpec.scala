package ru.yandex.vertis.promocoder.service.impl

import ru.yandex.vertis.promocoder.dao.PromocodeInstanceDao
import ru.yandex.vertis.promocoder.dao.impl.jvm.{JvmPromocodeDao, JvmPromocodeInstanceDao}
import ru.yandex.vertis.promocoder.model.Promocode
import ru.yandex.vertis.promocoder.service.FeaturesShippingPromocodeInstanceServiceSpec.NoOpPromocodeService
import ru.yandex.vertis.promocoder.service.{PromocodeInstanceService, PromocodeInstanceServiceSpec}

import scala.concurrent.Future

/** Runnable specs on [[PromocodeInstanceServiceImpl]]
  *
  * @author alex-kovalenko
  */
class PromocodeInstanceServiceImplSpec extends PromocodeInstanceServiceSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  def getService(promocodes: Iterable[Promocode], instances: Iterable[Record]): PromocodeInstanceService = {
    val promocodeDao = new JvmPromocodeDao()
    Future.sequence(promocodes.map(promocodeDao.upsert)).futureValue
    val dao = new JvmPromocodeInstanceDao(promocodeDao)
    Future.sequence {
      instances.map { case (user, promocode) =>
        dao.insert(PromocodeInstanceDao.Source(user, promocode))
      }
    }.futureValue

    new PromocodeInstanceServiceImpl(dao, new NoOpPromocodeService)
  }
}
