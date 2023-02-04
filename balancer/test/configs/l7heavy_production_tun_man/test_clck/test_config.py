# -*- coding: utf-8 -*-
import pytest
from balancer.test.configs.lib.locations_test import one_enabled_location_knoss_only, knoss_only_locations_backends_canon_test
from balancer.test.mock.mocked_balancer.term_ctx import knoss_backends_single_attempt_with_ids, knoss_backends_with_ids

NATIVE_LOCATION = 'MAN'

_CLCK_SB_BACKENDS = [
    ['backends_man#awacs-rtc_balancer_knoss_clicks_yp_man'],
    ['backends_sas#awacs-rtc_balancer_knoss_clicks_yp_sas', 'backends_vla#awacs-rtc_balancer_knoss_clicks_yp_vla'],
]

_CLCK_SB_BACKENDS, _CLCK_SB_IDS = knoss_backends_with_ids(_CLCK_SB_BACKENDS)
_CLCK_SB_BACKENDS_FAILING, _CLCK_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_CLCK_SB_BACKENDS)


@pytest.mark.parametrize('path', ['/clck/safeclick', '/clck/jsredir'], ids=['safeclick', 'jsredir'])
@pytest.mark.parametrize('successful_backend', _CLCK_SB_BACKENDS, ids=_CLCK_SB_IDS)
def test_clck(term_ctx, path, successful_backend):
    term_ctx.run_knoss_test('clck', path, successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', ['/clck/safeclick', '/clck/jsredir'], ids=['safeclick', 'jsredir'])
@pytest.mark.parametrize('backends', _CLCK_SB_BACKENDS_FAILING, ids=_CLCK_SB_FAILING_IDS)
def test_clck_single_attempt(term_ctx, path, backends):
    term_ctx.run_knoss_single_attempt_test('clck', path, backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', ['/clck/safeclick', '/clck/jsredir'], ids=['safeclick', 'jsredir'])
@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('clck'))
def test_clck_location_weights(term_ctx, path, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('clck', path, location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('path', ['/clck/safeclick', '/clck/jsredir'], ids=['safeclick', 'jsredir'])
@pytest.mark.parametrize('successful_backend', _CLCK_SB_BACKENDS, ids=_CLCK_SB_IDS)
def test_clck_header(term_ctx, path, successful_backend):
    term_ctx.run_knoss_header_test('clck', path, successful_backend, location=NATIVE_LOCATION)


def test_locations_canonical():
    return knoss_only_locations_backends_canon_test('clck')
