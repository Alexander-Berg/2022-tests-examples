# -*- coding: utf-8 -*-
import pytest
import time
from balancer.test.configs.lib.config import Config
from balancer.test.configs.lib.pattern import _XFFY_HEADERS, header_test, gen_test_ids, location_weights_test
from balancer.test.configs.lib.locations_test import one_enabled_location_subheavy, subheavy_locations_backends_canon_test


def main_test(subheavy_ctx, testcfg, path):
    subheavy_ctx.set_native_location(testcfg.native_location)

    subheavy_ctx.run_base_service_test(path,
                                       backends=testcfg.backends,
                                       service_stat_name=testcfg.module_name,
                                       service_total_stat_name="service_total",
                                       **testcfg.get_options())


def onerr_test(subheavy_ctx, testcfg, path):
    subheavy_ctx.set_native_location(testcfg.native_location)

    backends = [
        "backends_sas#production-pumpi-yp",
        "backends_vla#production-pumpi-yp"
    ]
    subheavy_ctx.run_base_service_test(path,
                                       backends=backends,
                                       service_stat_name=testcfg.module_name + "_requests_to_onerror",
                                       service_total_stat_name="service_total",
                                       **testcfg.get_options())


# --- TESTING MORDA ---


morda_config = Config(
    module_name="morda",
    paths=[
        "/",
        "/my-cookie.js",
        "/wsave.html",
        "/form.html",
        "/mailinfo.js",
        "/dropcounters.js",
        "/all.xml",
        "/original.xml",
        "/services.xml",
        "/hello.html",
        "/hellom.html",
        "/hello.html",
        "/hellotel.html",
        "/ok.html",
        "/index.html",
        "/index.htm",
        "/googlecom.html",
        "/application.xml",
        "/black.html",
        "/white.html",
        "/empty.html",
        "/crossdomain.xml",
        "/i-social__closer.html",
        "/login-status.html",
        "/mda.html",
        "/mdaxframeproxy.html",
        "/xframeproxy.html",
        "/apple-touch-icon.png",
        "/embeded.min.js",
        "/htc",
        "/HTC",
        "/mdaiframe.html",
        "/apple-app-site-association",
        "/wsave",
        "/autopattern",
        "/all",
        "/myclean",
        "/screenx",
        "/remotes-status",
        "/setmy",
        "/adddata",
        "/wcomplain",
        "/route",
        "/clean_route",
        "/save_route",
        "/drop_route",
        "/m",
        "/d",
        "/mdae",
        "/gpsave",
        "/mgpsave",
        "/jcb",
        "/gs",
        "/bjk",
        "/fb",
        "/sade",
        "/all",
        "/themes",
        "/skins",
        "/rapid",
        "/instant",
        "/postcard",
        "/y",
        "/json",
        "/data",
        "/test",
        "/banner",
        "/portal",
        "/log",
        "/black",
        "/white",
        "/map_router",
        "/ua",
        "/ru",
        "/kz",
        "/by",
        "/safari",
        "/ios7se",
        "/inline",
        "/jsb",
        "/i",
        "/dform",
        "/chrome",
        "/.well-known",
        "/1tv",
        "/matchtv",
        "/ntv",
        "/5tv",
        "/ru24",
        "/vesti",
        "/rentv",
        "/tnt",
        "/m24",
        "/a2",
        "/sovsekretno",
        "/izvestia",
        "/echotv",
        "/probusiness",
        "/uspeh",
        "/globalstar",
        "/tochkatv",
        "/hardlife",
        "/oceantv",
        "/gamanoid",
        "/hitv",
        "/rutv",
        "/topspb",
        "/tdk",
        "/oirtv",
        "/rt",
        "/rtdoc",
        "/hdmedia",
        "/wfc",
        "/sibir",
        "/ntvpravo",
        "/ntvserial",
        "/ntvstyle",
        "/ctc",
        "/samsung-bixby",
    ],
    backends=["backends_vla#stable-portal-apphost-vla",
              "backends_sas#stable-portal-apphost-sas",
              "backends_man#stable-portal-apphost-man"],
    native_location="MAN",
    has_laas=True,
    has_uaas=True,
)


@pytest.mark.parametrize("path", morda_config.paths, ids=gen_test_ids(morda_config.paths))
def test_morda_main(subheavy_ctx, path):
    main_test(subheavy_ctx, morda_config, path)


