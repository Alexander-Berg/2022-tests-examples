package ru.yandex.vertis.moderation.dao.impl.inmemory

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.dao.{LookbackDao, LookbackDaoSpecBase}

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class InMemoryLookbackDaoSpec extends LookbackDaoSpecBase[Int] {

  override val lookbackDao: LookbackDao[Int] = new InMemoryLookbackDao[Int]

  override val payloadGen: Gen[Int] = Gen.chooseNum(-9999, +9999)
}
