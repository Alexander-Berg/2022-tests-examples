package ru.yandex.vos2.autoru.utils

import ru.yandex.vos2.autoru.config.TestAutoruApiComponents

/**
  * Created by andrey on 3/17/17.
  */
trait PhotoUtilsApiSuite extends Vos2ApiSuite {
  private lazy val x: PhotoUtils = PhotoUtilsGenerator.generatePhotoUtils

  override lazy val apiComponents = new TestAutoruApiComponents {
    override lazy val coreComponents = components
    override lazy val actorSystem = system

    override lazy val photoUtils: PhotoUtils = x
  }
  val imageHashGenerator = PhotoUtilsGenerator.imageHashGenerator
}
