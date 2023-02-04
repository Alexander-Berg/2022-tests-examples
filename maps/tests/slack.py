from dataclasses import dataclass
import json
import logging
from hashlib import sha256
import hmac
import time
from urllib.parse import urlencode

from mock import patch, DEFAULT
import pytest

from pydantic import BaseModel, HttpUrl, ValidationError

from maps.infra.monitoring.st_monitor.lib.markup import SlackMrkdwnMarkup, CompositeChunk, Link
from maps.infra.monitoring.st_monitor.lib.messenger import MessengerAction
from maps.infra.monitoring.st_monitor.lib.slack import parse_body
from maps.infra.monitoring.st_monitor.lib.slack_types \
    import SlackResponse, Button, ButtonText, encode_action_id, decode_action_id

logger = logging.getLogger(__name__)


def test_format_message(test_app):
    issue_id = 42
    expected_blocks = [
        {
            'type': 'section',
            'text': {
                'type': 'mrkdwn',
                'text': 'Sample text with <https://ya.ru|an URL>'
            }
        },
        {
            'type': 'actions',
            'elements': [
                {
                    'text':
                    {
                        'type': 'plain_text',
                        'text': 'DT 1h',
                        'emoji': True,
                    },
                    'action_id': '1:42',
                    'type': 'button'
                },
                {
                    'text': {
                        'type': 'plain_text',
                        'text': 'Reopen',
                        'emoji': True
                    },
                    'action_id': 'R:42',
                    'type': 'button'
                }
            ]
        }
    ]
    bot = test_app.slack_bot
    chunk = CompositeChunk.from_iterable(items=['Sample text with', Link(url="https://ya.ru", text="an URL")],
                                         separator=' ')
    blocks = bot._construct_slack_blocks(text=chunk.format_message(SlackMrkdwnMarkup),
                                         issue_id=issue_id,
                                         actions=[MessengerAction.DOWNTIME_1H, MessengerAction.REOPEN])
    actual_blocks = [block.dict() for block in blocks]
    assert actual_blocks == expected_blocks


@pytest.mark.parametrize('action', MessengerAction.__members__.values())
def test_command_encode(action):
    issue_id = 42

    command = encode_action_id(action, issue_id)
    decoded_action, decoded_issue = decode_action_id(command)

    assert decoded_issue == issue_id
    assert decoded_action == action


def test_bad_command_not_decoded():
    with pytest.raises(ValidationError, match='.*Invalid action_id format.*'):
        Button(text=ButtonText(text='some text'), action_id='Bad action id')


def test_wrong_action_not_decoded():
    with pytest.raises(ValidationError, match='Unknown command .*'):
        Button(text=ButtonText(text='some text'), action_id='W:666')


CORRECT_JSON = {
    "type": "block_actions",
    "team": {
        "id": "T9TK3CUKW",
        "domain": "example"
    },
    "user": {
        "id": "UA8RXUSPL",
        "username": "john-reese",
        "team_id": "T9TK3CUKW"
    },
    "api_app_id": "AABA1ABCD",
    "token": "9s8d9as89d8as9d8as989",
    "container": {
        "type": "message_attachment",
        "message_ts": "1548261231.000200",
        "attachment_id": 1,
        "channel_id": "CBR2V3XEX",
        "is_ephemeral": False,
        "is_app_unfurl": False
    },
    "trigger_id": "12321423423.333649436676.d8c1bb837935619ccad0f624c448ffb3",
    "channel": {
        "id": "CBR2V3XEX",
        "name": "review-updates"
    },
    "message": {
        "bot_id": "BAH5CA16Z",
        "type": "message",
        "text": "This content can't be displayed.",
        "user": "UAJ2RU415",
        "ts": "1548261231.000200",
    },
    "other": [
        "fields"
    ],
    "response_url": "https://hooks.slack.com/actions/AABA1ABCD/1232321423432/D09sSasdasdAS9091209",
    "actions": [
        {
            "action_id": "D:777",
            "block_id": "=qXel",
            "text": {
                "type": "plain_text",
                "text": "View",
                "emoji": True
            },
            "value": "click_me_123",
            "type": "button",
            "action_ts": "1548426417.840180"
        }
    ]
}


from flask.testing import FlaskClient


