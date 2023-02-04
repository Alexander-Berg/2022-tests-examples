import factory
import pytest
import datetime

from watcher.db import Event
from watcher.logic.timezone import now
from watcher import enums
from .base import EVENT_SEQUENCE

@pytest.fixture(scope='function')
def event_factory(meta_base, staff_factory):
    class EventFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Event

        id = factory.Sequence(lambda n: n + EVENT_SEQUENCE)
        obj_id = factory.Sequence(lambda n: n)
        source = enums.EventSource.logbroker
        created_at = now() - datetime.timedelta(minutes=20)

    return EventFactory
