import pytest
from tvmauth.mock import TvmClientPatcher, MockedTvmClient
from paysys.sre.balance_notifier.sender import sender
from unittest import mock
import requests


class TestSender:
    @pytest.fixture
    def env(self):
        return ''

    @pytest.fixture
    def self_secret(self):
        return 'fake'

    @pytest.fixture
    def self_tvm_id(self):
        return 100500

    @pytest.fixture
    def sender_tvm_id(self):
        return 100501

    @pytest.fixture
    def message_id(self):
        return 'e00c0fbe-aaa0-45b8-92b6-70fbac223096'

    @pytest.fixture
    def notify_url(self):
        return 'https://test.sender.yandex-team.ru/api/0/yandex.balance/transactional'

    @pytest.fixture
    def requests_status_code(self):
        return 200

    @pytest.fixture
    def requests_get_status(self, mocker, requests_status_code):
        mocked_requests = mocker.patch('requests.get')
        mocked_requests.return_value.status_code = requests_status_code
        return mocked_requests

    @pytest.fixture
    def requests_post_status(self, mocker, requests_status_code):
        mocked_requests = mocker.patch('requests.post')
        mocked_requests.return_value.status_code = requests_status_code
        return mocked_requests

    @pytest.fixture
    def side_effect(self, mocker):
        mocker.raise_for_status = mock.Mock()
        mocker.text = "some error"
        mocker.raise_for_status.side_effect = requests.HTTPError("some error")
        return mocker

    @pytest.fixture
    def requests_get_raise_status(self, mocker, side_effect):
        mocked_requests = mocker.patch('requests.get')
        mocked_requests.return_value = side_effect
        return mocked_requests

    @pytest.fixture
    def unwrapped_emails(self):
        return [{'manager': 'some_mamager',
                 'email': 'some_email',
                 'to': '',
                 'cc': '',
                 'bcc': '',
                 'message_id': 'some_id',
                 'contracts': {},
                 'notify_url': 'https://url'
                 }]

    class TestUnwrapEmails:
        @pytest.fixture
        def wrapped_emails(self):
            return [{'message_id': 'some_id',
                     'message_to_send': {'manager': 'some_mamager',
                                         'email': 'some_email',
                                         'to': '',
                                         'cc': '',
                                         'bcc': '',
                                         'contracts': {},
                                         'notify_url': 'https://url'
                                         }}]

        def test_unwrap_emails_uuid(self, wrapped_emails, unwrapped_emails):
            result = sender.unwrap_emails_uuid(wrapped_emails)
            assert result == unwrapped_emails

    class TestMessageStatus:
        @pytest.mark.parametrize('requests_status_code', [250])
        @pytest.mark.usefixtures('requests_get_status')
        def test_check_message_status_250(self, sender_tvm_id, notify_url, message_id, requests_status_code):
            assert sender.check_message_status(notify_url, message_id, str(sender_tvm_id))

        @pytest.mark.usefixtures('requests_get_raise_status')
        def test_check_message_status_error(self, sender_tvm_id, notify_url, message_id):
            with pytest.raises(requests.HTTPError):
                assert sender.check_message_status(notify_url, message_id, str(sender_tvm_id))

    class TestListNotify:
        @pytest.fixture(autouse=True)
        def tvm_client_mock(self, self_tvm_id):
            with TvmClientPatcher(MockedTvmClient(self_tvm_id=self_tvm_id)) as mocked_tvm_client:
                yield mocked_tvm_client

        @pytest.mark.usefixtures('requests_get_status')
        @pytest.mark.usefixtures('requests_post_status')
        @pytest.mark.parametrize('env', ['test'])
        def test_sender(self, env, self_secret, self_tvm_id, sender_tvm_id, unwrapped_emails):
            assert sender.sender(unwrapped_emails, env, self_secret, self_tvm_id, sender_tvm_id) is None
