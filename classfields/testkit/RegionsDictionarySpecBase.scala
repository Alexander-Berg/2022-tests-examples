package ru.yandex.vertis.shark.dictionary.impl

import ru.yandex.vertis.shark.dictionary.RegionsDictionary
import ru.yandex.vertis.zio_baker.zio.resource.ResourceLoader
import ru.yandex.vertis.zio_baker.zio.resource.impl.RegionsResourceSpecBase._
import zio.URLayer
import zio.clock.Clock
import zio.blocking.Blocking

trait RegionsDictionarySpecBase {

  private val regionsResourceLoaderLayer = Clock.live >>> ResourceLoader.live

  protected val regionsDictionaryLayer: URLayer[Blocking with Clock, RegionsDictionary] =
    Blocking.any ++ s3ClientLayer ++ regionsResourceLoaderLayer ++ regionsResourceLayer >>> RegionsDictionary.live
}
