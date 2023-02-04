package ru.yandex.vertis.moderation.service.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.dao.impl.inmemory.InMemoryOwnerDao
import ru.yandex.vertis.moderation.service.{OwnerService, OwnerServiceSpecBase}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Specs on [[OwnerService]] implemented in-memory
  *
  * @author sunlight
  */
@RunWith(classOf[JUnitRunner])
class OwnerServiceSpec extends OwnerServiceSpecBase {

  override def getOwnerService: OwnerService =
    new OwnerServiceImpl(
      new InMemoryOwnerDao
    )
}
