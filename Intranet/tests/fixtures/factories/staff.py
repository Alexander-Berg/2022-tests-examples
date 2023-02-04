import factory
import pytest

from watcher.db import Staff
from .base import STAFF_SEQUENCE, STAFF_ID_SEQUENCE


@pytest.fixture(scope='function')
def staff_factory(meta_base):
    class StaffFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Staff

        id = factory.Sequence(lambda n: n + STAFF_SEQUENCE)
        staff_id = factory.Sequence(lambda n: n + STAFF_ID_SEQUENCE)
        uid = factory.Sequence(lambda n: n + 200)
        user_id = factory.Sequence(lambda n: n + 500)
        login = factory.Sequence(lambda n: f"login {n}")
        first_name = factory.Sequence(lambda n: f"first_name {n}")
        first_name_en = factory.Sequence(lambda n: f"first_name_en {n}")
        last_name = factory.Sequence(lambda n: f"last_name {n}")
        last_name_en = factory.Sequence(lambda n: f"last_name_en {n}")
        is_robot = False

    return StaffFactory
