package ru.yandex.complaints.dao.complaints.services.quality.impl

import ru.yandex.complaints.model.ComplaintType
import ru.yandex.complaints.services.quality.impl.ModerationQualityAnalyzerImpl

/**
  * Spec for [[ModerationQualityAnalyzerImpl]] used for with ComplaintType.NoAnswer
  *
  * @author frenki
  */
class AutoruNoAnswerModerationQualityAnalyzerSpec extends AutoruModerationQualityAnalyzerSpec {

  override protected val options: ModerationQualityAnalyzerImpl.Options =
    ModerationQualityAnalyzerImpl.Options.AutoruNoAnswer
}