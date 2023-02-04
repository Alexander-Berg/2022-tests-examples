from copy import copy

import pytest
from alice.protos.data.search_result.search_result_pb2 import TSearchResultData
from alice.protos.data.video.video_pb2 import TVideoItem
from google.protobuf import json_format
from smarttv.droideka.proxy.models import Category2
from smarttv.droideka.proxy.serializers.fast_response import (
    BaseSerializer,
    CinemaSerializer,
    EpisodeInfo,
    FullCardDetail,
    KpDocumentSerializer,
    MusicInfiniteFeedSerializer,
    PromoHotelSerializer,
    PromoMusicSerializer,
    ThinCardDetailsInfo,
)
from smarttv.droideka.proxy.serializers.protobuf import (
    ClipsSerializer,
    VhCarouselAsSearchResultSerializer,
)
from smarttv.droideka.proxy.serializers.response import CategorySerializer
from smarttv.droideka.tests import mock
from smarttv.droideka.utils import PlatformInfo, PlatformType

KEY_PERSISTENT_CLIENT_CATEGORY_ID = 'persistent_client_category_id'


def modify(x):
    return x**2


class TstSerializer(BaseSerializer):
    copy_field = 'copy_field'
    modify_field = 'field'
    rename_field_before = 'from'
    rename_field_after = 'to'

    fields_to_copy = [copy_field]
    fields_to_modify = {modify_field: modify}
    fields_to_rename = {rename_field_before: rename_field_after}


class TestBaseSerializer:
    def test_filtration(self):
        episode = 1

        data = {
            EpisodeInfo.FIELD_EPISODE_NUMBER: episode,
            EpisodeInfo.FIELD_PARENT_ID: None
        }
        result = EpisodeInfo().serialize(data)

        assert EpisodeInfo.FIELD_EPISODE_NUMBER in result and result[
            EpisodeInfo.FIELD_EPISODE_NUMBER] == episode, result
        assert EpisodeInfo.FIELD_PARENT_ID not in result, 'filter_none_items not working'

    def test_serialization(self):
        serializer = TstSerializer()

        copy_value = 1
        rename_value = 2
        modify_value = 3
        data = {
            serializer.copy_field: copy_value,
            serializer.rename_field_before: rename_value,
            serializer.modify_field: modify_value

        }
        result = serializer.serialize(data)

        assert result[serializer.copy_field] == copy_value
        # noinspection PyCallByClass
        assert result[serializer.modify_field] == modify(modify_value)
        assert result[serializer.rename_field_after] == rename_value, result


class TestEpisodeInfo:
    serializer = EpisodeInfo()

    def test_one_color(self):
        main_color = 1

        data = {EpisodeInfo.FIELD_MAIN_COLOR: main_color}
        result = self.serializer.serialize(data)

        assert EpisodeInfo.FIELD_MAIN_COLOR in result
        assert result[EpisodeInfo.FIELD_MAIN_COLOR] == main_color

    def test_both_colors(self):
        main_color = 1
        extra_color = 2

        data = {
            EpisodeInfo.FIELD_MAIN_COLOR: main_color,
            EpisodeInfo.FIELD_AUTO_FIELDS: {
                EpisodeInfo.FIELD_SMARTTV_ICON_COLOR: extra_color
            }
        }

        result = self.serializer.serialize(data)

        assert EpisodeInfo.FIELD_MAIN_COLOR in result
        assert result[EpisodeInfo.FIELD_MAIN_COLOR] == extra_color


class TestThinCardDetailsInfo:
    @pytest.fixture
    def data(self):
        return {
            'content_id': 'xxx',
            'content_detail': {
                'key': 'value',
                'onto_poster': '//ya.ru',
            }
        }

    def test_poster_with_schema_added_in_result(self, data):
        # we add https:// prefix to VH's response poster address
        serialized = ThinCardDetailsInfo().serialize(data)
        assert serialized['poster'] == 'https://ya.ru'

    def test_input_data_has_poster_poster_is_propagated(self, data):
        data['content_detail']['onto_poster'] = 'http://some.ru'
        assert ThinCardDetailsInfo().serialize(data)['poster'] == 'http://some.ru'

    def test_input_data_has_empty_poster_no_poster_field_in_result(self, data):
        data['content_detail']['onto_poster'] = None
        assert 'poster' not in ThinCardDetailsInfo().serialize(data)

    def test_input_data_has_no_poster_and_so_it_result(self, data):
        del data['content_detail']['onto_poster']
        assert 'poster' not in ThinCardDetailsInfo().serialize(data)


