package ru.yandex.vertis.subscriptions.model

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
  * Spec on [[WatchProtoFormats]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class WatchProtoFormatsSpec extends ProtoFormatSpecBase {

  testFormat(WatchProtoFormats.WatchProtoFormat, ModelGenerators.watch)

  testFormat(WatchProtoFormats.WatchPatchProtoFormat, ModelGenerators.watchPatch)
}
