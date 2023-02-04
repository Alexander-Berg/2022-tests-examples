package ru.yandex.vertis.telepony.journal.impl

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.testkit.TestKit
import ru.yandex.vertis.telepony.component.impl.{DomainKafkaComponentImpl, KafkaSupportComponentImpl}
import ru.yandex.vertis.telepony.component.{AkkaComponent, ComponentComponent, DomainComponent, TypedDomainComponent}
import ru.yandex.vertis.telepony.journal.JournalSpec
import ru.yandex.vertis.telepony.journal.kafka.KafkaJournals
import ru.yandex.vertis.telepony.journal.kafka.KafkaJournals.Topic
import ru.yandex.vertis.telepony.model.{Domain, TypedDomains}
import ru.yandex.vertis.telepony.settings.KafkaSettings
import ru.yandex.vertis.telepony.util.{KafkaTestContainer, TestComponent, TestPrometheusComponent}

/**
  * @author @logab
  */
class KafkaJournalIntSpec
  extends TestKit(ActorSystem("KafkaJournalSpec"))
  with JournalSpec
  with KafkaSupportComponentImpl
  with DomainKafkaComponentImpl
  with TypedDomainComponent
  with DomainComponent
  with TestPrometheusComponent
  with TestComponent
  with AkkaComponent
  with ComponentComponent {

  override def domain: Domain = TypedDomains.autotest.toString

  override def kafkaSettings: KafkaSettings = KafkaSettings(KafkaTestContainer.getBootstrapServers)

  override def testTopic: KafkaJournals.Topic = new Topic("telepony-test-no-keys") {}

  implicit override val actorSystem: ActorSystem = ActorSystem()

  implicit override val materializer: Materializer = Materializer(actorSystem)
}
