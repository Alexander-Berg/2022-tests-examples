import factory
import pytest
import datetime

from watcher.db import Interval, Revision
from watcher.logic.timezone import now

from .base import INTERVAL_SEQUENCE, REVISION_SEQUENCE


@pytest.fixture(scope='function')
def revision_factory(meta_base, schedule_factory):
    class RevisionFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Revision

        id = factory.Sequence(lambda n: n + REVISION_SEQUENCE)
        schedule = factory.SubFactory(schedule_factory)
        apply_datetime = now()

    return RevisionFactory


@pytest.fixture(scope='function')
def interval_factory(meta_base, schedule_factory, revision_factory):
    class IntervalFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Interval

        id = factory.Sequence(lambda n: n + INTERVAL_SEQUENCE)
        schedule = factory.SubFactory(schedule_factory)
        revision = factory.LazyAttribute(lambda a: revision_factory(schedule=a.schedule))
        order = factory.Sequence(lambda n: n+1)
        duration = datetime.timedelta(days=7)

    return IntervalFactory
