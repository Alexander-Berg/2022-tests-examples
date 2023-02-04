package ru.yandex.vertis.parsing.auto.components.dao

import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport._
import ru.yandex.vertis.parsing.auto.dao.accessors.AccessorsDao
import ru.yandex.vertis.parsing.auto.dao.legacy.LegacyDao
import ru.yandex.vertis.parsing.auto.dao.meta.MetaDao
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.auto.dao.parsedoffers.ParsedOffersDao
import ru.yandex.vertis.parsing.auto.dao.phones.PhonesDao
import ru.yandex.vertis.parsing.auto.dao.photo.PhotoDao
import ru.yandex.vertis.parsing.auto.dao.resellers.ResellersDao
import ru.yandex.vertis.parsing.auto.dao.stats.StatsDao
import ru.yandex.vertis.parsing.auto.dao.vinscrapper.VinScrapperDao
import ru.yandex.vertis.parsing.dao.deactivator.DeactivatorQueueDao
import ru.yandex.vertis.parsing.dao.holocron.HolocronDao

/**
  * TODO
  *
  * @author aborunov
  */
trait MockedDaoSupport extends DaoAware {
  override val parsedOffersDao: ParsedOffersDao = mock[ParsedOffersDao]

  override val vinScrapperDao: VinScrapperDao = mock[VinScrapperDao]

  override val metaDao: MetaDao = mock[MetaDao]

  override val phonesDao: PhonesDao = mock[PhonesDao]

  override val legacyDao: LegacyDao = mock[LegacyDao]

  override val photoDao: PhotoDao = mock[PhotoDao]

  override val statsDao: StatsDao = mock[StatsDao]

  override val holocronDao: HolocronDao[ParsedRow] = mock[HolocronDao[ParsedRow]]

  override val accessorsDao: AccessorsDao = mock[AccessorsDao]

  override val resellersDao: ResellersDao = mock[ResellersDao]

  override val deactivatorQueueDao: DeactivatorQueueDao[Category] = mock[DeactivatorQueueDao[Category]]
}
