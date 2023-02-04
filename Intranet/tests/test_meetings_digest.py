# coding: utf-8

from __future__ import unicode_literals

from freezegun import freeze_time
from mock import patch

from uhura.models import Session
from uhura.tests.utils import get_event_example


@freeze_time('2017-01-01T00:01:00.123456')
def test_meetings_digest(uid, tg_app):
    Session.objects.create(uuid=1, app_id='uhura')
    from uhura.tasks.notifications.calendar import meetings_digest
    with patch('uhura.external.calendar.get_meetings') as m, \
            patch('uhura.external.calendar.get_holidays') as m1:
        event, rendered_event = get_event_example()
        m.return_value = [event]
        m1.return_value = []
        meetings_digest(1, 1, 1, 'today')
        assert tg_app.sent_messages == ['Дайджест встреч на сегодня:', rendered_event]
