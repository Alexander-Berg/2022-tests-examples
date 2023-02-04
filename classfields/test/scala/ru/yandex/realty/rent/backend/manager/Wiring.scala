package ru.yandex.realty.rent.backend.manager

import org.scalatest.prop.PropertyChecks
import realty.palma.rent_contract_faq.RentContractFaqItem
import realty.palma.rent_flat.RentFlat
import realty.palma.rent_user.RentUser
import realty.palma.spectrum_report.SpectrumReport
import ru.yandex.extdata.core.event.{Event, EventListener}
import ru.yandex.extdata.core.service.ExtDataService
import ru.yandex.extdata.core.{Controller, DataType, TaskId}
import ru.yandex.realty.application.ng.palma.client.PalmaClient
import ru.yandex.realty.application.ng.palma.encrypted.PalmaEncryptedServiceClient
import ru.yandex.realty.clients.abc.AbcClient
import ru.yandex.realty.clients.amohub.AmohubLeadServiceClient
import ru.yandex.realty.clients.cadastr.CadastrClient
import ru.yandex.realty.clients.calendar.CalendarClient
import ru.yandex.realty.clients.dochub.DocumentServiceClient
import ru.yandex.realty.clients.geohub.GeohubClient
import ru.yandex.realty.clients.searcher.gen.SearcherResponseModelGenerators
import ru.yandex.realty.clients.sms.SmsSendClient
import ru.yandex.realty.context.v2.DochubRendererSettingsProvider
import ru.yandex.realty.features.{Features, SimpleFeatures}
import ru.yandex.realty.http.{HttpEndpoint, RequestAware}
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.rent.backend.converter.house.services.HouseServiceConverter
import ru.yandex.realty.rent.backend.converter.notifications.FlatNotifications
import ru.yandex.realty.rent.backend.converter.showing.{FlatShowingConverter, ShowingWidgetFactory}
import ru.yandex.realty.rent.backend.converter.{
  FlatConverter,
  FlatDetailedInfoBuilder,
  ImageConverter,
  OwnerRequestConverter
}
import ru.yandex.realty.rent.backend.manager.card.OwnerCardManager
import ru.yandex.realty.rent.backend.manager.document.FlatDocumentManager
import ru.yandex.realty.rent.backend.manager.flat.{FlatDraftManager, FlatsManager}
import ru.yandex.realty.rent.backend.manager.house.services.{HouseServiceManager, MeterReadingsManager}
import ru.yandex.realty.rent.backend.manager.moderation.{
  ContractManager,
  ContractStatusManager,
  ModerationFlatsManager,
  ModerationManager,
  TermsManager
}
import ru.yandex.realty.rent.backend.validator.{ContractValidator, FlatValidatorImpl}
import ru.yandex.realty.rent.backend.{ExtendedUserManager, InsurancePolicyManager, ShowingCreator}
import ru.yandex.realty.rent.clients.elastic.ElasticSearchClient
import ru.yandex.realty.rent.clients.mango.MangoClient
import ru.yandex.realty.clients.tinkoff.e2c.{TinkoffE2CClient, TinkoffE2CManager}
import ru.yandex.realty.rent.backend.manager.showing.{RoommatesManager, ShowingEnricher, ShowingManager}
import ru.yandex.realty.rent.dao.{
  FlatDao,
  FlatKeyCodeDao,
  FlatQuestionnaireDao,
  FlatShowingDao,
  HouseServiceDao,
  InventoryDao,
  KeysHandoverDao,
  MeterReadingsDao,
  OwnerRequestDao,
  PaymentDao,
  PeriodDao,
  RentContractDao,
  RoommateCandidateDao,
  StatusAuditLogDao,
  UserCardBindsDao,
  UserDao,
  UserFlatDao,
  UserShowingDao
}
import ru.yandex.realty.rent.util.AmoLinkBuilder
import ru.yandex.realty.telepony.{AsyncPhoneUnifierClient, PhoneUnifierClient}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.TestDataSettings
import ru.yandex.realty.util.gen.SnowflakeIdGenerator
import ru.yandex.realty.{AsyncSpecBase, SpecBase}

