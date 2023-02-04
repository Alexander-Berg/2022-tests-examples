# -*- coding: utf-8 -*-
import pytest
from balancer.test.configs.lib.locations_test import one_enabled_location_knoss_only, knoss_only_locations_backends_canon_test
from balancer.test.mock.mocked_balancer.term_ctx import knoss_backends_single_attempt_with_ids, knoss_backends_with_ids


NATIVE_LOCATION = 'MAN'

_MORDA_SB_BACKENDS = [
    ['backends_man#balancer_knoss_morda_yp_man'],
    ['backends_sas#balancer_knoss_morda_yp_sas', 'backends_vla#balancer_knoss_morda_yp_vla'],
]

_MORDA_ONERR_BACKENDS = ['backends_VLA_WEB_RUS_YALITE_SAS_WEB_RUS_YALITE']

_MORDA_SERVICE_BALANCER_BACKENDS, _MORDA_SERVICE_BALANCER_IDS = knoss_backends_with_ids(_MORDA_SB_BACKENDS)
_MORDA_SERVICE_BALANCER_BACKENDS_FAILING, _MORDA_SERVICE_BALANCER_FAILING_IDS = knoss_backends_single_attempt_with_ids(_MORDA_SB_BACKENDS)


# morda
@pytest.mark.parametrize('path', ['/', '/all', '/themes', '/portal', '/data', '/instant'],
                             ids=['index', 'all', 'themes', 'portal', 'data', 'instant'])
@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_morda(term_ctx, path, successful_backends):
    term_ctx.run_knoss_test('morda', path, successful_backends, location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', ['/', '/all', '/themes', '/portal', '/data', '/instant'],
                             ids=['index', 'all', 'themes', 'portal', 'data', 'instant'])
@pytest.mark.parametrize('backends', _MORDA_SERVICE_BALANCER_BACKENDS_FAILING, ids=_MORDA_SERVICE_BALANCER_FAILING_IDS)
def test_morda_single_attempt(term_ctx, path, backends):
    term_ctx.run_knoss_single_attempt_test('morda', path, backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', ['/', '/all', '/themes', '/portal', '/data', '/instant'],
                             ids=['index', 'all', 'themes', 'portal', 'data', 'instant'])