class TestMusicInfiniteFeedSerializer:
    @pytest.fixture
    def serializer(self):
        return MusicInfiniteFeedSerializer()

    def playlist_entity(self):
        return {
            'type': 'playlist',
            'data': {
                'uid': 100,
                'kind': 500,
                'title': 'Русский поп: открытия',
                'cover': {
                    'uri': 'avatars.yandex.net/get-music-user-playlist/71140/qquadybyrbRoZo/%%?1617203735672',
                }
            }
        }

    def album_entity(self):
        return {
            'type': 'album',
            'data': {
                'coverUri': 'avatars.yandex.net/get-music-content/4747389/eca2c693.a.12493873-2/%%',
                'id': 12493873,
                'ogImage': 'avatars.yandex.net/get-music-content/4747389/eca2c693.a.12493873-2/%%',
                'title': 'Внутри секты',
                'type': 'podcast',
                'year': '2020'
            },
        }

    def artist_entity(self):
        return {
            'type': 'artist',
            'data': {
                'id': '5781113',
                'name': 'MORGENSHTERN',
                'uri': 'avatars.mds.yandex.net/get-music-content/3927581/b2264449.p.5781113/%%',
                'genres': ['Русский рэп']
            }
        }

    def invalid_entity(self):
        return {
            'type': 'computer',
        }

    def valid_row(self):
        return {
            'rowId': 'xx',
            'title': 'yy',
            'typeForFrom': 'zz',
            'entities': [
                self.playlist_entity(),
                self.artist_entity(),
            ],
        }

    def empty_row(self):
        return {
            'rowId': 'xx',
            'title': 'yy',
            'typeForFrom': 'zz',
            'entities': [],
        }

    def music_response(self):
        return {
            'result': {
                'rows': []
            },
        }

    def test_single_row_nonsense_input(self, serializer):
        # unknown data types is fine, but invalid
        assert serializer.serialize_single_row(None) == {}
        assert serializer.serialize_single_row({}) == {}

        # what if entities is not an array?
        row = self.valid_row()
        row['entities'] = 'nonsense'
        assert serializer.serialize_single_row({}) == {}

    def test_single_row_without_entities(self, serializer):
        # empty entities is invalid
        assert serializer.serialize_single_row(self.empty_row()) == {}

    def test_single_row_some_entities_are_invalid(self, serializer):
        # invalid entities are not included in answer
        row = self.empty_row()
        row['entities'] = [
            self.playlist_entity(),
            self.invalid_entity(),
        ]

        result = serializer.serialize_single_row(row)
        assert len(result['includes']) == 1

    def test_single_row_forced_title_inserted(self, serializer):
        row = self.valid_row()

        result = serializer.serialize_single_row(row, title='Forced')
        assert result['title'] == 'Forced'

    def test_single_row_all_entities_are_invalid(self, serializer):
        # if all entities are invalid, whole row is invalid
        row = self.empty_row()
        row['entities'] = [
            self.invalid_entity(),
            self.invalid_entity(),
        ]
        assert serializer.serialize_single_row(row) == {}

    def test_serialize_invalid_inputs(self, serializer):
        # nonsense type as input
        assert serializer.serialize(None) == {}
        assert serializer.serialize({}) == {}

        # if we can not serialize any row - it's invalid
        data = self.music_response()
        data['result']['rows'] = [
            self.empty_row(),
            self.empty_row(),
        ]
        assert serializer.serialize(data) == {}

        # if we can serialize *some* rows, let's do it
        data = self.music_response()
        data['result']['rows'] = [
            self.valid_row(),  # valid rows will be in answer
            self.valid_row(),
            self.empty_row(),  # invalid row is skipped
        ]
        assert len(serializer.serialize(data)['carousels']) == 2

    def test_valid_row_serialization(self, serializer):
        valid_row = self.valid_row()
        result = serializer.serialize_single_row(valid_row)
        assert result['carousel_id'] == valid_row['rowId']
        assert len(result['includes']) == len(valid_row['entities'])

    def test_category_serialization(self, serializer):
        resp = self.music_response()
        resp['result']['rows'] = [
            self.valid_row(),
            self.valid_row(),
        ]
        result = serializer.serialize(resp)
        assert isinstance(result['carousels'], list)
        assert len(result['carousels']) == 2

    def test_medium_size_serialization_flag(self, serializer: MusicInfiniteFeedSerializer):
        row = self.valid_row()
        result = serializer.serialize_single_row(row, medium_size=True)
        assert result['carousel_type'] == MusicInfiniteFeedSerializer.SQUARE_MEDIUM

    def test_only_frist_square_is_big(self, serializer: MusicInfiniteFeedSerializer):
        response = self.music_response()
        response['result']['rows'] = [
            self.valid_row(),
            self.valid_row()
        ]
        result = serializer.serialize(response)

        assert result['carousels'][0]['carousel_type'] == MusicInfiniteFeedSerializer.SQUARE_MEDIUM
        assert result['carousels'][1]['carousel_type'] == MusicInfiniteFeedSerializer.SQUARE_SMALL

    def test_album_entity_serialization(self, serializer: MusicInfiniteFeedSerializer):
        result = serializer.serialize_entity(self.album_entity(), 'testfrom')
        assert result['content_id'] == 'Album-12493873'
        assert result['title']
        assert len(result['thumbnails']) == 2
        assert '400x400' in result['thumbnails']
        assert 'orig' in result['thumbnails']
        assert b'12493873' in result['alice_callback']

    def test_artist_entity_serialization(self, serializer: MusicInfiniteFeedSerializer):
        result = serializer.serialize_entity(self.artist_entity(), 'testfrom')
        assert result['content_id'] == 'Artist-5781113'
        assert result['title'] == 'MORGENSHTERN'
        assert len(result['thumbnails']) == 2
        assert '400x400' in result['thumbnails']
        assert 'orig' in result['thumbnails']
        assert b'5781113' in result['alice_callback']

    def test_serialize_empty_cover_playlist(self, serializer: MusicInfiniteFeedSerializer):
        playlist = self.playlist_entity()
        playlist['data']['cover'] = {'error': 'cover does not exist'}
        result = serializer.serialize_entity(playlist, 'testfrom')
        assert result['thumbnails']['orig'] == 'http://ya.ru/bucket/orig'

    def test_serialize_empty_cover_artist(self, serializer: MusicInfiniteFeedSerializer):
        artist = self.artist_entity()
        del artist['data']['uri']
        result = serializer.serialize_entity(artist, 'testfrom')
        assert result['thumbnails']['orig'] == 'http://ya.ru/bucket/orig'


