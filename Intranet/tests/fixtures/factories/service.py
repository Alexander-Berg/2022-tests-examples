import factory
import pytest

from watcher.db import Service
from .base import SERVICE_SEQUENCE


@pytest.fixture(scope='function')
def service_factory(meta_base):
    class ServiceFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Service

        id = factory.Sequence(lambda n: n + SERVICE_SEQUENCE)
        slug = factory.Sequence(lambda n: f"service_slug_{n}")
        name = factory.Sequence(lambda n: f"Service name {n}")
        name_en = factory.Sequence(lambda n: f"Service name_en {n}")

    return ServiceFactory
