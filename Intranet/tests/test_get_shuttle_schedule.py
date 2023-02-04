# coding: utf-8
from __future__ import unicode_literals

from freezegun import freeze_time
from mock import patch

from utils import handle_utterance


@freeze_time('2017-01-01')
def test_get_shuttle_schedule_avrora_success(uid, tg_app):
    with patch('uhura.external.calendar.request_to_calendar') as m,\
            patch('uhura.external.staff.get_uid_by_login') as m1:
        m.return_value = {'events': [
            {'startTs': '2017-01-01T13:00:00'}, {'startTs': '2017-01-01T14:00:00'}, {'startTs': '2017-01-01T15:00:00'}
        ]}
        m1.return_value = 123456789
        handle_utterance(
            tg_app,
            uid,
            'шаттл аврора',
            '''Ближайшие шаттлы:

От Авроры: 13:00 14:00 15:00
От Красной Розы: 13:00 14:00 15:00'''
        )
