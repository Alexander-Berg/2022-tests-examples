# -*- coding: utf-8 -*-
import pytest
from balancer.test.configs.lib.locations_test import one_enabled_location_knoss_only, knoss_only_locations_backends_canon_test
from balancer.test.mock.mocked_balancer.term_ctx import knoss_backends_single_attempt_with_ids, knoss_backends_with_ids


NATIVE_LOCATION = 'MAN'

_SUGGEST_SB_BACKENDS = [
    ['backends_man#awacs-rtc_balancer_knoss-suggest-yp_man'],
    ['backends_sas#awacs-rtc_balancer_knoss-suggest-yp_sas', 'backends_vla#awacs-rtc_balancer_knoss-suggest-yp_vla'],
]

_SUGGEST_SB_BACKENDS, _SUGGEST_SB_IDS = knoss_backends_with_ids(_SUGGEST_SB_BACKENDS)
_SUGGEST_SB_BACKENDS_FAILING, _SUGGEST_SB_FAILING_IDS = knoss_backends_single_attempt_with_ids(_SUGGEST_SB_BACKENDS)


@pytest.mark.parametrize('successful_backend', _SUGGEST_SB_BACKENDS, ids=_SUGGEST_SB_IDS)
def test_suggest_images(term_ctx, successful_backend):
    term_ctx.run_knoss_test('suggest-images', '/suggest-images', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _SUGGEST_SB_BACKENDS_FAILING, ids=_SUGGEST_SB_FAILING_IDS)
def test_suggest_images_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test('suggest-images', '/suggest-images', backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('suggest-images'))
def test_suggest_images_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('suggest-images', '/suggest-images', location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('successful_backend', _SUGGEST_SB_BACKENDS, ids=_SUGGEST_SB_IDS)
def test_suggest_images_header(term_ctx, successful_backend):
    term_ctx.run_knoss_header_test('suggest-images', '/suggest-images', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('successful_backend', _SUGGEST_SB_BACKENDS, ids=_SUGGEST_SB_IDS)
def test_suggest_video(term_ctx, successful_backend):
    term_ctx.run_knoss_test('suggest-video', '/suggest-video', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _SUGGEST_SB_BACKENDS_FAILING, ids=_SUGGEST_SB_FAILING_IDS)
def test_suggest_video_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test('suggest-video', '/suggest-video', backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('suggest-video'))
def test_suggest_video_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('suggest-video', '/suggest-video', location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('successful_backend', _SUGGEST_SB_BACKENDS, ids=_SUGGEST_SB_IDS)
def test_suggest_video_header(term_ctx, successful_backend):
    term_ctx.run_knoss_header_test('suggest-video', '/suggest-video', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', ['/suggest'], ids=['suggest'])
@pytest.mark.parametrize('successful_backend', _SUGGEST_SB_BACKENDS, ids=_SUGGEST_SB_IDS)
def test_suggest(term_ctx, path, successful_backend):
    term_ctx.run_knoss_test('suggest', path, successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', ['/suggest'], ids=['suggest'])
@pytest.mark.parametrize('backends', _SUGGEST_SB_BACKENDS_FAILING, ids=_SUGGEST_SB_FAILING_IDS)
def test_suggest_single_attempt(term_ctx, path, backends):
    term_ctx.run_knoss_single_attempt_test('suggest', path, backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', ['/suggest'], ids=['suggest'])
@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('suggest'))
def test_clck_location_weights(term_ctx, path, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('suggest', path, location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('path', ['/suggest'], ids=['suggest'])
@pytest.mark.parametrize('successful_backend', _SUGGEST_SB_BACKENDS, ids=_SUGGEST_SB_IDS)
def test_suggest_header(term_ctx, path, successful_backend):
    term_ctx.run_knoss_header_test('suggest', path, successful_backend, location=NATIVE_LOCATION)


def test_locations_canonical():
    return [knoss_only_locations_backends_canon_test(service) for service in ['suggest-images', 'suggest-video', 'suggest']]
