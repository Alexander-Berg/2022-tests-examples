import json
from typing import Callable

import mock
import pytest
import requests
from django.test import Client

from smarttv.droideka.proxy.api.access_config import tv_config
from smarttv.droideka.proxy import blackbox
from smarttv.droideka.proxy.transform import Channels
from smarttv.droideka.tests.mock import channel_category_1, channel_category_2, \
    GeobaseMock, VhApiMock, episodes, common_channels, get_valid_episode, hidden_channels, gen_channel, channel_id_1, \
    get_invalid_episodes, get_mixed_episodes, MockBlackboxClient, RequestStub
from smarttv.droideka.proxy import api
from smarttv.droideka.proxy.views.channels import RECOMMENDED_CATEGORY_TITLE
from smarttv.droideka.proxy.views.search import get_videosearch_client
from smarttv.droideka.tests.mock import graphql_carousel_500_response, graphql_carousel_200_response
from smarttv.droideka.tests.mock import MockTvmClient

from smarttv.droideka.utils import PlatformInfo, PlatformType, YANDEX_MODULE_QUASAR_PLATFORM

client = Client(content_type='application.json')
mock_tvm_client = MockTvmClient()


class ApiParams:
    TEST_ANDROID_USER_AGENT = 'me.anyone.testing/2.5 (ANY DEVICE ANYONE; Android 7.1.1) TV'

    @classmethod
    def get_params(cls, extra):
        result = {}
        result.update(extra)
        return result

    @classmethod
    def get_headers(cls, extra=None):
        result = {
            'HTTP_USER_AGENT': cls.TEST_ANDROID_USER_AGENT
        }
        if extra:
            result.update(extra)
        return result


vh_api = VhApiMock()

no_module_service_experiment = {}


@pytest.fixture
def required_params():
    return {'parent_id': '454184e140d418a1ad44375255598845'}


@mock.patch('smarttv.droideka.proxy.tvm.tvm_client', mock.Mock(mock_tvm_client))
@mock.patch('smarttv.droideka.proxy.common.geobase', GeobaseMock)
@mock.patch('smarttv.droideka.proxy.api.vh.channel_client', vh_api)
@mock.patch('smarttv.droideka.proxy.views.programs.ProgramsView.load_memento_configs',
            mock.Mock(return_value=None))
class TestProgramsV4:
    endpoint = '/api/v4/programs'

    def test_ok(self, required_params):
        vh_api.set_response_for_key(vh_api.episodes_key, episodes)

        response = client.get(path=self.endpoint,
                              data=ApiParams.get_params(extra=required_params),
                              **ApiParams.get_headers())
        assert response.status_code == 200, response.content

        items = json.loads(response.content)
        assert len(items) > 0
        for item in items:
            assert 'program_title' in item, 'program_title field not present in vh answer'

    def test_no_items(self, required_params):
        vh_api.set_response_for_key(vh_api.episodes_key, [])

        response = client.get(path=self.endpoint,
                              data=ApiParams.get_params(extra=required_params),
                              **ApiParams.get_headers())
        assert response.status_code == 200, response.content

    @pytest.mark.parametrize('response', [get_mixed_episodes()])
    def test_partially_incorrect_vh_answer(self, response, required_params):
        vh_api.set_response_for_key(vh_api.episodes_key, response)

        response = client.get(path=self.endpoint,
                              data=ApiParams.get_params(extra=required_params),
                              **ApiParams.get_headers())
        assert response.status_code == 200, response.content

    @pytest.mark.parametrize('response', get_invalid_episodes())
    def test_incorrect_vh_answers(self, response, required_params):
        vh_api.set_response_for_key(vh_api.episodes_key, response)

        response = client.get(path=self.endpoint,
                              data=ApiParams.get_params(extra=required_params),
                              **ApiParams.get_headers())
        assert response.status_code == 503, response.content

    @pytest.mark.parametrize('stream_type,url,expected', [
        ('DASH', '', '.mpd'),  # fake url if url is missed
        ('HLS', '', '.m3u8'),
        ('DASH', 'some', 'some'),  # not changed if present
    ])
    def test_missed_stream_url(self, required_params, stream_type, url, expected):
        """
        If VH doesn't include stream_url in answer, than we add it as fake suffix
        for back-compatibility for old clients.
        """
        data = get_valid_episode()
        data['streams'][0]['stream_type'] = stream_type
        data['streams'][0]['url'] = url
        vh_api.set_response_for_key(vh_api.episodes_key, [data])

        response = client.get(path=self.endpoint,
                              data=ApiParams.get_params(extra=required_params),
                              **ApiParams.get_headers())
        assert response.status_code == 200, response.content

        items = json.loads(response.content)
        assert items
        assert items[0]['streams'][0]['stream_type'] == stream_type
        assert items[0]['streams'][0]['url'] == expected


