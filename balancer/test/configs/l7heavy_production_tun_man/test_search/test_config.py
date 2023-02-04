# -*- coding: utf-8 -*-
import pytest
from balancer.test.configs.lib.locations_test import one_enabled_location_knoss_only, knoss_only_locations_backends_canon_test
from balancer.test.mock.mocked_balancer.term_ctx import knoss_backends_single_attempt_with_ids, knoss_backends_with_ids


NATIVE_LOCATION = 'MAN'

_SEARCH_SB_BACKENDS = [
    [
        'backends_man#balancer_knoss_search_yp_man'
    ], [
        'backends_sas#balancer_knoss_search_yp_sas',
        'backends_vla#balancer_knoss_search_yp_vla'
    ],
]

_SEARCH_SB_BACKENDS, _SEARCH_SB_IDS = knoss_backends_with_ids(_SEARCH_SB_BACKENDS)
_SEARCH_SB_BACKENDS_FAILING, _SEARCH_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_SEARCH_SB_BACKENDS)

_SEARCH_ONERR_BACKENDS = [
    'backends_VLA_WEB_RUS_YALITE_SAS_WEB_RUS_YALITE'
]

_SEARCH_PATHS = [
    ('/search', 'search'),
    ('/yandsearch', 'search'),
]

_SEARCH_OTHER = [
    ('/familysearch', 'search_other'),
    ('/msearch', 'search_other'),
    ('/schoolsearch', 'search_other'),
    ('/telsearch', 'search_other'),
]

_SEARCH_LAST_PATHS = [
    ('/jsonsearch', 'search_last'),
    ('/largesearch', 'search_last'),
    ('/yandcache.js', 'search_last'),
    ('/yandpage', 'search_last'),
    ('/yca', 'search_last'),
]

_SITESEARCH_PATHS = [
    ('/sitesearch', 'sitesearch'),
]

_PADSEARCH_PATHS = [
    ('/padsearch', 'padsearch'),
    ('/search/pad', 'padsearch'),
]

_SEARCH_STATIC_PREFETCH_PATHS = [
    ('/prefetch.txt', 'search_static_prefetch'),
]

_ALL_PATHS = _SEARCH_PATHS + _SEARCH_OTHER + _SEARCH_LAST_PATHS + _SITESEARCH_PATHS + _PADSEARCH_PATHS + _SEARCH_STATIC_PREFETCH_PATHS


def _gen_test_ids(paths):
    return [path.strip('/').replace('/', '-') for path, _ in paths]


@pytest.mark.parametrize(
    'path,section',
    _ALL_PATHS,
    ids=_gen_test_ids(_ALL_PATHS)
)
@pytest.mark.parametrize(
    'successful_backend',
    _SEARCH_SB_BACKENDS,
    ids=_SEARCH_SB_IDS
)
def test_search(term_ctx, path, section, successful_backend):
    term_ctx.run_knoss_test(
        section,
        path,
        successful_backend,
        location=NATIVE_LOCATION
    )


@pytest.mark.parametrize(
    'path,section',
    _ALL_PATHS,
    ids=_gen_test_ids(_ALL_PATHS)
)
@pytest.mark.parametrize(
    'failed_backend',
    _SEARCH_SB_BACKENDS,
    ids=_SEARCH_SB_IDS
)
def test_search_count_attempts(term_ctx, path, section, failed_backend):
    term_ctx.run_count_attempts_test(
        section,
        path,
        failed_backend,
        location=NATIVE_LOCATION
    )


@pytest.mark.parametrize(
    'path,section',
    _ALL_PATHS,
    ids=_gen_test_ids(_ALL_PATHS)
)
@pytest.mark.parametrize(
    'backends',
    _SEARCH_SB_BACKENDS_FAILING,
    ids=_SEARCH_SB_FAILING_IDS
)
def test_search_single_attempt(term_ctx, path, section, backends):
    term_ctx.run_knoss_single_attempt_test(
        section,
        path,
        backends[0],
        backends[1],
        location=NATIVE_LOCATION
    )


@pytest.mark.parametrize(
    'path,section',
    _SEARCH_PATHS,
    ids=_gen_test_ids(_SEARCH_PATHS)
)
@pytest.mark.parametrize(
    'locations_weights_info',
    **one_enabled_location_knoss_only('search')
)
def test_search_location_weights(term_ctx, path, section, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        section,
        path,
        location=NATIVE_LOCATION,
        **locations_weights_info
    )


@pytest.mark.parametrize(
    'path,section',
    _SEARCH_OTHER,
    ids=_gen_test_ids(_SEARCH_OTHER)
)
@pytest.mark.parametrize(
    'locations_weights_info',
    **one_enabled_location_knoss_only('search_other')
)
def test_search_other_location_weights(term_ctx, path, section, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        section,
        path,
        location=NATIVE_LOCATION,
        **locations_weights_info
    )


@pytest.mark.parametrize(
    'path,section',
    _SEARCH_LAST_PATHS,
    ids=_gen_test_ids(_SEARCH_LAST_PATHS)
)
@pytest.mark.parametrize(
    'locations_weights_info',
    **one_enabled_location_knoss_only('search_last')
)
def test_search_last_location_weights(term_ctx, path, section, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        section,
        path,
        location=NATIVE_LOCATION,
        **locations_weights_info
    )


