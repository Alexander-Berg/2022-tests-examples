import pytest
import mock
from copy import copy

from django.test import Client
from smarttv.droideka.proxy.views.base import APIView
from rest_framework import exceptions
from smarttv.droideka.proxy.blackbox import AndroidBlackBoxClient, InvalidTokenError, UserInfo
from alice.memento.proto.api_pb2 import TRespGetUserObjects, EConfigKey, TConfigKeyAnyPair
from alice.memento.proto.user_configs_pb2 import TSmartTvMusicPromoConfig
from google.protobuf import any_pb2
from smarttv.droideka.proxy.models import Category2


http_client = Client(content_type='application/json')


required_android_headers = {
    'HTTP_USER_AGENT': 'com.yandex.tv.home/1.77.38.3271 (Hi VHIX-43U169MSY; Android 7.1.1)',
    'HTTP_X_YAUUID': 'd68c1bca4efa403313837b12f5cdcd26',
    'HTTP_X_YADEVICEID': 'ca9b68da30474d5db0bbd1e8a25565bb',
    'HTTP_X_YAQUASARDEVICEID': '001c578e343b1b000000',
    'HTTP_X_YAQUASARPLATFORM': 'yandex_tv_rt2871_hikeen',
    'HTTP_X_WIFI_MAC': '10:01:BB:00:10:10',
    'HTTP_X_ETHERNET-MAC': '01:ff:74:79:00:10',
    'HTTP_X_BUILD_FINGERPRINT': 'Hi/VHIX43U169MSY/VHIX43U169MSY:7.1.1/NMF26Q/1222:user/dev-keys',
    'HTTP_X_DEVICE_ICOOKIE': '1000051080704000000',
    'HTTP_X_FORWARDED_FOR': '5.45.209.55',
}


def create_request():
    request = mock.MagicMock()
    request.META = {
        'HTTP_USER_AGENT': 'com.yandex.tv.home/1.2.773 (Realtek SmartTV; Android 7.1.1)',
        'HTTP_AUTHORIZATION': 'OAuth 123456789'
    }
    return request


request = create_request()


class ViewUnderTest(APIView):
    pass


class TestBBClient(AndroidBlackBoxClient):

    def __init__(self):
        super().__init__(request)


class TestTokenValidation:
    test_data = [
        # authorization required, token validation required
        (True, True),
        (True, False),
        (False, True),
        (False, False),
    ]

    def create_view(self, authorization_required, token_validation_required):
        view = ViewUnderTest()
        view.authorization_required = authorization_required
        view.oauth_token_validation_required = token_validation_required
        return view

    def assert_authentication_error_raised(self, view: APIView):
        with pytest.raises(exceptions.AuthenticationFailed):
            view.get_user_info(request)

    def assert_authentication_error_not_raised(self, view: APIView):
        user_info = view.get_user_info(request)
        assert isinstance(user_info, UserInfo)

    @pytest.mark.parametrize('authorization_required, token_validation_required', test_data)
    @mock.patch('__tests__.test_views.TestBBClient.ignore_expired_token', mock.Mock(return_value=True))
    @mock.patch('smarttv.droideka.proxy.views.base.APIView.create_bb_client', mock.Mock(
        return_value=TestBBClient()))
    @mock.patch('smarttv.droideka.proxy.blackbox.AndroidBlackBoxClient.get_user_info', mock.Mock(
        side_effect=InvalidTokenError))
    def test_get_user_info_token_expired_ignored(self, authorization_required, token_validation_required):
        view = self.create_view(authorization_required, token_validation_required)

        self.assert_authentication_error_not_raised(view)

    @pytest.mark.parametrize('authorization_required, token_validation_required', test_data)
    @mock.patch('__tests__.test_views.TestBBClient.ignore_expired_token', mock.Mock(return_value=False))
    @mock.patch('smarttv.droideka.proxy.views.base.APIView.create_bb_client', mock.Mock(
        return_value=TestBBClient()))
    @mock.patch('smarttv.droideka.proxy.blackbox.AndroidBlackBoxClient.get_user_info', mock.Mock(
        side_effect=InvalidTokenError))
    def test_get_user_info_token_expired_not_ignored_if_auth_required(
            self, authorization_required, token_validation_required):

        view = self.create_view(authorization_required, token_validation_required)

        if authorization_required or token_validation_required:
            self.assert_authentication_error_raised(view)
        else:
            self.assert_authentication_error_not_raised(view)

    @pytest.mark.parametrize('token_validation_required, token_presented_in_request', test_data)
    @mock.patch('smarttv.droideka.proxy.blackbox.AndroidBlackBoxClient.get_user_info')
    def test_validate_oauth_token(self, mock_method: mock.MagicMock, token_validation_required,
                                  token_presented_in_request):
        request = create_request()
        mock_method.side_effect = None
        view = ViewUnderTest()
        view.oauth_token_validation_required = token_validation_required
        if not token_presented_in_request:
            del request.META['HTTP_AUTHORIZATION']

        view.user_ip = None
        view.process_user_profile(request)

        if token_presented_in_request:
            mock_method.assert_called()
        else:
            mock_method.assert_not_called()


