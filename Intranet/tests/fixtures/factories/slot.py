import factory
import pytest

from watcher.db import Slot
from .base import SLOT_SEQUENCE


@pytest.fixture(scope='function')
def slot_factory(meta_base, interval_factory, composition_factory, role_factory):
    class SlotFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Slot

        id = factory.Sequence(lambda n: n + SLOT_SEQUENCE)
        interval = factory.SubFactory(interval_factory)
        composition = factory.LazyAttribute(lambda a: composition_factory(service=a.interval.schedule.service))
        role_on_duty = factory.SubFactory(role_factory)
        points_per_hour = 1

    return SlotFactory