@mock.patch('smarttv.droideka.proxy.tvm.tvm_client', mock.Mock(mock_tvm_client))
@mock.patch('smarttv.droideka.proxy.common.geobase', GeobaseMock)
@mock.patch('smarttv.droideka.proxy.api.vh.channel_client', vh_api)
@mock.patch('smarttv.droideka.proxy.api.usaas.LazyExperiments.get_value',
            mock.Mock(return_value=True))
@mock.patch('smarttv.droideka.proxy.views.channels.ChannelsView.load_memento_configs',
            mock.Mock(return_value=None))
class TestChannelsV4:
    endpoint = '/api/v4/channels'

    # noinspection PyMethodMayBeStatic
    def find_title(self, title, categories):
        return list(filter(lambda x: x['name'] == title, categories))

    @pytest.mark.django_db
    def test_response_scheme(self):
        vh_api.set_response_for_key(vh_api.channels_key, common_channels)

        response = client.get(path=self.endpoint,
                              **ApiParams.get_headers())
        assert response.status_code == 200, response.content

        response = json.loads(response.content)
        assert 'channels' in response
        assert 'categories' in response
        assert len(response['channels']) == len(common_channels)

        test_categories = (channel_category_1, channel_category_2)
        for cat_id in test_categories:
            cat_title = Channels.get_title(cat_id)
            categories = self.find_title(cat_title, response['categories'])
            assert categories, '%s key not in categories block' % cat_title

            for item in categories:
                assert len(item['channel_ids']) == 1  # one in which category like in mocked vh answer

        assert isinstance(response['categories'], list)
        assert response['categories'][0]['name'] == RECOMMENDED_CATEGORY_TITLE
        assert len(response['categories']) == len(test_categories) + 1

    @pytest.mark.django_db
    def test_show_hidden_false(self):
        self.make_show_hidden_check({'show_hidden': 0}, lambda l1, l2: l1 < l2)

    @pytest.mark.django_db
    def test_show_hidden_true(self):
        self.make_show_hidden_check({'show_hidden': 1}, lambda l1, l2: l1 == l2)

    @pytest.mark.django_db
    def test_show_hidden_default(self):
        self.make_show_hidden_check({}, lambda l1, l2: l1 < l2)

    def make_show_hidden_check(self, params, check_expr: Callable[[int, int], bool]):
        vh_api.set_response_for_key(vh_api.channels_key, hidden_channels)
        response = client.get(path=self.endpoint,
                              data=ApiParams.get_params(extra=params),
                              **ApiParams.get_headers())
        assert response.status_code == 200, response.content

        obj = json.loads(response.content)
        assert check_expr(len(obj['channels']), len(hidden_channels))

    @pytest.mark.django_db
    def test_missing_channel_category(self):
        vh_api.set_response_for_key(vh_api.channels_key, [gen_channel(channel_id_1, category=None)])

        response = client.get(path=self.endpoint,
                              **ApiParams.get_headers())
        assert response.status_code == 200, response.content


