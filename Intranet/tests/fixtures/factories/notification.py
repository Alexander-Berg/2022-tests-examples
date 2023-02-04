import factory
import pytest
import datetime

from watcher import enums
from watcher.db import Notification
from watcher.logic.timezone import now
from .base import NOTIFICATION_SEQUENCE


@pytest.fixture(scope='function')
def notification_factory(meta_base, shift_factory, staff_factory):
    class NotificationFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Notification

        id = factory.Sequence(lambda n: n + NOTIFICATION_SEQUENCE)
        staff = factory.SubFactory(staff_factory)
        shift = factory.SubFactory(shift_factory)

        valid_to = now() + datetime.timedelta(minutes=20)
        send_at = now() - datetime.timedelta(minutes=1)

        type = enums.NotificationType.start_shift

    return NotificationFactory
