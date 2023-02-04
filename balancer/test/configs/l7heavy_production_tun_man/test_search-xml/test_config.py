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

_SEARCH_XML_PATHS = [
    '/xmlsearch',
    '/search/xml'
]


def _gen_test_ids(paths):
    return [p.strip('/').replace('/', '-slash-') for p in paths]


@pytest.mark.parametrize(
    'path',
    _SEARCH_XML_PATHS,
    ids=_gen_test_ids(_SEARCH_XML_PATHS)
)
@pytest.mark.parametrize(
    'successful_backend',
    _SEARCH_SB_BACKENDS,
    ids=_SEARCH_SB_IDS
)
def test_search_xml(term_ctx, path, successful_backend):
    term_ctx.run_knoss_test(
        'search-xml',
        path,
        successful_backend,
        location=NATIVE_LOCATION
    )


@pytest.mark.parametrize(
    'path',
    _SEARCH_XML_PATHS,
    ids=_gen_test_ids(_SEARCH_XML_PATHS)
)
@pytest.mark.parametrize(
    'failed_backend',
    _SEARCH_SB_BACKENDS,
    ids=_SEARCH_SB_IDS
)
def test_search_xml_count_attempts(term_ctx, path, failed_backend):
    term_ctx.run_count_attempts_test(
        'search-xml',
        path,
        failed_backend,
        location=NATIVE_LOCATION
    )


@pytest.mark.parametrize(
    'path',
    _SEARCH_XML_PATHS,
    ids=_gen_test_ids(_SEARCH_XML_PATHS)
)
@pytest.mark.parametrize(
    'backends',
    _SEARCH_SB_BACKENDS_FAILING,
    ids=_SEARCH_SB_FAILING_IDS
)
def test_search_xml_single_attempt(term_ctx, path, backends):
    term_ctx.run_knoss_single_attempt_test(
        'search-xml',
        path,
        backends[0],
        backends[1],
        location=NATIVE_LOCATION
    )


@pytest.mark.parametrize(
    'path',
    _SEARCH_XML_PATHS,
    ids=_gen_test_ids(_SEARCH_XML_PATHS)
)
@pytest.mark.parametrize(
    'locations_weights_info',
    **one_enabled_location_knoss_only('search-xml')
)
def test_search_xml_location_weights(term_ctx, path, locations_weights_info):
    if 'KNOSS_IMAGES' in locations_weights_info['backends'][0]:
        path += '?type=pictures'
    term_ctx.run_knoss_location_weights_test(
        'search-xml',
        path,
        location=NATIVE_LOCATION,
        **locations_weights_info
    )


@pytest.mark.parametrize(
    'path',
    _SEARCH_XML_PATHS,
    ids=_gen_test_ids(_SEARCH_XML_PATHS)
)
@pytest.mark.parametrize(
    'successful_backend',
    _SEARCH_SB_BACKENDS,
    ids=_SEARCH_SB_IDS
)
def test_search_xml_header(term_ctx, path, successful_backend):
    term_ctx.run_knoss_header_test(
        'search-xml',
        path,
        successful_backend,
        location=NATIVE_LOCATION
    )


def test_locations_canonical():
    return [knoss_only_locations_backends_canon_test(service) for service in ['search-xml']]