@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('morda'))
def test_morda_location_weights(term_ctx, path, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('morda', path, location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('path', ['/', '/all', '/themes', '/portal', '/data', '/instant'],
                             ids=['index', 'all', 'themes', 'portal', 'data', 'instant'])
@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_morda_header(term_ctx, path, successful_backends):
    term_ctx.run_knoss_header_test('morda', path, successful_backends, location=NATIVE_LOCATION)


@pytest.mark.parametrize('path', ['/', '/all', '/themes', '/portal', '/data', '/instant'],
                             ids=['index', 'all', 'themes', 'portal', 'data', 'instant'])
def test_morda_onerr(term_ctx, path):
    term_ctx.run_knoss_test('morda', path, _MORDA_ONERR_BACKENDS, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_morda_5xx(term_ctx, backends):
    term_ctx.run_knoss_5xx_test('morda', '/', backends, location=NATIVE_LOCATION)


# android_widget_api
@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_android_widget_api(term_ctx, successful_backends):
    term_ctx.run_knoss_test('android_widget_api', '/android_widget_api', successful_backends, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _MORDA_SERVICE_BALANCER_BACKENDS_FAILING, ids=_MORDA_SERVICE_BALANCER_FAILING_IDS)
def test_android_widget_api_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test('android_widget_api', '/android_widget_api', backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('android_widget_api'))
def test_android_widget_api_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('android_widget_api', '/android_widget_api', location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_android_widget_api_header(term_ctx, successful_backends):
    term_ctx.run_knoss_header_test('android_widget_api', '/android_widget_api', successful_backends, location=NATIVE_LOCATION)


# partner_strmchannels
@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_partner_strmchannels(term_ctx, successful_backends):
    term_ctx.run_knoss_test('partner_strmchannels', '/partner-strmchannels', successful_backends, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _MORDA_SERVICE_BALANCER_BACKENDS_FAILING, ids=_MORDA_SERVICE_BALANCER_FAILING_IDS)
def test_partner_strmchannels_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test('partner_strmchannels', '/partner-strmchannels', backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('partner_strmchannels'))
def test_partner_strmchannels_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('partner_strmchannels', '/partner-strmchannels', location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_partner_strmchannels_header(term_ctx, successful_backends):
    term_ctx.run_knoss_header_test('partner_strmchannels', '/partner-strmchannels', successful_backends, location=NATIVE_LOCATION)


# portal_station
@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_portal_station(term_ctx, successful_backends):
    term_ctx.run_knoss_test('portal_station', '/portal/station', successful_backends, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _MORDA_SERVICE_BALANCER_BACKENDS_FAILING, ids=_MORDA_SERVICE_BALANCER_FAILING_IDS)
def test_portal_station_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test('portal_station', '/portal/station', backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('portal_station'))
def test_portal_station_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('portal_station', '/portal/station', location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_portal_station_header(term_ctx, successful_backends):
    term_ctx.run_knoss_header_test('portal_station', '/portal/station', successful_backends, location=NATIVE_LOCATION)


# portal_front
@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_portal_front(term_ctx, successful_backends):
    term_ctx.run_knoss_test('portal_front', '/portal/front', successful_backends, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _MORDA_SERVICE_BALANCER_BACKENDS_FAILING, ids=_MORDA_SERVICE_BALANCER_FAILING_IDS)
def test_portal_front_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test('portal_front', '/portal/front', backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('portal_front'))
def test_portal_front_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('portal_front', '/portal/front', location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_portal_front_header(term_ctx, successful_backends):
    term_ctx.run_knoss_header_test('portal_front', '/portal/front', successful_backends, location=NATIVE_LOCATION)


# morda_app
@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_morda_app(term_ctx, successful_backends):
    term_ctx.run_knoss_test('morda_app', '/portal/api/search/2', successful_backends, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _MORDA_SERVICE_BALANCER_BACKENDS_FAILING, ids=_MORDA_SERVICE_BALANCER_FAILING_IDS)
def test_morda_app_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test('morda_app', '/portal/api/search/2', backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('morda_app'))
def test_morda_app_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('morda_app', '/portal/api/search/2', location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_morda_app_header(term_ctx, successful_backends):
    term_ctx.run_knoss_header_test('morda_app', '/portal/api/search/2', successful_backends, location=NATIVE_LOCATION)


# morda_searchapp_config
@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_morda_searchapp_config(term_ctx, successful_backends):
    term_ctx.run_knoss_test('morda_searchapp_config', '/portal/mobilesearch/config/searchapp/', successful_backends, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _MORDA_SERVICE_BALANCER_BACKENDS_FAILING, ids=_MORDA_SERVICE_BALANCER_FAILING_IDS)
def test_morda_searchapp_config_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test('morda_searchapp_config', '/portal/mobilesearch/config/searchapp/', backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('morda_searchapp_config'))
def test_morda_searchapp_config_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('morda_searchapp_config', '/portal/mobilesearch/config/searchapp/', location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_morda_searchapp_config_header(term_ctx, successful_backends):
    term_ctx.run_knoss_header_test('morda_searchapp_config', '/portal/mobilesearch/config/searchapp/', successful_backends, location=NATIVE_LOCATION)


# morda_browser_config
@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_morda_browser_config(term_ctx, successful_backends):
    term_ctx.run_knoss_test('morda_browser_config', '/portal/mobilesearch/config/browser/', successful_backends, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _MORDA_SERVICE_BALANCER_BACKENDS_FAILING, ids=_MORDA_SERVICE_BALANCER_FAILING_IDS)
def test_morda_browser_config_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test('morda_browser_config', '/portal/mobilesearch/config/browser/', backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('morda_browser_config'))
def test_morda_browser_config_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('morda_browser_config', '/portal/mobilesearch/config/browser/', location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_morda_browser_config_header(term_ctx, successful_backends):
    term_ctx.run_knoss_header_test('morda_browser_config', '/portal/mobilesearch/config/browser/', successful_backends, location=NATIVE_LOCATION)


# morda_ibrowser_config
@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_morda_ibrowser_config(term_ctx, successful_backends):
    term_ctx.run_knoss_test('morda_ibrowser_config', '/portal/mobilesearch/config/ibrowser/', successful_backends, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _MORDA_SERVICE_BALANCER_BACKENDS_FAILING, ids=_MORDA_SERVICE_BALANCER_FAILING_IDS)
def test_morda_ibrowser_config_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test('morda_ibrowser_config', '/portal/mobilesearch/config/ibrowser/', backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('morda_ibrowser_config'))
def test_morda_ibrowser_config_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('morda_ibrowser_config', '/portal/mobilesearch/config/ibrowser/', location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_morda_ibrowser_config_header(term_ctx, successful_backends):
    term_ctx.run_knoss_header_test('morda_ibrowser_config', '/portal/mobilesearch/config/ibrowser/', successful_backends, location=NATIVE_LOCATION)


# morda_ntp
@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_morda_ntp(term_ctx, successful_backends):
    term_ctx.run_knoss_test('morda_ntp', '/portal/ntp/refresh_data', successful_backends, location=NATIVE_LOCATION)


@pytest.mark.parametrize('backends', _MORDA_SERVICE_BALANCER_BACKENDS_FAILING, ids=_MORDA_SERVICE_BALANCER_FAILING_IDS)
def test_morda_ntp_single_attempt(term_ctx, backends):
    term_ctx.run_knoss_single_attempt_test('morda_ntp', '/portal/ntp/refresh_data', backends[0], backends[1], location=NATIVE_LOCATION)


@pytest.mark.parametrize('locations_weights_info', **one_enabled_location_knoss_only('morda_ntp'))
def test_morda_ntp_location_weights(term_ctx, locations_weights_info):
    term_ctx.run_knoss_location_weights_test('morda_ntp', '/portal/ntp/refresh_data', location=NATIVE_LOCATION, **locations_weights_info)


@pytest.mark.parametrize('successful_backends', _MORDA_SERVICE_BALANCER_BACKENDS, ids=_MORDA_SERVICE_BALANCER_IDS)
def test_morda_ntp_header(term_ctx, successful_backends):
    term_ctx.run_knoss_header_test('morda_ntp', '/portal/ntp/refresh_data', successful_backends, location=NATIVE_LOCATION)


def test_locations_canonical():
    return [knoss_only_locations_backends_canon_test(service) for service in [
        'morda',
        'android_widget_api',
        'partner_strmchannels',
        'portal_station',
        'portal_front',
        'morda_app',
        'morda_searchapp_config',
        'morda_browser_config',
        'morda_ibrowser_config',
        'morda_ntp',
    ]]
