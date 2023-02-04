from hashlib import sha256

from mock import patch, DEFAULT
import pytest

from maps.infra.monitoring.st_monitor.lib.messenger import MessengerAction


@pytest.mark.parametrize('action', MessengerAction.__members__.values())
def test_command_encode(test_telegram_bot, action):
    issue_id = 42

    command = test_telegram_bot.encode_command(issue_id, action)
    decoded_issue, decoded_action = test_telegram_bot.decode_command(command)

    assert decoded_issue == issue_id
    assert decoded_action == action


def test_bad_command_not_decoded(test_telegram_bot):
    with pytest.raises(ValueError, match='Unable to decode .*'):
        test_telegram_bot.decode_command('not_a_command')


def test_wrong_action_not_decoded(test_telegram_bot):
    with pytest.raises(ValueError, match='Unknown command .*'):
        test_telegram_bot.decode_command('W:666:signature')


def test_wrong_command_sign_not_decoded(test_telegram_bot):
    with pytest.raises(ValueError, match='.*wrong sign.*'):
        test_telegram_bot.decode_command('D:666:bad_sign')


class StaffResponseCase(object):
    def __init__(self, name, staff_response, expected_user):
        self.name = name
        self.staff_response = staff_response
        self.expected_user = expected_user

    def __str__(self):
        return "test_{}".format(self.name)


STAFF_RESPONSES = [
    StaffResponseCase(
        name='unknown_user',
        staff_response=[],
        expected_user=None
    ),
    StaffResponseCase(
        name='dismissed_user',
        staff_response=[{'login': 'mr.reese', 'official': {'is_dismissed': True}}],
        expected_user=None
    ),
    StaffResponseCase(
        name='ambigous_user',
        staff_response=[{'login': 'mr.reese', 'official': {'is_dismissed': False}},
                        {'login': 'reese', 'official': {'is_dismissed': False}}],
        expected_user=None
    ),
    StaffResponseCase(
        name='ok_user',
        staff_response=[{'login': 'mr.reese', 'official': {'is_dismissed': False}}],
        expected_user='mr.reese'
    )
]


@pytest.mark.parametrize('case', STAFF_RESPONSES, ids=str)
def test_unknown_user(test_staff_client, case):
    test_staff_client.user_from_telegram.cache_clear()  # clear ttl cache
    with patch.object(test_staff_client._client, 'persons_by_telegram', return_value=case.staff_response):
        assert test_staff_client.user_from_telegram('Mr.Reese') == case.expected_user


TEST_MESSAGE = {
    'message_id': 1,
    'chat': {'id': 12345, 'type': 'supergroup', 'title': 'geo monitorings'},
    'from': {'id': 2, 'is_bot': False, 'username': 'mr.Reese', 'first_name': 'John'},
    'text': '/start',
    'date': 1571248748
}


def test_start_command(test_app):
    bot = test_app.telegram_bot
    with patch.object(bot, 'start_channel_command'):
        with test_app.test_client() as client:
            response = client.post(
                '/telegram_hook/' + sha256(bot.token.encode('utf-8')).hexdigest(),
                json={
                    'update_id': 1,
                    'message': TEST_MESSAGE
                }
            )

        assert response.status_code == 200
        assert bot.start_channel_command.call_count == 1
        for message in bot.start_channel_command.call_args.args:
            assert message.chat.title == 'geo monitorings'
            assert message.chat.id == 12345


class CallbackQueryCase(object):
    def __init__(self, name, staff_login, action, expected_call, expected_message):
        self.name = name
        self.staff_login = staff_login
        self.action = action
        self.expected_call = expected_call
        self.expected_message = expected_message

    def __str__(self):
        return "test_{}".format(self.name)


CALLBACK_QUERIES = [
    CallbackQueryCase(
        name='bad_login',
        staff_login=None,
        action=MessengerAction.DOWNTIME_3H,
        expected_call=None,
        expected_message='Access denied'
    ),
    CallbackQueryCase(
        name='downtime_1h',
        staff_login='mr.reese',
        action=MessengerAction.DOWNTIME_1H,
        expected_call='comment_to_downtime',
        expected_message='Downtime for 1h will be set'
    ),
    CallbackQueryCase(
        name='downtime_3h',
        staff_login='mr.reese',
        action=MessengerAction.DOWNTIME_3H,
        expected_call='comment_to_downtime',
        expected_message='Downtime for 3h will be set'
    ),
    CallbackQueryCase(
        name='reopen_issue',
        staff_login='mr.reese',
        action=MessengerAction.REOPEN,
        expected_call='reopen_issue',
        expected_message='Issue will be reopened'
    )
]


@pytest.mark.parametrize('case', CALLBACK_QUERIES, ids=str)
def test_callback_query(test_app, case):
    bot = test_app.telegram_bot
    with patch.object(bot, 'answer_callback_query'), \
            patch.multiple(test_app.startrek_client, comment_to_downtime=DEFAULT, reopen_issue=DEFAULT), \
            patch.object(test_app.staff_client, 'user_from_telegram', return_value=case.staff_login):
        with test_app.test_client() as client:
            response = client.post(
                '/telegram_hook/' + sha256(bot.token.encode('utf-8')).hexdigest(),
                json={
                    'update_id': 1,
                    'callback_query': {
                        'id': 100500,
                        'chat': {'id': 12345, 'type': 'supergroup', 'title': 'geo monitorings'},
                        'from': {'id': 2, 'is_bot': False, 'username': 'mr.Reese', 'first_name': 'John'},
                        'date': 1571248748,
                        'chat_instance': '100500',
                        'data': bot.encode_command(42, case.action)
                    }
                }
            )

        assert response.status_code == 200
        if case.expected_call:
            getattr(test_app.startrek_client, case.expected_call).assert_called_once()


def test_invalid_webhook(test_app):
    with test_app.test_client() as client:
        response = client.post('/telegram_hook/wrong_hash')
        assert response.status_code == 404
