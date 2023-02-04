import pytest

from dataclasses import dataclass
from typing import Union

from maps.infra.monitoring.st_monitor.lib.markup \
    import CompositeChunk, Link, HtmlMarkup, SlackMrkdwnMarkup, SlackNotificationsMarkup
from maps.infra.monitoring.st_monitor.lib.message_type import MessageType


@dataclass
class MockStartrekIssue:
    key_url: str
    issue_id: str
    check_url: str
    check: str
    yasm_alert: str


@dataclass
class MockStartrekEvent:
    login_url: str
    login: str
    issue: MockStartrekIssue


@dataclass
class MessageFormatCase:
    name: str
    message_type: MessageType
    juggler_action: dict[str, Union[Link, int]]
    expected_chunk: CompositeChunk
    expected_slack_blocks_markup: str
    expected_slack_notification_markup: str
    expected_telegram_markup: str

    def __str__(self):
        return "test_message_format_{}".format(self.name)


MESSAGE_FORMAT_CASES = [
    MessageFormatCase(
        name='simple_string_action',
        message_type=MessageType.CREATED,
        juggler_action={},
        expected_chunk=CompositeChunk.from_iterable(
            items=[
                '💥',
                Link(url='https://ya.ru/key_url', text='13'),
                Link(url='https://ya.ru/login_url', text='reese'),
                'created',
                Link(url='https://ya.ru/check_url', text='check_engine'),
                Link(url='https://ya.ru/yasm_alert', text='(yasm)')
            ], separator=' '
        ),
        expected_slack_blocks_markup='💥 <https://ya.ru/key_url|13> <https://ya.ru/login_url|reese>'
                                     ' created <https://ya.ru/check_url|check_engine>'
                                     ' <https://ya.ru/yasm_alert|(yasm)>',
        expected_slack_notification_markup='💥 13 reese created check_engine (yasm)',
        expected_telegram_markup='💥 <a href="https://ya.ru/key_url">13</a>'
                                 ' <a href="https://ya.ru/login_url">reese</a> created'
                                 ' <a href="https://ya.ru/check_url">check_engine</a>'
                                 ' <a href="https://ya.ru/yasm_alert">(yasm)</a>',
    ),
    MessageFormatCase(
        name='simple_string_action',
        message_type=MessageType.DOWNTIMED,
        juggler_action={'downtime': Link(url='https://ya.ru/downtime', text='downtime'), 'duration': 24},
        expected_chunk=CompositeChunk.from_iterable(
            items=[
                '⏰',
                Link(url='https://ya.ru/key_url', text='13'),
                Link(url='https://ya.ru/login_url', text='reese'),
                CompositeChunk.from_iterable(items=[
                    'set ', Link(url='https://ya.ru/downtime', text='downtime'), ' ', '24', 'h for'
                ], separator=''),
                Link(url='https://ya.ru/check_url', text='check_engine'),
                Link(url='https://ya.ru/yasm_alert', text='(yasm)')
            ], separator=' '
        ),
        expected_slack_blocks_markup='⏰ <https://ya.ru/key_url|13> <https://ya.ru/login_url|reese>'
                                     ' set <https://ya.ru/downtime|downtime> 24h for'
                                     ' <https://ya.ru/check_url|check_engine>'
                                     ' <https://ya.ru/yasm_alert|(yasm)>',
        expected_slack_notification_markup='⏰ 13 reese set downtime 24h for check_engine (yasm)',
        expected_telegram_markup='⏰ <a href="https://ya.ru/key_url">13</a>'
                                 ' <a href="https://ya.ru/login_url">reese</a> set'
                                 ' <a href="https://ya.ru/downtime">downtime</a> 24h for'
                                 ' <a href="https://ya.ru/check_url">check_engine</a>'
                                 ' <a href="https://ya.ru/yasm_alert">(yasm)</a>',
    ),
]


@pytest.mark.parametrize('case', MESSAGE_FORMAT_CASES, ids=str)
def test_message_format(case):
    event = MockStartrekEvent(login_url='https://ya.ru/login_url', login='reese',
                              issue=MockStartrekIssue(key_url='https://ya.ru/key_url', issue_id='13',
                                                      check_url='https://ya.ru/check_url', check='check_engine',
                                                      yasm_alert='https://ya.ru/yasm_alert'))
    actual = case.message_type.format(event=event, juggler_action=case.juggler_action)
    import logging
    logging.warning(f'actual\n{actual}')
    logging.warning(f'case.expected_chunk\n{case.expected_chunk}')
    assert actual == case.expected_chunk
    assert actual.format_message(HtmlMarkup) == case.expected_telegram_markup
    assert actual.format_message(SlackMrkdwnMarkup) == case.expected_slack_blocks_markup
    assert actual.format_message(SlackNotificationsMarkup) == case.expected_slack_notification_markup