@pytest.mark.parametrize("path", morda_config.paths, ids=gen_test_ids(morda_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_morda_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, morda_config, path, xffy_header)


@pytest.mark.parametrize("path", morda_config.paths[:5], ids=gen_test_ids(morda_config.paths[:5]))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(morda_config.module_name))
def test_morda_locations_weights(subheavy_ctx, path, locations_weights_info):
    location_weights_test(subheavy_ctx, morda_config, path, locations_weights_info)


@pytest.mark.parametrize("path", ["/", "/all", "/themes", "/portal", "/data", "/instant"],
                         ids=["index",  "all",  "themes",  "portal",  "data",  "instant"])
def test_morda_onerr(subheavy_ctx, path):
    onerr_test(subheavy_ctx, morda_config, path)


# --- TESTING ANDROID_WIDGET_API ---


awa_config = Config(
    module_name="android_widget_api",
    paths=["/android_widget_api"],
    backends=["backends_vla#stable-portal-androidwidget-vla",
              "backends_sas#stable-portal-androidwidget-sas",
              "backends_man#stable-portal-androidwidget-man"],
    native_location="MAN",
    has_laas=True,
    has_uaas=True,
)


@pytest.mark.parametrize("path", awa_config.paths, ids=gen_test_ids(awa_config.paths))
def test_android_widget_api_main(subheavy_ctx, path):
    main_test(subheavy_ctx, awa_config, path)


@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
@pytest.mark.parametrize("path", awa_config.paths, ids=gen_test_ids(awa_config.paths))
def test_android_widget_api_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, awa_config, path, xffy_header)


@pytest.mark.parametrize("path", awa_config.paths, ids=gen_test_ids(awa_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(awa_config.module_name))
def test_android_widget_api_location_weights(subheavy_ctx, locations_weights_info, path):
    location_weights_test(subheavy_ctx, awa_config, path, locations_weights_info)


@pytest.mark.parametrize("path", awa_config.paths, ids=gen_test_ids(awa_config.paths))
def test_android_widget_api_onerr(subheavy_ctx, path):
    onerr_test(subheavy_ctx, awa_config, path)


# --- TESTING MORDA_APP ---


morda_app_config = Config(
    module_name="morda_app",
    paths=[
        "/portal/api/search/2",
    ],
    backends=["backends_vla#stable-portal-app-apphost-vla",
              "backends_sas#stable-portal-app-apphost-sas",
              "backends_man#stable-portal-app-apphost-man"],
    native_location="MAN",
    has_laas=True,
    has_uaas=True,
)


@pytest.mark.parametrize("path", morda_app_config.paths, ids=gen_test_ids(morda_app_config.paths))
def test_morda_app_main(subheavy_ctx, path):
    main_test(subheavy_ctx, morda_app_config, path)


@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
@pytest.mark.parametrize("path", morda_app_config.paths, ids=gen_test_ids(morda_app_config.paths))
def test_morda_app_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, morda_app_config, path, xffy_header)


@pytest.mark.parametrize("path", morda_app_config.paths, ids=gen_test_ids(morda_app_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(morda_app_config.module_name))
def test_morda_app_location_weights(subheavy_ctx, locations_weights_info, path):
    location_weights_test(subheavy_ctx, morda_app_config, path, locations_weights_info)


@pytest.mark.parametrize("path", morda_app_config.paths, ids=gen_test_ids(morda_app_config.paths))
def test_morda_app_onerr(subheavy_ctx, path):
    onerr_test(subheavy_ctx, morda_app_config, path)


# --- TESTING MORDA_BROWSER_CONFIG ---


morda_browser_config_config = Config(
    module_name="morda_browser_config",
    paths=[
        "/portal/mobilesearch/config/browser/"
    ],
    backends=["backends_vla#stable-portal-app-apphost-vla",
              "backends_sas#stable-portal-app-apphost-sas",
              "backends_man#stable-portal-app-apphost-man"],
    native_location="MAN",
    has_laas=True,
    has_uaas=True,
)


@pytest.mark.parametrize("path", morda_browser_config_config.paths, ids=gen_test_ids(morda_browser_config_config.paths))
def test_morda_browser_config_main(subheavy_ctx, path):
    main_test(subheavy_ctx, morda_browser_config_config, path)


@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
@pytest.mark.parametrize("path", morda_browser_config_config.paths, ids=gen_test_ids(morda_browser_config_config.paths))
def test_morda_browser_config_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, morda_browser_config_config, path, xffy_header)


