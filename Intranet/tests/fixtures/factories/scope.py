import factory
import pytest

from watcher.db import Scope
from .base import SCOPE_SEQUENCE


@pytest.fixture(scope='function')
def scope_factory(meta_base):
    class ScopeFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Scope

        id = factory.Sequence(lambda n: n + SCOPE_SEQUENCE)
        slug = factory.Sequence(lambda n: f"scope_slug_{n}")
        name = factory.Sequence(lambda n: f"Scope name {n}")
        name_en = factory.Sequence(lambda n: f"Scope name_en {n}")
        protected = False

    return ScopeFactory
