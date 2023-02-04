# -*- coding: utf-8 -*-
import pytest
from balancer.test.configs.lib.locations_test import one_enabled_location_knoss_only, knoss_only_locations_backends_canon_test
from balancer.test.mock.mocked_balancer.term_ctx import knoss_backends_single_attempt_with_ids, knoss_backends_with_ids


NATIVE_LOCATION = 'MAN'

_IMAGES_BACKENDS_LIST = [
    ['backends_man#awacs-rtc_balancer_knoss-images-yp_man'],
    ['backends_sas#awacs-rtc_balancer_knoss-images-yp_sas', 'backends_vla#awacs-rtc_balancer_knoss-images-yp_vla'],
]

_IMAGES_BACKENDS, _IMAGES_IDS = knoss_backends_with_ids(_IMAGES_BACKENDS_LIST)
_IMAGES_BACKENDS_FAILING, _IMAGES_FAILING_IDS = knoss_backends_single_attempt_with_ids(_IMAGES_BACKENDS_LIST)


@pytest.mark.parametrize('successful_backend', _IMAGES_BACKENDS, ids=_IMAGES_IDS)
def test_images_xml(term_ctx, successful_backend):
    term_ctx.run_knoss_test('images-xml', '/images-xml', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _IMAGES_BACKENDS_FAILING, ids=_IMAGES_FAILING_IDS)
def test_images_xml_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test('images-xml', '/images-xml', backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('images-xml'))
def test_images_xml_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('images-xml', '/images-xml', location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('successful_backend', _IMAGES_BACKENDS, ids=_IMAGES_IDS)
def test_images_xml_header(term_ctx, successful_backend):
    term_ctx.run_knoss_header_test('images-xml', '/images-xml', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('successful_backend', _IMAGES_BACKENDS, ids=_IMAGES_IDS)
def test_images_apphost(term_ctx, successful_backend):
    term_ctx.run_knoss_test('images-apphost', '/images-apphost', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _IMAGES_BACKENDS_FAILING, ids=_IMAGES_FAILING_IDS)
def test_images_apphost_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test('images-apphost', '/images-apphost', backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('images-apphost'))
def test_images_apphost_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test(
        'images-apphost', '/images-apphost', location=NATIVE_LOCATION, **locations_weights_info
    )


@pytest.mark.parametrize('successful_backend', _IMAGES_BACKENDS, ids=_IMAGES_IDS)
def test_images_apphost_header(term_ctx, successful_backend):
    term_ctx.run_knoss_header_test('images-apphost', '/images-apphost', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('successful_backend', _IMAGES_BACKENDS, ids=_IMAGES_IDS)
@pytest.mark.parametrize('path', ['/images', '/gorsel'], ids=['images', 'gorsel'])
def test_images(term_ctx, path, successful_backend):
    term_ctx.run_knoss_test('images', path, successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _IMAGES_BACKENDS_FAILING, ids=_IMAGES_FAILING_IDS)
@pytest.mark.parametrize('path', ['/images', '/gorsel'], ids=['images', 'gorsel'])
def test_images_single_attempt(term_ctx, path, backends):
    term_ctx.run_knoss_single_attempt_test('images', path, backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', ['/images', '/gorsel'], ids=['images', 'gorsel'])
@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('images'))
def test_images_location_weights(term_ctx, path, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('images', path, location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('successful_backend', _IMAGES_BACKENDS, ids=_IMAGES_IDS)
@pytest.mark.parametrize('path', ['/images', '/gorsel'], ids=['images', 'gorsel'])
def test_images_header(term_ctx, path, successful_backend):
    term_ctx.run_knoss_header_test('images', path, successful_backend, location=NATIVE_LOCATION)


_COLLECTIONS_BACKENDS_LIST = [
    ['backends_man#collections-service-balancer-production-man-16020'],
    [
        'backends_sas#collections-service-balancer-production-sas-16020',
        'backends_vla#collections-service-balancer-production-vla-16020',
    ],
]

_COLLECTIONS_BACKENDS, _COLLECTIONS_IDS = knoss_backends_with_ids(_COLLECTIONS_BACKENDS_LIST)
_COLLECTIONS_BACKENDS_FAILING, _COLLECTIONS_FAILING_IDS = knoss_backends_single_attempt_with_ids(_COLLECTIONS_BACKENDS_LIST)


@pytest.mark.parametrize('successful_backend', _COLLECTIONS_BACKENDS, ids=_COLLECTIONS_IDS)
def test_collections(term_ctx, successful_backend):
    term_ctx.run_knoss_test('collections', '/collections', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _COLLECTIONS_BACKENDS_FAILING, ids=_COLLECTIONS_FAILING_IDS)
def test_collections_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test('collections', '/collections', backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('collections'))
def test_collections_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('collections', '/collections', location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('successful_backend', _COLLECTIONS_BACKENDS, ids=_COLLECTIONS_IDS)
def test_collections_header(term_ctx, successful_backend):
    term_ctx.run_knoss_header_test('collections', '/collections', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('successful_backend', _COLLECTIONS_BACKENDS, ids=_COLLECTIONS_IDS)
def test_feed(term_ctx, successful_backend):
    term_ctx.run_knoss_test('feed', '/feed', successful_backend, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _COLLECTIONS_BACKENDS_FAILING, ids=_COLLECTIONS_FAILING_IDS)
def test_feed_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test('feed', '/feed', backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('feed'))
def test_feed_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('feed', '/feed', location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('successful_backend', _COLLECTIONS_BACKENDS, ids=_COLLECTIONS_IDS)
def test_feed_header(term_ctx, successful_backend):
    term_ctx.run_knoss_header_test('feed', '/feed', successful_backend, location=NATIVE_LOCATION)


def test_locations_canonical():
    return [knoss_only_locations_backends_canon_test(service) for service in [
        'images-xml', 'images-apphost', 'images', 'collections', 'collections_api', 'feed'
    ]]
