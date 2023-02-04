package ru.yandex.realty.capa.plugin

import java.{lang => jl, util => ju}

import ru.yandex.capa.plugin.builder.{IndexErrorBuilder, IndexFreshnessInfoBuilder, IndexInfoBuilder}
import ru.yandex.capa.plugin.model.{CommonPartnerIndexError, CommonPartnerIndexFreshnessInfo, CommonPartnerIndexInfo}

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

/**
  * User: igormazur
  */
class RealtyIndexInfoBuilder extends IndexInfoBuilder {
  var indexTime: Option[ju.Date] = None
  var partnerId: Option[Long] = None
  var feedId: Option[Long] = None
  var totalAdsAmount: Option[Int] = None
  var acceptedAdsAmount: Option[Int] = None
  var declinedAdsAmount: Option[Int] = None
  var clusterHeadAmount: Option[Int] = None
  var noPlacementOffersAmount: Option[Int] = None
  var invalidPrimarySaleOffersAmount: Option[Int] = None
  var partnerErrors = List.empty[CommonPartnerIndexError]

  def withIndexTime(`val`: ju.Date): IndexInfoBuilder = { indexTime = Option(`val`); this }

  def withPartnerId(`val`: Long): IndexInfoBuilder = { partnerId = Option(`val`); this }

  def withFeedId(`val`: Long): IndexInfoBuilder = { feedId = Option(`val`); this }

  def withTotalAdsAmount(`val`: Int): IndexInfoBuilder = { totalAdsAmount = Option(`val`); this }

  def withAcceptedAdsAmount(`val`: Int): IndexInfoBuilder = { acceptedAdsAmount = Option(`val`); this }

  def withDeclinedAdsAmount(`val`: Int): IndexInfoBuilder = { declinedAdsAmount = Option(`val`); this }

  def withClusterHeadAmount(`val`: Int): IndexInfoBuilder = { clusterHeadAmount = Option(`val`); this }

  def withNoPlacementOffersAmount(`val`: Int): IndexInfoBuilder = { noPlacementOffersAmount = Option(`val`); this }

  def withInvalidPrimarySaleOffersAmount(`val`: Int): IndexInfoBuilder = {
    invalidPrimarySaleOffersAmount = Option(`val`); this
  }

  def withErrors(`val`: ju.List[CommonPartnerIndexError]): IndexInfoBuilder = { partnerErrors = `val`.toList; this }

  def build(): CommonPartnerIndexInfo =
    new RealtyCommonPartnerIndexInfo(
      indexTime.orNull,
      partnerId.get,
      feedId.get,
      totalAdsAmount.get,
      acceptedAdsAmount.get,
      declinedAdsAmount.get,
      clusterHeadAmount.get,
      noPlacementOffersAmount = noPlacementOffersAmount.get,
      invalidPrimarySaleOffersAmount = invalidPrimarySaleOffersAmount.get,
      partnerErrors
    )

  def getIndexErrorBuilder: IndexErrorBuilder = new RealtyIndexErrorBuilder

}

class RealtyIndexErrorBuilder extends IndexErrorBuilder {

  var errorType: Option[String] = None
  var count: Option[Int] = None
  var urls = List.empty[String]

  def withType(`val`: String): IndexErrorBuilder = { errorType = Option(`val`); this }

  def withCount(`val`: Int): IndexErrorBuilder = { count = Option(`val`); this }

  def withUrls(`val`: ju.List[String]): IndexErrorBuilder = { urls = `val`.toList; this }

  def build(): CommonPartnerIndexError = new RealtyCommonPartnerIndexError(errorType.orNull, count.get, urls)

}

class RealtyIndexFreshnessInfoBuilder extends IndexFreshnessInfoBuilder {

  var info: Option[CommonPartnerIndexInfo] = None
  var count: Option[Int] = None

  def withInfo(`val`: CommonPartnerIndexInfo): IndexFreshnessInfoBuilder = { info = Option(`val`); this }

  def withFreshAdsAmount(`val`: Int): IndexFreshnessInfoBuilder = { count = Option(`val`); this }

  def build(): CommonPartnerIndexFreshnessInfo = new CommonPartnerIndexFreshnessInfo {
    def getFreshAdsAmount: Int = count.getOrElse(0)
    def getInfo: CommonPartnerIndexInfo = info.orNull
  }

  def getIndexInfoBuilder: IndexInfoBuilder = new RealtyIndexInfoBuilder
}

class RealtyCommonPartnerIndexInfo(
  @BeanProperty val indexTime: ju.Date,
  @BeanProperty val partnerId: Long,
  @BeanProperty val feedId: Long,
  @BeanProperty val total: Int,
  @BeanProperty val accepted: Int,
  @BeanProperty val declined: Int,
  @BeanProperty val clusterHeadAmount: Int,
  @BeanProperty val noPlacementOffersAmount: Int,
  @BeanProperty val invalidPrimarySaleOffersAmount: Int,
  @BeanProperty val errors: ju.List[CommonPartnerIndexError]
) extends CommonPartnerIndexInfo

class RealtyCommonPartnerIndexError(
  val `type`: String,
  @BeanProperty val count: Int,
  @BeanProperty val urls: ju.List[String]
) extends CommonPartnerIndexError {
  def getType: String = `type`
}
