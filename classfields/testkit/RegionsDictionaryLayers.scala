package ru.yandex.vertis.shark

import ru.yandex.vertis.zio_baker.geobase.Tree
import ru.yandex.vertis.shark.dictionary.RegionsDictionary
import ru.yandex.vertis.zio_baker.zio.resource.impl.{RegionsResource, S3Resource}
import ru.yandex.vertis.zio_baker.zio.resource.{Resource, ResourceLoader}
import common.zio.clients.s3.{S3Client, S3ClientLive}
import common.zio.clients.s3.S3Client.S3Auth
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{Has, ULayer, URLayer, ZLayer}

import scala.concurrent.duration.DurationInt

trait RegionsDictionaryLayers {

  private val s3ClientConfig = {
    val auth = S3Auth(
      key = "введи меня",
      secret = "введи меня"
    )

    S3Client.S3Config(
      auth = auth,
      url = "http://s3.mds.yandex.net",
      signUrl = None,
      region = "yndx",
      requestTimeout = 300.seconds,
      connectionTimeout = 30.seconds,
      numRetries = 3,
      maxConnections = 10
    )
  }

  private val regionsResourceConfig = RegionsResource.Config(
    bucket = "classified-test",
    key = "geo/regions",
    refreshInterval = 1.minutes
  )

  protected lazy val regionsDictionaryLayer: URLayer[Blocking with Clock, RegionsDictionary] = {
    val regionsResourceLoaderLayer = ZLayer.requires[Clock] >>> ResourceLoader.live
    val regionsResourceLayer: ULayer[Has[Resource[S3Resource.Env, Tree]]] =
      ZLayer.succeed(regionsResourceConfig) >>> RegionsResource.live

    val s3ConfigLayer: ULayer[Has[S3Client.S3Config]] = ZLayer.succeed(s3ClientConfig)
    val s3ClienLayer = s3ConfigLayer >>> S3ClientLive.live

    ZLayer.requires[Blocking] ++ regionsResourceLoaderLayer ++ regionsResourceLayer ++ s3ClienLayer >>>
      RegionsDictionary.live
  }
}
