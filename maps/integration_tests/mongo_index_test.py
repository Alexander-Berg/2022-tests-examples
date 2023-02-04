import pymongo
import pytest
from motor.motor_asyncio import AsyncIOMotorCollection

from maps.infra.sedem.machine.tests.integration_tests.fixtures.machine_fixture import MachineFixture
from maps.pylibs.fixtures.matchers import Match


class TestMongoIndex:

    def test_index_version(self, fixture_factory):
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        machine_fixture.run_until_complete(self.async_test_index_version(machine_fixture))

    def test_multiservice(self, fixture_factory):
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        machine_fixture.run_until_complete(self.async_test_multiservice(machine_fixture))

    async def async_test_index_version(self, machine_fixture):
        mongo = machine_fixture.mongo()
        db = mongo.db_instance()

        release_collection: AsyncIOMotorCollection = db['release']

        await release_collection.insert_one(
            {'service_name': 'maps-core-teacup', 'major': 1, 'minor': 1}
        )
        with pytest.raises(pymongo.errors.DuplicateKeyError):
            await release_collection.insert_one(
                {'service_name': 'maps-core-teacup', 'major': 1, 'minor': 1}
            )

        releases = await mongo.async_get_collection_documents(name='release')
        assert releases == [Match.HasItems({'service_name': 'maps-core-teacup', 'major': 1, 'minor': 1})]

    async def async_test_multiservice(self, machine_fixture):
        mongo = machine_fixture.mongo()
        db = mongo.db_instance()

        release_collection: AsyncIOMotorCollection = db['release']

        await release_collection.insert_one(
            {'service_name': 'maps-core-teacup', 'major': 1, 'minor': 1}
        )
        await release_collection.insert_one(
            {'service_name': 'maps-core-teapot', 'major': 1, 'minor': 1}
        )

        releases = await mongo.async_get_collection_documents(name='release')
        assert releases == Match.Contains(
            Match.HasItems({'service_name': 'maps-core-teapot', 'major': 1, 'minor': 1}),
            Match.HasItems({'service_name': 'maps-core-teacup', 'major': 1, 'minor': 1})
        )