@pytest.mark.parametrize("path", morda_browser_config_config.paths, ids=gen_test_ids(morda_browser_config_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(morda_browser_config_config.module_name))
def test_morda_browser_config_location_weights(subheavy_ctx, locations_weights_info, path):
    location_weights_test(subheavy_ctx, morda_browser_config_config, path, locations_weights_info)


@pytest.mark.parametrize("path", morda_browser_config_config.paths, ids=gen_test_ids(morda_browser_config_config.paths))
def test_morda_browser_config_onerr(subheavy_ctx, path):
    onerr_test(subheavy_ctx, morda_browser_config_config, path)


# --- TESTING MORDA_IBROWSER_CONFIG ---


morda_ibrowser_config_config = Config(
    module_name="morda_ibrowser_config",
    paths=[
        "/portal/mobilesearch/config/ibrowser/"
    ],
    backends=["backends_vla#stable-portal-app-apphost-vla",
              "backends_sas#stable-portal-app-apphost-sas",
              "backends_man#stable-portal-app-apphost-man"],
    native_location="MAN",
    has_laas=True,
    has_uaas=True,
)


@pytest.mark.parametrize("path", morda_ibrowser_config_config.paths, ids=gen_test_ids(morda_ibrowser_config_config.paths))
def test_morda_ibrowser_config_main(subheavy_ctx, path):
    main_test(subheavy_ctx, morda_ibrowser_config_config, path)


@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
@pytest.mark.parametrize("path", morda_ibrowser_config_config.paths, ids=gen_test_ids(morda_ibrowser_config_config.paths))
def test_morda_ibrowser_config_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, morda_ibrowser_config_config, path, xffy_header)


@pytest.mark.parametrize("path", morda_ibrowser_config_config.paths, ids=gen_test_ids(morda_ibrowser_config_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(morda_ibrowser_config_config.module_name))
def test_morda_ibrowser_config_location_weights(subheavy_ctx, locations_weights_info, path):
    location_weights_test(subheavy_ctx, morda_ibrowser_config_config, path, locations_weights_info)


@pytest.mark.parametrize("path", morda_ibrowser_config_config.paths, ids=gen_test_ids(morda_ibrowser_config_config.paths))
def test_morda_ibrowser_config_onerr(subheavy_ctx, path):
    onerr_test(subheavy_ctx, morda_ibrowser_config_config, path)


# --- TESTING MORDA_SEARCHAPP_CONFIG ---


morda_searchapp_config_config = Config(
    module_name="morda_searchapp_config",
    paths=[
        "/portal/mobilesearch/config/searchapp/"
    ],
    backends=["backends_vla#stable-portal-app-apphost-vla",
              "backends_sas#stable-portal-app-apphost-sas",
              "backends_man#stable-portal-app-apphost-man"],
    native_location="MAN",
    has_laas=True,
    has_uaas=True,
)


@pytest.mark.parametrize("path", morda_searchapp_config_config.paths, ids=gen_test_ids(morda_searchapp_config_config.paths))
def test_morda_searchapp_config_main(subheavy_ctx, path):
    main_test(subheavy_ctx, morda_searchapp_config_config, path)


@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
@pytest.mark.parametrize("path", morda_searchapp_config_config.paths, ids=gen_test_ids(morda_searchapp_config_config.paths))
def test_morda_searchapp_config_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, morda_searchapp_config_config, path, xffy_header)


