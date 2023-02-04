import mock
import pytest

from django.test import Client
from rest_framework.exceptions import AuthenticationFailed

from smarttv.droideka.tests.helpers import PaginationInfo

from smarttv.droideka.proxy import api
from smarttv.droideka.proxy.data_source import MainMusicCarouselDataSource, negative_filter_by_attr_name, FeedDataSource, KpSelectionProvider, \
    SimpleCarouselDataSource, MusicSectionDataSource, HotelDataSource
from smarttv.droideka.proxy.models import Category2 as Category, PlatformModel
from smarttv.droideka.utils import PlatformInfo, PlatformType
from smarttv.droideka.tests.mock import required_android_headers, RequestStub
from smarttv.droideka.proxy.constants.carousels import VhCarousel, KpCarousel, FILTERABLE_CAROUSEL_ID, \
    EMBEDDED_SECTION_TYPE
from smarttv.droideka.proxy.request.carousels import MixedCarouselsInfo
from smarttv.droideka.proxy.request.promo import HotelRequestInfo

android_platform = PlatformInfo(PlatformType.ANDROID, '25', '1.2', None, None)

NEGATIVE_FILTER_BY_NAME_TEST_DATA = (
    ('id', ('id_to_exclude',), ({'id': 'id_to_exclude'}, {'id': 'other'}), ({'id': 'other'},)),
    ('id', ('id_to_exclude',), ({'id': 'id_to_exclude'},), tuple()),
    ('id', ('1', '2'), ({'id': '45'}, {'id': '1'}), ({'id': '45'},)),
    ('id', ('1', '2'), ({'id': '45'}, {'id': '1'}, {'id': '2'}), ({'id': '45'},)),
    ('id', ('1', '2'), ({'id': '1'}, {'id': '2'}), tuple()),
    ('non_existing_id', ('1', '2'), ({'id': '1'}, {'id': '2'}), ({'id': '1'}, {'id': '2'}))
)

request = RequestStub(platform_info=android_platform)

client = Client(content_type='application.json')


class TestHelperMethods:
    @pytest.mark.parametrize('attr_name, exclude_data, input_data, expected_result', NEGATIVE_FILTER_BY_NAME_TEST_DATA)
    def test_negative_filter_by_attr_name(self, attr_name, exclude_data, input_data, expected_result):
        assert tuple(negative_filter_by_attr_name(input_data, attr_name, exclude_data)) == expected_result


class TestFeedDataSource:
    request_category_id = 'main'
    platform_info = PlatformInfo(
        PlatformType.ANDROID,
        '25',
        '1.2',
        None,
        None)
    general_category = Category(category_id=request_category_id, title='gen1', rank=0)
    side_menu_category_1 = Category(category_id='general2', title='gen2', rank=1)
    side_menu_category_2 = Category(category_id='general3', title='gen3', rank=2)

    kp_carousels_content_type = 'kp_external_selections'
    kp_category_1 = Category(category_id='1', title='1', position=2,
                             content_type=kp_carousels_content_type, parent_category=general_category)
    kp_category_2 = Category(category_id='2', title='2', position=3,
                             content_type=kp_carousels_content_type, parent_category=general_category)

    categories = [
        general_category,
        side_menu_category_1,
        kp_category_1,
        kp_category_2,
        side_menu_category_2,
    ]

    child_category_test_data = [
        (PaginationInfo(0, 5), categories, [kp_category_1, kp_category_2]),
        (PaginationInfo(3, 8), categories, [kp_category_2]),
        (PaginationInfo(4, 9), categories, None),
        (PaginationInfo(0, 5), [], None)
    ]

    @pytest.mark.parametrize('pagination_info, available_child_categories, result', child_category_test_data)
    @mock.patch('smarttv.droideka.proxy.data_source.FeedDataSource.get_child_categories_by_parent_id')
    def test_get_child_categories_mapping(self,
                                          mock_method,
                                          pagination_info: PaginationInfo,
                                          available_child_categories: list,
                                          result: list):
        mock_method.return_value = available_child_categories
        offset = pagination_info.offset
        limit = pagination_info.limit

        serialized_data = {
            'category_id': self.request_category_id,
            'offset': offset,
            'limit': limit,
            'max_items_count': 5,
            'external_carousel_offset': 0
        }
        request_info = MixedCarouselsInfo(serialized_data, '', '', request, required_android_headers)
        ds = FeedDataSource(request_info)
        mixed_carousel_info: MixedCarouselsInfo = ds.get_result()[MixedCarouselsInfo.FIELD_KEY]

        assert mixed_carousel_info.external_categories_mapping.get(self.kp_carousels_content_type) == result


