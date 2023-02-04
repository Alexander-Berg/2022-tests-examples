package vertis.shiva.client

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class ShivaClientIntSpec extends ShivaClientSupport {

  "ShivaClient" should {
    "list services" in withShivaClient { shivaClient =>
      shivaClient.listServices() >>= { services =>
        check("services exist")(services should not be empty) *>
          check("service names are correct")(services.map(_.name) should contain("anubis-api"))
      }
    }
  }
}
