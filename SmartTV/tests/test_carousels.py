import json

import mock
import pytest
from django.test import Client
from urllib import parse
from copy import copy
import datetime

from smarttv.droideka.proxy.constants.carousels import KpCarousel, VhCarousel, VhFeed, CAROUSELS_WITH_FILTERS, \
    TAG_BY_CAROUSEL_MAPPING, EMBEDDED_SECTION_TYPE, CarouselType, KpCategory, MusicCarousel
from smarttv.droideka.proxy.vh.constants import PROMO_CAROUSEL_ID
from smarttv.droideka.proxy.models import Category2, PlatformModel, PlatformType, Promo
from smarttv.droideka.tests.helpers import ResponsesGenerator
from smarttv.droideka.tests.mock import required_android_headers, UserInfoMock, alice_for_business_mock_response, \
    MockTvmClient, ExperimentsMock
from alice.memento.proto.user_configs_pb2 import TSmartTvMusicPromoConfig
from smarttv.droideka.proxy.blackbox import UserInfo
from smarttv.droideka.utils import MementoConfigs
from smarttv.droideka.proxy import data_source
from smarttv.droideka.proxy.views.carousels import OttExperiment
from smarttv.droideka.utils import RequestInfo

client = Client(content_type='application.json')

carousel_generator = ResponsesGenerator()
test_quasar_id = 'test_quasar_id'

mock_tvm_client = MockTvmClient()
POSITION_TO_INSERT_EMBEDDED_SECTION = 3


def make_user_authorized(request):
    request.request_info.authorized = True


def make_user_unauthorized(request):
    request.request_info.authorized = False


class TestKpCarouselsV7Positioning:
    MAIN_CATEGORY_ID = 'main'
    KP_CATEGORY_1 = 'EXTERNAL_PLUS/5e3b4065a77eac00232c2d49'
    KP_CATEGORY_2 = 'EXTERNAL/5e3bfb22a77eac00232c2d5b'
    KP_CATEGORY_IDS = [KP_CATEGORY_1, KP_CATEGORY_2]

    main_category = Category2(id=0, category_id=MAIN_CATEGORY_ID, title='Главная', rank=1)
    watching_now_category = Category2(id=1, category_id=KP_CATEGORY_1,
                                      title='Смотрят сейчас', position=5, rank=0, parent_category=main_category,
                                      content_type=KpCarousel.TYPE)
    after_category = Category2(id=2, category_id=KP_CATEGORY_2,
                               title='Сразу после проката', position=6, rank=0, parent_category=main_category,
                               content_type=KpCarousel.TYPE)

    carousel_generator = ResponsesGenerator()

    @classmethod
    def create_categories(cls):
        cls.main_category.save()
        cls.main_category.exclude_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.main_category.above_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.ANY).first())
        cls.main_category.below_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.main_category.include_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())

        cls.watching_now_category.save()
        cls.watching_now_category.exclude_platforms.add(
            PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.watching_now_category.above_platforms.add(
            PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.watching_now_category.below_platforms.add(
            PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.watching_now_category.include_platforms.add(
            PlatformModel.objects.filter(platform_type=PlatformType.ANY).first())

        cls.after_category.save()
        cls.after_category.exclude_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.after_category.above_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.after_category.below_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.after_category.include_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.ANY).first())

    @staticmethod
    def delete_categories():
        Category2.objects.all().delete()
        PlatformModel.objects.all().delete()

    @pytest.mark.django_db
    @pytest.fixture(autouse=True)
    def categories_fixture(self):
        self.create_categories()
        yield
        self.delete_categories()

    def check_carousels(self, carousels, offset):
        watching_now_carousel_presented = False
        after_carousel_presented = False
        for index, carousel in enumerate(carousels, start=1):
            if carousel['carousel_id'] == self.KP_CATEGORY_1:
                watching_now_carousel_presented = True
                assert index == self.watching_now_category.position - offset
            elif carousel['carousel_id'] == self.KP_CATEGORY_2:
                assert index == self.after_category.position - offset
                after_carousel_presented = True
            else:
                assert carousel['carousel_id'] and carousel['carousel_id'] not in self.KP_CATEGORY_IDS
        assert watching_now_carousel_presented
        assert after_carousel_presented

    @pytest.mark.django_db
    @mock.patch('smarttv.droideka.proxy.api.usaas.LazyExperiments.get_value',
                mock.Mock(return_value=None))
    @mock.patch('smarttv.droideka.proxy.api.ott.client.selections')
    @mock.patch('smarttv.droideka.proxy.api.vh.client.feed')
    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselsV7View.load_memento_configs',
                mock.Mock(side_effect=None))
    def test_kp_carousels_added_at_correct_position(self,
                                                    vh_result_method: mock.MagicMock,
                                                    kp_result_method: mock.MagicMock):
        limit = 10
        max_items_count = 10
        offset = 0
        category_id = 'main'
        vh_result_method.return_value = self.carousel_generator.generate_vh_carousels_object(8, max_items_count)
        kp_result_method.side_effect = lambda *args, **kwargs: self.carousel_generator.generate_kp_carousel_object(
            max_items_count)
        response = client.get(
            path='/api/v7/carousels',
            data={
                'category_id': category_id,
                'limit': limit,
                'max_items_count': max_items_count,
            },
            **required_android_headers
        )

        actual_result = json.loads(response.content.decode())
        self.check_carousels(actual_result['carousels'], offset)


