package ru.yandex.vertis.safe_deal.controller.impl

import com.google.protobuf.ByteString
import ru.yandex.vertis.palma.encrypted.content.{Image => EncryptedImage}
import ru.yandex.vertis.palma.images.images.Image
import ru.yandex.vertis.safe_deal.controller.DocumentController
import zio.Task

class DocumentControllerMock() extends DocumentController.Service {

  override def uploadEncryptedImage(file: ByteString): Task[EncryptedImage] = {
    Task.succeed(EncryptedImage())
  }

  override def decryptImage(photo: EncryptedImage): Task[Image] = {
    Task.succeed(Image())
  }
}

object DocumentControllerMock {}