class TestKpSelectionsDataSource:
    kp_carousel_type = KpCarousel.TYPE

    def test_get_more_url(self):
        kp_provider = KpSelectionProvider(request, required_android_headers, 'carousel')
        pagination_state_params = kp_provider.get_pagination_state_params(
            {'from': 0, 'to': 10, 'sessionId': 'abc'},
            '1',
            KpCarousel.TYPE
        )
        more_url = kp_provider.get_more_url(
            'carousel6', request, pagination_state_params, None, VhCarousel.FIELD_DOCS_CACHE_HASH)

        assert '?offset=10&limit=10&docs_cache_hash=abc&carousel_type=external_kp&carousel_id=1' == more_url


class TestEmbeddedSectionsDataSource:

    @pytest.fixture(autouse=True)
    def setup_teardown(self):
        platform_any = PlatformModel(platform_type=PlatformType.ANY)
        platform_any.save()
        platform_none = PlatformModel(platform_type=PlatformType.NONE)
        platform_none.save()
        main = Category(
            id=0,
            category_id='main',
            title='Главная',
            content_type='',
            position=0,
        )
        main.save()
        main.exclude_platforms.add(platform_none)
        main.above_platforms.add(platform_none)
        main.below_platforms.add(platform_none)
        main.include_platforms.add(platform_any)
        container = Category(
            id=1,
            category_id='new_container',
            title='Container',
            content_type=EMBEDDED_SECTION_TYPE,
            position=1,
            parent_category=main,
        )
        self.container = container
        container.save()
        container.exclude_platforms.add(platform_none)
        container.above_platforms.add(platform_none)
        container.below_platforms.add(platform_none)
        container.include_platforms.add(platform_any)
        movie = Category(
            id=2,
            category_id='movie',
            title='Фильмы',
            content_type='',
            rank=0,
            parent_category_id=1
        )
        movie.save()
        movie.exclude_platforms.add(platform_none)
        movie.above_platforms.add(platform_none)
        movie.below_platforms.add(platform_none)
        movie.include_platforms.add(platform_any)
        series = Category(
            id=3,
            category_id='series',
            title='Сериалы',
            content_type='',
            rank=0,
            parent_category_id=1
        )
        series.save()
        series.exclude_platforms.add(platform_none)
        series.above_platforms.add(platform_none)
        series.below_platforms.add(platform_none)
        series.include_platforms.add(platform_any)

        yield

        Category.objects.all().delete()


class TestSimpleCarouselDataSource:

    @mock.patch('smarttv.droideka.proxy.api.vh.client.carousel_videohub')
    @mock.patch('smarttv.droideka.proxy.api.vh.client.carousel')
    def test_carousel_videohub_used(self, carousel_mock: mock.Mock, carousel_videohub_mock: mock.Mock):
        carousel_mock.side_effect = None
        carousel_videohub_mock.side_effect = None

        ds = SimpleCarouselDataSource('non_filterable_carousel_id')
        ds.tag = 'xx'
        ds.get_result()

        carousel_mock.assert_not_called()
        carousel_videohub_mock.assert_called_once_with(
            carousel_id='non_filterable_carousel_id', docs_cache_hash=None, headers=None, limit=5, offset=0,
            restriction_age=None, initial_request=None, tag='xx')

    @pytest.mark.parametrize('tag, _filter', [('movie', 'filter'), ('series', 'filter')])
    @mock.patch('smarttv.droideka.proxy.api.vh.client.carousel_videohub')
    @mock.patch('smarttv.droideka.proxy.api.vh.client.carousel')
    def test_carousel_used(self, carousel_mock: mock.Mock, carousel_videohub_mock: mock.Mock, tag, _filter):
        carousel_mock.side_effect = None
        carousel_videohub_mock.side_effect = None

        SimpleCarouselDataSource(FILTERABLE_CAROUSEL_ID, tag=tag, filters=_filter).get_result()

        carousel_videohub_mock.assert_not_called()
        carousel_mock.assert_called_once_with(
            carousel_id=FILTERABLE_CAROUSEL_ID, cache_hash=None, filters=_filter, headers=None, limit=5, offset=0,
            tag=tag, initial_request=None)