@mock.patch('smarttv.droideka.proxy.tvm.tvm_client', mock.Mock(mock_tvm_client))
@mock.patch('smarttv.droideka.proxy.blackbox.AndroidBlackBoxClient', MockBlackboxClient)
@mock.patch('smarttv.droideka.proxy.views.plus.PlusSubscription.load_memento_configs',
            mock.Mock(return_value=None))
class TestSubscription:
    endpoint = '/api/v5/subscription'

    def test_ok(self):
        response = client.get(path=self.endpoint,
                              HTTP_AUTHORIZATION='Oauth AgAAAAAHYVLuAAX_m477zavGV0GMlcItT-kpRm3',
                              **ApiParams.get_headers())
        assert response.status_code == 200, response.content

    def test_absent_authorization(self):
        response = client.get(path=self.endpoint,
                              **ApiParams.get_headers())
        assert response.status_code == 403, response.content


@mock.patch('smarttv.droideka.proxy.tvm.tvm_client', mock.Mock(mock_tvm_client))
@mock.patch('smarttv.droideka.proxy.views.base.CheckAuthView.load_memento_configs',
                mock.Mock(return_value=None, side_effect=None))
class TestCheckAuth:
    endpoint = '/api/v6/check_auth'

    @mock.patch('smarttv.droideka.proxy.blackbox.AndroidBlackBoxClient')
    def test_ok(self, bb_mock):
        response = client.get(path=self.endpoint,
                              HTTP_AUTHORIZATION='Oauth VALID_TOKEN',
                              **ApiParams.get_headers())
        assert bb_mock.called
        assert bb_mock.return_value.get_user_info.called
        assert response.status_code == 204, response.content

    def test_absent_authorization(self):
        response = client.get(path=self.endpoint,
                              **ApiParams.get_headers())
        assert response.status_code == 400, response.content

    @pytest.mark.parametrize('ignore_expired_token, response_code', [
        (False, 403),
        (True, 204),
    ])
    @mock.patch('smarttv.droideka.proxy.blackbox.AndroidBlackBoxClient')
    def test_bad_authorization(self, bb_mock, ignore_expired_token, response_code):
        bb_mock.return_value.get_user_info.side_effect = blackbox.InvalidTokenError
        bb_mock.return_value.ignore_expired_token.return_value = ignore_expired_token
        response = client.get(path=self.endpoint,
                              HTTP_AUTHORIZATION='Oauth INVALID_TOKEN',
                              **ApiParams.get_headers())
        assert response.status_code == response_code, response.content


