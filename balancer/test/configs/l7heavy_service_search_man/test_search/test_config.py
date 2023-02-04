# -*- coding: utf-8 -*-
import pytest
import requests

from balancer.test.configs.lib.config import Config
from balancer.test.configs.lib.pattern import _XFFY_HEADERS, header_test, gen_test_ids, extend_weights_test_params, location_weights_test
from balancer.test.configs.lib.locations_test import one_enabled_location_subheavy, subheavy_locations_backends_canon_test


_WEB_HTTP_ADAPTER_BACKENDS = [
    "backends_sas#production-app-host-http_adapter-sas-web-yp",
    "backends_sas#prestable-app-host-http_adapter-sas-web-yp",
    "backends_sas#prestable-exp-app-host-http_adapter-sas-web-yp",
    "backends_man#production-app-host-http_adapter-man-web-yp",
    "backends_vla#production-app-host-http_adapter-vla-web-yp",
]

_WEB_REPORT_BACKENDS = [
    "backends_sas#prestable-report-sas-web-yp",
    "backends_sas#production-report-sas-web-yp",
    "backends_man#production-report-man-web-yp",
    "backends_vla#production-report-vla-web-yp",
]

# --- TESTING SEARCH-XML ---


search_xml_config = Config(
    module_name="search-xml",
    paths=["/search/xml"],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
    has_laas=False,
    has_uaas=False,
)


@pytest.mark.parametrize("path", search_xml_config.paths, ids=gen_test_ids(search_xml_config.paths))
def test_search_xml(subheavy_ctx, path):
    subheavy_ctx.set_native_location(search_xml_config.native_location)

    subheavy_ctx.run_base_service_test(
        path,
        backends=search_xml_config.backends,
        service_stat_name="search-xml",
        service_total_stat_name="service_total",
        **search_xml_config.get_options()
    )