class TestMusicInfiniteFeedSerializerSf:
    @pytest.fixture
    def serializer(self):
        return MusicInfiniteFeedSerializer()

    def playlist_item(self):
        return {
            'type': 'auto-playlist',
            'data': {
                'uid': 100,
                'kind': 500,
            }
        }

    def artist_item(self):
        return {
            'type': 'artist',
            'data': {
                'id': '3149405'
            }
        }

    def radio_item(self):
        return {
            'type': 'radio',
            'data': {
                'id': '3149405'
            }
        }

    def test_object_id(self, serializer: MusicInfiniteFeedSerializer):
        assert serializer.object_id(self.playlist_item()) == '100:500'
        assert serializer.object_id(self.artist_item()) == '3149405'

    def test_object_type(self, serializer: MusicInfiniteFeedSerializer):
        assert serializer.object_type(self.playlist_item()) == 'Playlist'
        assert serializer.object_type(self.artist_item()) == 'Artist'

    def test_get_purpose(self, serializer: MusicInfiniteFeedSerializer):
        assert serializer.get_purpose(self.radio_item()) == 'play_radio'
        assert serializer.get_purpose(self.artist_item()) == 'play_music'


class TestKpDocumentSerializer:
    serializer = KpDocumentSerializer()

    @pytest.mark.parametrize('input_obj, expected_result', mock.kp_document_serializer_licence_info_mocks)
    def test_fill_licence_info(self, input_obj, expected_result):
        result = {}
        self.serializer.fill_licence_info(input_obj, result)
        assert expected_result == result