class TestVhApiClient:
    vh_api_client = api.vh.client

    fake_platform_info = PlatformInfo(PlatformType.ANDROID, '9', '1.77.1.2874', None, None,
                                      YANDEX_MODULE_QUASAR_PLATFORM)

    tv_service_params = {'from': tv_config.vh_from, 'service': tv_config.vh_service}

    @pytest.mark.parametrize('initial_request, expected_params', [
        (RequestStub(platform_info=PlatformInfo(PlatformType.ANDROID, None, None, None, None, 'not_module_quasar_platform'), experiments=no_module_service_experiment), tv_service_params),
        (RequestStub(platform_info=PlatformInfo(PlatformType.ANDROID, None, None, None, None, YANDEX_MODULE_QUASAR_PLATFORM), experiments=no_module_service_experiment), tv_service_params),
    ])
    def test_default_params(self, initial_request, expected_params):
        actual_params = self.vh_api_client.get_service_params(initial_request)

        assert expected_params == actual_params

    def test_get_graphql_params_ok(self):
        actual_result = self.vh_api_client.get_graphql_params({'a': 123, 'b': '123', 'c': None, 'd': 1.23})

        assert 'a:123,b:"123",d:1.23' == actual_result

    @pytest.mark.parametrize('input', [{}, None])
    def test_get_graphql_params_error(self, input):
        with pytest.raises(ValueError):
            self.vh_api_client.get_graphql_params(input)

    @mock.patch('smarttv.droideka.proxy.api.vh.client._request', mock.Mock(return_value=graphql_carousel_500_response))
    def test_carousel_500(self):
        with pytest.raises(self.vh_api_client.BadGatewayError):
            self.vh_api_client.carousel(RequestStub(platform_info=self.fake_platform_info), {}, '123', 'movie',
                                        'genre:ruw123')

    @mock.patch('smarttv.droideka.proxy.api.vh.client._request', mock.Mock(return_value=graphql_carousel_200_response))
    def test_carousel_ok(self):
        assert graphql_carousel_200_response['carousel']['content'] == \
               self.vh_api_client.carousel(RequestStub(platform_info=self.fake_platform_info), {}, '123', 'movie',
                                           'genre:ruw123')

    @mock.patch('smarttv.droideka.proxy.api.vh.client._request')
    def test_experiments_params(self, mock):
        request = RequestStub(experiments={'vh_params': ['invalid_param', 'test_param_1=1000',
                                                         'test_param_1=2000', 'test_param_2=rearr=123']},
                              platform_info=self.fake_platform_info)
        self.vh_api_client.feed(request, {}, tag='main')
        call_params = mock.call_args.kwargs['params']
        assert call_params['test_param_1'] == {'1000', '2000'}
        assert call_params['test_param_2'] == {'rearr=123'}

    @mock.patch('smarttv.droideka.proxy.api.vh.client.session.request')
    def test_404_answer(self, session_request):
        # if we get 404 answer from VH backend
        response = mock.Mock()
        response.status_code = 404
        response.raw.retries = None
        response.headers = {}
        session_request.return_value = response

        # than client raises NotFound
        with pytest.raises(self.vh_api_client.NotFoundError):
            self.vh_api_client.content_detail(RequestStub(platform_info=self.fake_platform_info), {}, '123')

    @pytest.mark.parametrize('input,expected', (
        ('', None),
        ('some', None),
        ('462007cda76f46f9837bdfc2b13504d1', '462007cda76f46f9837bdfc2b13504d1'),
    ))
    def test_get_uuid(self, input, expected):
        request = RequestStub(platform_info=self.fake_platform_info)
        request.headers['X-YaUUID'] = input
        result = self.vh_api_client.get_uuid(request)
        assert result == expected


class TestMusicApiClient:
    music = api.music.client

    @pytest.mark.parametrize('http_code', (400, 401))
    def test_4xx_response_doesnt_raise_exception(self, http_code):
        mocked_response = mock.Mock(headers={}, status_code=http_code)
        mocked_response.content = b'{"some": "json"}'
        self.music.make_request = mock.Mock(return_value=mocked_response)

        # response with code 401 is valid music response
        response = self.music.infinite_feed()
        assert response == {'some': 'json'}

    def test_unknown_response_raises_exception(self):
        # unknown response code gives error
        mocked_response = mock.Mock(headers={}, status_code=404)
        self.music.make_request = mock.Mock(return_value=mocked_response)

        with pytest.raises(self.music.NotFoundError):
            self.music.infinite_feed()


class TestAliceBusinessApi:
    api = api.alice.client

    def response(self, **kwargs):
        args = dict(headers={}, status_code=200, content=b'')
        args.update(kwargs)
        return mock.Mock(**args)

    @pytest.fixture
    def make_request(self):
        with mock.patch.object(self.api, 'make_request') as mocked:
            with mock.patch.object(api.alice, 'add_service_ticket') as f:
                f.side_effect = lambda dest, headers: headers  # skip tvm headers adding
                yield mocked

    @pytest.fixture
    def request_fn(self):
        with mock.patch.object(self.api, '_request') as mocked:
            yield mocked

    def test_404_answer(self, make_request):
        make_request.return_value = self.response(status_code=404)
        with pytest.raises(self.api.NotFoundError):
            self.api.device_info('xx')

    def test_error_answer(self, request_fn):
        request_fn.return_value = {'something': 'invalid'}
        with pytest.raises(self.api.ResponseError):
            self.api.device_info('xx')

    def test_device_info_returns_result_key_value(self, request_fn):
        data = {'result': {'hello': 'world'}}
        request_fn.return_value = data
        assert self.api.device_info('xx') == data['result']


