import factory
import pytest

from watcher.db import Gap
from watcher.logic.timezone import now

from .base import GAP_SEQUENCE

@pytest.fixture(scope='function')
def gap_factory(meta_base, staff_factory):
    class GapFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Gap

        id = factory.Sequence(lambda n: n + GAP_SEQUENCE)
        staff = factory.SubFactory(staff_factory)
        created_at = now()
        work_in_absence = False
        start = now()
        end = now()

    return GapFactory