@pytest.mark.parametrize("path", search_xml_config.paths, ids=gen_test_ids(search_xml_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_search_xml_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, search_xml_config, path, xffy_header)


@pytest.mark.parametrize("path", search_xml_config.paths, ids=gen_test_ids(search_xml_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(search_xml_config.module_name))
def test_search_xml_location_weights(subheavy_ctx, path, locations_weights_info):
    location_weights_test(subheavy_ctx, search_xml_config, path, locations_weights_info)


search_xml_http_adapter_config = Config(
    module_name="search-xml-http-adapter",
    paths=["/xmlsearch"],
    backends=_WEB_HTTP_ADAPTER_BACKENDS + _WEB_REPORT_BACKENDS,
    native_location="MAN",
    has_laas=False,
    has_uaas=False,
)


@pytest.mark.parametrize("path", search_xml_http_adapter_config.paths, ids=gen_test_ids(search_xml_http_adapter_config.paths))
def test_search_xml_http_adapter(subheavy_ctx, path):
    subheavy_ctx.set_native_location(search_xml_http_adapter_config.native_location)

    subheavy_ctx.run_base_service_test(
        path,
        backends=search_xml_http_adapter_config.backends,
        service_stat_name="search-xml",
        service_total_stat_name="service_total",
        **search_xml_http_adapter_config.get_options()
    )


@pytest.mark.parametrize("path", search_xml_http_adapter_config.paths, ids=gen_test_ids(search_xml_http_adapter_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_search_xml_http_adapter_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, search_xml_http_adapter_config, path, xffy_header)


@pytest.mark.parametrize("path", search_xml_http_adapter_config.paths, ids=gen_test_ids(search_xml_http_adapter_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(search_xml_http_adapter_config.module_name))
def test_search_xml_http_adapter_location_weights(subheavy_ctx, path, locations_weights_info):
    location_weights_test(
        subheavy_ctx, search_xml_http_adapter_config, path, locations_weights_info,
        service_stat_name="search-xml",
    )


# --- TESTING SEARCH ---

search_config = Config(
    module_name="search",
    paths=[
        "/search",
        "/yandsearch",
        "/msearch",
    ],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
    has_laas=False,
    has_uaas=False,
)


@pytest.mark.parametrize("path", search_config.paths, ids=gen_test_ids(search_config.paths))
def test_search(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")
    subheavy_ctx.run_base_service_test(
        path,
        backends=search_config.backends,
        service_stat_name="search",
        service_total_stat_name="service_total",
        has_laas=False,
        has_uaas=False,
    )


@pytest.mark.parametrize("path", search_config.paths, ids=gen_test_ids(search_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_search_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, search_config, path, xffy_header)


@pytest.mark.parametrize("path", search_config.paths, ids=gen_test_ids(search_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(search_config.module_name))
def test_search_location_weights(subheavy_ctx, path, locations_weights_info):
    subheavy_ctx.set_native_location("MAN")
    subheavy_ctx.run_location_weights_test(
        path,
        **dict(search_config.get_options().items() +
               extend_weights_test_params(subheavy_ctx, locations_weights_info, "search").items())
    )


# --- TESTING PADSEARCH ---

padsearch_config = Config(
    module_name="padsearch",
    paths=[
        "/padsearch",
    ],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
    has_laas=False,
    has_uaas=False,
)


@pytest.mark.parametrize("path", padsearch_config.paths, ids=gen_test_ids(padsearch_config.paths))
def test_padsearch(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")
    subheavy_ctx.run_base_service_test(
        path,
        backends=padsearch_config.backends,
        service_stat_name="padsearch",
        service_total_stat_name="service_total",
        has_laas=False,
        has_uaas=False,
    )


@pytest.mark.parametrize("path", padsearch_config.paths, ids=gen_test_ids(padsearch_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_padsearch_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, padsearch_config, path, xffy_header)


@pytest.mark.parametrize("path", padsearch_config.paths, ids=gen_test_ids(padsearch_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(padsearch_config.module_name))
def test_padsearch_location_weights(subheavy_ctx, path, locations_weights_info):
    subheavy_ctx.set_native_location("MAN")
    subheavy_ctx.run_location_weights_test(
        path,
        **dict(padsearch_config.get_options().items() +
               extend_weights_test_params(subheavy_ctx, locations_weights_info, "padsearch").items())
    )


# --- TESTING OPENSEARCH ---


opensearch_config = Config(
    module_name="opensearch",
    paths=[
        "/search/opensearch.xml",
        "/opensearch.xml",
    ],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
)


@pytest.mark.parametrize("path", opensearch_config.paths, ids=gen_test_ids(opensearch_config.paths))
def test_opensearch(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")
    service_stat_name = "opensearch"

    subheavy_ctx.run_base_service_test(
        path,
        backends=opensearch_config.backends,
        service_stat_name=service_stat_name,
        service_total_stat_name="service_total",
        **opensearch_config.get_options()
    )


@pytest.mark.parametrize("path", opensearch_config.paths, ids=gen_test_ids(opensearch_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_opensearch_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, opensearch_config, path, xffy_header)


@pytest.mark.parametrize("path", opensearch_config.paths, ids=gen_test_ids(opensearch_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(opensearch_config.module_name))
def test_opensearch_location_weights(subheavy_ctx, path, locations_weights_info):
    service_stat_name = "opensearch"
    subheavy_ctx.set_native_location("MAN")
    subheavy_ctx.run_location_weights_test(
        path,
        **dict(opensearch_config.get_options().items() +
               extend_weights_test_params(subheavy_ctx, locations_weights_info, service_stat_name).items()))


# --- TESTING SEARCH_OTHER ---


search_other_config = Config(
    module_name="search_other",
    paths=[
        "/telsearch",
        "/schoolsearch",
        "/familysearch",
    ],
    backends=_WEB_REPORT_BACKENDS,
    native_location="MAN",
)


@pytest.mark.parametrize("path", search_other_config.paths, ids=gen_test_ids(search_other_config.paths))
def test_search_other(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")
    service_stat_name = "search_other"

    subheavy_ctx.run_base_service_test(
        path,
        backends=search_other_config.backends,
        service_stat_name=service_stat_name,
        service_total_stat_name="service_total",
        **search_other_config.get_options()
    )


@pytest.mark.parametrize("path", search_other_config.paths, ids=gen_test_ids(search_other_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_search_other_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, search_other_config, path, xffy_header)


@pytest.mark.parametrize("path", search_other_config.paths, ids=gen_test_ids(search_other_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(search_other_config.module_name))
def test_search_other_location_weights(subheavy_ctx, path, locations_weights_info):
    service_stat_name = "search_other"
    subheavy_ctx.set_native_location("MAN")
    subheavy_ctx.run_location_weights_test(
        path,
        **dict(search_other_config.get_options().items() +
               extend_weights_test_params(subheavy_ctx, locations_weights_info, service_stat_name).items()))


# --- TESTING SEARCH_LAST ---


search_last_config = Config(
    module_name="search_last",
    paths=[
        "/yandpage",
        "/largesearch",
        "/msearchpart",
        "/jsonsearch",
        "/yca",
        "/yandcache.js"
    ],
    backends=_WEB_REPORT_BACKENDS,
    native_location="MAN",
)


@pytest.mark.parametrize("path", search_last_config.paths, ids=gen_test_ids(search_last_config.paths))
def test_search_last(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")
    service_stat_name = "search_last"

    subheavy_ctx.run_base_service_test(
        path,
        backends=search_last_config.backends,
        service_stat_name=service_stat_name,
        service_total_stat_name="service_total",
        **search_last_config.get_options()
    )


@pytest.mark.parametrize("path", search_last_config.paths, ids=gen_test_ids(search_last_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_search_last_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, search_last_config, path, xffy_header)


@pytest.mark.parametrize("path", search_last_config.paths, ids=gen_test_ids(search_last_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(search_last_config.module_name))
def test_search_last_location_weights(subheavy_ctx, path, locations_weights_info):
    service_stat_name = "search_last"
    subheavy_ctx.set_native_location("MAN")
    subheavy_ctx.run_location_weights_test(
        path,
        **dict(search_last_config.get_options().items() +
               extend_weights_test_params(subheavy_ctx, locations_weights_info, service_stat_name).items()))


# --- TESTING SEARCHAPI ---


searchapi_config = Config(
    module_name="searchapi",
    paths=[
        "/searchapi",
        "/search/searchapi",
    ],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
)


@pytest.mark.parametrize("path", searchapi_config.paths, ids=gen_test_ids(searchapi_config.paths))
def test_searchapi(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")

    subheavy_ctx.run_base_service_test(
        path,
        backends=searchapi_config.backends,
        service_stat_name="searchapi",
        service_total_stat_name="service_total",
    )


@pytest.mark.parametrize("path", searchapi_config.paths, ids=gen_test_ids(searchapi_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_searchapi_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, searchapi_config, path, xffy_header)


@pytest.mark.parametrize("path", searchapi_config.paths, ids=gen_test_ids(searchapi_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(searchapi_config.module_name))
def test_searchapi_location_weights(subheavy_ctx, path, locations_weights_info):
    subheavy_ctx.set_native_location("MAN")
    subheavy_ctx.run_location_weights_test(
        path,
        **dict(searchapi_config.get_options().items() +
               extend_weights_test_params(subheavy_ctx, locations_weights_info, "searchapi").items())
    )


# --- TESTING TOUCHSEARCH ---


touchsearch_config = Config(
    module_name="touchsearch",
    paths=[
        "/touchsearch",
        "/search/touch",
    ],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
    has_laas=False,
    has_uaas=False,
)


@pytest.mark.parametrize("path", touchsearch_config.paths, ids=gen_test_ids(touchsearch_config.paths))
def test_touchsearch(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")

    subheavy_ctx.run_base_service_test(
        path,
        backends=touchsearch_config.backends,
        service_stat_name="touchsearch",
        service_total_stat_name="service_total",
        has_laas=False,
        has_uaas=False,
    )


@pytest.mark.parametrize("path", touchsearch_config.paths, ids=gen_test_ids(touchsearch_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_touchsearch_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, touchsearch_config, path, xffy_header)


@pytest.mark.parametrize("path", touchsearch_config.paths, ids=gen_test_ids(touchsearch_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(touchsearch_config.module_name))
def test_touchsearch_location_weights(subheavy_ctx, path, locations_weights_info):
    subheavy_ctx.set_native_location("MAN")
    subheavy_ctx.run_location_weights_test(
        path,
        **dict(touchsearch_config.get_options().items() +
               extend_weights_test_params(subheavy_ctx, locations_weights_info, "touchsearch").items())
    )


# --- TESTING SEARCH ADS ---


searchads_config = Config(
    module_name="searchads",
    paths=[
        "/search/ads",
        "/search/touch/ads",
        "/search/pad/ads",
        "/search/direct",
        "/search/touch/direct",
        "/search/pad/direct",
    ],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
)


@pytest.mark.parametrize("path", searchads_config.paths, ids=gen_test_ids(searchads_config.paths))
def test_searchads(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")

    subheavy_ctx.run_base_service_test(
        path,
        backends=searchads_config.backends,
        service_stat_name="searchads",
        service_total_stat_name="service_total",
    )


@pytest.mark.parametrize("path", searchads_config.paths, ids=gen_test_ids(searchads_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_searchads_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, searchads_config, path, xffy_header)


@pytest.mark.parametrize("path", searchads_config.paths, ids=gen_test_ids(searchads_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(searchads_config.module_name))
def test_searchads_location_weights(subheavy_ctx, path, locations_weights_info):
    subheavy_ctx.set_native_location("MAN")
    subheavy_ctx.run_location_weights_test(
        path,
        **dict(searchads_config.get_options().items() +
               extend_weights_test_params(subheavy_ctx, locations_weights_info, "searchads").items())
    )


# --- TESTING SEARCHPRE ---


searchpre_config = Config(
    module_name="searchpre",
    paths=[
        "/search/pre",
        "/search/pad/pre",
        "/search/touch/pre",
    ],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
)


@pytest.mark.parametrize("path", searchpre_config.paths, ids=gen_test_ids(searchpre_config.paths))
def test_searchpre(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")

    subheavy_ctx.run_base_service_test(
        path,
        backends=searchpre_config.backends,
        service_stat_name="searchpre",
        service_total_stat_name="service_total",
    )


@pytest.mark.parametrize("path", searchpre_config.paths, ids=gen_test_ids(searchpre_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_searchpre_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, searchpre_config, path, xffy_header)


@pytest.mark.parametrize("path", searchpre_config.paths, ids=gen_test_ids(searchpre_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(searchpre_config.module_name))
def test_searchpre_location_weights(subheavy_ctx, path, locations_weights_info):
    subheavy_ctx.set_native_location("MAN")
    subheavy_ctx.run_location_weights_test(
        path,
        **dict(searchpre_config.get_options().items() +
               extend_weights_test_params(subheavy_ctx, locations_weights_info, "searchpre").items())
    )


# --- TESTING JSONPROXY ---


jsonproxy_config = Config(
    module_name="jsonproxy",
    paths=["/jsonproxy"],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
)


@pytest.mark.parametrize("path", jsonproxy_config.paths, ids=gen_test_ids(jsonproxy_config.paths))
def test_jsonproxy(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")

    subheavy_ctx.run_base_service_test(
        path,
        backends=jsonproxy_config.backends,
        service_stat_name="jsonproxy",
        service_total_stat_name="service_total",
    )


@pytest.mark.parametrize("path", jsonproxy_config.paths, ids=gen_test_ids(jsonproxy_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_jsonproxy_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, jsonproxy_config, path, xffy_header)


@pytest.mark.parametrize("path", jsonproxy_config.paths, ids=gen_test_ids(jsonproxy_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy(jsonproxy_config.module_name))
def test_jsonproxy_location_weights(subheavy_ctx, path, locations_weights_info):
    location_weights_test(subheavy_ctx, jsonproxy_config, path, locations_weights_info)


# --- TESTING SEARCHAPP ---


searchapp_config = Config(
    module_name="searchapp",
    paths=["/searchapp"],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
    has_laas=False,
    has_uaas=False,
)


@pytest.mark.parametrize("path", searchapp_config.paths, ids=gen_test_ids(searchapp_config.paths))
def test_searchapp(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")

    subheavy_ctx.run_base_service_test(
        path,
        backends=searchapp_config.backends,
        service_stat_name="searchapp",
        service_total_stat_name="service_total",
        has_laas=False,
        has_uaas=False,
    )


@pytest.mark.parametrize("path", searchapp_config.paths, ids=gen_test_ids(searchapp_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_searchapp_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, searchapp_config, path, xffy_header)


@pytest.mark.parametrize("path", searchapp_config.paths, ids=gen_test_ids(searchapp_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy("searchapp"))
def test_searchapp_location_weights(subheavy_ctx, path, locations_weights_info):
    location_weights_test(subheavy_ctx, searchapp_config, path, locations_weights_info)


# --- TESTING SEARCHAPP_OTHER ---


searchapp_other_config = Config(
    module_name="searchapp_other",
    paths=["/searchapp/meta", "/searchapp/sdch"],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
)


@pytest.mark.parametrize("path", searchapp_other_config.paths, ids=gen_test_ids(searchapp_other_config.paths))
def test_searchapp_other(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")

    subheavy_ctx.run_base_service_test(
        path,
        backends=searchapp_other_config.backends,
        service_stat_name="searchapp_other",
        service_total_stat_name="service_total",
    )


@pytest.mark.parametrize("path", searchapp_other_config.paths, ids=gen_test_ids(searchapp_other_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_searchapp_other_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, searchapp_other_config, path, xffy_header)


@pytest.mark.parametrize("path", searchapp_other_config.paths, ids=gen_test_ids(searchapp_other_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy("searchapp"))
def test_searchapp_other_location_weights(subheavy_ctx, path, locations_weights_info):
    location_weights_test(subheavy_ctx, searchapp_other_config, path, locations_weights_info)


# --- TESTING BLOGS ---


blogs_config = Config(
    module_name="blogs",
    paths=["/blogs"],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
)


@pytest.mark.parametrize("path", blogs_config.paths, ids=gen_test_ids(blogs_config.paths))
def test_blogs(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")

    subheavy_ctx.run_base_service_test(
        "/blogs",
        backends=blogs_config.backends,
        service_stat_name="blogs",
        service_total_stat_name="service_total",
    )


@pytest.mark.parametrize("path", blogs_config.paths, ids=gen_test_ids(blogs_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_blogs_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, blogs_config, path, xffy_header)


@pytest.mark.parametrize("path", blogs_config.paths, ids=gen_test_ids(blogs_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy("blogs"))
def test_blogs_location_weights(subheavy_ctx, path, locations_weights_info):
    location_weights_test(subheavy_ctx, blogs_config, path, locations_weights_info)


# --- TESTING CHAT ---


chat_config = Config(
    module_name="chat",
    paths=["/chat"],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
)


@pytest.mark.parametrize("path", chat_config.paths, ids=gen_test_ids(chat_config.paths))
def test_chat(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")

    subheavy_ctx.run_base_service_test(
        "/chat",
        backends=chat_config.backends,
        service_stat_name="chat",
        service_total_stat_name="service_total",
    )


@pytest.mark.parametrize("path", chat_config.paths, ids=gen_test_ids(chat_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_chat_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, chat_config, path, xffy_header)


@pytest.mark.parametrize("path", chat_config.paths, ids=gen_test_ids(chat_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy("chat"))
def test_chat_location_weights(subheavy_ctx, path, locations_weights_info):
    location_weights_test(subheavy_ctx, chat_config, path, locations_weights_info)


# --- TESTING REPORTMARKET ---


report_market_config = Config(
    module_name="reportmarket",
    paths=["/search/report_market"],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
)


@pytest.mark.parametrize("path", report_market_config.paths, ids=gen_test_ids(report_market_config.paths))
def test_report_market(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")

    subheavy_ctx.run_base_service_test(
        "/search/report_market",
        backends=report_market_config.backends,
        service_stat_name="reportmarket",
        service_total_stat_name="service_total",
    )


@pytest.mark.parametrize("path", report_market_config.paths, ids=gen_test_ids(report_market_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_report_market_header(subheavy_ctx, path, xffy_header):
    header_test(subheavy_ctx, report_market_config, path, xffy_header)


@pytest.mark.parametrize("path", report_market_config.paths, ids=gen_test_ids(report_market_config.paths))
@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy("reportmarket"))
def test_report_market_location_weights(subheavy_ctx, path, locations_weights_info):
    location_weights_test(subheavy_ctx, report_market_config, path, locations_weights_info)


# --- TESTING SEARCHSDCH ---

def _gen_apphost_map(section, apphost=False):
    w = -1 if apphost else 1
    return {
        "_".join([section, dc]): weight for dc, weight in [
            ("man", w),
            ("sas", w),
            ("vla", w),
            ("apphost_man", -w),
            ("apphost_sas", -w),
            ("apphost_vla", -w),
        ]
    }


searchsdch_config = Config(
    module_name="searchsdch",
    paths=["/search/sdch/AKVWm-x5.dict"],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
)


@pytest.mark.parametrize("path", searchsdch_config.paths, ids=gen_test_ids(searchsdch_config.paths))
def test_searchsdch(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")

    subheavy_ctx.run_base_service_test(
        path,
        backends=searchsdch_config.backends,
        service_stat_name="searchsdch",
        service_total_stat_name="service_total",
        **searchsdch_config.get_options()
    )


@pytest.mark.parametrize("path", searchsdch_config.paths, ids=gen_test_ids(searchsdch_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_searchsdch_header(subheavy_ctx, path, xffy_header):
    subheavy_ctx.set_native_location("MAN")
    header_test(subheavy_ctx, searchsdch_config, path, xffy_header)


# --- TESTING STRANGE ---


strange_config = Config(
    module_name="searchapp",
    paths=[
        "/search",
        "/search/strange",
        "/touchsearch",
        "/touchsearch/strange",
        "/search/pad",
        "/search/pad/strange"
    ],
    backends=["backends_VLA_WEB_RUS_YALITE_SAS_WEB_RUS_YALITE"],
    native_location="MAN",
)


@pytest.mark.parametrize("path", strange_config.paths, ids=gen_test_ids(strange_config.paths))
def test_last_sections(subheavy_ctx, path):
    """ Checks that strange request not forwarded to pumpkin"""
    subheavy_ctx.set_native_location(strange_config.native_location)
    request = {"path": path, "method": "get"}
    subheavy_ctx.start_mocked_backends(
        strange_config.backends,
        response={"status": 200, "Content-Length": 0, "headers": []},
    )
    try:
        response = subheavy_ctx.perform_prepared_request(request)
        assert response.status_code == 200 and not path.endswith("strange")
    except requests.ConnectionError:
        assert path.endswith("strange")


# --- TESTING SEARCHAPPHOST ---


searchapphost_true_config = Config(
    module_name="searchapp",
    paths=[
        "/search/film-catalog",
        "/search/film-catalog/entity",
        "/search/afisha-schedule",
        "/search/ugc2/desktop-digest",
        "/search/ugc2/discussions",
        "/search/ugc2/sideblock",
        "/search/catalogsearch",
        "/search/entity",
        "/search/suggest-history",
        "/search/vwizdoc2doc",
        "/search/zero",
    ],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
    has_laas=False,
    has_uaas=False,
)


@pytest.mark.parametrize("path", searchapphost_true_config.paths, ids=gen_test_ids(searchapphost_true_config.paths))
def test_true_apphost(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")

    with subheavy_ctx.weights_manager.get_file("production.weights") as weights_file:
        weights = _gen_apphost_map("searchapphost", apphost=True)
        weights_file.set(weights)
        subheavy_ctx.run_base_service_test(
            path,
            backends=searchapphost_true_config.backends,
            service_stat_name="searchapphost",
            service_total_stat_name="service_total",
            **searchapphost_true_config.get_options()
        )


@pytest.mark.parametrize("path", searchapphost_true_config.paths, ids=gen_test_ids(searchapphost_true_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_apphost_true_header(subheavy_ctx, xffy_header, path):
    subheavy_ctx.set_native_location("MAN")
    section = "searchapphost"
    with subheavy_ctx.weights_manager.get_file("production.weights") as weights_file:
        weights = _gen_apphost_map(section, apphost=True)
        weights_file.set(weights)
        header_test(subheavy_ctx, searchapphost_true_config, path, xffy_header)


searchapphost_shinydiscovery_config = Config(
    module_name="searchapphost_shinydiscovery",
    paths=["/search/shiny-discovery"],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
    has_laas=False,
    has_uaas=False,
)


@pytest.mark.parametrize("path", searchapphost_shinydiscovery_config.paths)
def test_searchapphost_shinydiscovery(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")

    with subheavy_ctx.weights_manager.get_file("production.weights") as weights_file:
        weights = _gen_apphost_map("searchapphost", apphost=True)
        weights_file.set(weights)
        subheavy_ctx.run_base_service_test(
            path,
            backends=searchapphost_shinydiscovery_config.backends,
            service_stat_name="searchapphost_shinydiscovery",
            service_total_stat_name="service_total",
            **searchapphost_shinydiscovery_config.get_options()
        )


@pytest.mark.parametrize("path", searchapphost_shinydiscovery_config.paths)
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_searchapphost_shinydiscovery_header(subheavy_ctx, path, xffy_header):
    subheavy_ctx.set_native_location("MAN")
    section = "searchapphost"
    with subheavy_ctx.weights_manager.get_file("production.weights") as weights_file:
        weights = _gen_apphost_map(section, apphost=True)
        weights_file.set(weights)
        header_test(subheavy_ctx, searchapphost_shinydiscovery_config, path, xffy_header)


search_http_adapter_config = Config(
    module_name="",
    paths=[
        "/search/recommendation",
        "/sitesearch",
        "/search/site",
        "/search/direct-preview",
        "/search/frontend-entity",
        "/search/itditp",
        "/prefetch",
        "/tutor/search",
    ],
    backends=_WEB_HTTP_ADAPTER_BACKENDS,
    native_location="MAN",
)


def section_from_path(path):
    sect = {"/search/recommendation": "recommendation",
            "/sitesearch": "sitesearch",
            "/search/site": "sitesearch",
            "/search/direct-preview": "search_direct_preview",
            "/search/frontend-entity": "search_fronted_entity",
            "/search/itditp": "itditp",
            "/prefetch": "search_prefetch",
            "/tutor/search": "tutor"}
    return sect[path]


@pytest.mark.parametrize("path", search_http_adapter_config.paths, ids=gen_test_ids(search_http_adapter_config.paths))
def test_search_http_adapter(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")
    subheavy_ctx.start_rps_limiter_backends()

    options = search_http_adapter_config.get_options()
    if section_from_path(path) == "search_direct_preview":
        options.update({
            'has_laas': False,
            'has_uaas': False,
        })

    subheavy_ctx.run_base_service_test(
        path,
        backends=search_http_adapter_config.backends,
        service_stat_name=section_from_path(path),
        service_total_stat_name="service_total",
        **options
    )


@pytest.mark.parametrize("path", search_http_adapter_config.paths, ids=gen_test_ids(search_http_adapter_config.paths))
@pytest.mark.parametrize("xffy_header", _XFFY_HEADERS)
def test_search_http_adapter_header(subheavy_ctx, xffy_header, path):
    subheavy_ctx.start_rps_limiter_backends()
    header_test(subheavy_ctx, search_http_adapter_config, path, xffy_header)


@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy("recommendation"))
def test_search_http_adapter_location_weights(subheavy_ctx, locations_weights_info):
    subheavy_ctx.set_native_location("MAN")
    subheavy_ctx.run_location_weights_test(
        "/search/recommendation",
        **extend_weights_test_params(subheavy_ctx, locations_weights_info, "recommendation")
    )


@pytest.mark.parametrize("locations_weights_info", **one_enabled_location_subheavy("itditp"))
def test_search_itditp_location_weights(subheavy_ctx, locations_weights_info):
    subheavy_ctx.set_native_location("MAN")
    subheavy_ctx.start_rps_limiter_backends()
    subheavy_ctx.run_location_weights_test(
        "/search/itditp",
        **extend_weights_test_params(subheavy_ctx, locations_weights_info, "itditp")
    )


def test_locations_canonical(subheavy_ctx):
    return [subheavy_locations_backends_canon_test(service) for service in [
        "search",
        "opensearch",
        "search_other",
        "search_smart",
        "search_yandsearch",
        "searchsdch",
        "search_last",
        "padsearch",
        "padsearch_last",
        "search-xml",
        "jsonproxy",
        "jsonproxy_last",
        "searchapi",
        "touchsearch",
        "touchsearch_last",
        "recommendation",
        "sitesearch",
        "searchapp",
        "searchapp_other",
        "searchapphost",
        "searchapphost_shinydiscovery",
        "search_direct_preview",
        "search_fronted_entity",
        "itditp",
        "searchwizardsjson",
        "blogs",
        "chat",
        "reportmarket",
        "search_prefetch",
        "tutor",
        "searchads",
        "searchpre",
    ]]


@pytest.mark.parametrize("path", [
    "/prefetch.txt",
    "/search/prefetch.txt",
    "/search/yandcache.js",
    "/search/padcache.js",
    "/search/touchcache.js"
])
def test_search_static_prefetch(subheavy_ctx, path):
    subheavy_ctx.set_native_location("MAN")
    subheavy_ctx.run_base_service_test(
        path,
        backends=_WEB_HTTP_ADAPTER_BACKENDS,
        service_stat_name="search_static_prefetch",
        service_total_stat_name="service_total",
    )


@pytest.mark.parametrize("path", ["/touchsearch", "/search/touch", "/searchapp"])
def test_per_platform_prefetch_disabling(subheavy_ctx, path):
    IOS_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 12_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0 YaBrowser/19.6.1.157.10 Mobile/15E148 Safari/605.1"
    ANDROID_UA = "Mozilla/5.0 (Linux; Android 8.1.0; Mi Note 3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 YaBrowser/19.4.4.24.00 (broteam) Mobile Safari/537.36"
    LINUX_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 YaBrowser/18.11.1.715 (beta) Yowser/2.5 Safari/537.36"
    MAC_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3800.1 YaBrowser/19.9.0.0 Yowser/2.5 Safari/537.36"
    WINDOWS_UA = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 YaBrowser/19.4.3.370 Yowser/2.5 Safari/537.36"

    ALL_UAS = [IOS_UA, ANDROID_UA, LINUX_UA, MAC_UA, WINDOWS_UA]
    DESKTOP_UAS = [LINUX_UA, MAC_UA, WINDOWS_UA]
    TOUCH_UAS = [IOS_UA, ANDROID_UA]

    subheavy_ctx.set_native_location("MAN")

    backends_response = {"content": "i am {}".format(path), "status": 200}
    subheavy_ctx.start_service_backends(response=backends_response, backends=_WEB_REPORT_BACKENDS + _WEB_HTTP_ADAPTER_BACKENDS)
    subheavy_ctx.start_uaas_backends()
    subheavy_ctx.start_laas_backends()
    subheavy_ctx.start_rps_limiter_backends()

    def check_query(code, ua, path=path):
        resp = subheavy_ctx.perform_unprepared_request({"path": path, "headers": {"Host": "yandex.ru", "Yandex-Preload": "prerender", "User-Agent": ua}, "method": "get"})
        assert resp.status_code == code, "failed for UA: '" + ua + "'"

    touch_file = subheavy_ctx.weights_manager.get_file("prefetch_switch_touch_yabrowser")
    desktop_file = subheavy_ctx.weights_manager.get_file("prefetch_switch_desktop_yabrowser")
    pp_file = subheavy_ctx.weights_manager.get_file("prefetch_switch_pp")

    for touch_enabled in [True, False]:
        for desktop_enabled in [True, False]:
            for pp_enabled in [True, False]:
                touch_file.set({"prefetch_disabled": int(not touch_enabled), "prefetch_enabled": int(touch_enabled)})
                desktop_file.set({"prefetch_disabled": int(not desktop_enabled), "prefetch_enabled": int(desktop_enabled)})
                pp_file.set({"prefetch_disabled": int(not pp_enabled), "prefetch_enabled": int(pp_enabled)})

                for ua in ALL_UAS:
                    check_query(200 if pp_enabled else 429, ua, path=path + "?ui=webmobileapp.yandex&test=1")
                    check_query(200 if pp_enabled else 429, ua, path=path + "?test=1&ui=webmobileapp.yandex")

                for ua in TOUCH_UAS:
                    check_query(200 if touch_enabled else 429, ua)

                for ua in DESKTOP_UAS:
                    check_query(200 if desktop_enabled else 429, ua)


@pytest.mark.parametrize("path", ["/touchsearch", "/search/touch", "/searchapp"])
def test_prefetch_disable_all(subheavy_ctx, path):
    disable_all_file = subheavy_ctx.weights_manager.get_file("prefetch_switch_disable_all")
    disable_all_file.set({"prefetch_disabled": 1, "prefetch_enabled": 0})

    resp = subheavy_ctx.perform_unprepared_request({"path": path, "headers": {"Host": "yandex.ru", "Yandex-Preload": "prerender"}, "method": "get"})
    assert resp.status_code == 429


PUMPKIN_PREFETCH_PATHS = [
    "/padsearch",
    "/search/pad",
    "/touchsearch",
    "/search/touch",
    "/jsonproxy",
    "/search/direct-preview",
    "/search",
    "/familysearch",
    "/msearch",
    "/schoolsearch",
    "/search/smart",
    "/search/touch",
    "/telsearch",
    "/touchsearch",
    "/yandsearch",
]


@pytest.mark.parametrize("is_geo_only", [True, False])
@pytest.mark.parametrize("response", [
    (200, "0", "search_backend"),
    (200, "1", "pumpkin"),
    (200, None, "search_backend"),
    (404, "0", "search_backend"),
    (404, "1", "search_backend"),
    (404, None, "search_backend"),
], ids=[
    "200-0-search_backend",
    "200-1-pumpkin",
    "200-None-search_backend",
    "404-0-search_backend",
    "404-1-search_backend",
    "404-None-search_backend",
])
def test_pumpkin_prefetch(subheavy_ctx, is_geo_only, response):
    status, header_value, answer = response

    headers = []
    if header_value is not None:
        headers.append(("X-Yandex-Pumpkin-Good-Query", header_value))

    subheavy_ctx.start_service_backends(
        response={"status": status, "content": "pumpkin", "headers": headers},
        backends=[
            "backends_VLA_WEB_RUS_YALITE_SAS_WEB_RUS_YALITE",
        ]
    )

    subheavy_ctx.start_service_backends(
        response={"content": "search_backend", "status": 200},
        backends=_WEB_REPORT_BACKENDS + _WEB_HTTP_ADAPTER_BACKENDS
    )

    for path in PUMPKIN_PREFETCH_PATHS:
        req = {"path": path, "headers": {"Host": "yandex.ru"}, "method": "get"}
        if is_geo_only:
            req["headers"]["X-Yandex-Balancing-Hint"] = "sas"
            req["headers"]["Host"] = "sas.yandex.ru"

        resp = subheavy_ctx.perform_unprepared_request(req)
        assert resp.status_code == 200
        assert resp.text == "search_backend"

    pumpkin_prefetch_switch = subheavy_ctx.weights_manager.get_file("pumpkin_prefetch_switch")
    pumpkin_prefetch_switch.set({"disabled": -1, "enabled": 1})

    for path in PUMPKIN_PREFETCH_PATHS:
        req = {"path": path, "headers": {"Host": "yandex.ru"}, "method": "get"}
        if is_geo_only:
            req["headers"]["X-Yandex-Balancing-Hint"] = "sas"
            req["headers"]["Host"] = "sas.yandex.ru"

        resp = subheavy_ctx.perform_unprepared_request(req)
        assert resp.status_code == 200
        assert resp.text == answer
