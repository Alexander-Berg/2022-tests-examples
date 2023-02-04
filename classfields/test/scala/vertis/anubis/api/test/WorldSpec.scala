package vertis.anubis.api.test

import ru.yandex.vertis.broker.BrokerOptions
import ru.yandex.vertis.palma.PalmaOptions
import vertis.anubis.api.test.broker.{Event, Inner, SomeModel}
import vertis.anubis.api.services.ValidationWorld
import vertis.anubis.api.services.validate.World
import vertis.anubis.api.test.WorldSpec.TestWorld
import vertis.zio.test.ZioSpecBase

/** @author kusaeva
  */
class WorldSpec extends ZioSpecBase with ValidationTestSupport {

  "World" should {
    "return descriptors by option" in {
      val world = createWorld(Seq(InvalidMessage.getDescriptor))

      val brokerMsgs = world
        .getDescriptorsByOption(BrokerOptions.config)

      brokerMsgs.map(_.getFullName) should contain theSameElementsAs Seq(InvalidMessage.getDescriptor.getFullName)

      val palmaMsgs = world
        .getDescriptorsByOption(PalmaOptions.message)

      palmaMsgs.map(_.getFullName) should contain theSameElementsAs Seq(Mark.getDescriptor, Model.getDescriptor)
        .map(_.getFullName)
    }

    "detect changes" in {
      val schema = fds(Seq(Event.getDescriptor, SomeModel.getDescriptor, Inner.getDescriptor))

      val world = new World(schema, schema) with TestWorld {
        def changes: Set[String] = Set("vertis.anubis.api.test.broker.Inner")
      }

      val brokerMsgs =
        world
          .getDescriptorsByOption(BrokerOptions.config, changedOnly = true)

      brokerMsgs.map(_.getFullName) should contain theSameElementsAs Seq("vertis.anubis.api.test.broker.Event")
    }
  }
}

object WorldSpec {

  trait TestWorld extends ValidationWorld {
    def changes: Set[String]
    abstract override def getChanges: Set[String] = changes
  }
}