@pytest.mark.parametrize(
    'path,section',
    _SITESEARCH_PATHS,
    ids=_gen_test_ids(_SITESEARCH_PATHS)
)
@pytest.mark.parametrize(
    'locations_weights_info',
    **one_enabled_location_knoss_only('sitesearch')
)
def test_sitesearch_location_weights(term_ctx, path, section, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        section,
        path,
        location=NATIVE_LOCATION,
        **locations_weights_info
    )


@pytest.mark.parametrize(
    'path,section',
    _PADSEARCH_PATHS,
    ids=_gen_test_ids(_PADSEARCH_PATHS)
)
@pytest.mark.parametrize(
    'locations_weights_info',
    **one_enabled_location_knoss_only('padsearch')
)
def test_padsearch_location_weights(term_ctx, path, section, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        section,
        path,
        location=NATIVE_LOCATION,
        **locations_weights_info
    )


@pytest.mark.parametrize(
    'path,section',
    _SEARCH_STATIC_PREFETCH_PATHS,
    ids=_gen_test_ids(_SEARCH_STATIC_PREFETCH_PATHS)
)
@pytest.mark.parametrize(
    'locations_weights_info',
    **one_enabled_location_knoss_only('search_static_prefetch')
)
def test_search_static_prefetch_location_weights(term_ctx, path, section, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        section,
        path,
        location=NATIVE_LOCATION,
        **locations_weights_info
    )


@pytest.mark.parametrize(
    'path,section',
    _ALL_PATHS,
    ids=_gen_test_ids(_ALL_PATHS)
)
@pytest.mark.parametrize(
    'successful_backend',
    _SEARCH_SB_BACKENDS,
    ids=_SEARCH_SB_IDS
)
def test_search_header(term_ctx, path, section, successful_backend):
    term_ctx.run_knoss_header_test(
        section,
        path,
        successful_backend,
        location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('successful_backend', _SEARCH_SB_BACKENDS, ids=_SEARCH_SB_IDS)
def test_searchapp(term_ctx, successful_backend):
    term_ctx.run_knoss_test('searchapp', '/searchapp', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _SEARCH_SB_BACKENDS_FAILING, ids=_SEARCH_SB_FAILING_IDS)
def test_searchapp_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test(
        'searchapp', '/searchapp', backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('searchapp'))
def test_searchapp_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        'searchapp', '/searchapp', location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('successful_backend', _SEARCH_SB_BACKENDS, ids=_SEARCH_SB_IDS)
def test_searchapp_header(term_ctx, successful_backend):
    term_ctx.run_knoss_header_test(
        'searchapp', '/searchapp', successful_backend, location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('successful_backend', _SEARCH_SB_BACKENDS, ids=_SEARCH_SB_IDS)
def test_blogs(term_ctx, successful_backend):
    term_ctx.run_knoss_test('blogs', '/blogs', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _SEARCH_SB_BACKENDS_FAILING, ids=_SEARCH_SB_FAILING_IDS)
def test_blogs_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test(
        'blogs', '/blogs', backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('blogs'))
def test_blogs_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        'blogs', '/blogs', location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('successful_backend', _SEARCH_SB_BACKENDS, ids=_SEARCH_SB_IDS)
def test_blogs_header(term_ctx, successful_backend):
    term_ctx.run_knoss_header_test(
        'blogs', '/blogs', successful_backend, location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('successful_backend', _SEARCH_SB_BACKENDS, ids=_SEARCH_SB_IDS)
def test_chat(term_ctx, successful_backend):
    term_ctx.run_knoss_test('chat', '/chat', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _SEARCH_SB_BACKENDS_FAILING, ids=_SEARCH_SB_FAILING_IDS)
def test_chat_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test(
        'chat', '/chat', backends[0], backends[1], location=NATIVE_LOCATION
    )


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('chat'))
def test_chat_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        'chat', '/chat', location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('successful_backend', _SEARCH_SB_BACKENDS, ids=_SEARCH_SB_IDS)
def test_chat_header(term_ctx, successful_backend):
    term_ctx.run_knoss_header_test(
        'chat', '/chat', successful_backend, location=NATIVE_LOCATION
    )


def test_search_onerr(term_ctx):
    term_ctx.run_knoss_test(
        'search',
        '/search',
        _SEARCH_ONERR_BACKENDS,
        location=NATIVE_LOCATION
    )


@pytest.mark.parametrize(
    'backends',
    _SEARCH_SB_BACKENDS,
    ids=_SEARCH_SB_IDS
)
def test_search_5xx(term_ctx, backends):
    term_ctx.run_knoss_5xx_test(
        'search',
        '/search',
        backends,
        location=NATIVE_LOCATION
    )


def test_search_balancer_hint(term_ctx):
    term_ctx.run_knoss_balancer_hint(
        'search',
        '/search',
        _SEARCH_SB_BACKENDS[0],
        location=NATIVE_LOCATION
    )


def test_search_host_banned(term_ctx):
    term_ctx.run_knoss_host_banned(
        'search',
        '/search',
        _SEARCH_SB_BACKENDS[0],
        location=NATIVE_LOCATION
    )


def test_locations_canonical():
    return [knoss_only_locations_backends_canon_test(service) for service in [
        'blogs',
        'chat',
        'search',
        'search_yandsearch',
        'search_smart',
        'padsearch',
        'searchapp',
    ]]
