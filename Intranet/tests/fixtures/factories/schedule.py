import factory
import pytest
import datetime

from watcher.db import (
    Schedule, SchedulesGroup,
    ScheduleGroupResponsible,
    ScheduleResponsible
)
from .base import (
    SCHEDULE_SEQUENCE,
    SCHEDULE_GROUP_SEQUENCE,
)


@pytest.fixture(scope='function')
def schedule_factory(meta_base, staff_factory, service_factory, schedules_group_factory):
    class ScheduleFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Schedule

        id = factory.Sequence(lambda n: n + SCHEDULE_SEQUENCE)
        slug = factory.Sequence(lambda n: f"schedule_slug_{n}")
        name = factory.Sequence(lambda n: f"Schedule name {n}")
        schedules_group = factory.SubFactory(schedules_group_factory)
        author = factory.SubFactory(staff_factory)
        service = factory.SubFactory(service_factory)

    return ScheduleFactory


@pytest.fixture(scope='function')
def schedules_group_factory(meta_base):
    class SchedulesGroupFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = SchedulesGroup

        id = factory.Sequence(lambda n: n + SCHEDULE_GROUP_SEQUENCE)
        slug = factory.Sequence(lambda n: f"schedule_group_slug_{n}")
        name = factory.Sequence(lambda n: f"Schedule group name {n}")

    return SchedulesGroupFactory


@pytest.fixture(scope='function')
def group_responsible_factory(meta_base):
    class ScheduleGroupResponsibleFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = ScheduleGroupResponsible

    return ScheduleGroupResponsibleFactory


@pytest.fixture(scope='function')
def schedule_responsible_factory(meta_base):
    class ScheduleResponsibleFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = ScheduleResponsible

    return ScheduleResponsibleFactory