@pytest.mark.parametrize('key, data', [
    (EConfigKey.CK_SMART_TV_MUSIC_PROMO, TSmartTvMusicPromoConfig(FirstShowTime=100500))
])
def test_parse_memento_config(key, data):
    view = ViewUnderTest()

    any_message = any_pb2.Any()
    any_message.Pack(data)
    response = TRespGetUserObjects(UserConfigs=[TConfigKeyAnyPair(Key=key, Value=any_message)])

    unpacked = view._find_memento_config(key, response.UserConfigs, data.__class__)

    assert data == unpacked


def test_is_tandem():
    view = ViewUnderTest()
    request = create_request()
    request.headers = {'X-Ya-Tandem': '1'}
    assert view.is_tandem(request) is True


def test_is_not_tandem():
    view = ViewUnderTest()
    request = create_request()
    assert view.is_tandem(request) is False


class TestTvChannelsCategory:
    REGION_KAZAKHSTAN = 159
    REGION_RUSSIA = 225

    def _get_categories(self) -> list:
        required_headers = copy(required_android_headers)
        response = http_client.get('/api/v7/categories', {}, **required_headers)
        return response.json()

    @mock.patch('smarttv.droideka.proxy.views.categories.CategoriesView.get_user_country_region_id', mock.Mock(return_value=REGION_RUSSIA))
    @mock.patch('smarttv.droideka.proxy.views.categories.CategoriesView.is_from_russia', mock.Mock(return_value=True))
    @mock.patch('smarttv.droideka.proxy.categories_provider.categories_provider.get_categories', mock.Mock(return_value=[
        Category2(id=1, category_id='tv_channels_vertical', rank=10, persistent_client_category_id='tv_channels_vertical', title='Test', icon_s3_key='test')
    ]))
    def test_req_from_russia_tv_channels_presented(self):
        categories = self._get_categories()

        assert len(categories) == 1
        assert categories[0]['category_id'] == 'tv_channels_vertical'

    @mock.patch('smarttv.droideka.proxy.views.categories.CategoriesView.get_user_country_region_id', mock.Mock(return_value=REGION_KAZAKHSTAN))
    @mock.patch('smarttv.droideka.proxy.views.categories.CategoriesView.is_from_russia', mock.Mock(return_value=False))
    @mock.patch('smarttv.droideka.proxy.categories_provider.categories_provider.get_categories', mock.Mock(return_value=[
        Category2(id=1, category_id='tv_channels_vertical', rank=10, persistent_client_category_id='tv_channels_vertical', title='Test', icon_s3_key='test')
    ]))
    @mock.patch('smarttv.droideka.proxy.views.categories.CategoriesView.get_channels', mock.Mock(return_value=[1, 2 , 3]))
    def test_req_from_kz_tv_channels_presented(self):
        categories = self._get_categories()

        assert len(categories) == 1
        assert categories[0]['category_id'] == 'tv_channels_vertical'

    @mock.patch('smarttv.droideka.proxy.views.categories.CategoriesView.get_user_country_region_id', mock.Mock(return_value=REGION_KAZAKHSTAN))
    @mock.patch('smarttv.droideka.proxy.views.categories.CategoriesView.is_from_russia', mock.Mock(return_value=False))
    @mock.patch('smarttv.droideka.proxy.categories_provider.categories_provider.get_categories', mock.Mock(return_value=[
        Category2(id=1, category_id='tv_channels_vertical', rank=10, persistent_client_category_id='tv_channels_vertical', title='Test', icon_s3_key='test')
    ]))
    @mock.patch('smarttv.droideka.proxy.views.categories.CategoriesView.get_channels', mock.Mock(return_value=[]))
    def test_req_from_kz_tv_channels_not_presented(self):
        categories = self._get_categories()

        assert len(categories) == 0