class TestPromoHotelSerializer:
    @pytest.fixture
    def serializer(self):
        return PromoHotelSerializer()

    def test_deeplink_encoding(self, serializer: PromoHotelSerializer):
        assert serializer.action_deeplink('ya.ru') == 'home-app://open_url?url=ya.ru'
        assert serializer.action_deeplink('http://ya.ru') == 'home-app://open_url?url=http%3A%2F%2Fya.ru'

    def test_dont_raise_exceptions(self, serializer: PromoHotelSerializer):
        # dont throw exception if can't serialize card data
        data = {'some': 'irrelevant'}
        assert serializer.serialize(data) == {}

    def test_thumbnails(self, serializer: PromoHotelSerializer):
        data = {'imageUrl': 'http://s3.ya.ru/bucket/%%'}
        result = {}
        serializer.fill_thumbnails(result, data)
        assert result['thumbnails']['orig'] == 'http://s3.ya.ru/bucket/orig'


class TestPromoMusicSerializer:
    @pytest.fixture
    def serializer(self):
        return PromoMusicSerializer()

    def test(self, serializer: PromoMusicSerializer):
        obj = {
            'auto_fields': {'promo_type': 'music'},
            'content_id': 'music/music_launch_1',
            'action': 'home-app://category?category_id=music',
            'content_type': 'action',
            'thumbnail': 'https://avatars.mds.yandex.net/get-dialogs/758954/music-default-cover/orig',
            'title': 'Title',
        }
        context = {
            'platform_info': PlatformInfo()
        }
        result = serializer.serialize(obj, context)
        assert result['title'] == obj['title']
        assert result['thumbnails']['orig'] == obj['thumbnail']
        assert result['subtitle'] == ''
        assert result['action'] == obj['action']
        assert 'auto_fields' not in result


class TestAdconfigSerializer:
    serializer = ThinCardDetailsInfo()

    def test_ad_config_presented(self):
        result = self.serializer.serialize(
            {ThinCardDetailsInfo.FIELD_CONTENT_ID: '1', ThinCardDetailsInfo.FIELD_CONTENT:
                {ThinCardDetailsInfo.FIELD_AD_CONFIG: {ThinCardDetailsInfo.FIELD_PARTNER_ID: 1},
                 ThinCardDetailsInfo.FIELD_CONTENT_ID: '1'}},
            {ThinCardDetailsInfo.NEED_AD_CONFIG: True})
        assert ThinCardDetailsInfo.FIELD_AD_CONFIG in result

    @pytest.mark.parametrize('document, context', [
        ({ThinCardDetailsInfo.FIELD_CONTENT_ID: '1', ThinCardDetailsInfo.FIELD_CONTENT:
            {ThinCardDetailsInfo.FIELD_AD_CONFIG: {ThinCardDetailsInfo.FIELD_PARTNER_ID: 1},
             ThinCardDetailsInfo.FIELD_CONTENT_ID: '1'}}, {ThinCardDetailsInfo.NEED_AD_CONFIG: False}),
        ({ThinCardDetailsInfo.FIELD_CONTENT_ID: '1', ThinCardDetailsInfo.FIELD_CONTENT:
            {ThinCardDetailsInfo.FIELD_AD_CONFIG: {ThinCardDetailsInfo.FIELD_PARTNER_ID: 0},
             ThinCardDetailsInfo.FIELD_CONTENT_ID: '1'}}, {ThinCardDetailsInfo.NEED_AD_CONFIG: False}),
        ({ThinCardDetailsInfo.FIELD_CONTENT_ID: '1', ThinCardDetailsInfo.FIELD_CONTENT:
            {ThinCardDetailsInfo.FIELD_AD_CONFIG: {ThinCardDetailsInfo.FIELD_PARTNER_ID: 0},
             ThinCardDetailsInfo.FIELD_CONTENT_ID: '1'}}, {ThinCardDetailsInfo.NEED_AD_CONFIG: True}),
        ({ThinCardDetailsInfo.FIELD_CONTENT_ID: '1', ThinCardDetailsInfo.FIELD_CONTENT:
            {ThinCardDetailsInfo.FIELD_CONTENT_ID: '1'}}, {ThinCardDetailsInfo.NEED_AD_CONFIG: True}),
    ])
    def test_ad_config_not_presented(self, document, context):
        result = self.serializer.serialize(document, context)
        assert ThinCardDetailsInfo.FIELD_AD_CONFIG not in result


