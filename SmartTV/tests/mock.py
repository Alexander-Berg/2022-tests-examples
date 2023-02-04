from typing import Optional
from copy import deepcopy
import mock

from plus.utils.mediabilling.api import MediaBillingAPI

from smarttv.droideka.proxy.serializers import fast_response

from smarttv.droideka.proxy.api import vh
from smarttv.droideka.proxy.blackbox import UserInfo, DbFields
from smarttv.droideka.utils import PlatformInfo, RequestInfo
from smarttv.droideka.proxy.models import ValidIdentifier


class MockBlackboxClient:
    def __init__(self, *args, **kwargs):
        pass

    def get_user_info(self, attrs, **kwargs):
        return UserInfo('1' * 32, {'1015': True, '1016': 'YA_PLUS'})


class MockTvmClient:
    def add_service_ticket(self, _) -> dict:
        return {}

    def get_service_ticket(self, _) -> dict:
        return {}


class MockModel:
    def save(self, force_insert=False, force_update=False, using=None,
             update_fields=None):
        pass


def get_valid_episode():
    return {
        'auto_fields': {
            'with_cmnt': True,
            'cmnt_id': '4c957360deaab522b9a13ad24d250014'
        },
        'release_date': '1580174400',
        'family_id': 790,
        'streams': [{
            'stream_type': 'DASH',
            'url': 'https://strm.yandex.ru/kal/fox_hd/manifest.mpd?',
            'drmConfig': {
                'servers': {
                    'com.widevine.alpha': 'https://widevine-proxy.ott.yandex.ru/proxy'
                },
                'advanced': {
                    'com.widevine.alpha': {
                        'serverCertificateUrl': 'https://widevine-proxy.ott.yandex.ru/certificate'
                    }
                },
                'requestParams': {
                    'verificationRequired': True,
                    'monetizationModel': 'SVOD',
                    'contentId': '4bb8ed65d82329d99e1db1a4b0863630',
                    'productId': 2,
                    'serviceName': 'ya-main',
                    'expirationTimestamp': 1580251991,
                    'contentTypeId': 2,
                    'puid': 3615398,
                    'signature': '68348ec3cc9fd116d92da988b36933786eb08a52',
                    'watchSessionId': '4a819a5bd9e2435b8fbc0fa7b6801ff9',
                    'version': 'V4'
                }
            }
        }],
        'duration': 2400,
        'channel_multiplex_number': 0,
        'release_date_ut': 1580174400,
        'skippableFragments': {},
        'content_type_name': 'episode',
        'views_count': 0,
        'stream': {
            'trackings': {
                'trackingEvents': []
            },
            'withCredentials': True
        },
        'internal': False,
        'content_url': 'https://strm.yandex.ru/kal/fox_hd/manifest.mpd?',
        'thumbnail': '//avatars.mds.yandex.net/get-vh/2351157/10741690844654885924-Vd2zDO5odxY7u0CoPWx9Sg-1580182038/orig',
        'end_time': 1580176800,
        'blacked': 0,
        'can_play_on_station': True,
        'program_id': 2998868,
        'episode_id': 2921897,
        'update_time': 1580187954,
        'embed_url': 'https://frontend.vh.yandex.ru/player/4c957360deaab522b9a13ad24d250014?from=efir',
        'computed_title': 'NCIS 11-я серия - Тоска по дому. Телеканал FOX. 28.01.2020 04:20',
        'start_time': 1580174400,
        'parent_id': '4bb8ed65d82329d99e1db1a4b0863630',
        'genre_id': 4,
        'meta': {
            'with_sport_prediction': False,
            'weight': 0,
            'with_overlay': False,
            'with_deep_hd': False,
            'with_subscribe': True,
            'without_timeline': False
        },
        'has_cachup': 1,
        'main_color': '#201010',
        'description': 'После того как дети из семей военных стали заболевать странной болезнью',
        'program_title': 'NCIS',
        'puid': '3615398',
        'catchup_age': 0,
        'channel_id': 1031,
        'serp_url': 'https://yandex.ru/video/search?text=',
        'deprecated_content_id': '10741690844654885924',
        'restriction_age': 16,
        'content_id': '4c957360deaab522b9a13ad24d250014',
        'dvr': 0,
        'is_special_project': False,
        'genre': 'сериалы',
        'title': '11-я серия - Тоска по дому',
        'download_episodes': 0,
        'can_play_till': 1580779200,
        'program_year': '2003 – 2017',
        'event_id': 150685209,
        'program_description': 'Криминальная драма, приправленная хорошей порцией юмора',
        'deep_hd': False,
        'has_schedule': 1,
        'adConfig': {
            'distr_id': 0,
            'partner_id': 0,
            'hasPreroll': False,
            'category': 1011,
            'useAdSession': True,
            'video_content_name': '11-я серия - Тоска по дому',
            'video_content_id': '4c957360deaab522b9a13ad24d250014',
            'video_genre_name': []
        },
        'can_play_on_efir': True,
        'has_vod': 0,
    }


episodes = [get_valid_episode()]
empty_data = (None,)


def get_invalid_episodes() -> list:
    for bad_episode in empty_data:
        yield bad_episode

    for field in fast_response.ProgramsSerializer.get_required_fields():
        bad_episode = get_valid_episode()
        del bad_episode[field]
        yield [bad_episode]


def get_mixed_episodes() -> list:
    required_field = fast_response.ProgramsSerializer.get_required_fields()[0]
    bad_episode = get_valid_episode()
    del bad_episode[required_field]
    return [bad_episode, get_valid_episode()]


channel_category_1 = 'educate'
channel_category_2 = 'inform'

channel_id_1 = 1333
channel_id_2 = 1331


def gen_channel(id, category, hidden=False):
    status = [
        'published',
        'has_schedule',
        'has_cachup'
    ]

    if hidden:
        status.append('hidden')

    result = {
        'adConfig': {
            'video_content_id': '4fec709cf581ef3c94a9aa39a8564724',
            'hasPreroll': False,
            'video_content_name': 'Univer TV',
            'category': 1017,
            'partner_id': 237369,
            'video_genre_name': [],
            'useAdSession': True,
            'distr_id': 0
        },
        'deprecated_content_id': '4442266703907959280',
        'update_time': 1577198235,
        'theme': {
            'title': 'Телеканалы'
        },
        'dvr': 300,
        'skippableFragments': {},
        'parent_id': '49efcf58f9f5b8c29324d8e764dcb41d',
        'family_id': 90,
        'content_url': 'https://strm.yandex.ru/kal/univer/univer0.m3u8?from=unknown&partner_id=237369&target_ref=https%3A//yastatic.net&uuid=4fec709cf581ef3c94a9aa39a8564724&video_category_id=1017',
        'views_count': 0,
        'is_special_project': False,
        'channel_multiplex_number': 27,
        'channel_type': 'local',
        'status': status,
        'deep_hd': False,
        'serp_url': 'https://yandex.ru/video/search?text=url%3Ahttps%3A%2F%2Ffrontend%2Evh%2Eyandex%2Eru%2Fplayer%2F4442266703907959280',
        'channel_id': id,
        'restriction_age': 18,
        'content_type_name': 'channel',
        'thumbnail': '//avatars.mds.yandex.net/get-vh/1528766/4442266703907959280-Cqd3xsoBX7b1qOs84vjBbSA-1562072242/orig',
        'stream': {
            'withCredentials': True,
            'trackings': {
                'trackingEvents': []
            }
        },
        'blacked': 0,
        'has_cachup': 1,
        'embed_url': 'https://frontend.vh.yandex.ru/player/4fec709cf581ef3c94a9aa39a8564724?from=unknown',
        'computed_title': 'Univer TV',
        'duration': 0,
        'internal': False,
        'puid': '1',
        'has_schedule': 1,
        'catchup_age': 1209600,
        'can_play_on_station': True,
        'has_vod': 0,
        'main_color': '#101010',
        'ya_video_preview': 'https://video-preview.s3.yandex.net/vh/4442266703907959280_preview.mp4',
        'auto_fields': {
            'chat_id': '0/7/4fec709cf581ef3c94a9aa39a8564724',
            'with_cmnt': True,
            'ether_watermark_required': True,
            'with_chat': True,
            'cmnt_id': '4fec709cf581ef3c94a9aa39a8564724',
            'channel_smarttv_number': 1,
        },
        'streams': [
            {
                'url': 'https://strm.yandex.ru/kal/univer/manifest.mpd?from=unknown&partner_id=237369&target_ref=https%3A//yastatic.net&uuid=4fec709cf581ef3c94a9aa39a8564724&video_category_id=1017',
                'options': [],
                'title': 'main'
            },
            {
                'title': 'main',
                'options': [],
                'url': 'https://strm.yandex.ru/kal/univer/univer0.m3u8?from=unknown&partner_id=237369&target_ref=https%3A//yastatic.net&uuid=4fec709cf581ef3c94a9aa39a8564724&video_category_id=1017'
            }
        ],
        'meta': {
            'without_timeline': False,
            'with_overlay': False,
            'weight': 0,
            'announce_thumbnail': '//avatars.mds.yandex.net/get-vh/1583218/2a0000016bb28d5de63e39936f314904ff5d/orig',
            'with_deep_hd': False,
            'with_sport_prediction': False
        },
        'title': 'Univer TV',
        'region_ids': [
            29387,
            29386,
            10003,
            10002,
            241,
            225,
            209,
            208,
            207,
            187,
            183,
            171,
            170,
            168,
            167,
            159,
            138,
            111,
            9999
        ],
        'description': 'Univer TV - канал с круглосуточным вещанием Казанского федерального университета (КФУ). Ориентирован на студенческую аудиторию. ',
        'can_play_on_efir': True,
        'logo': '//avatars.mds.yandex.net/get-vh/1455070/4442266703907959280-uHYqRqxMU7129hcevnw-KQ-1562069054/orig',
        'content_id': '4fec709cf581ef3c94a9aa39a8564724',
        'download_episodes': 1
    }

    if category:
        result['channel_category'] = [category]

    return result


