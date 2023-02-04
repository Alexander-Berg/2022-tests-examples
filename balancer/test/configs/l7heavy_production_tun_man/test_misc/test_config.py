# -*- coding: utf-8 -*-
import pytest
from balancer.test.configs.lib.locations_test import one_enabled_location_knoss_only, knoss_only_locations_backends_canon_test
from balancer.test.mock.mocked_balancer.term_ctx import knoss_backends_single_attempt_with_ids, knoss_backends_with_ids
from balancer.test.util import asserts


NATIVE_LOCATION = 'MAN'

# ==== VIDEO ====

_VIDEO_SB_BACKENDS = [
    ['backends_man#awacs-rtc_balancer_knoss_video_yp_man'],
    ['backends_sas#awacs-rtc_balancer_knoss_video_yp_sas', 'backends_vla#awacs-rtc_balancer_knoss_video_yp_vla'],
]

_VIDEO_SB_BACKENDS, _VIDEO_SB_IDS = knoss_backends_with_ids(_VIDEO_SB_BACKENDS)
_VIDEO_SB_BACKENDS_FAILING, _VIDEO_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_VIDEO_SB_BACKENDS)


@pytest.mark.parametrize('successful_backend', _VIDEO_SB_BACKENDS, ids=_VIDEO_SB_IDS)
def test_video(term_ctx, successful_backend):
    term_ctx.run_knoss_test(
        'video', '/video', successful_backend, location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('backends', _VIDEO_SB_BACKENDS_FAILING, ids=_VIDEO_SB_FAILING_IDS)
def test_video_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test(
        'video', '/video', backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('video'))
def test_video_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        'video', '/video', location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('successful_backend', _VIDEO_SB_BACKENDS, ids=_VIDEO_SB_IDS)
def test_video_header(term_ctx, successful_backend):
    term_ctx.run_knoss_header_test('video', '/video', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('successful_backend', _VIDEO_SB_BACKENDS, ids=_VIDEO_SB_IDS)
def test_video_xml(term_ctx, successful_backend):
    term_ctx.run_knoss_test(
        'video-xml', '/video-xml', successful_backend, location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('backends', _VIDEO_SB_BACKENDS_FAILING, ids=_VIDEO_SB_FAILING_IDS)
def test_video_xml_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test(
        'video-xml', '/video-xml', backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('video-xml'))
def test_video_xml_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        'video-xml', '/video-xml', location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('successful_backend', _VIDEO_SB_BACKENDS, ids=_VIDEO_SB_IDS)
def test_video_xml_header(term_ctx, successful_backend):
    term_ctx.run_knoss_header_test(
        'video-xml', '/video-xml', successful_backend, location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('successful_backend', _VIDEO_SB_BACKENDS, ids=_VIDEO_SB_IDS)
def test_video_api(term_ctx, successful_backend):
    term_ctx.run_knoss_test(
        'video_api', '/video/api', successful_backend, location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('backends', _VIDEO_SB_BACKENDS_FAILING, ids=_VIDEO_SB_FAILING_IDS)
def test_video_api_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test(
        'video_api', '/video/api', backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('video_api'))
def test_video_api_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        'video_api', '/video/api', location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('successful_backend', _VIDEO_SB_BACKENDS, ids=_VIDEO_SB_IDS)
def test_video_api_header(term_ctx, successful_backend):
    term_ctx.run_knoss_header_test(
        'video_api', '/video/api', successful_backend, location=NATIVE_LOCATION
    )


# ==== USLUGI ====

_USLUGI_SB_BACKENDS = [
    ['backends_man#rtc_balancer_uslugi_yandex_ru_man'],
    ['backends_sas#rtc_balancer_uslugi_yandex_ru_sas', 'backends_vla#rtc_balancer_uslugi_yandex_ru_vla'],
]

_USLUGI_SB_BACKENDS, _USLUGI_SB_IDS = knoss_backends_with_ids(_USLUGI_SB_BACKENDS)
_USLUGI_SB_BACKENDS_FAILING, _USLUGI_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_USLUGI_SB_BACKENDS)


USLUGI_PATHS = [
    '/uslugi',
    '/uslugi/api',
    '/uslugi/sitemap',
]


@pytest.mark.parametrize('path', USLUGI_PATHS)
@pytest.mark.parametrize('successful_backend', _USLUGI_SB_BACKENDS, ids=_USLUGI_SB_IDS)
def test_uslugiapi(term_ctx, successful_backend, path):
    term_ctx.run_knoss_test('uslugiapi', path, successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', USLUGI_PATHS)
@pytest.mark.parametrize('backends', _USLUGI_SB_BACKENDS_FAILING, ids=_USLUGI_SB_FAILING_IDS)
def test_uslugiapi_single_attempt(term_ctx, backends, path):
    term_ctx.run_knoss_single_attempt_test(
        'uslugiapi', path, backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('path', USLUGI_PATHS)
@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('uslugiapi'))
def test_uslugiapi_location_weights(term_ctx, locations_weights_info, path):
    term_ctx.run_knoss_location_weights_test(
        'uslugiapi', path, location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('path', USLUGI_PATHS)
@pytest.mark.parametrize('successful_backend', _USLUGI_SB_BACKENDS, ids=_USLUGI_SB_IDS)
def test_uslugiapi_header(term_ctx, successful_backend, path):
    term_ctx.run_knoss_header_test('uslugiapi', path, successful_backend, location=NATIVE_LOCATION)


# ==== GNC ====

_GNC_SB_BACKENDS = [
    ['backends_man#rtc_balancer_gnc_yandex_ru_man'],
    ['backends_sas#rtc_balancer_gnc_yandex_ru_sas', 'backends_vla#rtc_balancer_gnc_yandex_ru_vla'],
]

_GNC_SB_BACKENDS, _GNC_SB_IDS = knoss_backends_with_ids(_GNC_SB_BACKENDS)
_GNC_SB_BACKENDS_FAILING, _GNC_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_GNC_SB_BACKENDS)


GNC_PATHS = [
    '/gnc',
]


@pytest.mark.parametrize('path', GNC_PATHS)
@pytest.mark.parametrize('successful_backend', _GNC_SB_BACKENDS, ids=_GNC_SB_IDS)
def test_gncapi(term_ctx, successful_backend, path):
    term_ctx.run_knoss_test('gncapi', path, successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', GNC_PATHS)
@pytest.mark.parametrize('backends', _GNC_SB_BACKENDS_FAILING, ids=_GNC_SB_FAILING_IDS)
def test_gncapi_single_attempt(term_ctx, backends, path):
    term_ctx.run_knoss_single_attempt_test(
        'gncapi', path, backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('path', GNC_PATHS)
@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('gncapi'))
def test_gncapi_location_weights(term_ctx, locations_weights_info, path):
    term_ctx.run_knoss_location_weights_test(
        'gncapi', path, location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('path', GNC_PATHS)
@pytest.mark.parametrize('successful_backend', _GNC_SB_BACKENDS, ids=_GNC_SB_IDS)
def test_gncapi_header(term_ctx, successful_backend, path):
    term_ctx.run_knoss_header_test('gncapi', path, successful_backend, location=NATIVE_LOCATION)


# ==== NEWS ====

_NEWS_SB_BACKENDS = [
    ['backends_man#awacs-rtc_balancer_news_yandex_ru_yp_man'],
    ['backends_sas#awacs-rtc_balancer_news_yandex_ru_yp_sas', 'backends_vla#awacs-rtc_balancer_news_yandex_ru_yp_vla'],
]

_NEWS_SB_BACKENDS, _NEWS_SB_IDS = knoss_backends_with_ids(_NEWS_SB_BACKENDS)
_NEWS_SB_BACKENDS_FAILING, _NEWS_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_NEWS_SB_BACKENDS)


NEWS_PATHS = [
    '/news',
    '/mirror',
    '/sport',
]


@pytest.mark.parametrize('path', NEWS_PATHS)
@pytest.mark.parametrize('successful_backend', _NEWS_SB_BACKENDS, ids=_NEWS_SB_IDS)
def test_news(term_ctx, successful_backend, path):
    term_ctx.run_knoss_test('news', path, successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', NEWS_PATHS)
@pytest.mark.parametrize('backends', _NEWS_SB_BACKENDS_FAILING, ids=_NEWS_SB_FAILING_IDS)
def test_news_single_attempt(term_ctx, backends, path):
    term_ctx.run_knoss_single_attempt_test('news', path, backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', NEWS_PATHS)
@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('news'))
def test_news_location_weights(term_ctx, locations_weights_info, path):
    term_ctx.run_knoss_location_weights_test('news', path, location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('path', NEWS_PATHS)
@pytest.mark.parametrize('successful_backend', _NEWS_SB_BACKENDS, ids=_NEWS_SB_IDS)
def test_news_header(term_ctx, successful_backend, path):
    term_ctx.run_knoss_header_test('news', path, successful_backend, location=NATIVE_LOCATION)


# ==== UGC ====

_UGC_SB_BACKENDS = [
    ['backends_man#awacs-rtc_balancer_ugc_search_yandex_net_yp_man'],
    ['backends_sas#awacs-rtc_balancer_ugc_search_yandex_net_yp_sas', 'backends_vla#awacs-rtc_balancer_ugc_search_yandex_net_yp_vla'],
]
_UGC_SB_BACKENDS, _UGC_SB_IDS = knoss_backends_with_ids(_UGC_SB_BACKENDS)
_UGC_SB_BACKENDS_FAILING, _UGC_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_UGC_SB_BACKENDS)


UGC_SERVICES = [
    ('/ugcpub', 'ugcpub'),
    ('/user', 'user'),
    ('/my', 'ugc_my'),
]


@pytest.mark.parametrize(['path', 'service'], UGC_SERVICES)
@pytest.mark.parametrize('successful_backend', _UGC_SB_BACKENDS, ids=_UGC_SB_IDS)
def test_ugc(term_ctx, successful_backend, path, service):
    term_ctx.run_knoss_test(
        service, path, successful_backend, location=NATIVE_LOCATION
    )


@pytest.mark.parametrize(['path', 'service'], UGC_SERVICES)
@pytest.mark.parametrize('backends', _UGC_SB_BACKENDS_FAILING, ids=_UGC_SB_FAILING_IDS)
def test_ugc_single_attempt(term_ctx, backends, path, service):
    term_ctx.run_knoss_single_attempt_test(
        service, path, backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('ugcpub'))
def test_ugcpub_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        'ugcpub', '/ugcpub', location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('user'))
def test_user_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        'user', '/user', location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('ugc_my'))
def test_ugc_my_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        'ugc_my', '/my', location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize(['path', 'service'], UGC_SERVICES)
@pytest.mark.parametrize('successful_backend', _UGC_SB_BACKENDS, ids=_UGC_SB_IDS)
def test_ugc_header(term_ctx, successful_backend, path, service):
    term_ctx.run_knoss_header_test(service, path, successful_backend, location=NATIVE_LOCATION)


# ==== SERVICE-WORKERS ====

_SERVICE_WORKERS_SB_BACKENDS = [
    ['backends_MAN_SERVICE_WORKERS_BALANCER_hbf_mtn_1_'],
    ['backends_SAS_SERVICE_WORKERS_BALANCER_hbf_mtn_1_', 'backends_VLA_SERVICE_WORKERS_BALANCER_hbf_mtn_1_'],
]

_SERVICE_WORKERS_SB_BACKENDS, _SERVICE_WORKERS_SB_IDS = knoss_backends_with_ids(_SERVICE_WORKERS_SB_BACKENDS)
_SERVICE_WORKERS_SB_BACKENDS_FAILING, _SERVICE_WORKERS_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_SERVICE_WORKERS_SB_BACKENDS)


@pytest.mark.parametrize('path', ['/service-workers'])
@pytest.mark.parametrize('successful_backend', _SERVICE_WORKERS_SB_BACKENDS, ids=_SERVICE_WORKERS_SB_IDS)
def test_service_workers(term_ctx, successful_backend, path):
    term_ctx.run_knoss_test('serviceworkers', path, successful_backend, has_antirobot=False, location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', ['/service-workers'])
@pytest.mark.parametrize('backends', _SERVICE_WORKERS_SB_BACKENDS_FAILING, ids=_SERVICE_WORKERS_SB_FAILING_IDS)
def test_service_workers_single_attempt(term_ctx, backends, path):
    term_ctx.run_knoss_single_attempt_test(
        'serviceworkers', path, backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('path', ['/service-workers'])
@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('serviceworkers'))
def test_service_workers_location_weights(term_ctx, locations_weights_info, path):
    term_ctx.run_knoss_location_weights_test(
        'serviceworkers', path, has_antirobot=False, location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('path', ['/service-workers'])
@pytest.mark.parametrize('successful_backend', _SERVICE_WORKERS_SB_BACKENDS, ids=_SERVICE_WORKERS_SB_IDS)
def test_service_workers_header(term_ctx, successful_backend, path):
    term_ctx.run_knoss_header_test('serviceworkers', path, successful_backend, location=NATIVE_LOCATION)


# ==== COMMENTS ====

_COMMENTS_SB_BACKENDS = [
    ['backends_man#awacs-rtc_balancer_cmnt-prod-balancer_yandex_net_yp_man'],
    ['backends_sas#awacs-rtc_balancer_cmnt-prod-balancer_yandex_net_yp_sas', 'backends_vla#awacs-rtc_balancer_cmnt-prod-balancer_yandex_net_yp_vla'],
]

_COMMENTS_SB_BACKENDS, _COMMENTS_SB_IDS = knoss_backends_with_ids(_COMMENTS_SB_BACKENDS)
_COMMENTS_SB_BACKENDS_FAILING, _COMMENTS_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_COMMENTS_SB_BACKENDS)


@pytest.mark.parametrize('successful_backend', _COMMENTS_SB_BACKENDS, ids=_COMMENTS_SB_IDS)
def test_comments(term_ctx, successful_backend):
    term_ctx.run_knoss_test('comments', '/comments', successful_backend, location=NATIVE_LOCATION, has_antirobot=False)


@pytest.mark.parametrize('backends', _COMMENTS_SB_BACKENDS_FAILING, ids=_COMMENTS_SB_FAILING_IDS)
def test_comments_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test(
        'comments', '/comments', backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('comments'))
def test_comments_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        'comments', '/comments', location=NATIVE_LOCATION, has_antirobot=False, **locations_weights_info
    )


@pytest.mark.parametrize('successful_backend', _COMMENTS_SB_BACKENDS, ids=_COMMENTS_SB_IDS)
def test_comments_header(term_ctx, successful_backend):
    term_ctx.run_knoss_header_test('comments', '/comments', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('successful_backend', _COMMENTS_SB_BACKENDS, ids=_COMMENTS_SB_IDS)
def test_comments_5xx(term_ctx, successful_backend):
    term_ctx.run_knoss_5xx_test('comments', '/comments', successful_backend, location=NATIVE_LOCATION)


# ==== MESSENGER_API ====

_MESSENGER_API_SB_BACKENDS = [
    ['backends_man#awacs-rtc_balancer_l7_mssngr_search_yandex_net_yp_man'],
    ['backends_sas#awacs-rtc_balancer_l7_mssngr_search_yandex_net_yp_sas', 'backends_vla#awacs-rtc_balancer_l7_mssngr_search_yandex_net_yp_vla'],
]

_MESSENGER_API_SB_BACKENDS, _MESSENGER_API_SB_IDS = knoss_backends_with_ids(_MESSENGER_API_SB_BACKENDS)
_MESSENGER_API_SB_BACKENDS_FAILING, _MESSENGER_API_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_MESSENGER_API_SB_BACKENDS)


MESSENGER_API_PATHS = [
    '/messenger/api',
]


@pytest.mark.parametrize('path', MESSENGER_API_PATHS)
@pytest.mark.parametrize('successful_backend', _MESSENGER_API_SB_BACKENDS, ids=_MESSENGER_API_SB_IDS)
def test_messenger_api(term_ctx, successful_backend, path):
    term_ctx.run_knoss_test('messenger_api', path, successful_backend, has_antirobot=True, location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', MESSENGER_API_PATHS)
@pytest.mark.parametrize('backends', _MESSENGER_API_SB_BACKENDS_FAILING, ids=_MESSENGER_API_SB_FAILING_IDS)
def test_messenger_api_single_attempt(term_ctx, backends, path):
    term_ctx.run_knoss_single_attempt_test(
        'messenger_api', path, backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('path', MESSENGER_API_PATHS)
@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('messenger_api'))
def test_messenger_api_location_weights(term_ctx, locations_weights_info, path):
    term_ctx.run_knoss_location_weights_test(
        'messenger_api', path, has_antirobot=True, location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('path', MESSENGER_API_PATHS)
@pytest.mark.parametrize('successful_backend', _MESSENGER_API_SB_BACKENDS, ids=_MESSENGER_API_SB_IDS)
def test_messenger_api_header(term_ctx, successful_backend, path):
    term_ctx.run_knoss_header_test('messenger_api', path, successful_backend, location=NATIVE_LOCATION)


# ==== MESSENGER_API_ALPHA ====

_MESSENGER_API_ALPHA_SB_BACKENDS = [
    ['backends_man#awacs-rtc_balancer_alpha_l7_mssngr_search_yandex_net_man'],
    ['backends_sas#awacs-rtc_balancer_alpha_l7_mssngr_search_yandex_net_sas', 'backends_vla#awacs-rtc_balancer_alpha_l7_mssngr_search_yandex_net_vla'],
]

_MESSENGER_API_ALPHA_SB_BACKENDS, _MESSENGER_API_ALPHA_SB_IDS = knoss_backends_with_ids(_MESSENGER_API_ALPHA_SB_BACKENDS)
_MESSENGER_API_ALPHA_SB_BACKENDS_FAILING, _MESSENGER_API_ALPHA_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_MESSENGER_API_ALPHA_SB_BACKENDS)


MESSENGER_API_ALPHA_PATHS = [
    '/messenger/api/alpha',
]


@pytest.mark.parametrize('path', MESSENGER_API_ALPHA_PATHS)
@pytest.mark.parametrize('successful_backend', _MESSENGER_API_ALPHA_SB_BACKENDS, ids=_MESSENGER_API_ALPHA_SB_IDS)
def test_messenger_api_alpha(term_ctx, successful_backend, path):
    term_ctx.run_knoss_test('messenger_api_alpha', path, successful_backend, has_antirobot=True, location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', MESSENGER_API_ALPHA_PATHS)
@pytest.mark.parametrize('backends', _MESSENGER_API_ALPHA_SB_BACKENDS_FAILING, ids=_MESSENGER_API_ALPHA_SB_FAILING_IDS)
def test_messenger_api_alpha_single_attempt(term_ctx, backends, path):
    term_ctx.run_knoss_single_attempt_test(
        'messenger_api_alpha', path, backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('path', MESSENGER_API_ALPHA_PATHS)
@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('messenger_api_alpha'))
def test_messenger_api_alpha_location_weights(term_ctx, locations_weights_info, path):
    term_ctx.run_knoss_location_weights_test(
        'messenger_api_alpha', path, has_antirobot=True, location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('path', MESSENGER_API_ALPHA_PATHS)
@pytest.mark.parametrize('successful_backend', _MESSENGER_API_ALPHA_SB_BACKENDS, ids=_MESSENGER_API_ALPHA_SB_IDS)
def test_messenger_api_alpha_header(term_ctx, successful_backend, path):
    term_ctx.run_knoss_header_test('messenger_api_alpha', path, successful_backend, location=NATIVE_LOCATION)


# ==== GEOHELPER ====

_GEOHELPER_SB_BACKENDS = [
    ['backends_man#rtc_balancer_knoss_geohelper_man'],
    ['backends_sas#rtc_balancer_knoss_geohelper_sas', 'backends_vla#rtc_balancer_knoss_geohelper_vla'],
]

_GEOHELPER_SB_BACKENDS, _GEOHELPER_SB_IDS = knoss_backends_with_ids(_GEOHELPER_SB_BACKENDS)
_GEOHELPER_SB_BACKENDS_FAILING, _GEOHELPER_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_GEOHELPER_SB_BACKENDS)


@pytest.mark.parametrize('successful_backend', _GEOHELPER_SB_BACKENDS, ids=_GEOHELPER_SB_IDS)
def test_geohelper(term_ctx, successful_backend):
    term_ctx.run_knoss_test('geohelper', '/geohelper', successful_backend, has_antirobot=True, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _GEOHELPER_SB_BACKENDS_FAILING, ids=_GEOHELPER_SB_FAILING_IDS)
def test_geohelper_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test(
        'geohelper', '/geohelper', backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('geohelper'))
def test_geohelper_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        'geohelper', '/geohelper', has_antirobot=True, location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('successful_backend', _GEOHELPER_SB_BACKENDS, ids=_GEOHELPER_SB_IDS)
def test_geohelper_header(term_ctx, successful_backend):
    term_ctx.run_knoss_header_test('geohelper', '/geohelper', successful_backend, location=NATIVE_LOCATION)


# ==== MAPS ====

_MAPS_SB_BACKENDS = [
    ['backends_man#rtc_balancer_front-maps_slb_maps_yandex_net_man'],
    ['backends_sas#rtc_balancer_front-maps_slb_maps_yandex_net_sas', 'backends_vla#rtc_balancer_front-maps_slb_maps_yandex_net_vla'],
]

_MAPS_SB_BACKENDS, _MAPS_SB_IDS = knoss_backends_with_ids(_MAPS_SB_BACKENDS)
_MAPS_SB_BACKENDS_FAILING, _MAPS_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_MAPS_SB_BACKENDS)


MAPS_PATHS = [
    '/maps',
    '/harita',
    '/web-maps',
    '/navi',
    '/metro',
    '/profile'
]


@pytest.mark.parametrize('path', MAPS_PATHS)
@pytest.mark.parametrize('successful_backend', _MAPS_SB_BACKENDS, ids=_MAPS_SB_IDS)
def test_maps(term_ctx, successful_backend, path):
    term_ctx.run_knoss_test('maps', path, successful_backend, has_antirobot=True, location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', MAPS_PATHS)
@pytest.mark.parametrize('backends', _MAPS_SB_BACKENDS_FAILING, ids=_MAPS_SB_FAILING_IDS)
def test_maps_single_attempt(term_ctx, backends, path):
    term_ctx.run_knoss_single_attempt_test(
        'maps', path, backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('path', MAPS_PATHS)
@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('maps'))
def test_maps_location_weights(term_ctx, locations_weights_info, path):
    term_ctx.run_knoss_location_weights_test(
        'maps', path, has_antirobot=True, location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('path', MAPS_PATHS)
@pytest.mark.parametrize('successful_backend', _MAPS_SB_BACKENDS, ids=_MAPS_SB_IDS)
def test_maps_header(term_ctx, successful_backend, path):
    term_ctx.run_knoss_header_test('maps', path, successful_backend, location=NATIVE_LOCATION)


# ==== SPRAV ====

@pytest.mark.parametrize('path', ['/spravapi'])
@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('spravapi'))
def test_spravapi_location_weights(term_ctx, locations_weights_info, path):
    term_ctx.run_knoss_location_weights_test(
        'spravapi', path, has_antirobot=True, location=NATIVE_LOCATION, **locations_weights_info
    )


# ==== DEFAULT ====

_DEFAULT_SB_BACKENDS = [
    ['backends_man#awacs-rtc_balancer_knoss_fast_yp_man'],
    ['backends_sas#awacs-rtc_balancer_knoss_fast_yp_sas', 'backends_vla#awacs-rtc_balancer_knoss_fast_yp_vla'],
]

_DEFAULT_SB_BACKENDS, _DEFAULT_SB_IDS = knoss_backends_with_ids(_DEFAULT_SB_BACKENDS)
_DEFAULT_SB_BACKENDS_FAILING, _DEFAULT_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_DEFAULT_SB_BACKENDS)


DEFAULT_PATHS = [
    '/led-zappelin',
    '/sprav',
]


@pytest.mark.parametrize('path', DEFAULT_PATHS)
@pytest.mark.parametrize('successful_backend', _DEFAULT_SB_BACKENDS, ids=_DEFAULT_SB_IDS)
def test_default(term_ctx, successful_backend, path):
    term_ctx.run_knoss_test('default', path, successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', DEFAULT_PATHS)
@pytest.mark.parametrize('backends', _DEFAULT_SB_BACKENDS_FAILING, ids=_DEFAULT_SB_FAILING_IDS)
def test_default_single_attempt(term_ctx, backends, path):
    term_ctx.run_knoss_single_attempt_test(
        'default', path, backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('path', DEFAULT_PATHS)
@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('default'))
def test_default_location_weights(term_ctx, locations_weights_info, path):
    term_ctx.run_knoss_location_weights_test(
        'default', path, location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('path', DEFAULT_PATHS)
@pytest.mark.parametrize('successful_backend', _DEFAULT_SB_BACKENDS, ids=_DEFAULT_SB_IDS)
def test_default_header(term_ctx, successful_backend, path):
    term_ctx.run_knoss_header_test('default', path, successful_backend, location=NATIVE_LOCATION)


# ==== Other ====
@pytest.mark.parametrize('host', ['google.com', 'led.zeppelin'])
def test_non_yandex_host(term_ctx, host):
    term_ctx.initialize(NATIVE_LOCATION, None)

    request = {'path': '/', 'headers': {'host': host}, 'method': 'get'}
    response = term_ctx.perform_unprepared_request(request)
    asserts.status_code(response, 406)


def test_noc_static_check_200(term_ctx):
    term_ctx.initialize(NATIVE_LOCATION, None)

    weights_file = term_ctx.weights_manager.get_file('l7_noc_check')
    weights_file.set({'return_200_weighted': -1, 'return_200': 1})

    request = {'path': '/static_check.html', 'headers': {'host': 'yandex.ru'}, 'method': 'get'}
    response = term_ctx.perform_unprepared_request(request)

    asserts.status_code(response, 200)
    assert response.content == 'echo ok'


def test_noc_static_check_500(term_ctx):
    term_ctx.initialize(NATIVE_LOCATION, None)

    weights_file = term_ctx.weights_manager.get_file('l7_noc_check')
    weights_file.set({'return_200_weighted': -1, 'return_503': 1})

    request = {'path': '/static_check.html', 'headers': {'host': 'yandex.ru'}, 'method': 'get'}
    response = term_ctx.perform_unprepared_request(request)

    asserts.status_code(response, 503)
    assert response.content == 'echo NO'


def test_noc_static_check_weighted(term_ctx):
    term_ctx.initialize(NATIVE_LOCATION, None)

    request = {'path': '/static_check.html', 'headers': {'host': 'yandex.ru'}, 'method': 'get'}
    response = term_ctx.perform_unprepared_request(request)

    asserts.status_code(response, 200)
    assert response.headers['RS-Weight'] == '10'


def test_postedit(term_ctx):
    term_ctx.initialize(NATIVE_LOCATION, None)

    request = {'path': '/edit', 'headers': {'host': 'yandex.ru'}, 'method': 'get'}
    response = term_ctx.perform_unprepared_request(request)
    asserts.status_code(response, 403)


CAPTCHA_PATHS = [
    '/showcaptcha',
    '/checkcaptcha',
    '/captcha',
    '/xshowcaptcha',
    '/xcheckcaptcha',
    '/xcaptcha'
]


@pytest.mark.parametrize('path', CAPTCHA_PATHS)
def test_captcha(term_ctx, path):
    antirobot_backends = [
        'backends_{0}#prod-antirobot-yp-{0}'.format(NATIVE_LOCATION.lower()),
        'backends_{0}#prod-antirobot-yp-prestable-{0}'.format(NATIVE_LOCATION.lower()),
    ]
    term_ctx.initialize(NATIVE_LOCATION, antirobot_backends)

    _, antirobot_backends = term_ctx.start_antirobot_backends()

    request = {'path': path, 'headers': {'host': 'yandex.ru'}, 'method': 'get'}
    response = term_ctx.perform_unprepared_request(request)
    asserts.status_code(response, 403)

    assert sum(map(lambda x: len(x.state.requests), antirobot_backends)) > 0

    term_ctx.check_stats('captchasearch', 'service_total', True, False, False)


ZNATOKI_PATHS = [
    '/znatoki',
    '/q',
]

_ZNATOKI_SB_BACKENDS = [
    ['backends_man#production-balancer-answers-man-yp'],
    ['backends_sas#production-balancer-answers-sas-yp', 'backends_vla#production-balancer-answers-vla-yp'],
]

_ZNATOKI_SB_BACKENDS, _ZNATOKI_SB_IDS = knoss_backends_with_ids(_ZNATOKI_SB_BACKENDS)


@pytest.mark.parametrize('path', ZNATOKI_PATHS)
@pytest.mark.parametrize('successful_backend', _ZNATOKI_SB_BACKENDS, ids=_ZNATOKI_SB_IDS)
def test_znatoki(term_ctx, path, successful_backend):
    term_ctx.run_knoss_test('znatoki', path, successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('successful_backend', _ZNATOKI_SB_BACKENDS, ids=_ZNATOKI_SB_IDS)
def test_znatoki_landing(term_ctx, successful_backend):
    term_ctx.run_knoss_test('znatoki-landing', '/thequestion', successful_backend, location=NATIVE_LOCATION)


_APPCRY_SB_BACKENDS, _APPCRY_SB_IDS = knoss_backends_with_ids([
    ['backends_man#rtc_balancer_cryprox_yandex_net_man'],
    ['backends_sas#rtc_balancer_cryprox_yandex_net_sas', 'backends_vla#rtc_balancer_cryprox_yandex_net_vla'],
])


@pytest.mark.parametrize('successful_backend', _APPCRY_SB_BACKENDS, ids=_APPCRY_SB_IDS)
def test_appcry(term_ctx, successful_backend):
    term_ctx.run_knoss_test('appcry', '/appcry', successful_backend, location=NATIVE_LOCATION)


# ==== Games ====

GAMES_SERVICES = [
    'games',
]

_GAMES_SB_BACKENDS = [
    ['backends_man#awacs-rtc_balancer_games-prod-balancer_yandex_net_man'],
    ['backends_sas#awacs-rtc_balancer_games-prod-balancer_yandex_net_sas', 'backends_vla#awacs-rtc_balancer_games-prod-balancer_yandex_net_vla'],
]

_GAMES_SB_BACKENDS, _GAMES_SB_IDS = knoss_backends_with_ids(_GAMES_SB_BACKENDS)
_GAMES_SB_BACKENDS_FAILING, _GAMES_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_GAMES_SB_BACKENDS)


@pytest.mark.parametrize('service', GAMES_SERVICES)
@pytest.mark.parametrize('successful_backend', _GAMES_SB_BACKENDS, ids=_GAMES_SB_IDS)
def test_games(term_ctx, service, successful_backend):
    term_ctx.run_knoss_test(service, '/' + service, successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('service', GAMES_SERVICES)
@pytest.mark.parametrize('backends', _GAMES_SB_BACKENDS_FAILING, ids=_GAMES_SB_FAILING_IDS)
def test_games_single_attempt(term_ctx, service, backends):
    term_ctx.run_knoss_single_attempt_test(
        service, '/' + service, backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('service', GAMES_SERVICES)
@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('games'))
def test_games_location_weights(term_ctx, service, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        service, '/' + service, location=NATIVE_LOCATION, has_antirobot=True, **locations_weights_info
    )


@pytest.mark.parametrize('service', GAMES_SERVICES)
@pytest.mark.parametrize('successful_backend', _GAMES_SB_BACKENDS, ids=_GAMES_SB_IDS)
def test_games_header(term_ctx, service, successful_backend):
    term_ctx.run_knoss_header_test(service, '/' + service, successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('service', GAMES_SERVICES)
@pytest.mark.parametrize('successful_backend', _GAMES_SB_BACKENDS, ids=_GAMES_SB_IDS)
def test_games_5xx(term_ctx, service, successful_backend):
    term_ctx.run_knoss_5xx_test(service, '/' + service, successful_backend, location=NATIVE_LOCATION)


# ==== Weather ====

WEATHER_SERVICES = [
    'hava',
    'pogoda',
    'weather',
]

_WEATHER_SB_BACKENDS = [
    # ['backends_man#awacs-rtc_balancer_frontend_weather_yandex_net_man'], TODO_L switch after new backends
    # ['backends_sas#awacs-rtc_balancer_frontend_weather_yandex_net_sas', 'backends_vla#awacs-rtc_balancer_frontend_weather_yandex_net_vla'],
    ['backends_man#awacs-rtc_balancer_knoss_fast_yp_man'],
    ['backends_sas#awacs-rtc_balancer_knoss_fast_yp_sas', 'backends_vla#awacs-rtc_balancer_knoss_fast_yp_vla'],
]

_WEATHER_SB_BACKENDS, _WEATHER_SB_IDS = knoss_backends_with_ids(_WEATHER_SB_BACKENDS)
_WEATHER_SB_BACKENDS_FAILING, _WEATHER_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_WEATHER_SB_BACKENDS)


@pytest.mark.parametrize('service', WEATHER_SERVICES)
@pytest.mark.parametrize('successful_backend', _WEATHER_SB_BACKENDS, ids=_WEATHER_SB_IDS)
def test_weather(term_ctx, service, successful_backend):
    term_ctx.run_knoss_test(service, '/' + service, successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('service', WEATHER_SERVICES)
@pytest.mark.parametrize('backends', _WEATHER_SB_BACKENDS_FAILING, ids=_WEATHER_SB_FAILING_IDS)
def test_weather_single_attempt(term_ctx, service, backends):
    term_ctx.run_knoss_single_attempt_test(
        service, '/' + service, backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('service', WEATHER_SERVICES)
@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('weather'))
def test_weather_location_weights(term_ctx, service, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        service, '/' + service, location=NATIVE_LOCATION, has_antirobot=True, **locations_weights_info
    )


@pytest.mark.parametrize('service', WEATHER_SERVICES)
@pytest.mark.parametrize('successful_backend', _WEATHER_SB_BACKENDS, ids=_WEATHER_SB_IDS)
def test_weather_header(term_ctx, service, successful_backend):
    term_ctx.run_knoss_header_test(service, '/' + service, successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('service', WEATHER_SERVICES)
@pytest.mark.parametrize('successful_backend', _WEATHER_SB_BACKENDS, ids=_WEATHER_SB_IDS)
def test_weather_5xx(term_ctx, service, successful_backend):
    term_ctx.run_knoss_5xx_test(service, '/' + service, successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('service', [
    'turbo',
    'turboforms',
    'ugc_my',
    'ugcpub',
    'user',
    'video',
    'video-xml',
    'video_api',
    'news',
    'serviceworkers',
    'comments',
    'messenger_api',
    'messenger_api_alpha',
    'uslugi',
    'gnc',
    'geohelper',
    'ick',
    'bell',
    'conflagexp',
    'maps',
    'spravapi',
    'znatoki',
    'znatoki-landing',
    'appcry',
    'games',
    'an',
    'ads',
    'sport_live',
    'default',
])
def test_locations_canonical_knoss_only(service):
    return [knoss_only_locations_backends_canon_test(service)]