class TestClipsSerializer:

    @pytest.mark.parametrize('input_iframe, expected_result', [
        ({"players": {"autoplay": {"html":
                                       "<iframe src=\"//ok.ru/videoembed/318693247689?autoplay=1&amp;ya=1\" frameborder"
                                       "=\"0\" scrolling=\"no\" allowfullscreen=\"1\" allow=\"autoplay; fullscreen; acc"
                                       "elerometer; gyroscope; picture-in-picture\" aria-label=\"Video\"></iframe>"}}},
         'https://ok.ru/videoembed/318693247689?autoplay=1&ya=1'),
        ({"players": {"autoplay": {"html": "<iframe src=\"//www.youtube.com/embed/9FR_B-73ASo?autoplay=1&amp;enablejsap"
                                           "i=1&amp;wmode=opaque\" frameborder=\"0\" scrolling=\"no\" allowfullscreen="
                                           "\"1\" allow=\"autoplay; fullscreen; accelerometer; gyroscope; picture-in-pi"
                                           "cture\" aria-label=\"Video\"></iframe>"}}},
         'https://www.youtube.com/embed/9FR_B-73ASo?autoplay=1&enablejsapi=1&wmode=opaque'),
        ({"players": {"autoplay": {"html": "<iframe src=\"//vk.com/video_ext.php?autoplay=1&amp;hash=61426caff3b58070&a"
                                           "mp;id=456239153&amp;loop=0&amp;oid=-148595028\" frameborder=\"0\" scrolling"
                                           "=\"no\" allowfullscreen=\"1\" allow=\"autoplay; fullscreen; accelerometer; "
                                           "gyroscope; picture-in-picture\" aria-label=\"Video\"></iframe>"}}},
         'https://vk.com/video_ext.php?autoplay=1&hash=61426caff3b58070&id=456239153&loop=0&oid=-148595028'),
        (
        {"players": {"autoplay": {"html": "<iframe src=\"//дима.рф/вася?url=&lt;&gt;%&amp;some\" frameborder=\"0\" scro"
                                          "lling=\"no\" allowfullscreen=\"1\" allow=\"autoplay; fullscreen; accelerometer; "
                                          "gyroscope; picture-in-picture\" aria-label=\"Video\"></iframe>"}}},
        'https://дима.рф/вася?url=<>%&some'),
        ({"players": {"autoplay": {"html": None}}}, ''),
        ({"players": {"autoplay": {}}}, ''),
        ({"players": {"autoplay": None}}, ''),
        ({"players": {}}, ''),
        ({"players": None}, ''),
        ({}, ''),
        (None, ''),
    ])
    def test_embedded_uri_correct_serialized(self, input_iframe, expected_result):
        item = TVideoItem()
        ClipsSerializer()._fill_embedded_uri(input_iframe, item)

        assert expected_result == item.EmbedUri


class TestCategoriesSerializer:
    TEST_PERSISTENT_CLIENT_CATEGORY_ID = 'test_client_id'

    def perform_test(self, app_version: str, need_presence: bool):
        category_to_serialize = Category2(
            id=1,
            category_id='test',
            persistent_client_category_id=self.TEST_PERSISTENT_CLIENT_CATEGORY_ID
        )

        context = {'platform_info': PlatformInfo(PlatformType.ANDROID, app_version=app_version)}
        serializer = CategorySerializer(category_to_serialize, context=context)

        if need_presence:
            assert serializer.data[KEY_PERSISTENT_CLIENT_CATEGORY_ID] == 'test_client_id'
        else:
            assert KEY_PERSISTENT_CLIENT_CATEGORY_ID not in serializer.data

    @pytest.mark.parametrize('app_version', [
        '1.87.1.3467',
        '1.87.1.3468',
        '1.87.2.3467',
        '1.88.1.3467',
        '2.87.1.3467',
        '1.87.2.3466',
        '1.88.1.3467',
        '2.86.2.3467',
    ])
    def test_serialize_persistent_client_category_id_presented(self, app_version):
        self.perform_test(app_version, need_presence=True)

    @pytest.mark.parametrize('app_version', [
        '1.87.1.3466',
        '1.87.0.3467',
        '1.86.1.3467',
        '0.87.1.3467',
    ])
    def test_serialize_persistent_client_category_id_not_presented(self, app_version):
        self.perform_test(app_version, need_presence=False)