class TestSearchClient:
    @pytest.mark.parametrize('initial_request, expected_client', [
        (RequestStub(
            platform_info=PlatformInfo(PlatformType.ANDROID, None, None, None, None, 'not_module_quasar_platform'),
            experiments=no_module_service_experiment), tv_config.videopoisk_client),
        (RequestStub(
            platform_info=PlatformInfo(PlatformType.ANDROID, None, None, None, None, YANDEX_MODULE_QUASAR_PLATFORM),
            experiments=no_module_service_experiment), tv_config.videopoisk_client),
    ])
    def test_get_videosearch_client(self, initial_request, expected_client):
        actual_client = get_videosearch_client(initial_request)

        assert expected_client == actual_client


class TestSmotreshkaBillingApi:
    billing = api.smotreshka.billing_client

    @pytest.mark.parametrize('http_code,exception_class', [(403, 'Forbidden'), (429, 'ApiError')])
    def test_non_200_answers_raise_exception(self, http_code, exception_class):
        mocked_response = mock.Mock(headers={}, status_code=http_code)
        mocked_response.content = b'forbidden'
        self.billing.make_request = mock.Mock(return_value=mocked_response)

        with pytest.raises(getattr(self.billing, exception_class)):
            self.billing.create_account('username', 'email')


class TestSmotreshkaPartnerApi:
    domain = 'http://example.org'
    pbs_limit_reached_response = {'id': 'play back session limit reached',
                                  'message': 'play back session limit reached'}

    @pytest.fixture
    def partner_api(self):
        class Api(api.smotreshka.SmotreshkaApi):
            proxies = None
        client = Api(self.domain, 1, 2)
        return client

    def test_4xx_valid_responses_raises_error(self, partner_api: api.smotreshka.SmotreshkaApi, responses):
        responses.add(responses.GET, f'{self.domain}/tv/v2/medias',
                      json={'id': ''}, status=403)
        with pytest.raises(partner_api.Forbidden):
            partner_api.get_medias('xx')

    def test_invalid_json_raises_error(self, partner_api, responses):
        responses.add(responses.GET, f'{self.domain}/tv/v2/medias',
                      body='not-json-string', status=200)
        with pytest.raises(partner_api.BadGatewayError):
            partner_api.get_medias('xx')

    def test_4xx_non_json_answer_raises_error(self, partner_api, responses):
        responses.add(responses.GET, f'{self.domain}/tv/v2/medias',
                      body='not-json-string', status=403)
        with pytest.raises(partner_api.Forbidden):
            partner_api.get_medias('xx')

    def test_pbs_limit_reached_answer_raises_error(self, partner_api, responses):
        responses.add(responses.GET, f'{self.domain}/tv/v2/medias',
                      json=self.pbs_limit_reached_response, status=403)
        with pytest.raises(partner_api.PbsLimitReached):
            partner_api.get_medias('xx')

    def test_5xx_response_raises_error(self, partner_api, responses):
        responses.add(responses.GET, f'{self.domain}/tv/v2/medias',
                      body='Internal Server Error', status=500)
        with pytest.raises(partner_api.BadGatewayError):
            partner_api.get_medias('xx')

    def test_connection_error(self, partner_api, responses):
        exc = requests.ConnectionError
        responses.add(responses.GET, f'{self.domain}/tv/v2/medias',
                      body=exc())
        with pytest.raises(requests.ConnectionError):
            partner_api.get_medias('xx')

    def test_timeout_error(self, partner_api, responses):
        exc = requests.Timeout
        responses.add(responses.GET, f'{self.domain}/tv/v2/medias',
                      body=exc())
        with pytest.raises(requests.Timeout):
            partner_api.get_medias('xx')