@pytest.mark.parametrize("path", morda_searchapp_config_config.paths, ids=gen_test_ids(morda_searchapp_config_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(morda_searchapp_config_config.module_name))
def test_morda_searchapp_config_location_weights(subheavy_ctx, locations_weights_info, path):
    location_weights_test(subheavy_ctx, morda_searchapp_config_config, path, locations_weights_info)


@pytest.mark.parametrize("path", morda_searchapp_config_config.paths, ids=gen_test_ids(morda_searchapp_config_config.paths))
def test_morda_searchapp_config_onerr(subheavy_ctx, path):
    onerr_test(subheavy_ctx, morda_searchapp_config_config, path)


# --- TESTING PARTNER_STRMCHANNELS ---


ps_config = Config(
    module_name="partner_strmchannels",
    paths=["/partner-strmchannels"],
    backends=["backends_vla#stable-morda-yaru-vla-yp",
              "backends_sas#stable-morda-yaru-sas-yp",
              "backends_man#stable-morda-yaru-man-yp"],
    native_location="MAN",
    has_laas=True,
    has_uaas=True,
)


@pytest.mark.parametrize("path", ps_config.paths, ids=gen_test_ids(ps_config.paths))
def test_partner_strmchannels_main(subheavy_ctx, path):
    main_test(subheavy_ctx, ps_config, path)


@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
@pytest.mark.parametrize("path", ps_config.paths, ids=gen_test_ids(ps_config.paths))
def test_partner_strmchannels_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, ps_config, path, xffy_header)