@mock.patch('smarttv.droideka.proxy.api.vh.client.content_detail', mock.Mock(return_value=None))
@mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.load_memento_configs', mock.Mock(side_effect=None))
@mock.patch('smarttv.droideka.proxy.views.carousels.CarouselsV7View.load_memento_configs', mock.Mock(side_effect=None))
@pytest.mark.django_db
class TestEmbeddedItems:
    MAIN_CATEGORY_ID = 'main'
    EMBEDDED_SECTION = 'embedded_section'
    EMBEDDED_CATEGORY_1 = 'embedded_1'
    EMBEDDED_CATEGORY_2 = 'embedded_2'

    main_category = Category2(id=0, category_id=MAIN_CATEGORY_ID, title='Главная', rank=1)
    embedded_section = Category2(id=1, category_id=EMBEDDED_SECTION, title='Встроенная секция',
                                 position=POSITION_TO_INSERT_EMBEDDED_SECTION,
                                 parent_category=main_category, content_type=EMBEDDED_SECTION_TYPE,
                                 carousel_type=CarouselType.TYPE_SQUARE)
    embedded_carousel = Category2(id=2, category_id=EMBEDDED_CATEGORY_1,
                                  title='Тестовая карусель', rank=1, parent_category=embedded_section,
                                  content_type=VhCarousel.TYPE, carousel_type=CarouselType)
    embedded_category = Category2(id=3, category_id=EMBEDDED_CATEGORY_2,
                                  title='Тестовая категория', rank=2, parent_category=embedded_section,
                                  content_type=VhFeed.TYPE)

    carousel_generator = ResponsesGenerator()

    @classmethod
    def create_models(cls):
        cls.main_category.save()
        cls.main_category.exclude_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.main_category.above_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.main_category.below_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.main_category.include_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.ANY).first())

        cls.embedded_section.save()
        cls.embedded_section.exclude_platforms.add(
            PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.embedded_section.above_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.embedded_section.below_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.embedded_section.include_platforms.add(
            PlatformModel.objects.filter(platform_type=PlatformType.ANY).first())

        cls.embedded_carousel.save()
        cls.embedded_carousel.exclude_platforms.add(
            PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.embedded_carousel.above_platforms.add(
            PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.embedded_carousel.below_platforms.add(
            PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.embedded_carousel.include_platforms.add(
            PlatformModel.objects.filter(platform_type=PlatformType.ANY).first())

        cls.embedded_category.save()
        cls.embedded_category.exclude_platforms.add(
            PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.embedded_category.above_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.embedded_category.below_platforms.add(PlatformModel.objects.filter(platform_type=PlatformType.NONE).first())
        cls.embedded_category.include_platforms.add(
            PlatformModel.objects.filter(platform_type=PlatformType.ANY).first())

    @staticmethod
    def delete_models():
        Category2.objects.all().delete()
        PlatformModel.objects.all().delete()

    @pytest.fixture(autouse=True)
    def categories_fixture(self):
        self.create_models()
        yield
        self.delete_models()

    def check_embedded_section(self, carousels):
        for index, carousel in enumerate(carousels, start=1):
            if index == POSITION_TO_INSERT_EMBEDDED_SECTION:
                assert carousel['carousel_id'] == self.EMBEDDED_SECTION
                assert carousel['carousel_type'] == CarouselType.TYPE_SQUARE
                carousel_url = parse.urlsplit(carousel['includes'][0]['url'])
                assert {'carousel_id': ['embedded_1'], 'offset': ['0'], 'limit': ['10']} == parse.parse_qs(
                    carousel_url.query)
                assert '/api/v7/carousel' == carousel_url.path

                carousels_url = parse.urlsplit(carousel['includes'][1]['url'])
                assert {'category_id': ['embedded_2'], 'offset': ['0'], 'limit': ['10'],
                        'max_items_count': ['10']} == parse.parse_qs(carousels_url.query)
                assert '/api/v7/carousels' == carousels_url.path
            else:
                assert carousel['carousel_id'] != self.EMBEDDED_SECTION

    @mock.patch('smarttv.droideka.proxy.api.usaas.LazyExperiments.get_value',
                mock.Mock(return_value=None))
    @mock.patch('smarttv.droideka.proxy.api.vh.client.feed')
    def test_embedded_section_included_at_3rd_position(self, vh_result_method):
        limit = 10
        max_items_count = 10
        category_id = 'main'
        vh_result_method.return_value = self.carousel_generator.generate_vh_carousels_object(8, max_items_count)
        response = client.get(
            path='/api/v7/carousels',
            data={
                'category_id': category_id,
                'limit': limit,
                'max_items_count': max_items_count,
            },
            **required_android_headers
        )

        actual_result = json.loads(response.content.decode())
        self.check_embedded_section(actual_result['carousels'])

    @pytest.mark.parametrize('carousel_id', CAROUSELS_WITH_FILTERS)
    @mock.patch('smarttv.droideka.proxy.api.vh.client.carousel_videohub',
                mock.Mock(return_value=carousel_generator.generate_vh_carousel_response(10)))
    @mock.patch('smarttv.droideka.proxy.api.vh.client.carousel_content_filters',
                mock.Mock(return_value=carousel_generator.generate_filter_identities_response()))
    def test_filterable_categories_has_filter_object(self, carousel_id):
        response = client.get(
            path='/api/v7/carousel',
            data={
                'carousel_id': carousel_id,
                'limit': 10,
            },
            **required_android_headers
        )
        actual_result = json.loads(response.content.decode())

        assert 'filter' in actual_result
        assert f'api/v7/carousel?carousel_id=CATEG_NAVIGATION_VIDEO&offset=0&limit=10&tag={TAG_BY_CAROUSEL_MAPPING[carousel_id]}' in \
               actual_result['filter']['base_url']
        assert actual_result['filter']['filters'][0]['title'] == 'Жанр'
        assert actual_result['filter']['filters'][1]['title'] == 'Год'
        assert actual_result['filter']['filters'][2]['title'] == 'Рейтинг КиноПоиска'

    @pytest.mark.parametrize('carousel_id', CAROUSELS_WITH_FILTERS)
    @mock.patch('smarttv.droideka.proxy.api.vh.client.carousel_videohub',
                mock.Mock(return_value=carousel_generator.generate_vh_carousel_response(10)))
    @mock.patch('smarttv.droideka.proxy.api.vh.client.carousel_content_filters',
                mock.Mock(return_value=carousel_generator.generate_filter_identities_response()))
    def test_filterable_categories_with_offset_has_no_filter_object(self, carousel_id):
        response = client.get(
            path='/api/v7/carousel',
            data={
                'carousel_id': carousel_id,
                'limit': 10,
                'offset': 10
            },
            **required_android_headers
        )
        actual_result = json.loads(response.content.decode())

        assert 'filter' not in actual_result

    @mock.patch('smarttv.droideka.proxy.api.vh.client.carousel_videohub',
                mock.Mock(return_value=carousel_generator.generate_vh_carousel_response(10)))
    @mock.patch('smarttv.droideka.proxy.api.vh.client.carousel_content_filters',
                mock.Mock(return_value=carousel_generator.generate_filter_identities_response()))
    def test_filterable_categories_with_non_filterable_id_has_no_filter_object(self):
        response = client.get(
            path='/api/v7/carousel',
            data={
                'carousel_id': 'not_filterable_carousel_id',
                'limit': 10,
            },
            **required_android_headers
        )
        actual_result = json.loads(response.content.decode())

        assert 'filter' not in actual_result


@mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.load_memento_configs', mock.Mock(side_effect=None))
@pytest.mark.django_db
class TestVHDelayedCarousel:

    @mock.patch('smarttv.droideka.proxy.api.vh.client.carousel_videohub', mock.Mock(
        return_value=ResponsesGenerator().generate_vh_carousel_response(1)
    ))
    def test_fields_get_passed_through(self):
        response = client.get(
            path='/api/v7/carousel',
            data={
                'carousel_id': 'delayed_tvo',
                'limit': 10,
            },
            **required_android_headers
        )
        result = response.content.decode()
        assert 'is_new_delayed_episode' in result
        assert 'is_next_delayed_episode' in result


@mock.patch('smarttv.droideka.proxy.data_source.FeedDataSource.get_child_categories_by_parent_id',
            mock.Mock(return_value=[]))
@mock.patch('smarttv.droideka.proxy.views.carousels.CarouselsV7View.load_memento_configs', mock.Mock(side_effect=None))
@pytest.mark.django_db
class TestCarouselsMoreUrlPresence:
    test_main_category = Category2(category_id='testid', title='Встраиваемая карусель',
                                   rank=0, position=0, parent_category=None,
                                   content_type=VhCarousel.TYPE)
    request_params = {'category_id': 'some_id', 'limit': 10, 'max_items_count': 10}
    carousel_generator = ResponsesGenerator()

    def get_response(self) -> dict:
        return client.get(
            path='/api/v7/carousels',
            data=self.request_params,
            **required_android_headers
        ).json()

    @mock.patch('smarttv.droideka.proxy.data_source.FeedDataSource.filter_categories_by_paging_info',
                mock.Mock(return_value=[]))
    @mock.patch('smarttv.droideka.proxy.api.vh.client.feed',
                mock.Mock(return_value=carousel_generator.generate_vh_carousels_object(10, 10)))
    @mock.patch('smarttv.droideka.proxy.categories_provider.categories_provider.get_category',
                mock.Mock(return_value={'content_type': VhFeed.TYPE}))
    def test_no_injectable_carousels_more_appeared_in_result(self):
        assert self.get_response()['more']

    @mock.patch('smarttv.droideka.proxy.data_source.FeedDataSource.filter_categories_by_paging_info',
                mock.Mock(return_value=[test_main_category]))
    @mock.patch('smarttv.droideka.proxy.api.vh.client.feed',
                mock.Mock(return_value=carousel_generator.generate_vh_carousels_object(9, 10)))
    @mock.patch('smarttv.droideka.proxy.api.vh.client.carousels_videohub',
                mock.Mock(return_value=carousel_generator.generate_vh_carousel_response(10)))
    @mock.patch('smarttv.droideka.proxy.categories_provider.categories_provider.get_category',
                mock.Mock(return_value={'content_type': VhFeed.TYPE}))
    def test_one_injectable_carousel_more_appeared_in_result(self):
        assert self.get_response()['more']

    @mock.patch('smarttv.droideka.proxy.data_source.FeedDataSource.filter_categories_by_paging_info',
                mock.Mock(return_value=[test_main_category]))
    @mock.patch('smarttv.droideka.proxy.api.vh.client.feed',
                mock.Mock(return_value=carousel_generator.generate_vh_carousels_object(9, 10)))
    @mock.patch('smarttv.droideka.proxy.api.vh.client.carousels_videohub',
                mock.Mock(return_value={}))
    @mock.patch('smarttv.droideka.proxy.categories_provider.categories_provider.get_category',
                mock.Mock(return_value={'content_type': VhFeed.TYPE}))
    def test_one_injectable_carousel_did_not_come_more_appeared_in_result(self):
        assert self.get_response()['more']

    @mock.patch('smarttv.droideka.proxy.data_source.FeedDataSource.filter_categories_by_paging_info',
                mock.Mock(return_value=[test_main_category]))
    @mock.patch('smarttv.droideka.proxy.api.vh.client.feed',
                mock.Mock(return_value=carousel_generator.generate_vh_carousels_object(8, 10)))
    @mock.patch('smarttv.droideka.proxy.api.vh.client.carousels_videohub',
                mock.Mock(return_value={}))
    @mock.patch('smarttv.droideka.proxy.categories_provider.categories_provider.get_category',
                mock.Mock(return_value={'content_type': VhFeed.TYPE}))
    def test_one_injectable_carousel_feed_less_limit_more_not_presented(self):
        assert 'more' not in self.get_response()

    @mock.patch('smarttv.droideka.proxy.data_source.FeedDataSource.filter_categories_by_paging_info',
                mock.Mock(return_value=[]))
    @mock.patch('smarttv.droideka.proxy.api.vh.client.feed',
                mock.Mock(return_value=carousel_generator.generate_vh_carousels_object(9, 10)))
    @mock.patch('smarttv.droideka.proxy.categories_provider.categories_provider.get_category',
                mock.Mock(return_value={'content_type': VhFeed.TYPE}))
    def test_no_injectable_carousels_feed_less_limit_more_not_presented(self):
        assert 'more' not in self.get_response()


@mock.patch('smarttv.droideka.proxy.cache.budapest_device_ids', [test_quasar_id])
@mock.patch('smarttv.droideka.proxy.api.alice.client.device_info',
            mock.Mock(return_value=alice_for_business_mock_response))
@mock.patch('smarttv.droideka.proxy.api.vh.client.carousel_videohub',
            mock.Mock(return_value=carousel_generator.generate_vh_carousel_response(5)))
@mock.patch('smarttv.droideka.proxy.api.ott.client.selections',
            mock.Mock(return_value=carousel_generator.generate_kp_carousel_object(5)))
@mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.get_patching_carousel_ids',
            mock.Mock(return_value={PROMO_CAROUSEL_ID: PROMO_CAROUSEL_ID}))
@mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.get_internal_recommendations_carousel_id',
            mock.Mock(return_value=PROMO_CAROUSEL_ID))
@mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.extract_ott_experiment',
            mock.Mock(return_value=None))
@mock.patch('smarttv.droideka.proxy.tvm.tvm_client', mock.Mock(mock_tvm_client))
@mock.patch('smarttv.droideka.proxy.tvm.add_service_ticket', mock.Mock(return_value={}))
@pytest.mark.django_db
class TestPromoInjections:

    ZALOGIN_PROMO = 'promo_1'
    MUSIC_PROMO = 'promo_1'

    empty_music_promo_config = TSmartTvMusicPromoConfig()

    def get_response(self, carousel_id, headers):
        return client.get(
            path='/api/v7/carousel',
            data={
                'carousel_id': carousel_id,
                'limit': 5,
            },
            **headers
        ).json()

    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.load_memento_configs',
                mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.get_user_info',
                mock.Mock(return_value=UserInfoMock.user_with_no_subscription))
    @pytest.mark.parametrize('ott_experiment', [None, OttExperiment.YA_TV, OttExperiment.YA_TV_SPECIAL, OttExperiment.YA_TV_SPECIAL_MIX])
    def test_hotel_promo_exists(self, ott_experiment):
        with mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.extract_ott_experiment',
                        mock.Mock(return_value=ott_experiment)):
            headers = copy(required_android_headers)
            headers['HTTP_X_YAQUASARDEVICEID'] = test_quasar_id
            response = self.get_response(PROMO_CAROUSEL_ID, headers)

            assert response['includes'][0]['content_id'] == 'hotel_promo'

    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.load_memento_configs',
                mock.Mock(side_effect=None))
    def test_no_quasar_id_hotel_promo_does_not_exists(self):
        response = self.get_response(PROMO_CAROUSEL_ID, required_android_headers)
        assert response['includes'][0]['content_id'] != 'hotel_promo'

    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.load_memento_configs',
                mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.get_user_info',
                mock.Mock(return_value=UserInfoMock.anonymouse_user))
    @mock.patch('smarttv.droideka.proxy.data_source.get_promos',
                mock.Mock(return_value=[Promo(promo_id=ZALOGIN_PROMO, promo_type=Promo.ZALOGIN, content_type='action',
                                              thumbnail='https://some.thumb.com/fake/path')]))
    @mock.patch('smarttv.droideka.proxy.api.usaas.LazyExperiments.get_value',
                mock.Mock(return_value=ZALOGIN_PROMO))
    @pytest.mark.parametrize('ott_experiment', [None, OttExperiment.YA_TV, OttExperiment.YA_TV_SPECIAL, OttExperiment.YA_TV_SPECIAL_MIX])
    def test_zalogin_promo_exists(self, ott_experiment):
        with mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.extract_ott_experiment',
                        mock.Mock(return_value=ott_experiment)):
            response = self.get_response(PROMO_CAROUSEL_ID, required_android_headers)
            assert response['includes'][0]['content_id'] == f'zalogin/{self.ZALOGIN_PROMO}'

    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.load_memento_configs',
                mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.get_user_info',
                mock.Mock(return_value=UserInfoMock.user_with_no_subscription))
    @mock.patch('smarttv.droideka.proxy.data_source.get_promos',
                mock.Mock(return_value=[Promo(promo_id=ZALOGIN_PROMO, promo_type=Promo.ZALOGIN, content_type='action',
                                              thumbnail='https://some.thumb.com/fake/path')]))
    @mock.patch('smarttv.droideka.proxy.api.usaas.LazyExperiments.get_value',
                mock.Mock(return_value=ZALOGIN_PROMO))
    @pytest.mark.parametrize('ott_experiment', [None, OttExperiment.YA_TV, OttExperiment.YA_TV_SPECIAL, OttExperiment.YA_TV_SPECIAL_MIX])
    def test_user_authenticated_no_zalogin_promo(self, ott_experiment):
        with mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.extract_ott_experiment',
                        mock.Mock(return_value=ott_experiment)):
            headers = copy(required_android_headers)
            headers['HTTP_AUTHORIZATION'] = 'OAuth testoauthtoken'
            response = self.get_response(PROMO_CAROUSEL_ID, headers)
            assert 'zalogin' not in response['includes'][0]['content_id']

    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.load_memento_configs',
                mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.get_user_info',
                mock.Mock(return_value=UserInfoMock.user_with_no_subscription))
    @mock.patch('smarttv.droideka.proxy.data_source.get_promos',
                mock.Mock(return_value=[Promo(promo_id=MUSIC_PROMO, promo_type=Promo.MUSIC, content_type='action',
                                              thumbnail='https://some.thumb.com/fake/path')]))
    @mock.patch('smarttv.droideka.proxy.api.usaas.LazyExperiments.get_value',
                mock.Mock(return_value=MUSIC_PROMO))
    @pytest.mark.parametrize('ott_experiment', [None, OttExperiment.YA_TV, OttExperiment.YA_TV_SPECIAL, OttExperiment.YA_TV_SPECIAL_MIX])
    def test_unauthorized_user_no_music_promo(self, ott_experiment):
        with mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.extract_ott_experiment',
                        mock.Mock(return_value=ott_experiment)):
            headers = copy(required_android_headers)
            response = self.get_response(PROMO_CAROUSEL_ID, headers)
            assert 'music' not in response['includes'][0]['content_id']

    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View._user_info',
                mock.Mock(UserInfo('123', {}, 'someticket')))
    @mock.patch('smarttv.droideka.proxy.data_source.TopCarouselPromoDataSource.mark_music_promo_banner_shown',
                mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.get_user_info',
                mock.Mock(return_value=UserInfoMock.user_with_no_subscription))
    @mock.patch('smarttv.droideka.proxy.data_source.get_promos',
                mock.Mock(return_value=[Promo(promo_id=MUSIC_PROMO, promo_type=Promo.MUSIC, content_type='action',
                                              thumbnail='https://some.thumb.com/fake/path')]))
    @mock.patch('smarttv.droideka.proxy.api.usaas.LazyExperiments.get_value',
                mock.Mock(side_effect=lambda key: 'promo_1' if key == 'music_promo_types' else None))
    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.load_memento_configs',
                mock.Mock(return_value=MementoConfigs(music_promo_config=empty_music_promo_config)))
    @pytest.mark.parametrize('ott_experiment', [None, OttExperiment.YA_TV, OttExperiment.YA_TV_SPECIAL, OttExperiment.YA_TV_SPECIAL_MIX])
    def test_authorized_user_has_music_promo(self, ott_experiment):
        with mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.extract_ott_experiment',
                        mock.Mock(return_value=ott_experiment)):
            headers = copy(required_android_headers)
            headers['HTTP_AUTHORIZATION'] = 'OAuth testoauthtoken'
            # баннер музыки работает на 1.5
            headers['HTTP_USER_AGENT'] = 'com.yandex.tv.home/1.5.773 (Realtek SmartTV; Android 7.1.1)'
            response = self.get_response(PROMO_CAROUSEL_ID, headers)
            assert 'music' in response['includes'][0]['content_id']

    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View._user_info',
                mock.Mock(UserInfo('123', {}, 'someticket')))
    @mock.patch('smarttv.droideka.proxy.data_source.TopCarouselPromoDataSource.mark_music_promo_banner_shown',
                mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.get_user_info',
                mock.Mock(return_value=UserInfoMock.user_with_no_subscription))
    @mock.patch('smarttv.droideka.proxy.data_source.get_promos',
                mock.Mock(return_value=[Promo(promo_id=MUSIC_PROMO, promo_type=Promo.MUSIC, content_type='action',
                                              thumbnail='https://some.thumb.com/fake/path')]))
    @mock.patch('smarttv.droideka.proxy.api.usaas.LazyExperiments.get_value',
                mock.Mock(side_effect=lambda key: 'promo_1' if key == 'music_promo_types' else None))
    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.extract_ott_experiment', mock.Mock(return_value=OttExperiment.YA_TV))
    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.load_memento_configs',
                mock.Mock(return_value=MementoConfigs(music_promo_config=empty_music_promo_config)))
    @pytest.mark.parametrize('is_promo_expired, has_promo',
                             [(False, True),
                              (True, False), ])
    def test_music_promo_expiration(self, is_promo_expired, has_promo):
        with mock.patch('smarttv.droideka.proxy.data_source.TopCarouselPromoDataSource.is_music_promo_expired',
                        mock.Mock(return_value=is_promo_expired)):
            headers = copy(required_android_headers)
            headers['HTTP_AUTHORIZATION'] = 'OAuth testoauthtoken'
            # баннер музыки работает на 1.5
            headers['HTTP_USER_AGENT'] = 'com.yandex.tv.home/1.5.773 (Realtek SmartTV; Android 7.1.1)'
            response = self.get_response(PROMO_CAROUSEL_ID, headers)
            promo_actually_presented = 'music' in response['includes'][0]['content_id']
            assert has_promo == promo_actually_presented

    @pytest.mark.parametrize('first_show_time_ts, time_delta, is_expired',
                             [
                                 (0, datetime.timedelta(minutes=0), False),
                                 (0, datetime.timedelta(minutes=48 * 60 - 1), False),
                                 (0, datetime.timedelta(minutes=48 * 60), False),
                                 (0, datetime.timedelta(minutes=48 * 60 + 1), False),
                                 (100, datetime.timedelta(minutes=0), False),
                                 (100, datetime.timedelta(minutes=48 * 60 - 1), False),  # one minute before expiration
                                 (100, datetime.timedelta(minutes=48 * 60), True),  # exactly 2 days after after first time
                                 (100, datetime.timedelta(minutes=48 * 60 + 1), True),  # 1 minute after expiration
                             ])
    def test_is_music_promo_expired(self, first_show_time_ts, time_delta, is_expired):
        current_date_time = datetime.datetime.fromtimestamp(first_show_time_ts) + time_delta
        actual_result = data_source.TopCarouselPromoDataSource(None).is_music_promo_expired(
            first_show_time_ts, current_date_time.timestamp())
        assert is_expired == actual_result

    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.load_memento_configs',
                mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.get_user_info',
                mock.Mock(return_value=UserInfoMock.user_with_no_subscription))
    @mock.patch('smarttv.droideka.proxy.data_source.get_promos',
                mock.Mock(return_value=[Promo(promo_id=MUSIC_PROMO, promo_type=Promo.MUSIC, content_type='action',
                                              thumbnail='https://some.thumb.com/fake/path')]))
    @mock.patch('smarttv.droideka.proxy.api.usaas.LazyExperiments.get_value',
                mock.Mock(side_effect=lambda key: 'promo_1' if key == 'music_promo_types' else None))
    @pytest.mark.parametrize('ott_experiment', [None, OttExperiment.YA_TV, OttExperiment.YA_TV_SPECIAL, OttExperiment.YA_TV_SPECIAL_MIX])
    def test_authorized_user_have_no_music_promo_if_client_version_is_old(self, ott_experiment):
        with mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.extract_ott_experiment',
                        mock.Mock(return_value=ott_experiment)):
            headers = copy(required_android_headers)
            headers['HTTP_AUTHORIZATION'] = 'OAuth testoauthtoken'
            # баннер музыки не показывается на 1.4
            headers['HTTP_USER_AGENT'] = 'com.yandex.tv.home/1.4.773 (Realtek SmartTV; Android 7.1.1)'
            response = self.get_response(PROMO_CAROUSEL_ID, headers)
            assert 'music' not in response['includes'][0]['content_id']


