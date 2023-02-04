import datetime

import factory
import pytest

from watcher import enums
from watcher.db import (
    ManualGapSettings,
    ManualGap
)
from watcher.logic.timezone import now

from .base import (
    MANUAL_GAP_SETTINGS_SEQUENCE,
    MANUAL_GAP_SEQUENCE,
)


@pytest.fixture(scope='function')
def manual_gap_settings_factory(meta_base, staff_factory):
    class ManualGapSettingsFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = ManualGapSettings

        id = factory.Sequence(lambda n: n + MANUAL_GAP_SETTINGS_SEQUENCE)
        title = factory.Sequence(lambda n: f"manual gap settings name {n}")
        comment = factory.Sequence(lambda n: f"comment {n}")

        staff = factory.SubFactory(staff_factory)

        start = now()
        end = now() + datetime.timedelta(hours=1)

        recurrence = enums.ManualGapRecurrence.once
        all_services = False

    return ManualGapSettingsFactory


@pytest.fixture(scope='function')
def manual_gap_factory(meta_base, manual_gap_settings_factory, staff_factory):
    class ManualGapFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = ManualGap

        id = factory.Sequence(lambda n: n + MANUAL_GAP_SEQUENCE)
        gap_settings = factory.SubFactory(manual_gap_settings_factory)
        staff = factory.SubFactory(staff_factory)

        start = now()
        end = now() + datetime.timedelta(hours=1)

    return ManualGapFactory
