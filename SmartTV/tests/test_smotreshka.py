from copy import copy, deepcopy
import logging

import pytest
from mock import Mock, patch
from smarttv.droideka.proxy import smotreshka
from smarttv.droideka.proxy.views.smotreshka import SmotreshkaTvPlaybackBeginView
from smarttv.droideka.proxy.views.base import PlatformMixin

ACCOUNT_ID = 'someaccountid'
TOKEN_ANSWER = {
    'tvAssetToken': '11118df897asd09ff09a',
    'expiresAt': '2021-12-02T12:18:06+03:00',
}
TOKEN_ANSWER2 = {
    'tvAssetToken': '222298safd98789as7df',
    'expiresAt': '2021-12-04T12:18:06+03:00',
}

smotreshka_api = Mock()
profile_mock = Mock()
billing_mock = Mock()

logger = logging.getLogger(__name__)


class TestBeginView:
    """
    Тест на ручку /smotreshka/begin

    Проверяет, как разные входные данные в теле пост-запроса и заголовке
    влияют на запрос, который будет отправлен через api в смотрешку
    """
    account_id = 'hello'
    device_id = 'world'
    headers = {
        'HTTP_USER_AGENT': 'com.yandex.tv.home/1.2.773 (Realtek SmartTV; Android 7.1.1)',
        'HTTP_X_YAQUASARPLATFORM': 'yandex_tv_mt6681_cv',
        'HTTP_X_REAL_IP': '7.7.7.7',
    }
    post_data = {
        'content_id': 'contentid',
        'device_ip': '8.8.8.8',
        'purpose': 'change-media',
    }
    expected_output = {
        'accountId': 'hello',
        'mediaId': 'contentid',
        'purpose': 'change-media',
        'deviceInfo': {
            'deviceId': 'world',
            'deviceIp': '8.8.8.8',
            'deviceType': 'smarttv',
            'deviceUserAgent': 'com.yandex.tv.home/1.2.773 (Realtek SmartTV; Android 7.1.1)',
            'deviceName': '',
        },
    }
    begin_answer = {
        'playbackUrl': 'url',
        'playbackSession': {
            'id': 'pbsid'
        },
    }

    @pytest.fixture
    def view(self):
        return SmotreshkaTvPlaybackBeginView()

    @pytest.fixture(autouse=True)
    def account(self, mocker):
        account = smotreshka.Account(self.account_id, self.device_id)
        yield mocker.patch('smarttv.droideka.proxy.smotreshka.account_manager', **{
            'get_account.return_value': account,
            'get_or_register_account.return_value': account,
        })

    @pytest.fixture(autouse=True)
    def shared_pref(self, mocker):
        def get_int(key):
            return key == 'smotreshka_api_enabled'
        mocker.patch('smarttv.droideka.proxy.views.smotreshka.SharedPreferences.get_int', get_int)

    @pytest.fixture
    def api_begin(self, mocker):
        yield mocker.patch(
            'smarttv.droideka.proxy.smotreshka.smotreshka_api.begin',
            return_value=self.begin_answer
        )

    @pytest.fixture
    def post_request(self, rf):
        def factory(headers: dict, data: dict):
            request = rf.post('/', data, **headers)
            request.data = request.POST
            request.platform_info = PlatformMixin()._get_platform_info(request)
            return request
        return factory

    def test_view_returns_smotreshka_api_response(self, view, post_request, account, api_begin):
        """
        200-й ответ апи смотрешки выводится
        """
        headers = copy(self.headers)
        data = copy(self.post_data)
        expected_output = deepcopy(self.expected_output)

        request = post_request(headers, data)
        response = view.post(request)

        api_begin.assert_called_with(expected_output)
        assert response.data['playback_session_id'] == 'pbsid'
        assert response.data['playback_url'] == 'url'

    def test_module_passed_as_stb(self, view, post_request, account, api_begin):
        """
        Запрос с модуля передается как устройство stb
        """
        headers = copy(self.headers)
        data = copy(self.post_data)
        expected_output = deepcopy(self.expected_output)

        headers['HTTP_X_YAQUASARPLATFORM'] = 'yandexmodule_2'
        expected_output['deviceInfo']['deviceType'] = 'stb'

        request = post_request(headers, data)
        view.post(request)

        api_begin.assert_called_with(expected_output)

    def test_missed_ip_address_comes_from_headers(self, post_request, view, account, api_begin: Mock):
        """
        Если клиент не прислал ip, то он берется с балансера
        """
        headers = copy(self.headers)
        data = copy(self.post_data)
        expected_output = deepcopy(self.expected_output)

        data['device_ip'] = ''
        expected_output['deviceInfo']['deviceIp'] = '7.7.7.7'

        request = post_request(headers, data)
        view.post(request)

        api_begin.assert_called_with(expected_output)

    def test_playback_session_passed_from_client(self, post_request, view, account, api_begin: Mock):
        """
        PlaybackSessionId пробрасывается с клиента
        """
        headers = copy(self.headers)
        data = copy(self.post_data)
        expected_output = deepcopy(self.expected_output)

        data['playback_session_id'] = 'someid'
        expected_output['recyclePbsId'] = 'someid'
        data['purpose'] = 'resume'
        expected_output['purpose'] = 'resume'

        request = post_request(headers, data)
        view.post(request)

        api_begin.assert_called_with(expected_output)

    def test_user_agent_gets_default_header(self, post_request, account, view, api_begin):
        """
        Запрос без заголовка юзер-агента не ломается, просто ставится пустая строка
        """
        headers = copy(self.headers)
        data = copy(self.post_data)
        expected_output = deepcopy(self.expected_output)

        del headers['HTTP_USER_AGENT']
        expected_output['deviceInfo']['deviceUserAgent'] = ''

        request = post_request(headers, data)
        view.post(request)

        api_begin.assert_called_with(expected_output)

    def test_shared_preference_disables_view(self, view, post_request, api_begin):
        """
        Настройкой можно выключить ручку и она будет отдавать 403
        """
        headers = copy(self.headers)
        data = copy(self.post_data)
        request = post_request(headers, data)

        with patch('smarttv.droideka.proxy.views.smotreshka.SharedPreferences') as spmock:
            spmock.get_int.return_value = 0
            response = view.post(request)
            assert response.status_code == 403