@pytest.mark.django_db
class TestCarouselsOtt:
    VH_MAIN_CATEGORY_ID_WITH_MUSIC = 'main'
    MAIN_CATEGORY_ID_WITH_MUSIC = 'test_ott_music'
    MAIN_CATEGORY_ID_WITHOUT_MUSIC = 'test_ott_no_music'
    MUSIC_CAROUSEL_ID = 'main_music'

    def get_response(self, category_id, offset, headers):
        return client.get(
            path='/api/v7/carousels',
            data={
                'category_id': category_id,
                'offset': offset,
                'limit': 5,
            },
            **headers
        ).json()

    @classmethod
    def create_categories(cls):
        platform_any = PlatformModel(platform_type=PlatformType.ANY)
        platform_any.save()
        platform_none = PlatformModel(platform_type=PlatformType.NONE)
        platform_none.save()
        platform_1_5 = PlatformModel(platform_type=PlatformType.ANDROID, app_version='1.5')
        platform_1_5.save()

        main_feed_category_with_music = Category2(id=10, category_id=cls.MAIN_CATEGORY_ID_WITH_MUSIC, title='Главная',
                                                  rank=1, position=1, content_type=KpCategory.TYPE)
        main_feed_category_with_music.save()
        main_feed_category_without_music = Category2(id=20, category_id=cls.MAIN_CATEGORY_ID_WITHOUT_MUSIC,
                                                     title='Главная', position=1, rank=1, content_type=KpCategory.TYPE)
        main_feed_category_without_music.save()
        music_carousel = Category2(id=30, category_id=cls.MUSIC_CAROUSEL_ID, title='Музыка', rank=1,
                                   content_type=MusicCarousel.TYPE, parent_category_id=10, position=1,
                                   authorization_required=True)
        music_carousel.save()

        main_feed_category_with_music.above_platforms.add(platform_none)
        main_feed_category_with_music.below_platforms.add(platform_none)
        main_feed_category_with_music.include_platforms.add(platform_any)
        main_feed_category_with_music.exclude_platforms.add(platform_none)

        main_feed_category_without_music.above_platforms.add(platform_none)
        main_feed_category_without_music.below_platforms.add(platform_none)
        main_feed_category_without_music.include_platforms.add(platform_any)
        main_feed_category_without_music.exclude_platforms.add(platform_none)

        music_carousel.above_platforms.add(platform_1_5)
        music_carousel.below_platforms.add(platform_none)
        music_carousel.include_platforms.add(platform_none)
        music_carousel.exclude_platforms.add(platform_none)

    @classmethod
    def delete_categories(cls):
        Category2.objects.all().delete()
        PlatformModel.objects.all().delete()

    @pytest.fixture(autouse=True)
    def categories_fixture(self):
        self.create_categories()
        yield
        self.delete_categories()

    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.load_memento_configs',
                mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.api.ott.client.selections',
                mock.Mock(return_value=carousel_generator.generate_kp_carousel_object(5)))
    @mock.patch('smarttv.droideka.proxy.tvm.add_service_ticket', mock.Mock(return_value={}))
    @mock.patch('smarttv.droideka.proxy.api.music.client.infinite_feed',
                mock.Mock(return_value=carousel_generator.generate_music_infinite_feed_response(5, 10)))
    @mock.patch('smarttv.droideka.proxy.api.ott.client.multi_selections',
                mock.Mock(return_value=carousel_generator.generate_kp_feed_response(5, 10)))
    @mock.patch('smarttv.droideka.proxy.api.usaas.LazyExperiments.get_value', mock.Mock(return_value=None))
    @mock.patch('smarttv.droideka.proxy.views.base.APIView.get_user_info',
                mock.Mock(return_value=UserInfo('1', {}), side_effect=None))
    @mock.patch('smarttv.droideka.proxy.views.base.APIView.process_user_profile',
                mock.Mock(side_effect=make_user_authorized))
    @mock.patch('smarttv.droideka.utils.RequestInfo', mock.Mock(return_value=RequestInfo(True, ExperimentsMock({}))))
    def test_music_carousel_presented(self):
        headers = copy(required_android_headers)
        headers['HTTP_USER_AGENT'] = 'com.yandex.tv.home/1.5.773 (Realtek SmartTV; Android 7.1.1)'

        carousels_response = self.get_response(self.MAIN_CATEGORY_ID_WITH_MUSIC, 0, headers)

        assert self.MUSIC_CAROUSEL_ID == carousels_response['carousels'][0]['carousel_id']

    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.load_memento_configs',
                mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.tvm.add_service_ticket', mock.Mock(return_value={}))
    @mock.patch('smarttv.droideka.proxy.api.music.client.infinite_feed',
                mock.Mock(return_value=carousel_generator.generate_music_infinite_feed_response(5, 10)))
    @mock.patch('smarttv.droideka.proxy.api.vh.client.feed',
                mock.Mock(return_value=carousel_generator.generate_vh_carousels_object(10, 10)))
    @mock.patch('smarttv.droideka.proxy.views.base.APIView.get_user_info',
                mock.Mock(return_value=UserInfo('1', {}), side_effect=None))
    @mock.patch('smarttv.droideka.proxy.views.base.APIView.process_user_profile',
                mock.Mock(side_effect=make_user_authorized))
    @mock.patch('smarttv.droideka.proxy.api.usaas.LazyExperiments', mock.Mock(return_value=ExperimentsMock({'music_carousel_position': 4})))
    @mock.patch('smarttv.droideka.utils.RequestInfo', mock.Mock(return_value=RequestInfo(True, ExperimentsMock({'music_carousel_position': 4}))))
    def test_music_carousel_position(self):
        vh_main_feed_category_with_music = Category2(id=10, category_id=self.VH_MAIN_CATEGORY_ID_WITH_MUSIC,
                                                     title='Главная',
                                                     rank=1, position=1, content_type=VhFeed.TYPE)
        vh_main_feed_category_with_music.save()
        vh_music_carousel = Category2(id=30, category_id=self.MUSIC_CAROUSEL_ID, title='Музыка', rank=1,
                                      content_type=MusicCarousel.TYPE, parent_category_id=10, position=1,
                                      authorization_required=True)
        vh_music_carousel.save()

        headers = copy(required_android_headers)
        headers['HTTP_USER_AGENT'] = 'com.yandex.tv.home/1.5.773 (Realtek SmartTV; Android 7.1.1)'

        carousels_response = self.get_response(self.VH_MAIN_CATEGORY_ID_WITH_MUSIC, 0, headers)

        assert self.MUSIC_CAROUSEL_ID != carousels_response['carousels'][0]['carousel_id']
        assert self.MUSIC_CAROUSEL_ID == carousels_response['carousels'][3]['carousel_id']

    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.load_memento_configs',
                mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.api.ott.client.selections',
                mock.Mock(return_value=carousel_generator.generate_kp_carousel_object(5)))
    @mock.patch('smarttv.droideka.proxy.tvm.add_service_ticket', mock.Mock(return_value={}))
    @mock.patch('smarttv.droideka.proxy.api.music.client.infinite_feed',
                mock.Mock(return_value=carousel_generator.generate_music_infinite_feed_response(5, 10)))
    @mock.patch('smarttv.droideka.proxy.api.ott.client.multi_selections',
                mock.Mock(return_value=carousel_generator.generate_kp_feed_response(5, 10)))
    @mock.patch('smarttv.droideka.proxy.api.usaas.LazyExperiments.get_value', mock.Mock(return_value=None))
    @mock.patch('smarttv.droideka.proxy.views.base.APIView.process_user_profile',
                mock.Mock(side_effect=make_user_unauthorized))
    @mock.patch('smarttv.droideka.utils.RequestInfo', mock.Mock(return_value=RequestInfo(False, ExperimentsMock({}))))
    def test_not_authorized_music_carousel_not_presented(self):
        headers = copy(required_android_headers)
        headers['HTTP_USER_AGENT'] = 'com.yandex.tv.home/1.5.774 (Realtek SmartTV; Android 7.1.2)'

        carousels_response = self.get_response(self.MAIN_CATEGORY_ID_WITH_MUSIC, 0, headers)

        assert self.MUSIC_CAROUSEL_ID != carousels_response['carousels'][0]['carousel_id']

    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.load_memento_configs',
                mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.api.ott.client.selections',
                mock.Mock(return_value=carousel_generator.generate_kp_carousel_object(5)))
    @mock.patch('smarttv.droideka.proxy.tvm.add_service_ticket', mock.Mock(return_value={}))
    @mock.patch('smarttv.droideka.proxy.api.music.client.infinite_feed',
                mock.Mock(return_value=carousel_generator.generate_music_infinite_feed_response(5, 10)))
    @mock.patch('smarttv.droideka.proxy.api.ott.client.multi_selections',
                mock.Mock(return_value=carousel_generator.generate_kp_feed_response(5, 10)))
    @mock.patch('smarttv.droideka.proxy.api.usaas.LazyExperiments.get_value', mock.Mock(return_value=None))
    @mock.patch('smarttv.droideka.proxy.views.base.APIView.get_user_info', mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.views.base.APIView.process_user_profile',
                mock.Mock(side_effect=make_user_authorized))
    def test_not_in_range_music_carousel_not_presented(self):
        headers = copy(required_android_headers)
        headers['HTTP_USER_AGENT'] = 'com.yandex.tv.home/1.5.773 (Realtek SmartTV; Android 7.1.1)'

        carousels_response = self.get_response(self.MAIN_CATEGORY_ID_WITH_MUSIC, 1, headers)

        assert self.MUSIC_CAROUSEL_ID != carousels_response['carousels'][0]['carousel_id']

    @mock.patch('smarttv.droideka.proxy.views.carousels.CarouselV5View.load_memento_configs',
                mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.api.ott.client.selections',
                mock.Mock(return_value=carousel_generator.generate_kp_carousel_object(5)))
    @mock.patch('smarttv.droideka.proxy.tvm.add_service_ticket', mock.Mock(return_value={}))
    @mock.patch('smarttv.droideka.proxy.api.music.client.infinite_feed',
                mock.Mock(return_value=carousel_generator.generate_music_infinite_feed_response(5, 10)))
    @mock.patch('smarttv.droideka.proxy.api.ott.client.multi_selections',
                mock.Mock(return_value=carousel_generator.generate_kp_feed_response(5, 10)))
    @mock.patch('smarttv.droideka.proxy.api.usaas.LazyExperiments.get_value', mock.Mock(return_value=None))
    @mock.patch('smarttv.droideka.proxy.views.base.APIView.get_user_info', mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.views.base.APIView.process_user_profile',
                mock.Mock(side_effect=make_user_authorized))
    def test_old_version_music_carousel_not_presented(self):
        headers = copy(required_android_headers)
        headers['HTTP_USER_AGENT'] = 'com.yandex.tv.home/1.4.773 (Realtek SmartTV; Android 7.1.1)'

        carousels_response = self.get_response(self.MAIN_CATEGORY_ID_WITH_MUSIC, 0, headers)

        assert self.MUSIC_CAROUSEL_ID != carousels_response['carousels'][0]['carousel_id']