class VhApiMockEmptyResponses:
    def episodes(self, *args, **kwargs):
        return {}

    def carousels_videohub(self, *args, **kwargs):
        return {}

    def feed(self, *args, **kwargs):
        return {}

    def channels_regions(self, *args, **kwargs):
        return {}


class VhApiMock(VhApiMockEmptyResponses):
    channels_key = 'channels'
    episodes_key = 'episodes'

    keys = (channels_key, episodes_key)

    def __init__(self):
        self.mapping = {}

    def episodes(self, *args, **kwargs):
        return self._get_response(self.episodes_key)

    def channels_regions(self, *args, **kwargs):
        return self._get_response(self.channels_key)

    def set_response_for_key(self, key, response):
        assert key in self.keys, 'unknown response key'
        self.mapping[key] = response

    def _get_response(self, key):
        try:
            return {
                'set': self.mapping[key]
            }
        except KeyError:
            assert False, 'before call vh api set response by key first'


common_channels = [
    gen_channel(channel_id_1, channel_category_1),
    gen_channel(channel_id_2, channel_category_2)]

hidden_channels = [
    gen_channel(channel_id_1, channel_category_1, hidden=True),
    gen_channel(channel_id_2, channel_category_2, hidden=False)
]


class GeobaseMock:
    @staticmethod
    def get_region_by_ip(_):
        return {'id': 225}


class MediaBillingMock:
    def __init__(self, fail=None):
        self.fail = fail or []

    class APIError(Exception):
        pass

    class RequestError(APIError):
        pass

    def consume_promocode(self, uid, promocode, user_ip, timeout=None):
        if 'consume_promocode' in self.fail:
            return {'status': 'error'}
        return {'status': 'success'}

    def clone_promocode(self, prototype):
        if 'clone_promocode' in self.fail:
            raise self.APIError
        return prototype


class MediaBillingHttpMock(MediaBillingAPI):
    base_url = 'http://test.tst'


log_id_carousel_full_input = {"user_data": {"req_id": "some_req_id"}, "reqid": "some_another_req_id",
                              "apphost-reqid": "one_more_req_id",
                              "set": [{"parent_id": "11", "content_id": "12", "title": "Далласский клуб покупателей"},
                                      {"parent_id": "21", "content_id": "22", "title": "Иностранец"},
                                      {"parent_id": "31", "content_id": "32", "title": "Дивергент",
                                       'includes': [{"parent_id": "41", "content_id": "42", "title": "Дивергент"}]}]}

log_id_carousel_small_input = {"user_data": {"req_id": "some_req_id"}, "reqid": "some_another_req_id",
                               "apphost-reqid": "one_more_req_id",
                               "set": [{"parent_id": "31", "content_id": "32", "title": "Дивергент",
                                        'includes': [{"parent_id": "41", "content_id": "42", "title": "Дивергент"}]}]}

log_id_carousel_small_result = {'user_data': {'req_id': 'some_req_id'}, 'reqid': 'some_another_req_id',
                                'apphost-reqid': 'one_more_req_id', 'search-reqid': None,
                                'aggregated_ids': {'carousel_id': 'some_carousel_id', 'docs_cache_hash': None,
                                                   'content': [
                                                       {'content_id': "32", 'parent_id': '31', 'title': 'Дивергент',
                                                        'includes': {'content_id': '42', 'parent_id': '41',
                                                                     'title': 'Дивергент'}}]}}

log_id_carousel_full_result = {'user_data': {'req_id': 'some_req_id'}, 'reqid': 'some_another_req_id',
                               'apphost-reqid': 'one_more_req_id', 'search-reqid': None,
                               'aggregated_ids': {'carousel_id': 'some_carousel_id', 'docs_cache_hash': None,
                                                  'content': [
                                                      {'content_id': '12', 'parent_id': '11',
                                                       'title': 'Далласский клуб покупателей'},
                                                      {'content_id': '22', 'parent_id': '21', 'title': 'Иностранец'},
                                                      {'content_id': '32', 'parent_id': '31', 'title': 'Дивергент',
                                                       'includes': {'content_id': '42', 'parent_id': '41',
                                                                    'title': 'Дивергент'}}]}}

log_id_carousel_noid_result = {'user_data': {'req_id': 'some_req_id'}, 'reqid': 'some_another_req_id',
                               'apphost-reqid': 'one_more_req_id', 'search-reqid': None,
                               'aggregated_ids': {'carousel_id': None, 'docs_cache_hash': None, 'content': [
                                   {'content_id': '12', 'parent_id': '11', 'title': 'Далласский клуб покупателей'},
                                   {'content_id': '22', 'parent_id': '21', 'title': 'Иностранец'},
                                   {'content_id': '32', 'parent_id': '31', 'title': 'Дивергент',
                                    'includes': {'content_id': '42', 'parent_id': '41', 'title': 'Дивергент'}}]}}

log_id_full_doc2doc = {"user_data": {"req_id": "123"},
                       "reqid": "345",
                       "apphost-reqid": "678",
                       "search-reqid": "347",
                       "set": [{"parent_id": "11", "content_id": "12", "title": "Далласский клуб покупателей"},
                               {"parent_id": "21", "content_id": "22", "title": "Иностранец"},
                               {"parent_id": "31", "content_id": "32", "title": "Дивергент",
                                'includes': [{"parent_id": "41", "content_id": "42", "title": "Дивергент"}]}]}

log_id_full_doc2doc_result = {'user_data': {'req_id': '123'}, 'reqid': '345', 'apphost-reqid': '678',
                              'search-reqid': '347', 'aggregated_ids':
                                  [{'content_id': "12", 'parent_id': '11', 'title': 'Далласский клуб покупателей'},
                                   {'content_id': '22', 'parent_id': '21', 'title': 'Иностранец'},
                                   {'content_id': '32', 'parent_id': '31', 'title': 'Дивергент',
                                    'includes': {'content_id': '42', 'parent_id': '41', 'title': 'Дивергент'}}]}

log_id_small_doc2doc = {"user_data": {"req_id": "123"}, "reqid": "345", "apphost-reqid": "678", "search-reqid": "347",
                        "set": [{"parent_id": "31", "content_id": "32", "title": "Дивергент",
                                 'includes': [{"parent_id": "41", "content_id": "42", "title": "Дивергент"}]}]}

log_id_small_doc2doc_result = {'user_data': {'req_id': '123'}, 'reqid': '345', 'apphost-reqid': '678',
                               'search-reqid': '347', 'aggregated_ids':
                                   [{'content_id': '32', 'parent_id': '31', 'title': 'Дивергент',
                                     'includes': {'content_id': '42', 'parent_id': '41', 'title': 'Дивергент'}}]}

log_id_feed_full_input = {"user_data": {"req_id": "some_req_id"},
                          "reqid": "some_another_req_id",
                          "apphost-reqid": "one_more_req_id",
                          'cache_hash': 'asd',
                          'items': [
                              {
                                  'carousekl_id': 'c1',
                                  'cache_hash': '123',
                                  "set": [
                                      {"parent_id": "11", "content_id": "12", "title": "Film 1"},
                                      {"parent_id": "21", "content_id": "22", "title": "Film 2"},
                                      {"parent_id": "31", "content_id": "32", "title": "Film 3",
                                       'includes': [{"parent_id": "41", "content_id": "42", "title": "Film 3.1"}]}
                                  ]
                              },
                              {
                                  'carousekl_id': 'c2',
                                  'cache_hash': '123',
                                  "set": [
                                      {"parent_id": "211", "content_id": "212", "title": "Film 12"},
                                      {"parent_id": "221", "content_id": "222", "title": "Film 22"},
                                      {"parent_id": "231", "content_id": "232", "title": "Film 32", 'includes': [
                                          {"parent_id": "41", "content_id": "42", "title": "Film 32.1"}
                                      ]}
                                  ]
                              }]}