class TestGetMediaCases:
    """
    Тест на функцию smotreshka.get_medias (не api.smotreshka.get_medias)
    """
    ACCOUNT_ID = 'account_id'
    DEVICE_ID = 'device_id'
    TOKEN = 'xxx'

    @pytest.fixture
    def api(self):
        with patch('smarttv.droideka.proxy.smotreshka.smotreshka_api') as mock:
            yield mock

    @pytest.fixture
    def account(self):
        return smotreshka.Account(self.ACCOUNT_ID, self.DEVICE_ID)

    @pytest.fixture
    def account_manager(self, account):
        with patch('smarttv.droideka.proxy.smotreshka.account_manager') as mock:
            mock.get_or_register_account.return_value = account
            yield mock

    @pytest.fixture
    def get_token(self):
        with patch('smarttv.droideka.proxy.smotreshka.get_token') as mock:
            mock.return_value = self.TOKEN
            yield mock

    def test_normal_flow(self, account_manager, account, get_token, api):
        api.get_medias.return_value = {'medias': [1, 2, 3]}

        result = smotreshka.get_medias(self.DEVICE_ID)

        assert result == [1, 2, 3]
        account_manager.get_or_register_account.assert_called_with(self.DEVICE_ID)
        get_token.assert_called_with(self.ACCOUNT_ID, '')
        api.get_medias.assert_called_with(self.TOKEN)

    def test_timezone_offset_passed_as_region(self, account_manager, account, get_token, api):
        api.get_medias.return_value = {'medias': [3, 4, 5]}

        smotreshka.get_medias(self.DEVICE_ID, '180 ')

        get_token.assert_called_with(self.ACCOUNT_ID, 'special.timezone.utc-03')


class TestAccountManager:
    DEVICE_ID = '621c91d209b1889ff8ce'

    @patch('smarttv.droideka.proxy.smotreshka.smotreshka_profile', profile_mock)
    @patch('smarttv.droideka.proxy.smotreshka.billing_client', billing_mock)
    def test_it_creates_account_if_one_is_missed(self):
        mgr = smotreshka.AccountManager()
        profile_mock.get = Mock(return_value=None)
        billing_mock.create_account.return_value = {'id': 'xxx', 'created': True}

        mgr.get_or_register_account(self.DEVICE_ID)
        profile_mock.get.assert_called_with(self.DEVICE_ID)
        profile_mock.set_many.assert_called()
        billing_mock.create_account.assert_called()


class TestTimezoneConversion:

    @pytest.mark.parametrize('offset,result', [
        ('', ''),
        ('0', ''),
        ('illegal', ''),
        ('60', 'special.timezone.utc-01'),
        ('180', 'special.timezone.utc-03'),
        (720, 'special.timezone.utc-12'),
        ('780', ''),  # out of scope
    ])
    def test_offset_converted_to_string(self, offset, result):
        # минуты конвертируются в часовые пояса
        assert smotreshka.timezone_offset_to_timezone(offset) == result
