package ru.yandex.realty.componenttest.extdata.core

import java.io.InputStream

import ru.yandex.extdata.adapter.ExtDataServiceLoader
import ru.yandex.extdata.core.DataType

import scala.util.{Failure, Success}

trait ExtdataReplicatedResourceStub extends ExtdataResourceStub with ComponentTestReplicateServiceProvider {

  def stubFromTestingResourceService(dataType: DataType): Unit = {
    testingReplicateService.replicate(dataType) match {
      case Success(_) =>
        stub(dataType, readReplicatedResource(dataType))
      case Failure(e) =>
        throw new IllegalStateException(s"Failed to replicate resource: dataType=$dataType", e)
    }
  }

  private def readReplicatedResource(dataType: DataType): InputStream = {
    new ExtDataServiceLoader(extDataService, dataType).load()
  }

}
