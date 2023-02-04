import asyncio
from datetime import timedelta

import pytest

from maps.infra.sedem.machine.lib.lock import MongoLock, AlreadyLockedError
from maps.infra.sedem.machine.tests.integration_tests.fixtures.machine_fixture import MachineFixture
from maps.pylibs.fixtures.matchers import Match


class TestMongoLock:

    def test_lock_acquire(self, fixture_factory):
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        machine_fixture.run_until_complete(self.async_test_lock_acquire(machine_fixture))

    def test_lock_reacquire(self, fixture_factory):
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        machine_fixture.run_until_complete(self.async_test_lock_reacquire(machine_fixture))

    def test_lock_already_acquired(self, fixture_factory):
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        machine_fixture.run_until_complete(self.async_test_lock_already_acquired(machine_fixture))

    async def async_test_lock_acquire(self, machine_fixture):
        mongo = machine_fixture.mongo()
        db = mongo.db_instance()

        async with MongoLock(db, lock_name='lock0'):
            [lock] = await mongo.async_get_collection_documents(name='lock')

        assert lock == Match.HasItems({'lock_name': 'lock0'})

        cleared_locks = await mongo.async_get_collection_documents(name='lock')
        assert cleared_locks == []

    async def async_test_lock_reacquire(self, machine_fixture):
        mongo = machine_fixture.mongo()
        db = mongo.db_instance()

        async with MongoLock(db, lock_name='lock0', ttl=timedelta(milliseconds=10)):
            [lock0] = await mongo.async_get_collection_documents(name='lock')
            await asyncio.sleep(0.1)
            async with MongoLock(db, lock_name='lock0'):
                [lock0_reacquired] = await mongo.async_get_collection_documents(name='lock')

        cleared_locks = await mongo.async_get_collection_documents(name='lock')
        assert cleared_locks == []

        assert lock0 == Match.HasItems({'lock_name': 'lock0'})
        assert lock0_reacquired == Match.HasItems({'lock_name': 'lock0'})
        assert lock0['expires_at'] < lock0_reacquired['locked_at']

    async def async_test_lock_already_acquired(self, machine_fixture):
        mongo = machine_fixture.mongo()
        db = mongo.db_instance()

        async with MongoLock(db, lock_name='lock0'):
            with pytest.raises(AlreadyLockedError):
                async with MongoLock(db, lock_name='lock0'):
                    pass
