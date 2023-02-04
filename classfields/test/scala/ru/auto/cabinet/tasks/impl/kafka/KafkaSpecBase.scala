package ru.auto.cabinet.tasks.impl.kafka

import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpec

trait KafkaSpecBase extends AnyWordSpec with BeforeAndAfterAll with MockFactory
