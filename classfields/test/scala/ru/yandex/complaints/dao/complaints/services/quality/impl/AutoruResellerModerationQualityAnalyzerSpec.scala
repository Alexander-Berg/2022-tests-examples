package ru.yandex.complaints.dao.complaints.services.quality.impl

import ru.yandex.complaints.services.quality.impl.ModerationQualityAnalyzerImpl

/**
  * Spec for [[ModerationQualityAnalyzerImpl]] used for with ComplaintType.Reseller
  *
  * @author frenki
  */
class AutoruResellerModerationQualityAnalyzerSpec
  extends AutoruModerationQualityAnalyzerSpec {

  override protected val options: ModerationQualityAnalyzerImpl.Options =
    ModerationQualityAnalyzerImpl.Options.AutoruReseller
}