log_id_feed_result = {
    'user_data': {'req_id': 'some_req_id'},
    'reqid': 'some_another_req_id',
    'apphost-reqid': 'one_more_req_id',
    'search-reqid': None,
    'aggregated_ids': {
        'cache_hash': 'asd',
        'carousels': [
            {
                'carousel_id': None,
                'docs_cache_hash': '123',
                'content': [
                    {'content_id': '12', 'parent_id': '11', 'title': 'Film 1'},
                    {'content_id': '22', 'parent_id': '21', 'title': 'Film 2'},
                    {
                        'content_id': '32', 'parent_id': '31', 'title': 'Film 3',
                        'includes': {'content_id': '42', 'parent_id': '41', 'title': 'Film 3.1'}
                    }
                ]
            },
            {
                'carousel_id': None,
                'docs_cache_hash': '123',
                'content': [
                    {'content_id': '212', 'parent_id': '211', 'title': 'Film 12'},
                    {'content_id': '222', 'parent_id': '221', 'title': 'Film 22'},
                    {
                        'content_id': '232', 'parent_id': '231', 'title': 'Film 32',
                        'includes': {'content_id': '42', 'parent_id': '41', 'title': 'Film 32.1'}
                    }
                ]
            }
        ]
    }}


class VideoSearchMocks:
    COLLECTIONS_INDEX = 0
    ASSOC_INDEX = 1

    LEGAL_ASSOC_N_1 = 0
    LEGAL_ASSOC_N_2 = 0

    video_search_full_vs_response = {
        'entity_data': {
            'base_info': {
                'id': 'ruw1436255',
                'title': 'Спецназ',
                'legal': {
                    'vh_licenses': {
                        'uuid': '45bb293e7f407056861eded9be375f12',
                    }
                }
            },
            'related_object': [
                {  # COLLECTIONS_INDEX
                    'type': 'collections',
                    'next_start_index': 6,
                    'object': [
                        {
                            'title': 'Сериалы про спецназ',
                            'search_request': 'Сериалы про спецназ',
                            'name': 'Сериалы про спецназ',
                            'entref': '0oEg5sc3Q1OTgyYTllMy4uMBgCsbar8Q'
                        },
                        {
                            'title': 'Русские сериалы про настоящих мужчин',
                            'search_request': 'Русские сериалы про настоящих мужчин',
                            'name': 'Русские сериалы про настоящих мужчин',
                            'entref': '0oEg9sc3QtNGIzY2IwM2EuLjAYAu5Qaj4'
                        },
                    ],
                    'search_request': 'спецназ сериал похожие подборки',
                    'entref': '123'
                },
                {  # ASSOC_INDEX
                    'type': 'assoc',
                    'id': 'ruw1436255:assoc',
                    'list_name': 'Смотрите также',
                    'object': [
                        {
                            'name': 'Русский спецназ',
                            'entref': '0oCgpydXczOTI5ODExEhBydXcxNDM2MjU1OmFzc29jGAKxsTc9',
                            'search_request': 'русский спецназ фильм 2002',
                            'id': 'ruw3929811',
                            'type': 'Film',
                            'title': 'Русский спецназ',
                        },
                        {
                            'name': 'Спецназ 2',
                            'entref': '0oCgpraW4wNDIxNDI4EhBydXcxNDM2MjU1OmFzc29jGAL6FtY5',
                            'search_request': 'спецназ 2 сериал',
                            'id': 'kin0421428',
                            'type': 'Film',
                            'title': 'Спецназ 2',
                        },
                    ],
                    'search_request': 'спецназ сериал',
                    'entref': '0oEhBydXcxNDM2MjU1OmFzc29jGAKgb5YE'
                }
            ],
            'legal_assoc': [
                {
                    'search_request': 'спецназ сериал смотрите также',
                    'object': [
                        {
                            'name': 'Грозовые ворота',
                            'search_request': 'грозовые ворота сериал',
                            'id': 'ruw250464',
                        },
                        {
                            'name': 'Тарас Бульба',
                            'search_request': 'тарас бульба фильм 2009',
                            'id': 'ruw913411'
                        }
                    ],
                }
            ],
        },
        'clips': [
            {
                'title': 'Зеленая книга (2018) — драма, комедия, HD',
                'PlayerId': 'vh',
                'onto_id': 'ruw7567858',
                'url': 'http://frontend.vh.yandex.ru/player/6674448304671144216'
            },
            {
                'title': 'Зеленая книга (2018) - Трейлер (дублированный)',
                'onto_id': None,
                'url': 'http://frontend.vh.yandex.ru/player/15759882631768381962'
            },
        ]
    }

    expected_full_response = {
        'base_info': {
            'id': 'ruw1436255',
            'title': 'Спецназ',
            'uuid': '45bb293e7f407056861eded9be375f12'
        },
        'legal_assoc': {
            'search_request': 'спецназ сериал смотрите также',
            'object': [
                {'name': 'Грозовые ворота', 'id': 'ruw250464'},
                {'name': 'Тарас Бульба', 'id': 'ruw913411'}
            ]
        },
        'clips': [
            'http://frontend.vh.yandex.ru/player/6674448304671144216',
            'http://frontend.vh.yandex.ru/player/15759882631768381962'
        ],
        'collections': {
            'search_request': 'спецназ сериал похожие подборки',
            'entref': '123'
        },
        'assoc': {
            'search_request': 'спецназ сериал',
            'object': [
                {'name': 'Русский спецназ', 'id': 'ruw3929811'},
                {'name': 'Спецназ 2', 'id': 'kin0421428'}
            ]
        }
    }


class ProgramsMocks:
    INDEX_FIRST_EPISODE = 0
    INDEX_SECOND_EPISODE = 1

    programs_full_response = {
        'user_data': {
            'req_id': '1585915783269265-5505942486911868562'
        },
        'apphost-reqid': '1585915783269265-550594248691186856200140-ztt4qtpb3mbfljbq.man.yp-c.yandex.net',
        'set': [
            {
                'end_time': 1585913991,
                'program_id': 7808205388063,
                'episode_id': 7808205388063,
                'start_time': 1585906831,
                'parent_id': '4b31418eeea713b6b99433e6063ce707',
                'puid': '1',
                'catchup_age': 0,
                'channel_id': 100003,
                'channel_type': 'yatv',
                'content_id': '48af311d7c2218049158b947381ffe2d',
                'title': 'Красавчик 2',
                'can_play_till': 1587116431,
                'has_cachup': 0,
            },
            {
                'end_time': 1585920284,
                'program_id': 506269006663,
                'episode_id': 506269006663,
                'update_time': 1585583594,
                'has_cachup': 1,
                'title': 'Куриоса',
                'catchup_age': 0,
                'channel_id': 100003,
                'channel_type': 'yatv',
                'content_id': '4714a73d51533eab8f5f3b263d7710f1',
            },
            {
                'banned': True
            }
        ]
    }

    programs_full_expected_result = {
        'user_data': {
            'req_id': '1585915783269265-5505942486911868562'
        },
        'reqid': None,
        'apphost-reqid': '1585915783269265-550594248691186856200140-ztt4qtpb3mbfljbq.man.yp-c.yandex.net',
        'search-reqid': None,
        'aggregated_ids': [
            {
                'end_time': 1585913991,
                'program_id': 7808205388063,
                'start_time': 1585906831,
                'channel_id': 100003,
                'title': 'Красавчик 2'
            },
            {
                'end_time': 1585920284,
                'program_id': 506269006663,
                'channel_id': 100003,
                'title': 'Куриоса'
            },
            {
                'banned': True
            }
        ]
    }


