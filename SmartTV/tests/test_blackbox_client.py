from copy import deepcopy

import pytest

import mock
from smarttv.droideka.proxy import blackbox
from smarttv.droideka.proxy.blackbox import SubscriptionType, DbFields
from smarttv.droideka.tests.mock import MockTvmClient

mock_tvm_client = MockTvmClient()


class TestClientVersion:
    USER_AGENTS_11 = [
        'com.yandex.tv.input.efir/549 (DEXP U55E9100Q; Android 7.1.1) TV',
        'com.yandex.tv.input.efir/549 (DEXP U50E9100Q; Android 7.1.1) TV',
        'Dalvik/2.1.0 (Linux; Android 7.1.1; U55E9100Q Build/NMF26Q)',
        'Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1468.0 Safari/537.36',
        'Dalvik/2.1.0 (Linux; Android 7.1.1; U50E9100Q Build/NMF26Q)',
        'com.yandex.tv.input.efir/487 (DEXP U50E9100Q; Android 7.1.1) TV',
        'com.yandex.tv.input.efir/479 (DEXP U55E9100Q; Android 7.1.1) TV',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.122 Safari/537.36',
        'Dalvik/2.1.0 (Linux; U; Android 7.1.1; U55E9100Q Build/NMF26Q)',
        'Dalvik/2.1.0 (Linux; U; Android 7.1.1; U50E9100Q Build/NMF26Q)',
        'YandexTV Lib (7.1.1 / api25), com.yandex.tv.home/1.1.625 (625), ru.kinopoisk.lib/1.5.0.2-SNAPSHOT (-1)',
        'okhttp/3.12.10',
        'com.yandex.tv.input.efir/538 (Realtek RealtekATV; Android 7.1.1) TV',
        'Nuclei (@pdiscoveryio)',
        'YandexTV Lib (7.1.1 / api25), com.yandex.tv.home/1.2.2147483647 (2147483647), ru.kinopoisk.lib/1.5.0.3-SNAPSHOT (-1)',
        'com.yandex.tv.input.efir/528 (Realtek RealtekATV; Android 7.1.1) TV',
        'okhttp/3.12.3',
    ]

    # TODO: more positive examples from real logs
    USER_AGENTS_12 = [
        'com.yandex.tv.home/1.2.654 (Realtek RealtekATV; Android 7.1) TV',
    ]

    @pytest.mark.parametrize('user_agent', USER_AGENTS_11)
    def test_old_client_version(self, user_agent):
        request = mock.MagicMock()
        request.META = {'HTTP_USER_AGENT': user_agent}
        bb_client = blackbox.AndroidBlackBoxClient(request)
        assert bb_client.get_client_version() == bb_client.DEFAULT_VERSION
        assert bb_client.ignore_expired_token() is True

    @pytest.mark.parametrize('user_agent', USER_AGENTS_12)
    def test_new_client_version(self, user_agent):
        request = mock.MagicMock()
        request.META = {'HTTP_USER_AGENT': user_agent}
        bb_client = blackbox.AndroidBlackBoxClient(request)
        assert bb_client.get_client_version() >= bb_client.VERSION_WITH_EXPIRED_TOKEN_SUPPORT
        assert bb_client.ignore_expired_token() is False

    def test_no_user_agent_old_client_version(self):
        request = mock.MagicMock()
        request.META = {}
        bb_client = blackbox.AndroidBlackBoxClient(request)
        assert bb_client.get_client_version() >= bb_client.DEFAULT_VERSION
        assert bb_client.ignore_expired_token() is True


class TestParseAuthentification:

    def test_token_obtained_successfully(self):
        request = mock.MagicMock()
        request.META = {
            'HTTP_AUTHORIZATION': 'OAuth 1',
            'HTTP_USER_AGENT': 'com.yandex.tv.home/1.2.654 (Realtek RealtekATV; Android 7.1) TV'
        }

        bb_client = blackbox.AndroidBlackBoxClient(request)
        assert bb_client.token == '1'

    def test_bad_oauth_token_error_raised(self):
        request = mock.MagicMock()
        request.META = {
            'HTTP_AUTHORIZATION': '1',
            'HTTP_USER_AGENT': 'com.yandex.tv.home/1.2.654 (Realtek RealtekATV; Android 7.1) TV'
        }

        bb_client = blackbox.AndroidBlackBoxClient(request)
        assert bb_client.token is None

    def test_bad_auth_protocol(self):
        request = mock.MagicMock()
        request.META = {
            'HTTP_AUTHORIZATION': 'badprotocol 1',
            'HTTP_USER_AGENT': 'com.yandex.tv.home/1.2.654 (Realtek RealtekATV; Android 7.1) TV'
        }

        bb_client = blackbox.AndroidBlackBoxClient(request)
        assert bb_client.token is None


