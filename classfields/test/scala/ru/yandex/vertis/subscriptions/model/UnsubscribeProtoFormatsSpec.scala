package ru.yandex.vertis.subscriptions.model

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
  * Runnable spec on [[UnsubscribeProtoFormats]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class UnsubscribeProtoFormatsSpec extends ProtoFormatSpecBase {

  testFormat(UnsubscribeProtoFormats.UnsubscribeProtoFormat, UnsubscribeGenerators.unsubscribe)

}