class TestFullCardDetailSerializer:
    TEST_TRAILER_OBJ = {
        'thumbnail': {
            'thmb_href': '//avatars.mds.yandex.net/get-ott/test/test/orig'
        },
        'stream_url': 'https://strm.yandex.ru/vh-ottenc-converted/vod-content/test/master_sdr_hd_avc_aac.m3u8',
        'url': 'https://strm.yandex.ru/vh-ottenc-converted/vod-content/test/hls-v3/master_sdr_hd_avc_aac.m3u8',
        'vh_uuid': '405b02b6d0c19422bcfcaf1d84c79336'
    }

    @pytest.mark.parametrize('object_response, is_trailer_expected', [
        ({'view': {}}, False),
        ({'view': None}, False),
        ({'view': {'Gallery': {}}}, False),
        ({'view': {'Gallery': None}}, False),
        ({'view': {'Gallery': {'Trailer': {}}}}, False),
        ({'view': {'Gallery': {'Trailer': None}}}, False),
        ({'view': {'Gallery': {'Trailer': TEST_TRAILER_OBJ}}, 'base_info': {'id': 'ruw123456'}}, True),
    ])
    def test_trailer_serializer(self, object_response, is_trailer_expected):
        actual_result = FullCardDetail().get_trailer(object_response)
        if not is_trailer_expected:
            assert actual_result is None
        else:
            assert actual_result


class TestVhCarouselAsSearchResultSerializer:
    @pytest.fixture
    def valid_entry_serialized(self):
        return mock.get_vh_carousel_as_search_result()

    def test_serializer(self, valid_entry_serialized):
        """
        Сериализатор работает
        """
        serializer = VhCarouselAsSearchResultSerializer()
        data = {'includes': mock.vh_valid_response['set']}
        context = {'platform_info': PlatformInfo()}
        result = TSearchResultData()
        serializer.serialize(data, result, context)

        assert json_format.MessageToDict(result) == valid_entry_serialized

    def test_serializer_skips_incorrect_entries(self):
        """
        Сериализатор скипает записи с экзепшнами, не падает целиком
        """
        serializer = VhCarouselAsSearchResultSerializer()
        data = {'includes': [
            'some',
            {},
            mock.get_vh_carousel_item(),
            None,
            mock.get_vh_carousel_item(),
        ]}
        context = {'platform_info': PlatformInfo()}
        result = TSearchResultData()
        serializer.serialize(data, result, context)

        assert len(result.Galleries) == 1
        assert len(result.Galleries[0].Items) == 2

    def test_hint_description(self, mocker):
        serializer = VhCarouselAsSearchResultSerializer()
        input = {'genres': ['драма', 'триллер'], 'release_year': '2008'}
        result = mocker.Mock()
        serializer._fill_hint_description(input, result)
        assert result.HintDescription == '2008, драма, триллер'

    def test_hint_description_without_year(self, mocker):
        """
        Хинт дескрипшн не генерируется, если нет части данных
        Иначе клиент не сможет распарсить полученную строку
        """
        serializer = VhCarouselAsSearchResultSerializer()
        input = {'genres': ['драма']}
        result = mocker.Mock()
        serializer._fill_hint_description(input, result)
        assert result.HintDescription == ''


