package ru.auto.api.testkit

import auto.c2b.common.MobileTextsOuterClass.MobileTexts
import ru.auto.api.auth.ExtApiTokens
import ru.auto.api.currency.CurrencyRates
import ru.auto.api.extdata.DataService
import ru.auto.api.geo.{GeoSuggestList, Tree}
import ru.auto.api.metro.MetroBase
import ru.auto.api.model.bunker._
import ru.auto.api.model.bunker.c2b.auction.C2bAuctionMobileTexts
import ru.auto.api.model.bunker.carfax.resellers.ResellersWithFreeReportAccess
import ru.auto.api.model.bunker.chatbot.ChatBotInfo
import ru.auto.api.model.bunker.fake.FakePhonesList
import ru.auto.api.model.bunker.favoritereseller.FavoriteResellerList
import ru.auto.api.model.bunker.forceupdate.ForceUpdateVersions
import ru.auto.api.model.bunker.phoneview.PhoneViewWithAuth
import ru.auto.api.model.bunker.promolanding.ElectroPromoLandingInfo
import ru.auto.api.model.bunker.shark.CalculatorStaticContainer
import ru.auto.api.model.bunker.telepony.{CallbackWithExtendedInfo, PhoneRedirectInfo}
import ru.auto.api.model.catalog.EquipmentDict
import ru.auto.api.model.chat.preset.action.ChatActionPresets
import ru.auto.api.model.chat.preset.message.ChatMessagePresets
import ru.auto.api.model.moderation.{BanReasons, ReviewBanReasons}
import ru.auto.api.services.passport.util.UserProfileStubsProvider
import ru.auto.api.verba.AutoTagsDict

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 17.02.17
  */
object TestData extends DataService {
  val tree: Tree = Tree.from(TestDataEngine)
  val currency: CurrencyRates = CurrencyRates.from(TestDataEngine)
  val banReasons: BanReasons = BanReasons.from(TestDataEngine)
  val reviewBanReasons: ReviewBanReasons = ReviewBanReasons.from(TestDataEngine)
  val chatMessagePresets: ChatMessagePresets = ChatMessagePresets.from(TestDataEngine)
  val chatActionPresets: ChatActionPresets = ChatActionPresets.from(TestDataEngine)
  val geoSuggestListing: GeoSuggestList = GeoSuggestList.from(TestDataEngine)
  val metroBase: MetroBase = MetroBase.empty
  val userProfileStubs: UserProfileStubsProvider = UserProfileStubsProvider.Empty
  val certBunkerInfos: CertBunkerInfoBase = CertBunkerInfoBase.empty
  val dealerVasDescriptionBase: DealerVasDescriptionBase = DealerVasDescriptionBase.empty
  val chatBotInfo: ChatBotInfo = ChatBotInfo.from(TestDataEngine)
  val videoSearchBlackList: VideoSearchBlackList = VideoSearchBlackList.from(TestDataEngine)
  val equipmentDict: EquipmentDict = EquipmentDict.from(TestDataEngine)
  val autoTagsDict: AutoTagsDict = AutoTagsDict.from(TestDataEngine)
  val extApiTokens: ExtApiTokens = ExtApiTokens.from(TestDataEngine)
  val forceUpdateVersions: ForceUpdateVersions = ForceUpdateVersions.from(TestDataEngine)
  val phoneRedirectInfo: PhoneRedirectInfo = PhoneRedirectInfo.from(TestDataEngine)
  val favoriteResellerList: FavoriteResellerList = FavoriteResellerList.from(TestDataEngine)
  val fakePhonesList: FakePhonesList = FakePhonesList(Set.empty)
  val callbackWithExtendedInfo: CallbackWithExtendedInfo = CallbackWithExtendedInfo.from(TestDataEngine)
  val phoneViewWithAuth: PhoneViewWithAuth = PhoneViewWithAuth.Empty
  val c2bAuctionMobileTexts: C2bAuctionMobileTexts = C2bAuctionMobileTexts(MobileTexts.getDefaultInstance)
  val c2bAuctionStatuses: List[c2b.auction.Status] = List.empty
  val calculatorStatic: CalculatorStaticContainer = CalculatorStaticContainer(None)
  val electroPromoLandingInfo: ElectroPromoLandingInfo = ElectroPromoLandingInfo(None, None, None, None, None)
  val resellersWithFreeReportAccess: ResellersWithFreeReportAccess = ResellersWithFreeReportAccess(Set.empty)
}