class TestMainMusicCarouselDataSource:
    def row(self):
        return {'rowId': 'xx', 'title': 'yyy'}

    @pytest.fixture
    def feed(self):
        return {
            'result': {
                'rows': [
                    self.row(),
                    self.row(),
                ]
            }
        }

    @pytest.fixture
    def headers(self):
        return {
            'Authorization': 'hello world',
            'Something': 'Else',
        }

    @pytest.fixture
    def request_info(self):
        info = mock.Mock()
        info.headers = {}
        info.external_categories_mapping = {
            'music_main_carousel': [mock.Mock(internal_position=5)]
        }
        return info

    @mock.patch('smarttv.droideka.proxy.api.music.client')
    def test_it_works(self, music_client, feed, request_info):
        music_client.infinite_feed = mock.Mock(return_value=feed)
        result = MainMusicCarouselDataSource(request_info).get_result()
        assert isinstance(result, list) and len(result) == 1
        assert result[0]['position'] == 5
        assert result[0]['music_response'] == self.row()

    @mock.patch('smarttv.droideka.proxy.api.music.client')
    def test_music_not_called_if_category_not_presented(self, request_info):
        request_info.external_categories_mapping = {}  # empty
        assert MainMusicCarouselDataSource(request_info).get_result() == {}

    @mock.patch('smarttv.droideka.proxy.api.music.client')
    def test_important_headers_passed_from_request(self, music_client, request_info, feed, headers):
        request_info.headers = headers
        music_client.infinite_feed = mock.Mock(return_value=feed)
        MainMusicCarouselDataSource(request_info).get_result()
        passed_headers = music_client.infinite_feed.call_args.kwargs['headers']
        assert list(passed_headers.keys()) == ['Authorization']


class TestMusicSectionDataSource:
    def row(self):
        return {'x': 'x', 'y': 'yy'}

    def request_info(self):
        return mock.Mock(headers={})

    def valid_feed(self):
        return {
            'result': {
                'rows': [
                    self.row(),
                    self.row(),
                ]
            }
        }

    def expired_token_response(self):
        return {
            'error': {
                'name': 'session-expired',
                'message': 'Your OAuth token is likely expired'
            }
        }

    def invalid_token_response(self):
        return {
            'error': {
                'name': 'validate',
                'message': 'Parameters requirements are not met. Errors: '
                           '[eitherUserId: Parameter with custom binder is '
                            'not bound]; ErrorMessages: []'
            }
        }

    @pytest.fixture
    def data_source(self):
        with mock.patch('smarttv.droideka.proxy.api.music.client') as c:
            c.infinite_feed.return_value = self.valid_feed()
            yield MusicSectionDataSource(self.request_info())

    def test_it_works(self, data_source):
        # this data source returns whole music answer as is
        assert data_source.get_result() == self.valid_feed()

    @mock.patch('smarttv.droideka.proxy.api.music.client')
    def test_correct_response(self, music_client):
        music_client.infinite_feed.return_value = {'result': 'valid'}
        result = MusicSectionDataSource(self.request_info()).get_result()
        assert result == {'result': 'valid'}

    @mock.patch('smarttv.droideka.proxy.api.music.client')
    def test_underling_exceptions_passed_through(self, music_client):
        music_client.infinite_feed.side_effect = KeyError('foo')
        with pytest.raises(KeyError):
            MusicSectionDataSource(self.request_info()).get_result()

    @mock.patch('smarttv.droideka.proxy.api.music.client')
    def test_response_expired_token(self, music_client):
        # if music api says that token is expired
        music_client.infinite_feed.return_value = self.expired_token_response()
        # than droideka sends 403 error to the client
        with pytest.raises(AuthenticationFailed):
            MusicSectionDataSource(self.request_info()).get_result()

    @mock.patch('smarttv.droideka.proxy.api.music.client')
    def test_response_invalid_token(self, music_client):
        # same true for token without required scopes
        music_client.infinite_feed.return_value = self.invalid_token_response()
        with pytest.raises(AuthenticationFailed):
            MusicSectionDataSource(self.request_info()).get_result()


class TestHotelDataSource:
    test_device_id = 'some-quazar-id'

    alice_business_response = {
        'imageUrl': 'url',
        'infoUrl': 'url',
        'title': 'title',
        'subtitle': 'subtitle',
    }

    @pytest.fixture
    def device_info(self):
        with mock.patch.object(api.alice.client, 'device_info') as fn:
            yield fn

    @pytest.fixture
    def data_source(self):
        return HotelDataSource(HotelRequestInfo(self.test_device_id))

    def test_not_found_device_returns_empty_dict(self, device_info, data_source):
        device_info.side_effect = api.alice.client.NotFoundError
        assert data_source.get_result() == {}

    def test_response_error_returns_empty_dict(self, device_info, data_source):
        device_info.side_effect = api.alice.client.ResponseError
        assert data_source.get_result() == {}

    def test_api_returned_data_added_to_result(self, device_info, data_source):
        device_info.return_value = self.alice_business_response
        assert data_source.get_result()['result']['imageUrl'] == 'url'


class TestCarouselPromoDataSource:
    def test_olympics(self):
        # from smarttv.droideka.proxy.data_source import TopCarouselPromoDataSource
        pass