class TestCinemaSerializer:
    @pytest.fixture
    def valid_cinema_item(self):
        return {
            "tv_deeplink": "deep",
            "tv_fallback_link": "fallback",
            "tv_package_name": "tv.okko.androidtv",
            "link": "https://tv.apple.com/",
            "duration": 0,
            "code": "appletv",
            "favicon": "//avatars.mds.yandex.net/search/1/x/orig",
            "hide_price": False,
            "variants": [
                {"price": 129, "type": "tvod", "quality": "FullHD"},
                {"price": 0, "type": "something", "quality": "FullHD"},  # отфильтруется
                {"price": 0, "type": "avod", "quality": "FullHD"},
                {"price": 199, "type": "est", "quality": "FullHD"},
                {"price": 100, "type": "svod", "quality": "FullHD"},
                {"price": 0, "type": "fvod", "quality": "FullHD"},
            ],
        }

    @pytest.fixture
    def valid_result(self):
        return {
            'tv_deeplink': 'deep',
            'tv_fallback_link': 'fallback',
            'tv_package_name': 'tv.okko.androidtv',
            'link': 'https://tv.apple.com/',
            'duration': 0,
            'code': 'appletv',
            'favicon': 'https://avatars.mds.yandex.net/search/1/x/orig',
            'hide_price': False,
            'variants': [
                # порядок важен в интерфейсе
                {'price': 129, 'type': 'tvod', 'quality': 'FullHD'},
                {'price': 199, 'type': 'est', 'quality': 'FullHD'},
                {'price': 0, 'type': 'avod', 'quality': 'FullHD'},
                {'price': 100, 'type': 'svod', 'quality': 'FullHD'},
                {'price': 0, 'type': 'fvod', 'quality': 'FullHD'},
            ]
        }

    @pytest.fixture
    def build_cinema_item(self, valid_cinema_item):
        def _build(code=None, tv_package_name=None):
            item = copy(valid_cinema_item)
            if code:
                item['code'] = code
            if tv_package_name is not None:
                item['tv_package_name'] = tv_package_name
            return item
        return _build

    def test_valid_cinema_serialization(self, valid_cinema_item, valid_result):
        """
        Общая проверка работоспособности сериализатора
        """
        result = CinemaSerializer().serialize(valid_cinema_item)
        assert result == valid_result

    def test_empty_deeplink_cinema_serialization(self, valid_cinema_item):
        """
        Сериализатор скипает кинотеатры с пустыми диплинками
        """
        del valid_cinema_item["tv_deeplink"]
        with pytest.raises(CinemaSerializer.UnsupportedCinema):
            CinemaSerializer().serialize(valid_cinema_item)

    def test_tv_package_name_autodetected_from_code(self, valid_cinema_item):
        """
        Сериализатор заполняет tv_package_name по коду, если поля нет в ответе ОО
        """
        del valid_cinema_item['tv_package_name']
        valid_cinema_item['code'] = 'okko'
        result = CinemaSerializer().serialize(valid_cinema_item)
        assert result['tv_package_name'] == 'ru.more.play'  # дройдека автозаполнила

    def test_empty_package_produces_exception(self, valid_cinema_item):
        del valid_cinema_item['tv_package_name']
        valid_cinema_item['code'] = 'unknown'
        with pytest.raises(CinemaSerializer.UnsupportedCinema):
            CinemaSerializer().serialize(valid_cinema_item)

    def test_cinemas_without_package_name_skipped(self, build_cinema_item):
        """
        Кинотеатры без tv_package_name скипаются из ответа
        """
        cinemas = [
            build_cinema_item(),
            build_cinema_item(code='unknown', tv_package_name=''),  # will be skipped
            build_cinema_item(code='unknown', tv_package_name='tv.package.name'),
        ]

        oo = {'rich_info': {'cinema_data': {'cinemas': cinemas}}}

        result = FullCardDetail().get_cinemas(oo)
        assert len(result) == 2

    def test_cinemas_order_in_result(self, build_cinema_item):
        """
        Кинопоиск в списке кинотеатров не выводится
        """
        cinemas = [
            build_cinema_item(code='start'),
            build_cinema_item(code='kp'),
            build_cinema_item(code='appletv'),
        ]

        oo = {'rich_info': {'cinema_data': {'cinemas': cinemas}}}

        result = FullCardDetail().get_cinemas(oo)
        assert len(result) == 2
        assert result[0]['code'] == 'start'
        assert result[1]['code'] == 'appletv'
