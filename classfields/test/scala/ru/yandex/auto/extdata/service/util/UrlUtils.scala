package ru.yandex.auto.extdata.service.util

import ru.yandex.auto.extdata.service.organic.builder.IdBuilder
import ru.yandex.auto.extdata.service.organic.model.LandingUrl
import cats.syntax.option._

object UrlUtils {
  private def listingUrl(
      isMobile: Boolean,
      markName: Option[String] = None,
      modelName: Option[String] = None,
      snippetId: Option[String] = None,
      onCredit: Option[Boolean] = None
  ): String = {
    val mobileDesktopParam = if (isMobile) "mobile" else "desktop"
    val markParam = markName.getOrElse("_no_mark_")
    val modelParam = modelName.getOrElse("_no_model_")
    val snippetIdParam = snippetId.getOrElse("_no_snippet_")
    val creditParam = onCredit.map(if (_) "on_credit=true" else "on_credit=false").getOrElse("_no_on_credit")

    List(mobileDesktopParam, markParam, modelParam, snippetIdParam, creditParam).mkString("_") + "_url"
  }

  def nonCreditMarkUrl(markName: String): String =
    listingUrl(markName = markName.some, isMobile = false)

  def creditMarkUrl(markName: String): String =
    listingUrl(markName = markName.some, onCredit = Some(true), isMobile = false)

  def nonCreditModelUrl(modelName: String, snippetId: Option[String] = None): String =
    listingUrl(modelName = modelName.some, snippetId = snippetId, isMobile = false)

  def creditModelUrl(modelName: String, snippetId: Option[String] = None): String =
    listingUrl(modelName = modelName.some, snippetId = snippetId, onCredit = Some(true), isMobile = false)

  def landingListingUrl(
      isMobile: Boolean,
      markName: Option[String] = None,
      modelName: Option[String] = None,
      snippetId: Option[String] = None,
      onCredit: Option[Boolean] = None
  ): LandingUrl = {
    val url = listingUrl(
      markName = markName,
      modelName = modelName,
      snippetId = snippetId,
      onCredit = onCredit,
      isMobile = isMobile
    )
    LandingUrl(url)
  }

  val creditDesktopLandingUrl: LandingUrl = landingListingUrl(onCredit = Some(true), isMobile = false)
  val creditMobileLandingUrl: LandingUrl = landingListingUrl(onCredit = Some(true), isMobile = true)
  val nonCreditDesktopLandingUrl: LandingUrl = landingListingUrl(isMobile = false)
  val nonCreditMobileLandingUrl: LandingUrl = landingListingUrl(isMobile = true)

  private val urlOnCreditRegex = "^.*on_credit=true.*$".r

  def isUrlOnCredit(url: String): Boolean = urlOnCreditRegex.findFirstIn(url).nonEmpty
}
