import factory
import pytest

from watcher.db import (
    TaskMetric,
    Unistat,
)
from .base import (
    TASK_METRICS_SEQUENCE,
    UNISTAT_SEQUENCE,
)


@pytest.fixture(scope='function')
def unistat_factory(meta_base):
    class UnistatFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Unistat

        id = factory.Sequence(lambda n: n + UNISTAT_SEQUENCE)

    return UnistatFactory


@pytest.fixture(scope='function')
def task_metrics_factory(meta_base):
    class TaskMetricsFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = TaskMetric

        id = factory.Sequence(lambda n: n + TASK_METRICS_SEQUENCE)

    return TaskMetricsFactory
