package auto.dealers.calltracking.model.testkit

import common.zio.testkit.CommonGen._
import ru.auto.calltracking.proto.model.CallTranscription

object TranscriptionGen {

  val oneTranscription = for {
    sourceString <- anyString1
    targetString <- anyString1
  } yield CallTranscription(
    Seq(
      CallTranscription.Phrase(CallTranscription.Speaker.SOURCE, 0, 22, sourceString),
      CallTranscription.Phrase(CallTranscription.Speaker.TARGET, 22, 44, targetString)
    )
  )
}
