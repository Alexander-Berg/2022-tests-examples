import factory
import pytest
import datetime

from watcher.logic.timezone import now
from watcher.db import Shift
from .base import SHIFT_SEQUENCE


@pytest.fixture(scope='function')
def shift_factory(meta_base, slot_factory, schedule_factory):
    class ShiftFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Shift

        id = factory.Sequence(lambda n: n + SHIFT_SEQUENCE)
        slot = factory.SubFactory(slot_factory)
        schedule = factory.SubFactory(schedule_factory)
        start = now()
        end = now() + datetime.timedelta(hours=1)

    return ShiftFactory