class ProgramsRawTestData:
    raw_input = {
        'user_data': {
            'req_id': '1595510998816798-6286770842402735974'
        },
        'apphost-reqid': '1595510998816798-628677084240273597400173-qmyg3s4f34txxvy5',
        'set': [
            {
                'auto_fields': {
                    'ether_watermark_required': True,
                    'with_cmnt': True,
                    'cmnt_id': '474d603980c236ec9f40d8f10f5ecfce'
                },
                'release_date': '1595419560',
                'family_id': 106,
                'streams': [
                    {
                        'options': [],
                        'url': 'https://strm.yandex.ru/kal/fresh/manifest.mpd?from=unknown&partner_id=251562&target_ref='
                               'https%3A//yastatic.net&uuid=474d603980c236ec9f40d8f10f5ecfce&video_category_id=1017',
                        'title': 'fresh_stream'
                    },
                    {
                        'options': [],
                        'url': 'https://strm.yandex.ru/kal/fresh/fresh0.m3u8?from=unknown&partner_id=251562&target_ref='
                               'https%3A//yastatic.net&uuid=474d603980c236ec9f40d8f10f5ecfce&video_category_id=1017',
                        'title': 'fresh_stream'
                    },
                    {
                        'options': [],
                        'title': 'fresh_stream',
                        'stream_type': 'HLS',
                    }
                ],
                'duration': 240,
                'channel_multiplex_number': 0,
                'skippableFragments': {},
                'release_date_ut': 1595419560,
                'content_type_name': 'episode',
                'stream': {
                    'trackings': {
                        'trackingEvents': []
                    },
                    'withCredentials': True
                },
                'internal': False,
                'content_url': 'https://strm.yandex.ru/kal/fresh/fresh0.m3u8?from=unknown&partner_id=251562&target_ref='
                               'https%3A//yastatic.net&uuid=474d603980c236ec9f40d8f10f5ecfce&video_category_id=1017',
                'thumbnail': '//avatars.mds.yandex.net/get-vh/3488499/'
                             '2220958098861037-IwrXvoaf50yhCA-nqDFcIA-1595031025/orig',
                'end_time': 1595419800,
                'blacked': 0,
                'can_play_on_station': True,
                'program_id': 3970297,
                'update_time': 1595044898,
                'episode_id': 3970297,
                'embed_url': 'https://frontend.vh.yandex.ru/player/474d603980c236ec9f40d8f10f5ecfce?from=unknown',
                'start_time': 1595419560,
                'genre_id': 6,
                'computed_title': 'Елена Князева & Родион Газманов. Теряю. Телеканал FreshTV. 22.07.2020 15:06',
                'parent_id': '4e5161e8071d12dfa45ffb03b77230aa',
                'meta': {
                    'weight': 0,
                    'with_sport_prediction': False,
                    'with_overlay': False,
                    'with_deep_hd': False,
                    'without_timeline': False
                },
                'has_cachup': 1,
                'main_color': '#E86848',
                'description': 'Елена Князева & Родион Газманов. Теряю. (16+) 2016. Россия',
                'program_title': 'Елена Князева & Родион Газманов. Теряю',
                'puid': '493032576',
                'catchup_age': 0,
                'orbital_channel': False,
                'channel_id': 1802,
                'serp_url': 'https://yandex.ru/video/search?text='
                            'url%3Ahttps%3A%2F%2Ffrontend%2Evh%2Eyandex%2Eru%2Fplayer%2F2220958098861037',
                'restriction_age': 16,
                'channel_type': 'satelite',
                'deprecated_content_id': '2220958098861037',
                'is_special_project': False,
                'dvr': 0,
                'content_id': '474d603980c236ec9f40d8f10f5ecfce',
                'title': 'Елена Князева & Родион Газманов. Теряю',
                'genre': 'досуг',
                'download_episodes': 0,
                'can_play_till': 1595419800,
                'event_id': 160862941,
                'deep_hd': False,
                'program_description': 'Елена Князева & Родион Газманов. Теряю. (16+) 2016. Россия',
                'has_schedule': 1,
                'adConfig': {
                    'distr_id': 0,
                    'partner_id': 251562,
                    'hasPreroll': False,
                    'category': 1017,
                    'useAdSession': True,
                    'video_content_name': 'Елена Князева & Родион Газманов. Теряю',
                    'video_content_id': '474d603980c236ec9f40d8f10f5ecfce',
                    'video_genre_name': []
                },
                'has_vod': 0,
                'can_play_on_efir': True
            },
        ]
    }

    serializer_output = {'123': [{
        'title': 'Елена Князева & Родион Газманов. Теряю',
        'program_title': 'Елена Князева & Родион Газманов. Теряю',
        'has_schedule': 1,
        'has_cachup': 1,
        'start_time': 1595419560,
        'program_id': 3970297,
        'end_time': 1595419800,
        'content_id': '474d603980c236ec9f40d8f10f5ecfce',
        'restriction_age': 16,
        'deep_hd': False,
        'content_url': 'https://fakeurl.com',
        'streams': [{
            'options': [],
            'url': 'https://strm.yandex.ru/kal/fresh/manifest.mpd?from=unknown&partner_id=251562&target_ref='
                   'https%3A//yastatic.net&uuid=474d603980c236ec9f40d8f10f5ecfce&video_category_id=1017',
            'title': 'fresh_stream'
        }, {
            'options': [],
            'url': 'https://strm.yandex.ru/kal/fresh/fresh0.m3u8?from=unknown&partner_id=251562&target_ref='
                   'https%3A//yastatic.net&uuid=474d603980c236ec9f40d8f10f5ecfce&video_category_id=1017',
            'title': 'fresh_stream'
        }, {
            'options': [],
            'title': 'fresh_stream',
            'stream_type': 'HLS',
            'url': '.m3u8',
        }],
        'channel_id': 1802,
        'channel_type': 'satelite',
        'catchup_age': 0,
        'release_date': '1595419560',
        'release_date_ut': 1595419560,
        'parent_id': '4e5161e8071d12dfa45ffb03b77230aa',
        'main_color': '#E86848',
        'description': 'Елена Князева & Родион Газманов. Теряю. (16+) 2016. Россия',
        'duration_s': 240,
        'content_type': 'episode',
        'thumbnail': 'https://avatars.mds.yandex.net/get-vh/3488499/'
                     '2220958098861037-IwrXvoaf50yhCA-nqDFcIA-1595031025/orig',
        'player_id': 'vh',
        'live_stream': True,
        'combined_title': 'Елена Князева & Родион Газманов. Теряю'
    }]}


oo_collection_images_with_bad_image = {
    'entity_data': {
        'related_object': [
            {
                'type': 'collections',
                'object': [
                    {
                        'collection_images': [
                            {
                                'mds_avatar_id': '67347/78419931',
                                'original': 'https://pbs.twimg.com/media/BfoCUBbCIAA44yt.jpg',
                            }
                        ]
                    }
                ]
            }
        ]
    }
}
oo_parent_collection_images_with_bad_image = {
    'entity_data': {
        'parent_collection': {
            'object': [
                {
                    'image': {
                        'mds_avatar_id': '67347/78419931',
                        'original': 'https://pbs.twimg.com/media/BfoCUBbCIAA44yt.jpg',
                    }
                }
            ]
        }
    }
}
oo_actual_bad_collection_image_url = 'https://pbs.twimg.com/media/BfoCUBbCIAA44yt.jpg'
oo_expected_good_collection_image_url = 'https://avatars.mds.yandex.net/get-entity_search/67347/78419931/orig'


