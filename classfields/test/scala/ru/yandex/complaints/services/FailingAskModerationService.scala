package ru.yandex.complaints.services

import ru.yandex.complaints.model.ModerationObjectRequest

import scala.util.Try

/**
  * Created by s-reznick on 20.07.16.
  */
abstract class FailingAskModerationService extends CountingModerationService {

  class AskModerationObjectFailedException extends Exception

  override def askModerationObject(request: ModerationObjectRequest): Try[ModerationObjectInfo] =
    Try {
      super.askModerationObject(request)
      throw new AskModerationObjectFailedException
    }
}