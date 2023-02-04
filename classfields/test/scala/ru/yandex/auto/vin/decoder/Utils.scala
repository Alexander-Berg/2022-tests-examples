package ru.yandex.auto.vin.decoder

import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import ru.yandex.extdata.core.Data.FileData
import ru.yandex.extdata.core.event.ContainerEventListener
import ru.yandex.extdata.core.service.ExtDataService
import ru.yandex.extdata.core.{Controller, DataType, Instance, InstanceHeader}

import java.io.File
import java.nio.file.Files
import scala.util.Try

/**
  * Created by artvl on 13.07.16.
  */
trait Utils extends MockitoSugar {

  def prepareController(fileName: String, dataType: DataType): Controller = {
    val fs = getClass.getResourceAsStream("/" + fileName)

    val file = new File("tmp_" + fileName)
    Files.write(file.toPath, fs.readAllBytes());

    val streamingData = FileData(file)
    val instanceHeader = InstanceHeader("1", 0, null)
    val instance = Instance(instanceHeader, streamingData)

    val controller = mock[Controller]

    val extDataService = mock[ExtDataService]

    val containerEventListener = mock[ContainerEventListener]

    when(extDataService.getLast(dataType)).thenReturn(Try(instance))

    when(controller.extDataService).thenReturn(extDataService)

    when(controller.listenerContainer).thenReturn(containerEventListener)
    controller
  }

}