trait Wiring
  extends SpecBase
  with AsyncSpecBase
  with RequestAware
  with PropertyChecks
  with SearcherResponseModelGenerators {

  implicit val traced: Traced = Traced.empty

  val amoEndpoint = HttpEndpoint("amo-host")
  val amoLinkBuilder = new AmoLinkBuilder(amoEndpoint)

  val mockFlatDao: FlatDao = mock[FlatDao]
  val mockFlatQuestionnaireDao: FlatQuestionnaireDao = mock[FlatQuestionnaireDao]
  val mockContractDao: RentContractDao = mock[RentContractDao]
  val mockUserFlatDao: UserFlatDao = mock[UserFlatDao]
  val mockUserDao: UserDao = mock[UserDao]
  val mockPaymentDao: PaymentDao = mock[PaymentDao]
  val mockUserShowingDao: UserShowingDao = mock[UserShowingDao]
  val mockFlatShowingDao: FlatShowingDao = mock[FlatShowingDao]
  val mockKeysHandoverDao: KeysHandoverDao = mock[KeysHandoverDao]
  val mockFlatInfoBuilder: FlatDetailedInfoBuilder = mock[FlatDetailedInfoBuilder]
  val mockFlatKeyCodeDao: FlatKeyCodeDao = mock[FlatKeyCodeDao]

  // workaround: there is an issue of mocking UserManager
  val smsConfirmationManager = new SmsConfirmationManager(
    smsSendClient = mock[SmsSendClient],
    smsSendTimeout = None
  )

  val statusAuditLogDao: StatusAuditLogDao = mock[StatusAuditLogDao]

  val ownerCardManager: OwnerCardManager = new OwnerCardManager(
    mock[TinkoffE2CManager],
    mock[TinkoffE2CClient],
    mockUserDao,
    mock[UserCardBindsDao],
    mock[RentContractDao]
  )

  val ownerRequestDao: OwnerRequestDao = mock[OwnerRequestDao]

  val flatDao: FlatDao = mock[FlatDao]
  val flatQuestionnaireDao: FlatQuestionnaireDao = mock[FlatQuestionnaireDao]
  val rentContractDao: RentContractDao = mock[RentContractDao]
  val paymentDao: PaymentDao = mock[PaymentDao]
  val mockMdsUrlBuilder: MdsUrlBuilder = new MdsUrlBuilder("//")
  val mockImageConverter: ImageConverter = new ImageConverter(mockMdsUrlBuilder)
  val flatShowingConverter = new FlatShowingConverter(mockImageConverter, amoLinkBuilder, new ShowingWidgetFactory)
  val mockCalendarClient: CalendarClient = mock[CalendarClient]
  val mockRoommateCandidateDao: RoommateCandidateDao = mock[RoommateCandidateDao]
  val mockHouseServiceDao: HouseServiceDao = mock[HouseServiceDao]
  val mockMeterReadingsDao: MeterReadingsDao = mock[MeterReadingsDao]
  val mockPeriodDao: PeriodDao = mock[PeriodDao]
  val mockHouseServiceConverter = new HouseServiceConverter(mockImageConverter)
  val mockInventoryDao: InventoryDao = mock[InventoryDao]

  val mockTestDataSettings: TestDataSettings = mock[TestDataSettings]
  val features: Features = new SimpleFeatures
  val flatNotifications = new FlatNotifications()(features)
  val flatConverter: FlatConverter = new FlatConverter(mockImageConverter, flatNotifications, features)

  val mockPhoneUnifierClient: PhoneUnifierClient = mock[PhoneUnifierClient]
  val mockAsyncPhoneUnifierClient: AsyncPhoneUnifierClient = AsyncPhoneUnifierClient.fromSync(mockPhoneUnifierClient)
  val mockAmohubLeadClient: AmohubLeadServiceClient = mock[AmohubLeadServiceClient]
  val mockShowingEnricher: ShowingEnricher = mock[ShowingEnricher]

  val flatInfoBuilder: FlatDetailedInfoBuilder =
    new FlatDetailedInfoBuilder(flatConverter, amoLinkBuilder, mockImageConverter)

  val roommatesManager: RoommatesManager = new RoommatesManager(
    mockUserDao,
    mockUserFlatDao,
    mockRoommateCandidateDao,
    mockUserShowingDao,
    mockFlatShowingDao,
    ownerRequestDao,
    showingManager,
    mockShowingEnricher,
    mockAsyncPhoneUnifierClient,
    features = features
  )

  val mockUserManager: UserManager = new UserManager(
    userDao = mockUserDao,
    phoneUnifierClient = mockAsyncPhoneUnifierClient,
    palmaRentUserClient = mock[PalmaClient[RentUser]],
    smsConfirmationManager = smsConfirmationManager,
    ownerRequestDao = ownerRequestDao,
    flatQuestionnaireDao = mockFlatQuestionnaireDao,
    userFlatDao = mockUserFlatDao,
    flatShowingDao = mockFlatShowingDao,
    showingManager = showingManager,
    userIdGenerator = new SnowflakeIdGenerator()
  )
  val extendedUserManager = new ExtendedUserManager(mockUserDao, mock[PalmaClient[RentUser]])
  val mockPalmaRentUserClient: PalmaClient[RentUser] = mock[PalmaClient[RentUser]]
  val mockPalmaRentFlatClient: PalmaClient[RentFlat] = mock[PalmaClient[RentFlat]]
  val mockPalmaEncryptedServiceClient: PalmaEncryptedServiceClient = mock[PalmaEncryptedServiceClient]
  val mockPalmaSpectrumDataClient: PalmaClient[SpectrumReport] = mock[PalmaClient[SpectrumReport]]

  val mockCadastrClient: CadastrClient = mock[CadastrClient]
  val mockFlatExcerptRequestManager: FlatExcerptRequestManager = mock[FlatExcerptRequestManager]

  val mockMangoClient: MangoClient = mock[MangoClient]
  val mockUtilManager: UtilManager = mock[UtilManager]
  val mockElasticClient: ElasticSearchClient = mock[ElasticSearchClient]
  val mockAbcClient: AbcClient = mock[AbcClient]
  val mockPalmaRentUser: PalmaClient[RentUser] = mock[PalmaClient[RentUser]]

  lazy val mockRentStatisticsManager: RentStatisticsManager = mock[RentStatisticsManager]

  val rendererSettingsController: Controller = new Controller {
    override def start(): Unit = {}
    override def close(): Unit = {}
    override def replicate(dataType: DataType): Unit = {}
    override def register(listener: EventListener): Unit = {}
    override def onEvent(e: Event): Unit = {}
    override def dispatch(id: TaskId, weight: Int, payload: () => Unit): Unit = {}
    override def extDataService: ExtDataService = mock[ExtDataService]
  }

  val rendererSettingsClient = new DochubRendererSettingsProvider(rendererSettingsController)

  val presetManager = new PresetManager(mockFlatDao, mockElasticClient)
  val moderationFlatsManager = new ModerationFlatsManager(mockFlatDao, presetManager, mockElasticClient)

  val calendarToken: String = "calendar-token-12345"

  lazy val showingCreator = new ShowingCreator(mockFlatShowingDao, flatDao, mockUserDao, mockUserShowingDao)

  val palmaRentContractFaqClient: PalmaClient[RentContractFaqItem] = mock[PalmaClient[RentContractFaqItem]]

  lazy val showingManager =
    new ShowingManager(
      mockUserDao,
      mockUserShowingDao,
      mockFlatShowingDao,
      ownerRequestDao,
      flatDao,
      flatQuestionnaireDao,
      flatShowingConverter,
      statusAuditLogDao,
      mockAsyncPhoneUnifierClient,
      mockCalendarClient,
      calendarToken,
      OnlineShowingsConfig.DefaultConfig,
      mockAbcClient,
      showingCreator,
      mockShowingEnricher
    )

  val termsManager = new TermsManager(mockUserDao)
  val documentClient: DocumentServiceClient = mock[DocumentServiceClient]

  val moderationManager: ModerationManager = new ModerationManager(
    flatDao = mockFlatDao,
    ownerRequestDao,
    mockFlatShowingDao,
    flatQuestionnaireDao = mockFlatQuestionnaireDao,
    contractDao = mockContractDao,
    userFlatDao = mockUserFlatDao,
    userDao = mockUserDao,
    flatInfoBuilder = flatInfoBuilder,
    flatShowingConverter = flatShowingConverter,
    palmaRentUserClient = mockPalmaRentUserClient,
    palmaRentFlatClient = mockPalmaRentFlatClient,
    cadastrClient = mockCadastrClient,
    flatExcerptRequestManager = mockFlatExcerptRequestManager,
    smsConfirmationManager = smsConfirmationManager,
    ownerCardManager = ownerCardManager,
    utilManager = mockUtilManager,
    imageConverter = mockImageConverter,
    presetManager = presetManager,
    moderationFlatsManager = moderationFlatsManager,
    amohubLeadClient = mockAmohubLeadClient,
    flatKeyCodeDao = mockFlatKeyCodeDao,
    features = features
  )

  val contractValidator = new ContractValidator(features)
  val contractPdfManager: ContractPdfManager = mock[ContractPdfManager]

  val contractManager =
    new ContractManager(
      mockFlatDao,
      mockFlatQuestionnaireDao,
      mockContractDao,
      mockPaymentDao,
      mockUserDao,
      ownerRequestDao,
      mockFlatShowingDao,
      statusAuditLogDao,
      flatInfoBuilder,
      palmaRentContractFaqClient,
      rendererSettingsClient,
      smsConfirmationManager,
      contractValidator,
      contractPdfManager,
      documentClient,
      extendedUserManager,
      features
    )

  val insurancePolicyManager = new InsurancePolicyManager(mockFlatDao, mockMangoClient, mockTestDataSettings)

  val contractStatusManager = new ContractStatusManager(
    contractManager,
    mockFlatDao,
    mockContractDao,
    mockUserFlatDao,
    mockUserDao,
    mockPaymentDao,
    statusAuditLogDao,
    ownerRequestDao,
    insurancePolicyManager,
    contractValidator,
    contractPdfManager,
    flatQuestionnaireDao,
    documentClient,
    features
  )

  val avatarnicaDownloadUrl = "avatars-url"

  val flatDocumentManager = new FlatDocumentManager(
    moderationManager = moderationManager,
    userDao = mockUserDao,
    flatDao = mockFlatDao,
    palmaRentFlatClient = mockPalmaRentFlatClient,
    palmaEncryptedServiceClient = mockPalmaEncryptedServiceClient,
    avatarnicaDownloadUrl = avatarnicaDownloadUrl
  )

  val meterReadingsManager = new MeterReadingsManager(
    mockHouseServiceDao,
    mockMeterReadingsDao,
    mockPeriodDao
  )

  val flatsManager = new FlatsManager(
    mockUserDao,
    mockFlatDao,
    mockContractDao,
    mockPaymentDao,
    flatQuestionnaireDao,
    inventoryDao = mockInventoryDao,
    mockPalmaRentUserClient,
    spectrumDataPalmaClient = mockPalmaSpectrumDataClient,
    flatConverter = flatConverter,
    imageConverter = mockImageConverter,
    meterReadingsManager = meterReadingsManager,
    flatValidator = FlatValidatorImpl,
    roommatesManager = roommatesManager,
    flatShowingDao = mockFlatShowingDao,
    keysHandoverDao = mockKeysHandoverDao,
    flatInfoBuilder = mockFlatInfoBuilder,
    rentStatisticsManager = mockRentStatisticsManager
  )

  val houseServiceManager = new HouseServiceManager(
    flatsManager,
    mockContractDao,
    ownerRequestDao,
    mockHouseServiceDao,
    statusAuditLogDao = statusAuditLogDao,
    userDao = mockUserDao,
    mockHouseServiceConverter,
    mockFlatShowingDao
  )

  val ownerRequestConverter = new OwnerRequestConverter(
    flatInfoBuilder
  )

  val ownerRequestManager = new OwnerRequestManager(
    flatDao,
    mockHouseServiceDao,
    ownerRequestDao,
    ownerRequestConverter,
    mockContractDao,
    houseServiceManager,
    meterReadingsManager,
    mockUserFlatDao
  )

  val mockGeohubClient: GeohubClient = mock[GeohubClient]

  val flatDraftManager = new FlatDraftManager(
    mockUserDao,
    flatDao,
    ownerRequestManager,
    mockUserFlatDao,
    mockAsyncPhoneUnifierClient,
    flatConverter,
    mockImageConverter,
    mockGeohubClient
  )
}
