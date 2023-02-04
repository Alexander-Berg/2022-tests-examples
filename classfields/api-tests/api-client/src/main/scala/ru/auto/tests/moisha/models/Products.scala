package ru.auto.tests.moisha.models

object Products extends Enumeration {
  val Placement = Value("placement")
  val Special = Value("special-offer")
  val Boost = Value("boost")
  val Reset = Value("reset")
  val Premium = Value("premium")
  val PremiumOffer = Value("premium-offer")
  val Highlighting = Value("highlighting")
  val Top = Value("top")
  val Certification = Value("certification")
  val MobileCertification = Value("certification-mobile")
  val Badge = Value("badge")
  val VinHistory = Value("vin-history")
  val TradeInRequestCarsNew = Value("trade-in-request:cars:new")
  val TradeInRequestCarsUsed = Value("trade-in-request:cars:used")

  val TurboPackage = Value("turbo-package")
  val GibddHistoryReport = Value("gibdd-history:report")
  val CmExpertHistoryReport = Value("cm-expert-history:report")
  val ApplicationCreditSingle = Value("application-credit:single")

  val ShowInStories = Value("show-in-stories")
}
