import factory
import pytest

from watcher.db import Rating
from .base import RATING_SEQUENCE


@pytest.fixture(scope='function')
def rating_factory(meta_base, staff_factory, schedule_factory):
    class RatingFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Rating

        id = factory.Sequence(lambda n: n + RATING_SEQUENCE)
        staff = factory.SubFactory(staff_factory)
        schedule = factory.SubFactory(schedule_factory)

    return RatingFactory
