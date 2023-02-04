package ru.yandex.realty.componenttest.extdata.core

import java.nio.file.{Path, Paths}

import ru.yandex.extdata.core.DataType
import ru.yandex.extdata.core.builder.StorageBuilder
import ru.yandex.extdata.core.event.EventListener
import ru.yandex.extdata.core.service.ExtDataService
import ru.yandex.realty.application.ng.{DefaultExtdataClientConfigProvider, TypesafeConfigProvider}
import ru.yandex.realty2.extdataloader.{ExtDataServiceImpl, ExtDataServicePublisher}

trait ExtdataServiceProvider {

  def extDataService: ExtDataService

}

trait ComponentTestExtdataServiceProvider
  extends ExtdataServiceProvider
  with ComponentTestExdataEventListenerProvider
  with DefaultExtdataClientConfigProvider
  with StorageBuilder
  with TypesafeConfigProvider {

  def extDataPath: String = extdataClientConfig.extDataPath

  def dataIndexPath(dataType: DataType): Path = {
    Paths.get(s"$extDataPath/$dataType/index")
  }

  override lazy val extDataService: ExtDataService =
    new ExtDataServiceImpl(buildStorage(extDataPath)) with ExtDataServicePublisher {

      override def listener: EventListener = ComponentTestExtdataServiceProvider.this.listener

    }

}
