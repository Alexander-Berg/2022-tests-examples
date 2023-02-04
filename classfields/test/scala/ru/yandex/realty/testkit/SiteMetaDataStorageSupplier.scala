package ru.yandex.realty.testkit

import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.method.SiteStatistic
import ru.yandex.realty.model.offer.Rooms
import ru.yandex.realty.model.sites.Site
import ru.yandex.realty.service.storage.SupportCanonicalsUrlStorage
import ru.yandex.realty.service.{SiteInfoService, SiteStatisticsService}
import ru.yandex.realty.sites.stat.SiteInfoStorage
import ru.yandex.realty.traffic.model.site.SiteMetaDataItem
import ru.yandex.realty.traffic.service.site.SiteMetaDataStorage
import zio._

object SiteMetaDataStorageSupplier {

  private object CanonicalsStub extends SupportCanonicalsUrlStorage.Service {
    override def findForSite(site: Site): IO[Option[Nothing], String] =
      UIO.succeed(
        s"/${site.getId}/mainpage/"
      )

    override def findForSiteOffersListing(site: Site, rooms: Option[Rooms]): IO[Option[Nothing], String] =
      UIO.effectTotal(
        s"/${site.getId}/listing/" + rooms.map(Rooms.asString).map(_ + "/").getOrElse(None)
      )
  }

  object StubStatisticsService extends SiteStatisticsService.Service {
    override def monthly(siteId: Long, siteStat: SiteStatistic): Task[Int] = Task(1)
  }

  private def siteInfoServiceLayer(infoStorage: SiteInfoStorage) = {
    val infoStorageL =
      ZLayer.succeed(new Provider[SiteInfoStorage] {
        override def get(): SiteInfoStorage = infoStorage
      })

    ZLayer.succeed[SupportCanonicalsUrlStorage.Service](CanonicalsStub) ++ ZLayer
      .succeed[SiteStatisticsService.Service](StubStatisticsService) ++ infoStorageL >>> SiteInfoService.live
  }

  def supply(sites: Seq[Site], infoStorage: SiteInfoStorage): Task[SiteMetaDataStorage.Service] =
    ZIO
      .foreach(sites) { site =>
        SiteInfoService
          .getInfo(site)
          .map(Some(_))
          .orElseSucceed(None)
          .map { metaOpt =>
            site.getId -> SiteMetaDataItem(
              metaOpt,
              relevance = 1
            )
          }
      }
      .map(_.toMap)
      .map(new SiteMetaDataStorage.Live(_))
      .provideLayer(siteInfoServiceLayer(infoStorage))
}
