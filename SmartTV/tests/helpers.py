import json
import io
from django.core.files.uploadedfile import InMemoryUploadedFile
from copy import deepcopy


def load_json_fixture(path):
    with open(path, 'r', encoding='utf-8') as file:
        return json.loads(file.read())


def remove_part(path: list, target_obj: dict):
    """
    Allows to easily remove some parts of the object
    Args:
        path (list): the list of path parts. All parts from 0 to N-2 must be int / str, the last one(N-1) can be list,
            which means, multiple objects can be removed
            Possible values: [['a']], ['nested_obj', ['a']], ['nested_obj', 0, ['a']], ['nested_obj', [0]],
        target_obj (dict): From this object requested parts will be removed
    """
    if not path:
        return target_obj
    full_path = path[:-1]
    target_part = path[-1:][0]
    target_part.reverse()
    intermediate_path = target_obj
    for path_part in full_path:
        if isinstance(intermediate_path, dict):
            intermediate_path = intermediate_path.get(path_part)
        elif isinstance(intermediate_path, list):
            intermediate_path = intermediate_path[path_part]
        if not intermediate_path:
            raise ValueError('Provided incorrect path')

    for item in target_part:
        del intermediate_path[item]

    return target_obj


def to_in_memory_uploaded_file(input: str) -> InMemoryUploadedFile:
    bytes = input.encode()
    file_content = io.BytesIO(bytes)
    return InMemoryUploadedFile(
        file=file_content,
        content_type='text/plain',
        size=len(bytes),
        charset='utf-8',
        name='test.txt',
        field_name=None
    )


class PaginationInfo:
    offset = 0
    limit = 0

    def __init__(self, offset, limit):
        self.offset = offset
        self.limit = limit