class PlayerIdTestData:

    @property
    def vh_card_detail(self):
        return [
            {'content_detail': {'content_type_name': 'channel', 'content_id': '123'}},
            {'content_detail': {'content_type_name': 'episode', 'content_id': '123'}},
            {'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123', 'blogger_id': '123'}},
            {'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123'}},
            {'content_detail': {'content_type_name': vh.CONTENT_TYPE_EPISODE, 'content_id': '12', 'blogger_id': '123'}},
            {'content_detail': {'content_type_name': vh.CONTENT_TYPE_EPISODE, 'content_id': '12'}},
        ]

    @property
    def kinopoisk_card_detail(self):
        return [
            (None, {'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123',
                                       'ottParams': {'licenses': [
                                           {'monetizationModel': 'SVOD', 'primary': True,
                                            'purchaseTag': 'kp-basic'}]}}}),
            (None, {'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123',
                                       'ottParams': {'licenses': [
                                           {'monetizationModel': 'SVOD', 'primary': True,
                                            'purchaseTag': 'kp-amediateka'}]}}}),
            (None, {'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123',
                                       'includes': [{'ottParams': {'licenses': [
                                           {'monetizationModel': 'SVOD', 'primary': True,
                                            'purchaseTag': 'kp-basic'}]}}]}}),
            (None, {'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123',
                                       'includes': [{'ottParams': {'licenses': [
                                           {'monetizationModel': 'SVOD', 'primary': True,
                                            'purchaseTag': 'kp-amediateka'}]}}]}}),
            (None, {'content_detail': {'content_type_name': vh.CONTENT_TYPE_EPISODE, 'content_id': '123',
                                       'ottParams': {'licenses': [
                                           {'monetizationModel': 'EST', 'primary': True}]}}}),
            (None, {'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123',
                                       'ottParams': {'licenses': [
                                           {'monetizationModel': 'EST', 'primary': True}]}}}),
            (None, {'content_detail': {'content_type_name': vh.CONTENT_TYPE_EPISODE, 'content_id': '123',
                                       'ottParams': {'licenses': [
                                           {'monetizationModel': 'TVOD', 'primary': True}]}}}),
            (None, {'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123',
                                       'ottParams': {'licenses': [
                                           {'monetizationModel': 'TVOD', 'primary': True}]}}}),
            (None, {'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123',
                                       'ottParams': {'licenses': [
                                           {'monetizationModel': 'SVOD',
                                            'primary': True,
                                            'purchaseTag': 'plus',
                                            'active': True}]}}}),
            (None, {'content_detail': {'content_type_name': vh.CONTENT_TYPE_EPISODE, 'content_id': '123',
                                       'includes': [{'ottParams': {'licenses': [
                                           {'monetizationModel': 'SVOD',
                                            'primary': True,
                                            'purchaseTag': 'plus',
                                            'active': True}]}}]}}),
            (vh.CONTENT_TYPE_EPISODE, {
                'content_id': '123', 'onto_id': '453',
                'content_type_name': vh.CONTENT_TYPE_EPISODE, 'object_response': {
                    'rich_info': {'vh_meta': {'content_groups': [{'licenses': [{'monetization_model': 'EST'}]}]}}}}),
            (vh.CONTENT_TYPE_SERIES, {
                'content_id': '123', 'onto_id': '453', 'content_type_name': vh.CONTENT_TYPE_SERIES, 'object_response': {
                    'rich_info': {'vh_meta': {'content_groups': [{'licenses': [{'monetization_model': 'EST'}]}]}}}}),
            (vh.CONTENT_TYPE_EPISODE, {
                'content_id': '123', 'onto_id': '453', 'content_type_name': vh.CONTENT_TYPE_EPISODE,
                'object_response': {'rich_info': {'vh_meta': {
                    'content_groups': [{'licenses': [{'monetization_model': 'TVOD'}]}]}}}}),
            (vh.CONTENT_TYPE_SERIES, {
                'content_id': '123', 'onto_id': '453', 'content_type_name': vh.CONTENT_TYPE_SERIES, 'object_response': {
                    'rich_info': {'vh_meta': {'content_groups': [{'licenses': [{'monetization_model': 'TVOD'}]}]}}}}),
            (vh.CONTENT_TYPE_EPISODE, {
                'content_id': '123', 'onto_id': '453', 'content_type_name': vh.CONTENT_TYPE_EPISODE,
                'object_response': {'rich_info': {'vh_meta': {
                    'content_groups': [{'child_licenses': [{'monetization_model': 'EST'}]}]}}}}),
            (vh.CONTENT_TYPE_SERIES, {
                'content_id': '123', 'onto_id': '453', 'content_type_name': vh.CONTENT_TYPE_SERIES,
                'object_response': {'rich_info': {'vh_meta': {
                    'content_groups': [{'child_licenses': [{'monetization_model': 'EST'}]}]}}}}),
            (vh.CONTENT_TYPE_EPISODE, {
                'content_id': '123', 'onto_id': '453', 'content_type_name': vh.CONTENT_TYPE_EPISODE,
                'object_response': {'rich_info': {'vh_meta': {
                    'content_groups': [{'child_licenses': [{'monetization_model': 'TVOD'}]}]}}}}),
            (vh.CONTENT_TYPE_SERIES, {
                'content_id': '123', 'onto_id': '453', 'content_type_name': vh.CONTENT_TYPE_SERIES,
                'object_response': {'rich_info': {'vh_meta': {
                    'content_groups': [{'child_licenses': [{'monetization_model': 'TVOD'}]}]}}}}),
            (vh.CONTENT_TYPE_SERIES, {
                'content_id': '45b63ee83d9ea3c293a927da58e6ce1d', 'onto_id': 'ruw7538508',
                'content_type_name': 'vod-episode', 'object_response': {
                    'base_info': {'legal': {'vh_licenses': {'est': {'EST': False}}}}}}),
            (vh.CONTENT_TYPE_SERIES, {
                'content_id': '45b63ee83d9ea3c293a927da58e6ce1d', 'onto_id': 'ruw7538508',
                'content_type_name': 'vod-episode', 'object_response': {
                    'base_info': {'legal': {'vh_licenses': {'est': {'TVOD': False}}}}}}),
        ]

    @property
    def ott_card_details(self):
        return [
            (None, {'content_detail': {'content_type_name': vh.CONTENT_TYPE_EPISODE, 'content_id': '123',
                                       'ottParams': {'licenses': [{'monetizationModel': 'AVOD',
                                                                   'primary': True,
                                                                   'active': True}]}, 'onto_category': 'film'}}),
            (None, {'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123',
                                       'ottParams': {'licenses': [{'monetizationModel': 'AVOD',
                                                                   'primary': True,
                                                                   'active': True}]}, 'onto_category': 'series'}}),
            (None, {'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123',
                                       'includes': [{'ottParams': {'licenses': [
                                           {'monetizationModel': 'AVOD',
                                            'primary': True,
                                            'active': True}]}}], 'onto_category': 'film'}}),
            (vh.CONTENT_TYPE_EPISODE, {
                'content_id': '123', 'onto_id': '453', 'content_type_name': vh.CONTENT_TYPE_SERIES,
                'onto_category': 'film', 'object_response': {
                    'rich_info': {'vh_meta': {'content_groups': [{'licenses': [{'monetization_model': 'AVOD'}]}]}}}}),
            (vh.CONTENT_TYPE_SERIES, {
                'content_id': '123', 'onto_id': '453', 'content_type_name': vh.CONTENT_TYPE_EPISODE,
                'onto_category': 'film', 'object_response': {'rich_info': {'vh_meta': {
                    'content_groups': [{'child_licenses': [{'monetization_model': 'AVOD'}]}]}}}}),
        ]

    @property
    def web_card_details(self):
        return [
            (None, {'content_detail': {'content_type_name': 'unknown_content_type',
                                       'content_id': '123', }}),
            (None, {'content_detail': {
                'content_type_name': vh.CONTENT_TYPE_EPISODE,
                'content_id': '123', 'object_response': {'not empty': '123'}}}),
            (vh.CONTENT_TYPE_SERIES, {
                'content_id': '123', 'onto_id': '453', 'content_type_name': vh.CONTENT_TYPE_SERIES,
                'object_response': {}}),
            (vh.CONTENT_TYPE_SERIES, {
                'content_id': '123', 'onto_id': '453', 'content_type_name': vh.CONTENT_TYPE_SERIES,
                'object_response': {'rich_info': {}}}),
            (vh.CONTENT_TYPE_SERIES, {
                'content_id': '123', 'onto_id': '453', 'content_type_name': vh.CONTENT_TYPE_SERIES,
                'object_response': {'rich_info': {'vh_meta': {}}}}),
            (vh.CONTENT_TYPE_SERIES, {
                'content_id': '123', 'onto_id': '453', 'content_type_name': vh.CONTENT_TYPE_SERIES,
                'object_response': {'rich_info': {'vh_meta': {'content_groups': []}}}}),
            (vh.CONTENT_TYPE_SERIES, {
                'content_id': '123', 'onto_id': '453', 'content_type_name': vh.CONTENT_TYPE_SERIES,
                'object_response': {'rich_info': {'vh_meta': {
                    'content_groups': [{'bad_licenses': [{'monetization_model': 'EST'}]}]}}}}),
            (vh.CONTENT_TYPE_SERIES, {
                'content_id': '123', 'onto_id': '453', 'content_type_name': vh.CONTENT_TYPE_SERIES,
                'object_response': {'rich_info': {'vh_meta': {
                    'content_groups': [{'child_licenses': []}]}}}}),
        ]

    @property
    def kinopoisk_thin_card_detail(self):
        return [
            {'content_id': '123', 'ott_metadata': {'not_empty': '123'}},
            {'content_id': '123', 'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123',
                                                     'ottParams': {'licenses': [{'monetizationModel': 'SVOD',
                                                                                 'primary': True,
                                                                                 'purchaseTag': 'kp-basic'}]}}},
            {'content_id': '123', 'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123',
                                                     'includes': [{'ottParams': {'licenses': [
                                                         {'monetizationModel': 'SVOD',
                                                          'primary': True,
                                                          'purchaseTag': 'kp-amediateka'}]}}]}},
            {'content_id': '123', 'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123',
                                                     'ottParams': {'licenses': [{'monetizationModel': 'SVOD',
                                                                                 'primary': True,
                                                                                 'purchaseTag': 'kp-basic'}]}}},
            {'content_id': '123', 'content_detail': {'content_type_name': vh.CONTENT_TYPE_EPISODE, 'content_id': '123',
                                                     'ottParams': {'licenses': [{'monetizationModel': 'SVOD',
                                                                                 'primary': True,
                                                                                 'purchaseTag': 'kp-amediateka'}]}}},
            {'content_id': '123', 'content_detail': {'content_type_name': vh.CONTENT_TYPE_EPISODE, 'content_id': '123',
                                                     'ottParams': {'licenses': [{'monetizationModel': 'SVOD',
                                                                                 'primary': True,
                                                                                 'purchaseTag': 'kp-basic'}]}}},
            {'content_id': '123', 'content_detail': {'content_type_name': vh.CONTENT_TYPE_EPISODE, 'content_id': '123',
                                                     'includes': [{'ottParams': {'licenses': [
                                                         {'monetizationModel': 'SVOD',
                                                          'primary': True,
                                                          'purchaseTag': 'kp-amediateka'}]}}]}},
            {'content_id': '123', 'content_detail': {'content_type_name': vh.CONTENT_TYPE_EPISODE, 'content_id': '123',
                                                     'ottParams': {'licenses': [{'monetizationModel': 'EST',
                                                                                 'primary': True}]}}},
            {'content_id': '123', 'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123',
                                                     'ottParams': {'licenses': [{'monetizationModel': 'EST',
                                                                                 'primary': True}]}}},
            {'content_id': '123', 'content_detail': {'content_type_name': vh.CONTENT_TYPE_EPISODE, 'content_id': '123',
                                                     'ottParams': {'licenses': [{'monetizationModel': 'TVOD',
                                                                                 'primary': True}]}}},
            {'content_id': '123', 'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123',
                                                     'ottParams': {'licenses': [{'monetizationModel': 'TVOD',
                                                                                 'primary': True}]}}},
            {'content_id': '123', 'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES, 'content_id': '123',
                                                     'ottParams': {'licenses': [{'monetizationModel': 'SVOD',
                                                                                 'purchaseTag': 'plus',
                                                                                 'primary': True,
                                                                                 'active': True}],
                                                                   'primary': True}}},
        ]

    @property
    def vh_thin_card_detail(self):
        return [
            {'content_id': '123', 'content_detail': {'content_type_name': vh.CONTENT_TYPE_SERIES,
                                                     'content_id': '123', 'blogger_id': '123'}},
            {'content_id': '123', 'content_detail': {'content_type_name': 'channel', 'content_id': '123'}},
            {'content_id': '123', 'content_detail': {'content_type_name': 'episode', 'content_id': '123'}},
        ]

    @property
    def ott_thin_card_detail(self):
        return [
            {'content_id': '123', 'content_detail': {'content_type_name': vh.CONTENT_TYPE_EPISODE, 'content_id': '123',
                                                     'onto_category': 'film',
                                                     'ottParams': {'licenses': [{'monetizationModel': 'AVOD',
                                                                                 'primary': True,
                                                                                 'active': True}]}}},
        ]

    @property
    def web_thin_card_detail(self):
        return [
            {'content_id': '123', 'content_detail': {'content_type_name': 'unknown_content_type',
                                                     'content_id': '123', }},
            {'content_id': '123', 'content_detail': {
                'content_type_name': vh.CONTENT_TYPE_EPISODE,
                'content_id': '123', 'object_response': {'not empty': '123'}}},
        ]

    @property
    def series_episodes_kinopoisk(self):
        base = {
            "user_data": {
                "req_id": "1595518943642994-2739387437746790529"
            },
            "apphost-reqid": "1595518943642994-273938743774679052900169-er6lxfscucdeficr",
            "set": [
                {
                    "title": "11.22.63 - Сезон 1 - Серия 1 - Кроличья нора. Часть 1",
                    "episode_number": 1,
                    "content_id": "4e1eac497f11de4ab6247e6874f2310f",
                    "onto_id": "ruw6213497",
                    "episode_name": "Кроличья нора. Часть 1",
                    "content_type_name": "vod-episode",
                    "ottParams": {
                        "monetizationModel": "SVOD",
                        "licenses": [{
                            "primary": True,
                            "monetizationModel": "SVOD",
                        }],
                    },
                    "computed_title": "11.22.63 - Сезон 1 - Серия 1 - Кроличья нора. Часть 1",
                },
            ]
        }
        kp = deepcopy(base)
        kp['set'][0]['ottParams']['licenses'][0]['purchaseTag'] = 'kp-basic'
        amediateka = deepcopy(base)
        amediateka['set'][0]['ottParams']['licenses'][0]['purchaseTag'] = 'kp-amediateka'

        return [kp, amediateka]

    @property
    def series_episodes_ott(self):
        return [
            {
                "user_data": {
                    "req_id": "123"
                },
                "apphost-reqid": "123",
                "set": [
                    {
                        "title": "Грабь награбленное - Сезон 1 - Серия 1 - Нигерийское дело",
                        "episode_number": 1,
                        "content_id": "44252eb2518cf71d99d2a0c0e9832812",
                        "onto_id": "ruw1764572",
                        "ottParams": {
                            "monetizationModel": "AVOD",
                            'licenses': [{"monetizationModel": "AVOD", 'primary': True}]
                        },
                        "content_type_name": "vod-episode",
                        "type": "series-episode",
                        "onto_category": "series",
                    }
                ]
            }
        ]