class OkClient(FlaskClient):
    def open(self, *args, **kwargs):
        kwargs.setdefault('content_type', 'application/json')
        return super().open(*args, **kwargs)


class UrlModel(BaseModel):
    url: HttpUrl


def test_ok(test_app):
    test_app.test_client_class = OkClient
    bot = test_app.slack_bot
    with patch.object(bot, 'respond'), \
            patch.multiple(test_app.startrek_client, comment_to_downtime=DEFAULT, reopen_issue=DEFAULT):
        with test_app.test_client() as client:
            json_string = json.dumps(CORRECT_JSON)
            timestamp = int(time.time())
            signature_basestring = ':'.join(('v0', str(timestamp), json_string))
            signature_digest = hmac.new(
                key=str(bot.signing_secret).encode('utf-8'),
                msg=str(signature_basestring).encode('utf-8'),
                digestmod=sha256
            ).hexdigest()

            response = client.post('/slack_hook', data=json_string, headers={
                'X-Slack-Request-Timestamp': str(timestamp),
                'X-Slack-Signature': f'v0={signature_digest}',
            })
            assert response.status_code == 200
            bot.respond.assert_called_with(UrlModel(url=CORRECT_JSON['response_url']).url,
                                           SlackResponse(text='Downtime for 24h will be set', response_type='ephemeral'))


@dataclass
class ParseBodyCase:
    name: str
    content_type: str
    body: str
    expected: str

    def __str__(self):
        return f'test_{self.name}'


SIMPLE_JSON_FOR_TESTING_PARSE_BODY = {"one": "true", "two": "2", "three": "payload"}
COMPLEX_JSON_FOR_TESTING_PARSE_BODY = {
    "one": True,
    "two": 2,
    "three": "payload",
    "payload": {"nested_payload": "LOL"}
}
PARSE_BODY_CASES = [
    ParseBodyCase(
        name='application_json',
        content_type='application/json',
        body=json.dumps(COMPLEX_JSON_FOR_TESTING_PARSE_BODY),
        expected=COMPLEX_JSON_FOR_TESTING_PARSE_BODY,
    ),
    ParseBodyCase(
        name='application_x_www_form_urlencoded_with_payload_param',
        content_type='application/x-www-form-urlencoded',
        body=urlencode({'payload': json.dumps(COMPLEX_JSON_FOR_TESTING_PARSE_BODY)}),
        expected=COMPLEX_JSON_FOR_TESTING_PARSE_BODY,
    ),
    ParseBodyCase(
        name='application_x_www_form_urlencoded_with_payload_body',
        content_type='application/x-www-form-urlencoded',
        body=urlencode(SIMPLE_JSON_FOR_TESTING_PARSE_BODY),
        expected=SIMPLE_JSON_FOR_TESTING_PARSE_BODY,
    ),
    ParseBodyCase(
        name='application_x_www_form_urlencoded_but_it_really_is_a_json',
        content_type='application/x-www-form-urlencoded',
        body=json.dumps(COMPLEX_JSON_FOR_TESTING_PARSE_BODY),
        expected=COMPLEX_JSON_FOR_TESTING_PARSE_BODY,
    ),
    ParseBodyCase(
        name='empty_body',
        content_type='literally/any-content-type',
        body='',
        expected={},
    ),
]


@pytest.mark.parametrize('case', PARSE_BODY_CASES, ids=str)
def test_parse_body(case):
    response = parse_body(body=case.body, content_type=case.content_type)
    assert response == case.expected


def _mock_startrek_unable_to_act(*args, **kwargs) -> str:
    return False


def test_startrek_failed(test_app):
    test_app.test_client_class = OkClient
    bot = test_app.slack_bot
    with patch.object(bot, 'respond'), \
            patch.multiple(test_app.startrek_client,
                           comment_to_downtime=_mock_startrek_unable_to_act,
                           reopen_issue=_mock_startrek_unable_to_act):
        with test_app.test_client() as client:
            json_string = json.dumps(CORRECT_JSON)
            timestamp = int(time.time())
            signature_basestring = ':'.join(('v0', str(timestamp), json_string))
            signature_digest = hmac.new(
                key=str(bot.signing_secret).encode('utf-8'),
                msg=str(signature_basestring).encode('utf-8'),
                digestmod=sha256
            ).hexdigest()

            response = client.post('/slack_hook', data=json_string, headers={
                'X-Slack-Request-Timestamp': str(timestamp),
                'X-Slack-Signature': f'v0={signature_digest}',
            })
            assert response.status_code == 200
            bot.respond.assert_called_with(UrlModel(url=CORRECT_JSON['response_url']).url,
                                           SlackResponse(text='Unable to set downtime for closed issue', response_type='ephemeral'))


