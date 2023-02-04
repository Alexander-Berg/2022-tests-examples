import factory
import pytest

from watcher.db import AbcMigration
from .base import ABC_MIGRATION_SEQUENCE


@pytest.fixture(scope='function')
def abc_migration_factory(meta_base, service_factory, staff_factory):
    class AbcMigrationFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = AbcMigration

        id = factory.Sequence(lambda n: n + ABC_MIGRATION_SEQUENCE)
        abc_schedule_id = factory.Sequence(lambda n: n + 500)
        service = factory.SubFactory(service_factory)
        author = factory.SubFactory(staff_factory)

    return AbcMigrationFactory
