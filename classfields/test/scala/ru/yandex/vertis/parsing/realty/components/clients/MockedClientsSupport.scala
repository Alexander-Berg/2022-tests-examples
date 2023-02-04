package ru.yandex.vertis.parsing.realty.components.clients

import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.clients.bunker.BunkerClient
import ru.yandex.vertis.parsing.clients.downloader.UrlDownloader
import ru.yandex.vertis.parsing.clients.kafka.KafkaCreator
import ru.yandex.vertis.parsing.clients.mds.MdsUploader
import ru.yandex.vertis.parsing.clients.s3.FileStorage
import ru.yandex.vertis.parsing.clients.bucket.BucketClient
import ru.yandex.vertis.parsing.clients.sender.SenderClient
import ru.yandex.vertis.parsing.realty.clients.geocoder.RealtyGeocoder

trait MockedClientsSupport extends ClientsAware with MockitoSupport {
  val realtyGeocoder: RealtyGeocoder = mock[RealtyGeocoder]

  val fileStorage: FileStorage = mock[FileStorage]

  val smartAgentClient: BucketClient = mock[BucketClient]

  val anonymousUrlDownloader: UrlDownloader = mock[UrlDownloader]

  val urlDownloader: UrlDownloader = mock[UrlDownloader]

  val kafkaCreator: KafkaCreator = mock[KafkaCreator]

  val bunkerClient: BunkerClient = mock[BunkerClient]

  val senderClient: SenderClient = mock[SenderClient]

  override val mdsUploader: MdsUploader = mock[MdsUploader]
}
