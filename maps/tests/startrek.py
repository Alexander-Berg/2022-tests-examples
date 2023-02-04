from datetime import datetime

import pytest
from attrdict import AttrDict
from dateutil import tz
from mock import patch, DEFAULT

from maps.infra.monitoring.st_monitor.lib.startrek_client import StartrekEvent
from maps.infra.monitoring.st_monitor.lib.message_type import MessageType
from maps.infra.monitoring.st_monitor.lib.config import JUGGLER_ROBOT
from maps.infra.monitoring.st_monitor.lib.startrek import juggler_action


def test_startrek_event_assembling(test_startrek_client, test_yasm_client):
    st_client = test_startrek_client._client

    juggler_host = 'maps_core_teapot_stable'
    juggler_service = 'base_image_revision'
    event_id = '24'
    issue_key = 'GEOMONITORINGS-42'
    issue_patch = AttrDict({
        'status': {'key': 'closed'},
        'components': [{'display': 'maps-infra'}],
        'summary': '{}:{}'.format(juggler_host, juggler_service),
        'changelog': {
            event_id: AttrDict({
                'type': 'IssueWorkflow',
                'fields': [{'field': AttrDict({'id': 'status'}), 'to': AttrDict({'key': 'closed'})}],
                'updatedBy': {'id': 'mr.reese'},
                'updatedAt': '2019-10-21T10:07:36.778+0000',
                'comments': {
                    'added': [AttrDict({'id': '1', 'display': 'Sample comment...'})]
                }
            })
        },
        'comments': {
            '1': AttrDict({'text': 'Sample comment\nwith more than one line\ndowntime 24h'})
        }
    })

    with patch.object(st_client, 'issues', {issue_key: issue_patch}), \
            patch.object(test_yasm_client, 'alert', return_value=test_yasm_client.alert_url(
                juggler_host, juggler_service
            )):
        event = StartrekEvent(st_client, test_yasm_client, event_id=event_id, issue_key=issue_key)

    issue = event.issue
    assert issue.key == issue_key
    assert issue.key_url == 'https://st.yandex-team.ru/' + issue_key
    assert issue.issue_id == 42
    assert issue.status == 'closed'
    assert issue.components == ['maps-infra']
    assert issue.check == '{}:{}'.format(juggler_host, juggler_service)
    assert issue.check_host == juggler_host
    assert issue.check_service == juggler_service
    assert issue.check_url == (
        'https://juggler.yandex-team.ru/check_details/?'
        'host={}&service={}'.format(juggler_host, juggler_service)
    )
    assert issue.yasm_alert == (
        'https://yasm.yandex-team.ru/alert/{}.{}'.format(juggler_host, juggler_service)
    )

    assert event.event_id == event_id
    assert event.login == 'mr.reese'
    assert event.login_url == 'https://staff.yandex-team.ru/mr.reese'
    assert event.time == datetime(2019, 10, 21, 10, 7, 36, 778000, tzinfo=tz.tzutc())
    assert event.status == 'closed'
    assert event.status_changed
    assert event.added_comment == 'Sample comment\nwith more than one line\ndowntime 24h'
    assert not event.juggler_commented
    assert event.downtime_duration == 24


def test_key_extract_from_dt_description():
    dt_description = 'mr.reese is working in https://st.yandex-team.ru/GEOMONITORINGS-42'
    assert StartrekEvent.Issue.key_from_text(dt_description) == 'GEOMONITORINGS-42'


class StartrekEventCase(object):
    def __init__(self, name,
                 expected_action_message,
                 expected_juggler_action,
                 login=None,
                 downtime_duration=None,
                 remove_downtimes: bool = False,
                 status=None):
        self.name = name
        self.login = login
        self.downtime_duration = downtime_duration
        self.remove_downtimes = remove_downtimes
        self.status = status
        self.expected_action_message = expected_action_message
        self.expected_juggler_action = expected_juggler_action

    def __str__(self):
        return 'test_{}'.format(self.name)

    @property
    def event(self):
        return AttrDict({
            'login': self.login,
            'juggler_commented': self.login == JUGGLER_ROBOT and self.status != 'open',
            'downtime_duration': self.downtime_duration,
            'remove_downtimes': self.remove_downtimes,
            'downtime_description': '{} is working...'.format(self.login),
            'status': self.status,
            'yasm_alert': None,
            'issue': {
                'key': 'GEOMONITORINGS-42',
                'check_host': 'maps_core_teapot_stable',
                'check_service': 'base_image_revision'
            }
        })


STARTREK_EVENTS = [
    StartrekEventCase(
        name='juggler_created_issue',
        login=JUGGLER_ROBOT,
        status='open',
        expected_action_message=MessageType.CREATED,
        expected_juggler_action=None
    ),
    StartrekEventCase(
        name='juggler_commented_issue',
        login=JUGGLER_ROBOT,
        expected_action_message=MessageType.UPDATED,
        expected_juggler_action=None
    ),
    StartrekEventCase(
        name='user_commented_issue',
        login='mr.reese',
        expected_action_message=None,
        expected_juggler_action=None
    ),
    StartrekEventCase(
        name='user_set_downtime',
        login='mr.reese',
        downtime_duration=1,
        expected_action_message=MessageType.DOWNTIMED,
        expected_juggler_action='set_downtime'
    ),
    StartrekEventCase(
        name='user_removed_downtime',
        login='mr.reese',
        remove_downtimes=True,
        expected_action_message=MessageType.REMOVED_DOWNTIME,
        expected_juggler_action='remove_downtimes',
    ),
    StartrekEventCase(
        name='user_reopened_issue',
        login='mr.reese',
        status='open',
        expected_action_message=MessageType.REOPENED,
        expected_juggler_action='remove_downtimes'
    ),
    StartrekEventCase(
        name='user_closed_issue',
        login='mr.reese',
        status='closed',
        expected_action_message=MessageType.FIXED,
        expected_juggler_action='remove_downtimes'
    )
]


@pytest.mark.parametrize('case', STARTREK_EVENTS, ids=str)
def test_startrek_action_by_event(test_app, case):
    juggler = test_app.juggler_client
    message = MessageType.by_startrek_event(case.event)
    assert message is case.expected_action_message
    with patch.multiple(juggler, set_downtime=DEFAULT, remove_downtimes=DEFAULT):
        juggler_action(message, case.event, juggler)
        if case.expected_juggler_action:
            getattr(juggler, case.expected_juggler_action).assert_called_once()
