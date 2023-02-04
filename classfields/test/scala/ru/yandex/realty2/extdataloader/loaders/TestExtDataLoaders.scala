package ru.yandex.realty2.extdataloader.loaders

import java.nio.file.{Files, Paths}

import ru.yandex.realty.storage.verba.VerbaStorage
import ru.yandex.realty.util.IOUtil
import ru.yandex.verba2.parse.SaxVerbaParser

object TestExtDataLoaders {

  def createVerbaStorage(resourceClasspath: String): VerbaStorage = {
    val verbaFilePath = getClass.getClassLoader.getResource(resourceClasspath).getFile
    IOUtil.using(Files.newInputStream(Paths.get(verbaFilePath))) { stream =>
      val parser = new SaxVerbaParser()
      val service = parser.parseXml(stream)
      new VerbaStorage(service.getDictionaries)
    }
  }
}