class ResponsesGenerator:
    carousel_item = {
        'parent_id': '46a299cbc63c865db2e7cc49296e4a5e',
        'catchup_age': 0,
        'percentage_score': 92,
        'download_episodes': 0,
        'content_url': 'https://strm.yandex.ru/vh-ott-converted/ott-content/a4cf-e3a42af4894d.ism/manifest.mpd',
        'title': 'Генсбур. Любовь хулигана',
        'rating_kp': 7.590000153,
        'actors': 'Эрик Элмоснино, Люси Гордон, Летиция Каста',
        'thumbnail': '//avatars.mds.yandex.net/get-vh/3532317/9436859426044245449-RHbwh51CrlxsB6a671167xsg-1595615575/orig',
        'meta': {
            'without_timeline': False
        },
        'content_id': '4bc38d0f327f252ca4cfe3a42af4894d',
        'release_year': '2010',
        'onto_id': 'ruw2489622',
        'main_color': '#301020',
        'onto_otype': 'Film/Film',
        'producers': 'Марк Ду Понтавиче, Мэттью Гледхилл, Дидье Люпфер',
        'show_tv_promo': 'post',
        'update_time': 1594648199,
        'has_cachup': 1,
        'ottParams': {
            'monetizationModel': 'AVOD',
            'licenses': [
                {
                    'monetizationModel': 'AVOD',
                    'active': True,
                    'primary': True
                }
            ],
        },
        'countries': 'Франция',
        'deep_hd': False,
        'short_description': 'Музыкальный фильм о знаменитом певце и поэте Серже Генсбуре',
        'computed_title': 'Генсбур. Любовь хулигана (2010) — драма, биографический, HD',
        'ya_video_preview': 'https://video-preview.s3.yandex.net/vh/9436859426044245449/720p.mp4',
        'streams': [
            {
                'stream_type': 'DASH',
                'url': 'https://strm.yandex.ru/vh-ott-converted/ott-content/500519814/4bc38d0f-327f-252c-a4cf-e3a42af4894d.ism/manifest_quality.mpd',
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
                        'verificationRequired': False,
                        'monetizationModel': 'AVOD',
                        'contentId': '4bc38d0f327f252ca4cfe3a42af4894d',
                        'productId': 2,
                        'serviceName': 'ya-tv-android',
                        'expirationTimestamp': 1596662072,
                        'puid': 493032576,
                        'contentTypeId': 20,
                        'watchSessionId': 'dcb6b67dcb2a4ee5a667526c3c3091f3',
                        'signature': '45902b208ba6b1c07d1d143f5944569fb953a411',
                        'version': 'V4'
                    }
                }
            },
            {
                'stream_type': 'HLS',
                'url': 'https://strm.yandex.ru/vh-ott-converted/ott-content/500519814/master_quality.m3u8'
            }
        ],
        'onto_poster': '//avatars.mds.yandex.net/get-vh/103154/9436859426044245449-1531894275/orig',
        'is_new_delayed_episode': True,
        'is_next_delayed_episode': True,
        'restriction_age': 18,
        'content_type_name': 'vod-episode',
        'onto_category': 'film',
        'duration': 7778,
        'genres': [
            'драма',
            'биография',
            'музыка'
        ],
        'has_schedule': 1,
        'description': 'В жизни легендарного французского шансонье было много женщин.'
    }

    def generate_vh_carousel_items(self, count: int):
        return [deepcopy(self.carousel_item) for _ in range(count)]

    def generate_vh_carousel_object(self, items_count):
        items = self.generate_vh_carousel_items(items_count)
        return {
            'title': 'Фильмы для вас',
            'carousel_id': 'CATEG_FILM',
            'includes': items,
        }

    def generate_vh_carousel_response(self, items_count):
        carousel = self.generate_vh_carousel_object(items_count)
        carousel['user_data'] = {'req_id': '1596641369156061-7023720469802306536'}
        carousel['reqid'] = '1596641369156061-702372046980230653600114-ytlltxs7dtxqt522'
        carousel['apphost-reqid'] = '1596641369156061-702372046980230653600114-ytlltxs7dtxqt522'
        carousel['set'] = carousel.pop('includes')
        return carousel

    def generate_vh_carousels_object(self, carousels_amount, items_amount_per_carousel):
        carousels = [self.generate_vh_carousel_object(items_amount_per_carousel) for _ in range(carousels_amount)]
        return {
            'user_data': {'req_id': '1596640472336438-11173066188852132466'},
            'reqid': '1596640472336438-1117306618885213246600164-mxoc2elrvx7uxo4a',
            'apphost-reqid': '1596640472336438-1117306618885213246600164-mxoc2elrvx7uxo4a',
            'cache_hash': 'efg',
            'items': carousels
        }

    def generate_kp_carousel_item(self):
        return {
            'id': '5ecd2e6e6d290d00210b5cf2',
            'title': 'Зов предков',
            'type': 'ITEM_CONTENT',
            'filmId': '45f134200076e6d791d9a6727422bb6e',
            'kpId': 1060511,
            'kpRating': 7.236000061035156,
            'genres': [
                'драма',
                'приключения',
                'семейный'
            ],
            'posterUrl': 'http://avatars.mds.yandex.net/get-ott/239697/2a00000171cf38b9a3de5c7ba8615d0e2814/orig',
            'horizontalPoster': 'http://avatars.mds.yandex.net/get-ott/2385704/2a00000171cf3903579707ad6a1a1273d9da/orig',
            'monetizationModels': [
                'EST',
                'TVOD'
            ],
            'years': '2020',
            'shortDescription': 'Харрисон Форд в уютной экранизации романа Джека Лондона. Отважный пес Бэк',
            'contentType': 'ott-movie',
            'restrictionAge': 6,
            'countries': [
                'США',
                'Канада'
            ],
            'medianColor': '#254646',
            'duration': 5740,
        }

    def generate_kp_carousel_items(self, items_amount):
        return [self.generate_kp_carousel_item() for _ in range(items_amount)]

    def generate_kp_carousel_object(self, items_amount, start_rank=1):
        return {
            "data": self.generate_kp_carousel_items(items_amount),
            'selectionId': 'personal_films',
            'selection_window_id': 'ya_tv',
            'selection_id': 'personal_films',
            'type': 'SELECTION',
            "pagingMeta": {
                "from": start_rank - 1,
                "to": start_rank - 1 + items_amount,
                "hasMore": True,
                "sessionId": "5534b18e-7fc1-4140-bfcf-f8d6556c86aa"
            }
        }

    def generate_kp_feed_response(self, carousels_amount: int, items_count: int):
        return {
            'status': 'success',
            'collections': [self.generate_kp_carousel_object(items_count) for _ in range(carousels_amount)],
            'unavailabilityDetails': [],
            'pagingMeta': {
                'from': 0,
                'to': 10,
                'hasMore': True,
                'sessionId': 'window-1627311250754551-18268493973860926464:7419:1632-1627311250953'
            }
        }

    def generate_filter_identities_response(self, include_genre=True, include_year=True, include_rating=True):
        result = []
        if include_genre:
            result.append(
                {'type': 'select', 'title': 'Жанр', 'id': 'genre', 'values': [
                    {'title': 'Фантастика', 'id': 'ruw74051'}, {'title': 'Триллер', 'id': 'ruw98644'},
                    {'title': 'Биография', 'id': 'ruw267584'}]})
        if include_year:
            result.append({'begin': 1900, 'type': 'range', 'title': 'Год', 'step': 1, 'id': 'year', 'end': 3000})
        if include_rating:
            result.append({'begin': 0, 'type': 'range', 'title': 'Рейтинг КиноПоиска', 'step': 0.01, 'id': 'kp_rating',
                           'end': 10})
        return result

    def generate_music_carousel_items(self, item_count: int) -> dict:
        return deepcopy({
            'type': 'auto-playlist',
            'autoPlaylistType': 'playlist-of-the-day',
            'data': {
                'uid': 503646255,
                'kind': 71908726,
                'title': 'Плейлист дня',
                'modified': '2021-07-26T03:00:00+00:00',
                'cover': {
                    'type': 'pic',
                    'dir': '/get-music-user-playlist/34120/qqu99vZMZcX7wN/',
                    'version': '1618259939547',
                    'uri': 'avatars.yandex.net/get-music-user-playlist/28719/qrgxcywwhQvqbu/%%?1618259939547',
                    'custom': True
                }
            }
        })

    def generate_music_carousel(self, item_count: int):
        return deepcopy({
            'rowId': 'JDJemb9h2tFjMWAu',
            'type': 'general',
            'typeForFrom': 'inf_feed_auto_playlists',
            'title': 'Собрано для вас',
            'entities': [self.generate_music_carousel_items(item_count) for _ in range(item_count)]
        })

    def generate_music_infinite_feed_response(self, carousels_amount: int, items_amount: int):
        return deepcopy({
            'invocationInfo': {
                'hostname': 'music-stable-back-vla-35.vla.yp-c.yandex.net',
                'req-id': '1627311325261251-4008644984033393827',
                'exec-duration-millis': '217'
            },
            'result': {
                'pumpkin': False,
                'hasNextBatch': False,
                'batchNumber': 0,
                'rows': [self.generate_music_carousel(items_amount) for _ in range(carousels_amount)]
            }
        })
