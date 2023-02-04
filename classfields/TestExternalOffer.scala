package ru.yandex.vertis.feedprocessor.autoru.utils

import ru.yandex.vertis.feedprocessor.autoru.model.{ExternalOffer, TaskContext}

/**
  * @author pnaydenov
  */
case class TestExternalOffer(position: Int, taskContext: TaskContext) extends ExternalOffer
