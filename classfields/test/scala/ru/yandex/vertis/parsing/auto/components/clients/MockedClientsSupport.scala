package ru.yandex.vertis.parsing.auto.components.clients

import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.clients._24auto.Auto24Client
import ru.yandex.vertis.parsing.auto.clients.av100.Av100Client
import ru.yandex.vertis.parsing.auto.clients.catalog.CatalogClient
import ru.yandex.vertis.parsing.auto.clients.importer.ImportClient
import ru.yandex.vertis.parsing.auto.clients.searcher.SearcherClient
import ru.yandex.vertis.parsing.auto.clients.searchline.SearchlineClient
import ru.yandex.vertis.parsing.auto.clients.verba.VerbaClient
import ru.yandex.vertis.parsing.auto.clients.vinscrapper.VinScrapperClient
import ru.yandex.vertis.parsing.auto.clients.vos.VosClient
import ru.yandex.vertis.parsing.clients.bunker.BunkerClient
import ru.yandex.vertis.parsing.clients.callcenterHelperApi.CallCenterHelperClient
import ru.yandex.vertis.parsing.clients.downloader.UrlDownloader
import ru.yandex.vertis.parsing.clients.geocoder.Geocoder
import ru.yandex.vertis.parsing.clients.kafka.KafkaCreator
import ru.yandex.vertis.parsing.clients.mds.MdsUploader
import ru.yandex.vertis.parsing.clients.s3.FileStorage
import ru.yandex.vertis.parsing.clients.bucket.BucketClient
import ru.yandex.vertis.parsing.clients.sender.SenderClient
import ru.yandex.vertis.parsing.clients.statface.StatfaceClient
import ru.yandex.vertis.parsing.clients.telegram.TelegramClient
import ru.yandex.vertis.parsing.clients.telepony.TeleponyClient

/**
  * TODO
  *
  * @author aborunov
  */
trait MockedClientsSupport extends ClientsAware with MockitoSupport {
  val importClient: ImportClient = mock[ImportClient]

  val urlDownloader: UrlDownloader = mock[UrlDownloader]

  val anonymousUrlDownloader: UrlDownloader = mock[UrlDownloader]

  val zoraUrlDownloader: UrlDownloader = mock[UrlDownloader]

  val mdsUploader: MdsUploader = mock[MdsUploader]

  val searcherClient: SearcherClient = mock[SearcherClient]

  val searchlineClient: SearchlineClient = mock[SearchlineClient]

  val catalogClient: CatalogClient = mock[CatalogClient]

  val geocoder: Geocoder = mock[Geocoder]

  val senderClient: SenderClient = mock[SenderClient]

  val telegramClient: TelegramClient = mock[TelegramClient]

  val av100Client: Av100Client = mock[Av100Client]

  val fileStorage: FileStorage = mock[FileStorage]

  val auto24Client: Auto24Client = mock[Auto24Client]

  val bucketClient: BucketClient = mock[BucketClient]

  val vosClient: VosClient = mock[VosClient]

  val verbaClient: VerbaClient = mock[VerbaClient]

  val bunkerClient: BunkerClient = mock[BunkerClient]

  val statfaceClient: StatfaceClient = mock[StatfaceClient]

  val kafkaCreator: KafkaCreator = mock[KafkaCreator]

  val vinScrapperClient: VinScrapperClient = mock[VinScrapperClient]

  val teleponyClient: TeleponyClient = mock[TeleponyClient]

  val callCenterHelperClient: CallCenterHelperClient = mock[CallCenterHelperClient]
}
