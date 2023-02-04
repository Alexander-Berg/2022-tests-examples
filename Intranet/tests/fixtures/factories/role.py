import factory
import pytest
import datetime

from watcher.db import Role
from .base import ROLE_SEQUENCE


@pytest.fixture(scope='function')
def role_factory(meta_base, service_factory, scope_factory):
    class RoleFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Role

        id = factory.Sequence(lambda n: n + ROLE_SEQUENCE)
        name = factory.Sequence(lambda n: f"Role name {n}")
        name_en = factory.Sequence(lambda n: f"Role name_en {n}")
        code = factory.Sequence(lambda n: f"Role code {n}")
        created_at = datetime.datetime.now()
        modified_at = datetime.datetime.now()

        scope = factory.SubFactory(scope_factory)

    return RoleFactory
