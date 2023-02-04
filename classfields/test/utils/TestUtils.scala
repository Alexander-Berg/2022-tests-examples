package ru.yandex.vertis.general.wizard.meta.utils

import ru.yandex.vertis.general.wizard.meta.resources.{IntentionPragmaticsSnapshot, MetaPragmaticsSnapshot}

object TestUtils {
  val EmptyMetaPragmaticsSnapshot: MetaPragmaticsSnapshot = MetaPragmaticsSnapshot(Seq.empty)
  val EmptyIntentionsSnapshot: IntentionPragmaticsSnapshot = IntentionPragmaticsSnapshot(Seq.empty)
}