class SearchPlayerIdFillerTestData:

    @property
    def web_clips(self):
        return [
            {
                "is_avod": None,
                "VisibleHost": "youtube.com",
                "title": "Дамблдор соглашается с Северусом.  Гарри Поттер и Кубок огня.",
                "PlayerId": "youtube",
                "duration": 89,
                "is_svod": None,
                "url": "http://www.youtube.com/watch?v=g0vLxn6UPh4"
            }
        ]

    @property
    def ott_clips(self):
        return [
            {
                "is_avod": "1",
                "VisibleHost": "kinopoisk.ru",
                "title": "Исходный код (2011) — фантастика, боевик, HD",
                "PlayerId": "vh",
                "onto_id": "ruw2868427",
                "vh_uuid": "48863683b5ec3ad08474a73adab9a975",
                "duration": 5347,
                "is_svod": None,
                "url": "http://frontend.vh.yandex.ru/player/8938219830857489067"
            }
        ]

    @property
    def ott_assoc(self):
        return [
            {
                "name": "Аполлон 18",
                "quality": 5,
                "id": "ruw3071443",
                "type": "Film",
                "subtitle": "Apollo 18, 2011 (16+)",
                "title": "Аполлон 18",
                "personal_score": 67,
                "age_limit": "16",
                "legal": {
                    "vh_licenses": {
                        "est": {
                            "discount_price": 30,
                            "price": 299,
                            "is_avaliable": True,
                            "EST": False
                        },
                        "svod": {
                            "purchase_tags": [
                                "plus",
                                "kp-basic",
                                "kp-amediateka"
                            ],
                            "is_avaliable": True,
                            "subscriptions": [
                                "YA_PREMIUM",
                                "YA_PLUS",
                                "KP_BASIC",
                                "YA_PLUS_3M"
                            ]
                        },
                        "has_streams": True,
                        "watch_film_url": "https://kinopoisk.ru/film/571324/watch",
                        "uuid": "47bb006604ca83a79a8a823edef3ff2c",
                        "content_type": "MOVIE",
                        "status": "published",
                        "tvod": {
                            "discount_price": 10,
                            "price": 99,
                            "is_avaliable": True,
                            "TVOD": False
                        },
                        "avod": {
                            "is_avaliable": True
                        }
                    }
                },
            }
        ]

    @property
    def kinopoisk_assoc(self):
        return [
            {
                "name": "Гаттака",
                "id": "ruw265758",
                "title": "Гаттака",
                "legal": {
                    "vh_licenses": {
                        "content_type": "MOVIE",
                        "watch_film_url": "https://kinopoisk.ru/film/5012/watch",
                        "status": "published",
                        "est": {
                            "discount_price": 30,
                            "price": 299,
                            "is_avaliable": True,
                            "EST": False
                        },
                        "uuid": "4d10ec5677c602bcb4d7880b62fb2a13",
                        "has_streams": True
                    }
                },
            }
        ]

    @property
    def kinopoisk_parent_collection(self):
        return [
            {
                "subtitle": "The Terminator, 1984 (16+)",
                "search_request": "терминатор фильм 1984",
                "type": "Film",
                "age_limit": "16",
                "id": "ruw14752",
                "legal": {
                    "vh_licenses": {
                        "content_type": "MOVIE",
                        "watch_film_url": "https://kinopoisk.ru/film/507/watch",
                        "status": "published",
                        "uuid": "495b54767a560fa8a384780e1152a14e",
                        "svod": {
                            "purchase_tags": [
                                "kp-basic",
                                "kp-amediateka"
                            ],
                            "is_avaliable": True,
                            "subscriptions": [
                                "KP_BASIC",
                                "YA_PREMIUM"
                            ]
                        },
                        "has_streams": True
                    }
                },
                "title": "Терминатор",
                "name": "Терминатор",
            }
        ]

    @property
    def ott_parent_collection(self):
        return [
            # synthetic example
            {
                "legal": {
                    "vh_licenses": {
                        "uuid": "47bb006604ca83a79a8a823edef3ff2c",
                        "tvod": {
                            "discount_price": 10,
                            "price": 99,
                            "is_avaliable": True,
                            "TVOD": False
                        },
                        "avod": {
                            "is_avaliable": True
                        }
                    }
                },
            }
        ]


required_android_headers = {
    'HTTP_USER_AGENT': 'com.yandex.tv.home/1.2.773 (Realtek SmartTV; Android 7.1.1)',
    'HTTP_X_YAUUID': 'd68c1bca4efa403313837b12f5cdcd26',
    'HTTP_X_YADEVICEID': 'ca9b68da30474d5db0bbd1e8a25565bb',
}


class ExperimentsMock:
    def __init__(self, experiments):
        self.experiments_dict = experiments

    def get_value(self, key):
        return self.experiments_dict.get(key)

    def get_value_or_default(self, key, default=None):
        return self.experiments_dict.get(key) or default


