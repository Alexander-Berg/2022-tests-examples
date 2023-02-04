package ru.yandex.auto.vin.decoder.partners.checkburo

import ru.yandex.auto.vin.decoder.partners.checkburo.model.{CheckburoOrderResponse, ReadyOrder}

import scala.language.implicitConversions

package object converter {

  implicit def responseToReadyReport[R](response: CheckburoOrderResponse): ReadyOrder[R] = {
    response match {
      case r: ReadyOrder[R @unchecked] => r
      case _ => throw new RuntimeException(s"ReadyReport expected")
    }
  }
}
