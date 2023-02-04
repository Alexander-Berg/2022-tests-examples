package auto.dealers.loyalty.logic.test.placementdiscountservice

import auto.dealers.loyalty.logic.PlacementDiscountServiceLive.findPolicy
import auto.dealers.loyalty.logic.PlacementDiscountService._
import ru.auto.cabinet.api_model.{ClientProperties, DetailedClient}
import ru.auto.loyalty.placement_discount_policies.PlacementDiscountPolicies
import ru.auto.loyalty.placement_discount_policies.PlacementDiscountPolicies._
import zio.test.Assertion.{equalTo, isLeft, isRight}
import zio.test.environment.TestEnvironment
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object FindPolicySpec extends DefaultRunnableSpec {

  private val regionA = PlacementDiscountPolicies("regions A", regions = List(region(1), region(2)))
  private val regionB = PlacementDiscountPolicies("regions B", regions = List(region(3), region(4)))
  private val cityC = PlacementDiscountPolicies("city C", cities = List(region(5)))
  private val dealerD = PlacementDiscountPolicies("dealer D", clients = List(client(6)))
  private val dealerE = PlacementDiscountPolicies("dealer E", clients = List(client(7)))

  private val examplePolicies = List(regionA, regionB, cityC, dealerD, dealerE)

  override def spec: ZSpec[TestEnvironment, Any] = suite("findPolicy")(
    test("Find region policy") {
      assertPolicyForClient(regionB, detailedClient(id = 12, region = 4))
    },
    test("Find city policy") {
      assertPolicyForClient(cityC, detailedClient(id = 12, city = 5))
    },
    test("Find dealer policy") {
      assertPolicyForClient(dealerD, detailedClient(id = 6))
    },
    test("Prefer city over region") {
      assertPolicyForClient(cityC, detailedClient(id = 12, city = 5, region = 4))
    },
    test("Prefer client over city and region") {
      assertPolicyForClient(dealerE, detailedClient(id = 7, city = 5, region = 2))
    },
    test("Error NoPolicy") {
      val client = detailedClient(id = 12, region = 16, city = 18)
      val policy = findPolicy(client, examplePolicies)
      assert(policy)(isLeft(equalTo(NoPolicy(client))))
    },
    test("Error DuplicatePolicy") {
      val client = detailedClient(id = 12, region = 4)
      val policiesWithDuplicate = List(
        PlacementDiscountPolicies("A", regions = List(region(4, "4a"), region(5))),
        PlacementDiscountPolicies("B", regions = List(region(4, "4b"), region(6)))
      )
      val policy = findPolicy(client, policiesWithDuplicate)
      assert(policy)(isLeft(equalTo(DuplicatePolicy("Duplicate policy for regionId = 4"))))
    }
  )

  private def assertPolicyForClient(expectedPolicy: PlacementDiscountPolicies, client: DetailedClient) =
    assert(findPolicy(client, examplePolicies))(isRight(equalTo(expectedPolicy)))

  private def detailedClient(id: Long, region: Long = 0, city: Long = 0) =
    DetailedClient(id = id, properties = Some(ClientProperties(regionId = region, cityId = city)))

  private def region(id: Long, name: String = "") = RegionId(id, name)

  private def client(id: Long, name: String = "") = ClientId(id, name)
}