class RequestStub:
    def __init__(self, method: Optional[str] = None, platform_info: Optional[PlatformInfo] = None, experiments=None,
                 authorized=False):
        self.platform_info = platform_info
        self.method = method
        self.GET = []
        self.FILES = {}
        self.headers = {}
        self._messages = mock.Mock()
        self.request_info = RequestInfo(authorized, ExperimentsMock(experiments=experiments or {}))

    def build_absolute_uri(self, *args, **kwargs):
        return ''


class IdentifierFileContent:
    VALID_MAC = '7E:11:6C:5F:B3:54'

    wi_fi_valid_content = f'{ValidIdentifier.WIFI_MAC}\n' \
                          f'{VALID_MAC}'

    ethernet_valid_content = f'{ValidIdentifier.ETHERNET_MAC}\n' \
                             f'{VALID_MAC}'

    invalid_mac_type_content = 'invalid_type'

    empty_mac_value = f'{ValidIdentifier.ETHERNET_MAC}'

    invalid_mac_value = f'{ValidIdentifier.ETHERNET_MAC}\n' \
                        f'invalid_value'


graphql_carousel_500_response = {'carousel': {'status_code': 500}}
graphql_carousel_200_response = {
    'carousel': {
        'status_code': 200,
        'content': {
            'user_data': {
                'req_id': '123'
            },
            'reqid': '123',
            'apphost-reqid': '123',
            'set': [
                {
                    'content_id': '42c6bad243ca8ec8a957b4f4056d4026',
                    'title': 'Атлантида',
                },
                {
                    'content_id': '40569438bea10e59974584684412a2e8',
                    'title': 'Капитан Марвел',
                },
            ],
            'cache_hash': '123'
        }
    },
    'apphost-reqid': '123'
}


series_seasons_big_seasons_response = {
    'set': [
        {"season_number": 1, "episodes_count": 15, 'content_id': '1'},
        {"season_number": 2, "episodes_count": 15, 'content_id': '2'},
        {"season_number": 3, "episodes_count": 15, 'content_id': '3'},
        {"season_number": 4, "episodes_count": 15, 'content_id': '4'},
    ]
}


series_seasons_medium_seasons_response = {
    'set': [
        {"season_number": 1, "episodes_count": 2, 'content_id': '1'},
        {"season_number": 2, "episodes_count": 2, 'content_id': '2'},
        {"season_number": 3, "episodes_count": 2, 'content_id': '3'},
        {"season_number": 4, "episodes_count": 2, 'content_id': '4'},
        {"season_number": 5, "episodes_count": 2, 'content_id': '5'},
        {"season_number": 6, "episodes_count": 2, 'content_id': '6'},
        {"season_number": 7, "episodes_count": 2, 'content_id': '7'},
        {"season_number": 8, "episodes_count": 2, 'content_id': '8'},
        {"season_number": 9, "episodes_count": 2, 'content_id': '9'},
        {"season_number": 11, "episodes_count": 2, 'content_id': '11'},
        {"season_number": 12, "episodes_count": 2, 'content_id': '12'},
        {"season_number": 13, "episodes_count": 2, 'content_id': '13'},
        {"season_number": 14, "episodes_count": 2, 'content_id': '14'},
        {"season_number": 15, "episodes_count": 2, 'content_id': '15'},
        {"season_number": 16, "episodes_count": 2, 'content_id': '16'},
    ]
}


series_seasons_small_seasons_response = {
    'set': [
        {"season_number": 1, "episodes_count": 1, 'content_id': '1'},
        {"season_number": 2, "episodes_count": 1, 'content_id': '2'},
        {"season_number": 3, "episodes_count": 1, 'content_id': '3'},
        {"season_number": 4, "episodes_count": 1, 'content_id': '4'},
        {"season_number": 5, "episodes_count": 1, 'content_id': '5'},
        {"season_number": 6, "episodes_count": 1, 'content_id': '6'},
        {"season_number": 7, "episodes_count": 1, 'content_id': '7'},
        {"season_number": 8, "episodes_count": 1, 'content_id': '8'},
        {"season_number": 9, "episodes_count": 1, 'content_id': '9'},
        {"season_number": 11, "episodes_count": 1, 'content_id': '11'},
        {"season_number": 12, "episodes_count": 1, 'content_id': '12'},
        {"season_number": 13, "episodes_count": 1, 'content_id': '13'},
        {"season_number": 14, "episodes_count": 1, 'content_id': '14'},
        {"season_number": 15, "episodes_count": 1, 'content_id': '15'},
        {"season_number": 16, "episodes_count": 1, 'content_id': '16'},
    ]
}


kp_document_serializer_licence_info_mocks = [
    ({"watchingOption": {"type": "PAID_MULTIPLE", "monetizations": ["EST", "TVOD"], "purchased": False,
                         "minimumPriceDetails": {"value": 169, "currency": "RUB"}, "minimumPrice": 169,
                         "discountPriceDetails": {"value": 135, "currency": "RUB"}},
      "monetizationModels": ["EST", "TVOD"]},
     {'owned': {'TVOD': False}, 'licenses': ['TVOD'], 'ottParams': {'monetizationModel': 'TVOD', 'primary': True,
                                                                    'active': False}}),
    ({"watchingOption": {"type": "PAID_MULTIPLE", "monetizations": ["EST", "TVOD"], "purchased": True,
                         "minimumPriceDetails": {"value": 169, "currency": "RUB"}, "minimumPrice": 169,
                         "discountPriceDetails": {"value": 135, "currency": "RUB"}},
      "monetizationModels": ["EST", "TVOD"]},
     {'owned': {'TVOD': True}, 'licenses': ['TVOD'], 'ottParams': {'monetizationModel': 'TVOD', 'primary': True,
                                                                   'active': True}}),
    ({"watchingOption": {"type": "PAID", "monetizations": ["EST"], "purchased": False,
                         "minimumPriceDetails": {"value": 169, "currency": "RUB"}, "minimumPrice": 169,
                         "discountPriceDetails": {"value": 135, "currency": "RUB"}},
      "monetizationModels": ["EST"]},
     {'owned': {'EST': False}, 'licenses': ['EST'], 'ottParams': {'monetizationModel': 'EST', 'primary': True,
                                                                  'active': False}}),
    ({"watchingOption": {"type": "PAID", "monetizations": ["EST"], "purchased": True,
                         "minimumPriceDetails": {"value": 169, "currency": "RUB"}, "minimumPrice": 169,
                         "discountPriceDetails": {"value": 135, "currency": "RUB"}}, "monetizationModels": ["EST"]},
     {'owned': {'EST': True}, 'licenses': ['EST'], 'ottParams': {'monetizationModel': 'EST', 'primary': True,
                                                                 'active': True}}),
    ({"watchingOption": {"type": "SUBSCRIPTION", "monetizations": ["SVOD"], "purchased": False,
                         "subscription": "YA_PREMIUM", "subscriptionPurchaseTag": "kp-amediateka"},
      "monetizationModels": ["SVOD"]},
     {'owned': {'SVOD': False}, 'licenses': ['SVOD'], 'ottParams': {'monetizationModel': 'SVOD', 'primary': True,
                                                                    'active': False, 'purchaseTag': 'kp-amediateka'},
      'ya_plus': ['YA_PREMIUM']}),
    ({"watchingOption": {"type": "SUBSCRIPTION", "monetizations": ["SVOD"], "purchased": True,
                         "subscription": "YA_PREMIUM", "subscriptionPurchaseTag": "kp-amediateka"},
      "monetizationModels": ["SVOD"]},
     {'owned': {'SVOD': True}, 'licenses': ['SVOD'], 'ottParams': {'monetizationModel': 'SVOD', 'primary': True,
                                                                   'active': True, 'purchaseTag': 'kp-amediateka'},
      'ya_plus': ['YA_PREMIUM']}),
    ({"watchingOption": {"type": "FREE", "monetizations": ["SVOD", "AVOD"], "purchased": True},
      "monetizationModels": ["AVOD"]},
     {'owned': {'AVOD': True}, 'licenses': ['AVOD'], 'ottParams': {'monetizationModel': 'AVOD', 'primary': True,
                                                                   'active': True}})
]


alice_for_business_mock_response = {
    "imageUrl": "https://avatars.mds.yandex.net/get-travel-hotels/2000187/meta/%%",
    "infoUrl": "https://travel.yandex.ru/promo/metaguidetv",
    "title": "Добро пожаловать в отель!",
    "subtitle": "Нажмите и посмотрите, что интересного в отеле и рядом"
}


class UserInfoMock:
    anonymouse_user = UserInfo()
    user_with_no_subscription = UserInfo(1, {})
    user_with_ya_plus = UserInfo(1, {DbFields.have_plus: '1', DbFields.kinopoisk_ott_subscription_name: 'YA_PLUS'})


