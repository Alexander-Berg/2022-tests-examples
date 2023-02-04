package ru.yandex.vertis.zio_baker.zio.resource.impl

import ru.yandex.vertis.zio_baker.geobase.Tree
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import ru.yandex.vertis.zio_baker.zio.resource.impl.S3Resource.Env
import ru.yandex.vertis.zio_baker.zio.resource.impl.RegionsResourceSpecBase._
import zio.ZIO
import zio.test.Assertion._
import zio.test._

object RegionsResourceSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Resource")(
      testM("Load and parse REGIONS resource") {
        for {
          regionsResource <- ZIO.service[Resource[Env, Tree]]
          regionTree <- regionsResource.load
          pathToRootFromMoscow = regionTree.pathToRoot(MoscowRegionId)
        } yield assert(pathToRootFromMoscow)(hasSize(equalTo(6)))
      }
    ).provideCustomLayerShared(s3ClientLayer ++ regionsResourceLayer)
}
