package ru.yandex.vertis.punisher.dao.impl

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.punisher.MockAutoruDaoBuilder
import ru.yandex.vertis.punisher.dao.{RecentYandexUidsDao, RecentYandexUidsDaoSpec}

@RunWith(classOf[JUnitRunner])
class RecentYandexUidsDaoImplSpec extends RecentYandexUidsDaoSpec {

  override lazy val dao: RecentYandexUidsDao[F] = MockAutoruDaoBuilder.recentYandexUidsDao
}
