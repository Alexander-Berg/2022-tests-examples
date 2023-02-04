import logging
import os

import yaml

import yatest
from maps.infra.sedem.cli.lib.monitorings import notify_joiner
from maps.infra.sedem.cli.lib import service


logger = logging.getLogger(__name__)


class TestConfig:
    def srv(self):
        if not hasattr(self, '_srv'):
            os.environ['SEDEM_YA_ROOT'] = yatest.common.test_source_path('../test-data')
            self._srv = service.Service('fake_srv')
        return self._srv

    def _notification_check(self, notification):
        return yaml.dump(notify_joiner.collect_notifications(self.srv(), notification), default_flow_style=None)

    def test_notifications_simple_telegram(self):
        return self._notification_check([
            {
                'type': 'telegram',
                'login': ['a', 'b', 'c']
            }
        ])

    def test_notifications_simple_slack(self):
        return self._notification_check([
            {
                'type': 'slack',
                'chats': [
                {
                    'name': 'a',
                    'link': 'aa',
                },
                {
                    'name': 'b',
                    'link': 'bb'
                },
                ],
            }
        ])

    def test_notifications_profile1(self):
        return self._notification_check([
            {
                'type': 'profile',
                'name': 'infra'
            }
        ])

    def test_notifications_profile2(self):
        return self._notification_check([
            {
                'type': 'profile',
                'name': 'infra'
            },
            {
                'type': 'profile',
                'name': 'geo_monitorings'
            },
            {
                'type': 'profile',
                'name': 'geo_monitorings_working_time'
            }
        ])

    def test_notifications_duty(self):
        return self._notification_check([
            {
                'type': 'service_duty',
                'st_components': ['comp_a', 'comp_b', 'comp_c'],
                'telegram_chats': ['chat1'],
                'slack_chats': [{
                    'name': 'chat-1',
                    'link': 'https://yndx-maps-platform.slack.com/archives/C01ABCDEFG',
                }],
                'escalation_list': ['login1', 'login2'],
                'calendar_id1': 1
            }
        ])

    def test_notifications_duty2(self):
        return self._notification_check([
            {
                'type': 'service_duty',
                'st_components': ['comp_a', 'comp_b', 'comp_c'],
                'telegram_chats': ['chat1'],
                'slack_chats': [
                    {
                        'name': 'chat-1',
                        'link': 'https://yndx-maps-platform.slack.com/archives/C01ABCDEFG',
                    },
                    {
                        'name': 'chat-2',
                        'link': 'https://yndx-maps-platform.slack.com/archives/C012345678',
                    },
                ],
                'escalation_list': ['login1', 'login2'],
                'calendar_id1': 1,
                'calendar_id2': 2
            }
        ])

    def test_notifications_duty3(self):
        return self._notification_check([
            {
                'type': 'profile',
                'name': 'geo_monitorings'
            },
            {
                'type': 'service_duty',
                'st_components': ['comp_a', 'comp_b', 'comp_c'],
                'telegram_chats': ['chat1'],
                'slack_chats': [{
                    'name': 'chat-1',
                    'link': 'https://yndx-maps-platform.slack.com/archives/C01ABCDEFG',
                }],
                'escalation_list': ['login1', 'login2'],
                'calendar_id1': 1,
                'calendar_id2': 2
            }
        ])