get_vh_carousel_item = lambda: {
    "parent_id": "46a299cbc63c865db2e7cc49296e4a5e",
    "catchup_age": 0,
    "licenses": [
        {
            "total_purchases": 0,
            "monetizationModel": "EST",
            "price": 399,
            "price_with_discount": 39
        },
        {
            "monetizationModel": "SVOD",
            "subscriptionTypes": [
                "YA_PLUS",
                "KP_BASIC",
                "YA_PLUS_KP",
                "YA_PREMIUM",
                "YA_PLUS_3M",
                "YA_PLUS_SUPER"
            ]
        },
        {
            "total_purchases": 0,
            "monetizationModel": "TVOD",
            "price": 169,
            "price_with_discount": 16
        }
    ],
    "supertag": "movie",
    "channel_multiplex_number": 0,
    "percentage_score": 71,
    "sport_event": {},
    "download_episodes": 0,
    "content_url": "https://strm.yandex.ru/vh-ottenc-converted/vod-content/4599fa80d6df7c03ada9b62708f00658/9053005x1642764291x2d4dbe79-b07a-4982-a5d9-0729cbdb812e/mss-pr.ism/manifest_sdr_hd_avc_aac.ismc",  # noqa
    "title": "Главный герой",
    "supertag_title": "Фильм",
    "stream": {
        "trackings": {
            "trackingEvents": []
        },
        "withCredentials": True
    },
    "rating_kp": 7.389999866,
    "actors": "Райан Рейнольдс, Джоди Комер, Тайка Вайтити",
    "auto_fields": {
        "with_cmnt": True,
        "cmnt_id": "4599fa80d6df7c03ada9b62708f00658"
    },
    "thumbnail": "//avatars.mds.yandex.net/get-vh/5480927/2a0000017efd3e8489c4468df08aceeb8286/orig",
    "meta": {
        "weight": 0,
        "with_sport_prediction": False,
        "with_overlay": False,
        "with_deep_hd": False,
        "without_timeline": False
    },
    "content_id": "4599fa80d6df7c03ada9b62708f00658",
    "skippableFragments": [
        {
            "endTime": 45,
            "type": "intro",
            "result": "OK",
            "startTime": 0
        },
        {
            "endTime": 6626,
            "type": "credits",
            "result": "OK",
            "startTime": 6205
        }
    ],
    "adTags": {
        "content-genre": [],
        "brand-safety-categories": [],
        "content-category": []
    },
    "orbital_channel": False,
    "cover": "//avatars.mds.yandex.net/get-ott/374297/2a0000017bd0bb4ee0941aec3aa1ccd7ea5c/orig",
    "adConfig": {
        "distr_id": 0,
        "partner_id": 0,
        "hasPreroll": False,
        "vh_sid": "VH-SID-1646642390228767-7436076183246605799-hj4yuoqbv3jqwvym-BAL-1759",
        "category": 1018,
        "useAdSession": True,
        "video_content_name": "Главный герой",
        "video_content_id": "4599fa80d6df7c03ada9b62708f00658",
        "video_genre_name": []
    },
    "release_year": "2021",
    "onto_id": "ruw7953638",
    "views_count": 2577417,
    "embed_url": "https://frontend.vh.yandex.ru/player/4599fa80d6df7c03ada9b62708f00658?from=tvandroid",
    "main_color": "#081830",
    "can_play_on_station": False,
    "directors": "Шон Леви",
    "ya_plus": [
        "YA_PLUS",
        "KP_BASIC",
        "YA_PLUS_KP",
        "YA_PREMIUM",
        "YA_PLUS_3M",
        "YA_PLUS_SUPER"
    ],
    "deprecated_content_id": "6506047141026712401",
    "onto_otype": "Film/Film",
    "producers": "Грег Берланти, Адам Колбеннер, Сара Шехтер",
    "update_time": 1646146098,
    "has_cachup": 1,
    "ottParams": {
        "monetizationModel": "SVOD",
        "serviceName": "ya-tv-android",
        "contentTypeID": 20,
        "kpId": "1199100",
        "licenses": [
            {
                "monetizationModel": "SVOD",
                "primary": True,
                "purchaseTag": "plus"
            }
        ],
        "reqid": "1646642390228767-7436076183246605799-hj4yuoqbv3jqwvym-BAL",
        "yandexuid": "8336489141646210595",
        "uuid": "4599fa80d6df7c03ada9b62708f00658",
        "from": "tvandroid",
        "subscriptionType": ""
    },
    "countries": "США, Канада, Япония",
    "deep_hd": False,
    "is_special_project": False,
    "serp_url": "https://yandex.ru/video/search?text=url%3Ahttps%3A%2F%2Ffrontend%2Evh%2Eyandex%2Eru%2Fplayer%2F6506047141026712401",
    "short_description": "Банковский клерк обнаруживает, что он персонаж видеоигры. Фантастическая экшен-комедия с Райаном Рейнольдсом",
    "ott_status": "published",
    "computed_title": "Главный герой",
    "has_vod": 0,
    "dvr": 0,
    "ya_video_preview": "https://video-preview.s3.yandex.net/vh/6506047141026712401_vmaf-preview-536.mp4",
    "streams": [
        {
            "stream_type": "DASH"
        }
    ],
    "onto_poster": "//avatars.mds.yandex.net/get-ott/1652588/2a0000017efd3e5f19289757d9d427e01d65/orig",
    "restriction_age": 16,
    "content_type_name": "vod-episode",
    "blacked": 0,
    "logo": "http://avatars.mds.yandex.net/get-ott/1534341/2a0000017ef7781f78c13636d2b4d89e5155/orig",
    "onto_category": "film",
    "theme": {
        "title": "Фантастика",
        "id": "ChJoaGJ1ZHl2aXR1dGhvcnliaGgSA2FsbBojbW92aWUmZ2VucmVfZmFudGFzdGljJnBvc3Rlcl9leGlzdHMgAQ=="
    },
    "duration": 6627,
    "genres": [
        "фантастика",
        "боевик",
        "комедия",
        "мелодрама"
    ],
    "internal": False,
    "has_schedule": 1,
    "description": "У сотрудника крупного банка всё идёт по накатанной"
}

vh_valid_response = {
    "user_data": {
        "req_id": "1644829742121010-7341514597644793268-jhpguuwb44l7o6ki-BAL"
    },
    "request_info": {
        "apphost-reqid": "1644829742121010-7341514597644793268-jhpguuwb44l7o6ki-BAL-5987",
        "nonbanned_docs_count": 10,
        "logging_url": "https://strm.yandex.ru/get/4KKQf1NhbHRlZF9fAvMk9TxLavhE94XwwBPhPQEVzvvdxthenUbbN28q3sDPy3RLOUr3Rw,,"
    },
    "reqid": "1644829742121010-7341514597644793268-jhpguuwb44l7o6ki-BAL-5987",
    "cache_hash": "bwAAAAAAAAAotS_9IG8VAwAEBRIAGAAiAioAMAA4AEJfCA8QCxgAIlEXAAAACgAW__8_YAEGAAAA_AdoAnMJAADk_SAQhBZAaFMcSD4DAAApAHqCBBleL11E3CgJMAE4AEoABgB2Blu54WarNLGxtAwGnA,,",
    "set": [
        get_vh_carousel_item(),
    ]
}

get_vh_carousel_as_search_result = lambda: {
    "galleries": [
        {
            "items": [
                {
                    "content_type": "entity",
                    "video_item": {
                        "type": "movie",
                        "provider_name": "kinopoisk",
                        "provider_item_id": "4599fa80d6df7c03ada9b62708f00658",
                        "misc_ids": {"onto_id": "ruw7953638"},
                        "available": 1,
                        "name": "Главный герой",
                        "normalized_name": "главный герой",
                        "description": "У сотрудника крупного банка всё идёт по накатанной",
                        "rating": 7.389999866,
                        "release_year": 2021,
                        "provider_info": [
                            {
                                "type": "movie",
                                "provider_name": "kinopoisk",
                                "provider_item_id": "4599fa80d6df7c03ada9b62708f00658",
                                "available": 1,
                            }
                        ],
                        "min_age": 16,
                        "age_limit": "16",
                        "thumbnail": {
                            "base_url": "https://avatars.mds.yandex.net/get-vh/5480927/2a0000017efd3e8489c4468df08aceeb8286/",
                            "sizes": ["orig"],
                        },
                        "poster": {
                            "base_url": "https://avatars.mds.yandex.net/get-ott/1652588/2a0000017efd3e5f19289757d9d427e01d65/",
                            "sizes": [
                                "120x90",
                                "400x300",
                                "360x540",
                                "1920x1080",
                                "orig",
                            ],
                        },
                        "vh_licenses": {
                            "svod": [
                                "YA_PLUS"
                            ],
                            "content_type": "MOVIE",
                        },
                        "hint_description": "2021, фантастика, боевик, комедия, мелодрама",
                    },
                }
            ]
        }
    ]
}
