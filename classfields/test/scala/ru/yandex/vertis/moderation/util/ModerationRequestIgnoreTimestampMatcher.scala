package ru.yandex.vertis.moderation.util

import org.mockito.ArgumentMatcher
import ru.yandex.vertis.moderation.model.ModerationRequest

/**
  * @author potseluev
  */
class ModerationRequestIgnoreTimestampMatcher(left: ModerationRequest) extends ArgumentMatcher[scala.Any] {

  override def matches(obj: scala.Any): Boolean =
    obj match {
      case right: ModerationRequest =>
        right.withTimestamp(left.timestamp) == left
      case _ => false
    }
}
