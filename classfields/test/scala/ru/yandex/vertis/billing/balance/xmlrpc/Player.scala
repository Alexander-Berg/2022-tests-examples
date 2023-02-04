package ru.yandex.vertis.billing.balance.xmlrpc

import scala.xml.Elem

/**
  * Created by IntelliJ IDEA.
  * User: alesavin
  * Date: 24.09.14
  * Time: 21:39
  */
trait Player extends Recorder {

  override def dump(xml: Elem): Elem = {
    throw new IndexOutOfBoundsException("No play file found")
  }

}