def test_invalid_webhook(test_app):
    with test_app.test_client() as client:
        response = client.post('/slack_hook/wrong_hash')
        assert response.status_code == 404


def test_no_body(test_app):
    bot = test_app.slack_bot
    with patch.object(bot, 'respond'), \
            patch.object(bot, '_validate_signature', return_value=True), \
            test_app.test_client() as client:
        response = client.post('/slack_hook')
        assert response.status_code == 200
        bot.respond.assert_not_called()


def test_wrong_timestamp(test_app):
    test_app.test_client_class = OkClient
    bot = test_app.slack_bot
    with patch.object(bot, 'respond'), \
            patch.multiple(test_app.startrek_client, comment_to_downtime=DEFAULT, reopen_issue=DEFAULT):
        with test_app.test_client() as client:
            json_string = json.dumps(CORRECT_JSON)
            timestamp = int(time.time() - 10 * 60)
            signature_digest = hmac.new(
                key=str(bot.signing_secret).encode('utf-8'),
                msg=b'wrong message',
                digestmod=sha256
            ).hexdigest()

            response = client.post('/slack_hook', data=json_string, headers={
                'X-Slack-Request-Timestamp': str(timestamp),
                'X-Slack-Signature': f'v0={signature_digest}',
            })
            assert response.status_code == 200
            bot.respond.assert_not_called()


def test_wrong_signature(test_app):
    test_app.test_client_class = OkClient
    bot = test_app.slack_bot
    with patch.object(bot, 'respond'), \
            patch.multiple(test_app.startrek_client, comment_to_downtime=DEFAULT, reopen_issue=DEFAULT):
        with test_app.test_client() as client:
            json_string = json.dumps(CORRECT_JSON)
            timestamp = int(time.time())
            signature_digest = hmac.new(
                key=str(bot.signing_secret).encode('utf-8'),
                msg=b'wrong message',
                digestmod=sha256
            ).hexdigest()

            response = client.post('/slack_hook', data=json_string, headers={
                'X-Slack-Request-Timestamp': str(timestamp),
                'X-Slack-Signature': f'v0={signature_digest}',
            })
            assert response.status_code == 200
            bot.respond.assert_not_called()


def test_invalid_signature(test_app):
    test_app.test_client_class = OkClient
    bot = test_app.slack_bot
    with patch.object(bot, 'respond'), \
            patch.multiple(test_app.startrek_client, comment_to_downtime=DEFAULT, reopen_issue=DEFAULT):
        with test_app.test_client() as client:
            json_string = json.dumps(CORRECT_JSON)
            timestamp = int(time.time())
            signature_digest = 'invalid'

            response = client.post('/slack_hook', data=json_string, headers={
                'X-Slack-Request-Timestamp': str(timestamp),
                'X-Slack-Signature': f'v0={signature_digest}',
            })
            assert response.status_code == 200
            bot.respond.assert_not_called()


def test_missing_essential_field(test_app):
    bot = test_app.slack_bot
    with patch.object(bot, 'respond'), \
            patch.object(bot, '_validate_signature', return_value=True), \
            patch.dict(CORRECT_JSON, {'response_url': 'not an url'}) as incorrect_json, \
            test_app.test_client() as client:
        json_string = json.dumps(incorrect_json)
        response = client.post('/slack_hook', data=json_string)
        assert response.status_code == 200
        bot.respond.assert_not_called()


def test_wrong_action_id(test_app):
    bot = test_app.slack_bot
    with patch.object(bot, 'respond'), \
            patch.object(bot, '_validate_signature', return_value=True), \
            patch.dict(CORRECT_JSON['actions'][0], {'action_id': 'WRONG'}), \
            test_app.test_client() as client:
        json_string = json.dumps(CORRECT_JSON)
        response = client.post('/slack_hook', data=json_string)
        assert response.status_code == 200
        bot.respond.assert_not_called()
