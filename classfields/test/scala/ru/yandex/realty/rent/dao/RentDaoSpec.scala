package ru.yandex.realty.rent.dao

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.UserFlat
import ru.yandex.realty.rent.model.enums.Role
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class RentDaoSpec extends WordSpecLike with RentSpecBase with RentModelsGen with CleanSchemaBeforeEach {

  implicit val trace: Traced = Traced.empty

  "Rent DAOs" should {

    "create and find users" in {
      val users = userGen(recursive = false).next(10).toList

      Future.sequence(users.map(userDao.create)).futureValue

      users.foreach { user =>
        val u = userDao.findByUidOpt(user.uid).futureValue

        assert(u.isDefined)
        assert(user == u.get)
      }
    }

    "create and find flats" in {
      val flats = flatGen(recursive = false).next(10).toList

      Future.sequence(flats.map(flatDao.create)).futureValue

      flats.foreach { flat =>
        val f = flatDao.findByIdOpt(flat.flatId).futureValue

        assert(f.isDefined)
        assert(flat == f.get)
      }
    }

    "create flats and users with attached existing flats" in {
      // prepare user with flats
      val user = userGen().next

      // insert flats
      val allFlats = user.assignedFlats.flatMap(_._2)
      Future.sequence(allFlats.map(flatDao.create)).futureValue

      // insert user and user-flat relations
      userDao.create(user).futureValue
      val userFlats = user.assignedFlats
        .flatMap { case (role, flats) => flats.map(f => UserFlat(user.uid, f.flatId, role)) }
      userFlatDao.create(userFlats).futureValue

      // get and check user and relations
      val insertedUserOpt = userDao.findByUidOpt(user.uid).futureValue
      assert(insertedUserOpt.isDefined)

      val insertedUser = insertedUserOpt.get
      insertedUser.assignedFlats
        .mapValues(_.sortBy(_.flatId))
        .toList should contain theSameElementsAs user.assignedFlats.mapValues(_.sortBy(_.flatId)).toList
    }

    "create users and flats with attached existing users" in {
      // prepare flat with users
      val flat = flatGen().next

      // insert users
      val allUsers = flat.assignedUsers.flatMap(_._2)
      Future.sequence(allUsers.map(userDao.create)).futureValue

      // insert flat and user-flat relations
      flatDao.create(flat).futureValue
      val userFlats = flat.assignedUsers
        .flatMap { case (role, users) => users.map(u => UserFlat(u.uid, flat.flatId, role)) }
      userFlatDao.create(userFlats).futureValue

      // get and check flat and relations
      val insertedFlatOpt = flatDao.findByIdOpt(flat.flatId).futureValue
      assert(insertedFlatOpt.isDefined)

      val insertedFlat = insertedFlatOpt.get
      insertedFlat.assignedUsers.mapValues(_.sortBy(_.uid)).toList should contain theSameElementsAs flat.assignedUsers
        .mapValues(_.sortBy(_.uid))
        .toList
    }

    "independently create users and flats, create and delete relations between them" in {
      // users
      val alice = userGen(recursive = false).next
      val bob = userGen(recursive = false).next
      val charlie = userGen(recursive = false).next

      Future.sequence(List(alice, bob, charlie).map(userDao.create)).futureValue

      // flats
      val apartment = flatGen(recursive = false).next
      val house = flatGen(recursive = false).next
      val flat = flatGen(recursive = false).next

      Future.sequence(List(apartment, house, flat).map(flatDao.create)).futureValue

      // add user-flat relations.
      // Alice is an owner of apartment and house and tenant in flat
      // Bob is confidant in Alice's apartment and house and additional tenant in flat
      // Charlie is owner of flat and tenant in house
      // Bob is a husband of Alice, Charlie is a secret lover of Alice, But Bob is suspecting that Alice cheating on.
      // Ta-tada-taaa
      userFlatDao.create(UserFlat(alice.uid, apartment.flatId, Role.Owner)).futureValue
      userFlatDao.create(UserFlat(alice.uid, house.flatId, Role.Owner)).futureValue
      userFlatDao.create(UserFlat(alice.uid, flat.flatId, Role.Tenant)).futureValue

      userFlatDao.create(UserFlat(bob.uid, apartment.flatId, Role.Confidant)).futureValue
      userFlatDao.create(UserFlat(bob.uid, house.flatId, Role.Confidant)).futureValue
      userFlatDao.create(UserFlat(bob.uid, flat.flatId, Role.AdditionalTenant)).futureValue

      userFlatDao.create(UserFlat(charlie.uid, flat.flatId, Role.Owner)).futureValue
      userFlatDao.create(UserFlat(charlie.uid, house.flatId, Role.Tenant)).futureValue

      // get modified users
      val modifiedAliceO = userDao.findByUidOpt(alice.uid).futureValue
      assert(modifiedAliceO.isDefined)
      val modifiedAlice = modifiedAliceO.get

      val modifiedBobO = userDao.findByUidOpt(bob.uid).futureValue
      assert(modifiedBobO.isDefined)
      val modifiedBob = modifiedBobO.get

      val modifiedCharlieO = userDao.findByUidOpt(charlie.uid).futureValue
      assert(modifiedCharlieO.isDefined)
      val modifiedCharlie = modifiedCharlieO.get

      // check modified users
      assert(modifiedAlice.flatsAsOwner.size == 2)
      assert(modifiedAlice.flatsAsOwner.contains(apartment))
      assert(modifiedAlice.flatsAsOwner.contains(house))
      assert(modifiedAlice.flatsAsConfidant.isEmpty)
      assert(modifiedAlice.flatsAsTenant.size == 1)
      assert(modifiedAlice.flatsAsTenant.contains(flat))
      assert(modifiedAlice.flatsAsAdditionalTenant.isEmpty)

      assert(modifiedBob.flatsAsOwner.isEmpty)
      assert(modifiedBob.flatsAsConfidant.size == 2)
      assert(modifiedBob.flatsAsConfidant.contains(apartment))
      assert(modifiedBob.flatsAsConfidant.contains(house))
      assert(modifiedBob.flatsAsTenant.isEmpty)
      assert(modifiedBob.flatsAsAdditionalTenant.size == 1)
      assert(modifiedBob.flatsAsAdditionalTenant.contains(flat))

      assert(modifiedCharlie.flatsAsOwner.size == 1)
      assert(modifiedCharlie.flatsAsOwner.contains(flat))
      assert(modifiedCharlie.flatsAsConfidant.isEmpty)
      assert(modifiedCharlie.flatsAsTenant.size == 1)
      assert(modifiedCharlie.flatsAsTenant.contains(house))
      assert(modifiedCharlie.flatsAsAdditionalTenant.isEmpty)

      // get modified flats
      val modifiedApartmentO = flatDao.findByIdOpt(apartment.flatId).futureValue
      assert(modifiedApartmentO.isDefined)
      val modifiedApartment = modifiedApartmentO.get

      val modifiedHouseO = flatDao.findByIdOpt(house.flatId).futureValue
      assert(modifiedHouseO.isDefined)
      val modifiedHouse = modifiedHouseO.get

      val modifiedFlatO = flatDao.findByIdOpt(flat.flatId).futureValue
      assert(modifiedFlatO.isDefined)
      val modifiedFlat = modifiedFlatO.get

      // check modified flats
      assert(modifiedApartment.owners.size == 1)
      assert(modifiedApartment.owners.contains(alice))
      assert(modifiedApartment.confidants.size == 1)
      assert(modifiedApartment.confidants.contains(bob))
      assert(modifiedApartment.tenants.isEmpty)
      assert(modifiedApartment.additionalTenants.isEmpty)

      assert(modifiedHouse.owners.size == 1)
      assert(modifiedHouse.owners.contains(alice))
      assert(modifiedHouse.confidants.size == 1)
      assert(modifiedHouse.confidants.contains(bob))
      assert(modifiedHouse.tenants.size == 1)
      assert(modifiedHouse.tenants.contains(charlie))
      assert(modifiedHouse.additionalTenants.isEmpty)

      assert(modifiedFlat.owners.size == 1)
      assert(modifiedFlat.owners.contains(charlie))
      assert(modifiedFlat.confidants.isEmpty)
      assert(modifiedFlat.tenants.size == 1)
      assert(modifiedFlat.tenants.contains(alice))
      assert(modifiedFlat.additionalTenants.size == 1)
      assert(modifiedFlat.additionalTenants.contains(bob))

      // delete some relations
      userFlatDao
        .delete(
          Iterable(
            UserFlat(bob.uid, flat.flatId, Role.AdditionalTenant)
          )
        )
        .futureValue

      // check modified user
      val modifiedBob2O = userDao.findByUidOpt(bob.uid).futureValue
      assert(modifiedBob2O.isDefined)
      val modifiedBob2 = modifiedBob2O.get

      assert(modifiedBob2.flatsAsOwner.isEmpty)
      assert(modifiedBob2.flatsAsConfidant.size == 2)
      assert(modifiedBob2.flatsAsConfidant.contains(apartment))
      assert(modifiedBob2.flatsAsConfidant.contains(house))
      assert(modifiedBob2.flatsAsTenant.isEmpty)
      assert(modifiedBob2.flatsAsAdditionalTenant.isEmpty)

      // check modified flat
      val modifiedFlat2O = flatDao.findByIdOpt(flat.flatId).futureValue
      assert(modifiedFlat2O.isDefined)
      val modifiedFlat2 = modifiedFlat2O.get

      assert(modifiedFlat2.owners.size == 1)
      assert(modifiedFlat2.owners.contains(charlie))
      assert(modifiedFlat2.confidants.isEmpty)
      assert(modifiedFlat2.tenants.size == 1)
      assert(modifiedFlat2.tenants.contains(alice))
      assert(modifiedFlat2.additionalTenants.isEmpty)

      // check not modified users, they must the same
      val unmodifiedAliceO = userDao.findByUidOpt(alice.uid).futureValue
      assert(unmodifiedAliceO.isDefined)
      val unmodifiedAlice = unmodifiedAliceO.get

      val unmodifiedCharlieO = userDao.findByUidOpt(charlie.uid).futureValue
      assert(unmodifiedCharlieO.isDefined)
      val unmodifiedCharlie = unmodifiedCharlieO.get

      assert(unmodifiedAlice.flatsAsOwner.size == 2)
      assert(unmodifiedAlice.flatsAsOwner.contains(apartment))
      assert(unmodifiedAlice.flatsAsOwner.contains(house))
      assert(unmodifiedAlice.flatsAsConfidant.isEmpty)
      assert(unmodifiedAlice.flatsAsTenant.size == 1)
      assert(unmodifiedAlice.flatsAsTenant.contains(flat))
      assert(unmodifiedAlice.flatsAsAdditionalTenant.isEmpty)

      assert(unmodifiedCharlie.flatsAsOwner.size == 1)
      assert(unmodifiedCharlie.flatsAsOwner.contains(flat))
      assert(unmodifiedCharlie.flatsAsConfidant.isEmpty)
      assert(unmodifiedCharlie.flatsAsTenant.size == 1)
      assert(unmodifiedCharlie.flatsAsTenant.contains(house))
      assert(unmodifiedCharlie.flatsAsAdditionalTenant.isEmpty)

      // check not modified flats
      val unmodifiedApartmentO = flatDao.findByIdOpt(apartment.flatId).futureValue
      assert(unmodifiedApartmentO.isDefined)
      val unmodifiedApartment = unmodifiedApartmentO.get

      val unmodifiedHouseO = flatDao.findByIdOpt(house.flatId).futureValue
      assert(unmodifiedHouseO.isDefined)
      val unmodifiedHouse = unmodifiedHouseO.get

      assert(unmodifiedApartment.owners.size == 1)
      assert(unmodifiedApartment.owners.contains(alice))
      assert(unmodifiedApartment.confidants.size == 1)
      assert(unmodifiedApartment.confidants.contains(bob))
      assert(unmodifiedApartment.tenants.isEmpty)
      assert(unmodifiedApartment.additionalTenants.isEmpty)

      assert(unmodifiedHouse.owners.size == 1)
      assert(unmodifiedHouse.owners.contains(alice))
      assert(unmodifiedHouse.confidants.size == 1)
      assert(unmodifiedHouse.confidants.contains(bob))
      assert(unmodifiedHouse.tenants.size == 1)
      assert(unmodifiedHouse.tenants.contains(charlie))
      assert(unmodifiedHouse.additionalTenants.isEmpty)
    }
  }
}
