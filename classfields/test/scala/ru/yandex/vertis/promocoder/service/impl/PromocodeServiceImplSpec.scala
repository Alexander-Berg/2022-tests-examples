package ru.yandex.vertis.promocoder.service.impl

import ru.yandex.vertis.promocoder.dao.PromocodeAliasDao.Record
import ru.yandex.vertis.promocoder.dao.impl.jvm.{JvmPromocodeAliasDao, JvmPromocodeDao}
import ru.yandex.vertis.promocoder.model.Promocode
import ru.yandex.vertis.promocoder.service.{PromocodeService, PromocodeServiceSpec}
import ru.yandex.vertis.promocoder.util.{CharsGenerator, DefaultPromocodeGenerator}

import scala.concurrent.Future

/** Runnable specs on [[PromocodeServiceImpl]]
  *
  * @author alex-kovalenko
  */
class PromocodeServiceImplSpec extends PromocodeServiceSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def getService(promocodes: Iterable[Promocode]): PromocodeService = {
    val promocodeDao = new JvmPromocodeDao()
    Future.sequence(promocodes.map(promocodeDao.upsert)).futureValue

    val aliasesDao = new JvmPromocodeAliasDao()
    Future.sequence(promocodes.map(p => aliasesDao.upsert(Record(p.code, p.aliases)))).futureValue

    new PromocodeServiceImpl(promocodeDao, aliasesDao, new DefaultPromocodeGenerator(CharsGenerator.Default))
  }
}