@pytest.mark.parametrize("path", ps_config.paths, ids=gen_test_ids(ps_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(ps_config.module_name))
def test_partner_strmchannels_weights(subheavy_ctx, path, locations_weights_info):
    location_weights_test(subheavy_ctx, ps_config, path, locations_weights_info)


@pytest.mark.parametrize("path", ["/"])
def test_prefetch_disabling(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")

    backends_response = {"content": "i am {}".format(path), "status": 200}
    subheavy_ctx.start_service_backends(response=backends_response, backends=[
        "backends_vla#stable-portal-apphost-vla",
        "backends_sas#stable-portal-apphost-sas",
        "backends_man#stable-portal-apphost-man",
    ])

    def check_prefetch_requests(code):
        cgi_variations = [path + "?yandex_prefetch=prerender", path + "?yandex_prefetch=prefetch"]
        for prefetch_header in ("prefetch", "prerender"):
            for p in [path] + cgi_variations:
                headers = {"Host": "yandex.ru", "Yandex-Preload": prefetch_header}
                resp = subheavy_ctx.perform_unprepared_request({
                    "path": p,
                    "headers": headers,
                    "method": "get"
                })
                assert resp.status_code == code

        for p in cgi_variations:
            resp = subheavy_ctx.perform_unprepared_request({"path": p, "headers": {"Host": "yandex.ru"}, "method": "get"})
            assert resp.status_code == code

        resp = subheavy_ctx.perform_unprepared_request({"path": path, "headers": {"Host": "yandex.ru"}, "method": "get"})
        assert resp.status_code == 200

    check_prefetch_requests(code=200)

    with subheavy_ctx.weights_manager.get_file("morda_prefetch_switch") as weights_file:
        weights_file.set({"prefetch_enabled": 0, "prefetch_disabled": 1})
        time.sleep(1)

        check_prefetch_requests(code=429)


# --- TESTING PORTAL_STATION ---


portal_station_config = Config(
    module_name="portal_station",
    paths=[
        "/portal/station",
        "/portal/station/",
        "/portal/station?test=1",
        "/portal/station/test?test=1",
    ],
    backends=["backends_vla#stable-morda-station-vla-yp",
              "backends_sas#stable-morda-station-sas-yp",
              "backends_man#stable-morda-station-man-yp"],
    native_location="MAN",
    has_laas=True,
    has_uaas=True,
)


@pytest.mark.parametrize("path", portal_station_config.paths, ids=gen_test_ids(portal_station_config.paths))
def test_portal_station_main(subheavy_ctx, path):
    main_test(subheavy_ctx, portal_station_config, path)


@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
@pytest.mark.parametrize("path", portal_station_config.paths, ids=gen_test_ids(portal_station_config.paths))
def test_portal_station_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, portal_station_config, path, xffy_header)


@pytest.mark.parametrize("path", portal_station_config.paths, ids=gen_test_ids(portal_station_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(portal_station_config.module_name))
def test_portal_station_location_weights(subheavy_ctx, locations_weights_info, path):
    location_weights_test(subheavy_ctx, portal_station_config, path, locations_weights_info)


@pytest.mark.parametrize("path", portal_station_config.paths, ids=gen_test_ids(portal_station_config.paths))
def test_portal_station_onerr(subheavy_ctx, path):
    onerr_test(subheavy_ctx, portal_station_config, path)


# --- TESTING PORTAL_FRONT ---


portal_front_config = Config(
    module_name="portal_front",
    paths=[
        "/portal/front",
        "/portal/front/",
        "/portal/front?test=1",
        "/portal/front/test?test=1",
    ],
    backends=["backends_vla#stable-portal-apphost-vla",
              "backends_sas#stable-portal-apphost-sas",
              "backends_man#stable-portal-apphost-man"],
    native_location="MAN",
    has_laas=True,
    has_uaas=True,
)


@pytest.mark.parametrize("path", portal_front_config.paths, ids=gen_test_ids(portal_front_config.paths))
def test_portal_front_main(subheavy_ctx, path):
    main_test(subheavy_ctx, portal_front_config, path)


@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
@pytest.mark.parametrize("path", portal_front_config.paths, ids=gen_test_ids(portal_front_config.paths))
def test_portal_front_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, portal_front_config, path, xffy_header)


@pytest.mark.parametrize("path", portal_front_config.paths, ids=gen_test_ids(portal_front_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(portal_front_config.module_name))
def test_portal_front_location_weights(subheavy_ctx, locations_weights_info, path):
    location_weights_test(subheavy_ctx, portal_front_config, path, locations_weights_info)


@pytest.mark.parametrize("path", portal_front_config.paths, ids=gen_test_ids(portal_front_config.paths))
def test_portal_front_onerr(subheavy_ctx, path):
    onerr_test(subheavy_ctx, portal_front_config, path)


# --- TESTING WY ---


wy_config = Config(
    module_name="wy",
    paths=["/wy"],
    backends=["backends_man#awacs-rtc_balancer_morda-yp-hw_man",
              "backends_sas#awacs-rtc_balancer_morda-yp-hw_sas",
              "backends_vla#awacs-rtc_balancer_morda-yp-hw_vla"],
    native_location="MAN",
    has_laas=False,
    has_uaas=False,
)


@pytest.mark.parametrize("path", wy_config.paths, ids=gen_test_ids(wy_config.paths))
def test_wy_main(subheavy_ctx, path):
    main_test(subheavy_ctx, wy_config, path)


@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
@pytest.mark.parametrize("path", wy_config.paths, ids=gen_test_ids(wy_config.paths))
def test_wy_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, wy_config, path, xffy_header)


@pytest.mark.parametrize("path", wy_config.paths, ids=gen_test_ids(wy_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(wy_config.module_name))
def test_wy_location_weights(subheavy_ctx, locations_weights_info, path):
    location_weights_test(subheavy_ctx, wy_config, path, locations_weights_info)


# --- TESTING MORDA_NTP ---


morda_ntp = Config(
    module_name="morda_ntp",
    paths=[
        "/portal/api/data/1",
        "/portal/ntp/notifications",
        "/portal/ntp/informers",
        "/portal/ntp/refresh_data",
    ],
    backends=["backends_vla#stable-portal-app-apphost-vla",
              "backends_sas#stable-portal-app-apphost-sas",
              "backends_man#stable-portal-app-apphost-man"],
    native_location="MAN",
    has_laas=True,
    has_uaas=True,
)


@pytest.mark.parametrize("path", morda_ntp.paths, ids=gen_test_ids(morda_ntp.paths))
def test_morda_ntp_main(subheavy_ctx, path):
    main_test(subheavy_ctx, morda_ntp, path)


@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
@pytest.mark.parametrize("path", morda_ntp.paths, ids=gen_test_ids(morda_ntp.paths))
def test_morda_ntp_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, morda_ntp, path, xffy_header)


@pytest.mark.parametrize("path", morda_ntp.paths, ids=gen_test_ids(morda_ntp.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(morda_ntp.module_name))
def test_morda_ntp_location_weights(subheavy_ctx, locations_weights_info, path):
    location_weights_test(subheavy_ctx, morda_ntp, path, locations_weights_info)


@pytest.mark.parametrize("path", morda_ntp.paths, ids=gen_test_ids(morda_ntp.paths))
def test_morda_ntp_onerr(subheavy_ctx, path):
    onerr_test(subheavy_ctx, morda_ntp, path)


def test_locations_canonical():
    return [subheavy_locations_backends_canon_test(service) for service in [
        "morda",
        "android_widget_api",
        "partner_strmchannels",
        "portal_station",
        "portal_front",
        "wy",
        "morda_app",
        "morda_browser_config",
        "morda_ibrowser_config",
        "morda_searchapp_config",
        "morda_ntp",
    ]]