class TestTvmBlackboxClient:

    @mock.patch('smarttv.droideka.proxy.tvm.tvm_client', mock.Mock(return_value={}))
    @mock.patch('smarttv.droideka.proxy.tvm.get_uid_from_user_ticket', mock.Mock(return_value=100500))
    def test_parse_user_ticket(self):
        request = mock.MagicMock()
        request.META = {
            'X-Ya-User-Ticket': '3:user:CA0Q__________9_GhIKBAiUkQYQlJEGINKF2MwEKAE:D2cB9WOt7RRq392dHy_ZGvp3Vu6Jsb'
                                '-GcSylGew'
                                '-dfuGtx2OX4CmVHegr_8HIpjWBzDGWHLy7vXixuykEcDvUKYZ3h75l1mVLgwiBbu7gXFRUkcfTe_zT'
                                '-Qr6iFUcPwEkQyQENbQ_7ksJvKwfk1bMpBTnbbC0KSX9iXT-xRsjBg',
            'HTTP_USER_AGENT': 'com.yandex.tv.home/1.2.654 (Realtek RealtekATV; Android 7.1) TV'
        }

        bb_client = blackbox.TvmBlackboxClient(request)
        assert bb_client.uid == 100500
        user_info = bb_client.get_user_info()
        assert user_info.passport_uid == '100500'
        assert user_info.attributes == {}


@mock.patch('smarttv.droideka.proxy.tvm.tvm_client', mock.Mock(mock_tvm_client))
class TestGetUserInfo:
    BB_RESPONSE_INVALID = {"error": "expired_token", "status": {"id": 5, "value": "INVALID"}}
    BB_RESPONSE_VALID = {'uid': {'value': 123}, 'attributes': {'1': {'a': 'b'}, '2': {'a': 'b'}}, 'status': {},
                         'user_ticket': '123'}
    BB_BAD_RESPONSE = {'bad_uuid': {'value': 123}, 'status': {}}

    headers = {
        'HTTP_AUTHORIZATION': 'OAuth 1',
        'HTTP_USER_AGENT': 'com.yandex.tv.home/1.2.654 (Realtek RealtekATV; Android 7.1) TV'
    }

    @mock.patch('smarttv.droideka.proxy.blackbox.AndroidBlackBoxClient.get_bb_response',
                mock.Mock(return_value=BB_RESPONSE_INVALID))
    def test_get_user_info_invalid_token(self):
        request = mock.MagicMock()
        request.META = deepcopy(self.headers)

        with pytest.raises(blackbox.InvalidTokenError):
            blackbox.AndroidBlackBoxClient(request).get_user_info()

    @mock.patch('smarttv.droideka.proxy.tvm.add_service_ticket', mock.Mock(return_value={}))
    @mock.patch('smarttv.droideka.proxy.blackbox.AndroidBlackBoxClient.bb_call',
                mock.Mock(return_value=BB_RESPONSE_VALID))
    def test_get_user_info_ok(self):
        request = mock.MagicMock()
        request.META = deepcopy(self.headers)

        user_info = blackbox.AndroidBlackBoxClient(request).get_user_info([1, 2, 3])

        assert user_info
        assert user_info.passport_uid == 123
        assert user_info.subscription is blackbox.SubscriptionType.UNKNOWN

    @mock.patch('smarttv.droideka.proxy.tvm.add_service_ticket', mock.Mock(return_value={}))
    @mock.patch('smarttv.droideka.proxy.blackbox.AndroidBlackBoxClient.bb_call',
                mock.Mock(return_value=BB_BAD_RESPONSE))
    def test_get_user_bad_response(self):
        request = mock.MagicMock()
        request.META = deepcopy(self.headers)

        user_info = blackbox.AndroidBlackBoxClient(request).get_user_info([1, 2, 3])

        assert user_info
        assert user_info.passport_uid is None
        assert user_info.subscription is blackbox.SubscriptionType.UNKNOWN


class TestUserInfo:

    @pytest.mark.parametrize('attributes, expected_result', [
        ({DbFields.have_plus: '1', DbFields.kinopoisk_ott_subscription_name: SubscriptionType.YA_PLUS.value},
         SubscriptionType.YA_PLUS),
        ({DbFields.have_plus: '1', DbFields.kinopoisk_ott_subscription_name: SubscriptionType.YA_PLUS_3M.value},
         SubscriptionType.YA_PLUS_3M),
        ({DbFields.have_plus: '1', DbFields.kinopoisk_ott_subscription_name: SubscriptionType.YA_PLUS_KP.value},
         SubscriptionType.YA_PLUS_KP),
        ({DbFields.have_plus: '1', DbFields.kinopoisk_ott_subscription_name: SubscriptionType.KP_BASIC.value},
         SubscriptionType.KP_BASIC),
        ({DbFields.have_plus: '1', DbFields.kinopoisk_ott_subscription_name: SubscriptionType.YA_PLUS_SUPER.value},
         SubscriptionType.YA_PLUS_SUPER),
        ({DbFields.have_plus: '1', DbFields.kinopoisk_ott_subscription_name: SubscriptionType.YA_PREMIUM.value},
         SubscriptionType.YA_PREMIUM),
    ])
    def test_subscription_detection(self, attributes, expected_result):
        assert blackbox.UserInfo('0', attributes).subscription == expected_result
