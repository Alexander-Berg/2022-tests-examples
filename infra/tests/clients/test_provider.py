import pytest

import walle.clients.dns.local_dns_resolver
import walle.clients.dns.slayer_dns_api
from infra.walle.server.tests.lib.util import monkeypatch_config, mock_box_config
from infra.walle.server.tests.lib.util import monkeypatch_request, mock_response, patch_attr
from walle import boxes
from walle.clients import eine, deploy, ipmiproxy


@pytest.fixture
def mock_deploy_request(mp):
    mocked_empty_page = {
        "result": {
            "mac": ["72:9d:2b:7f:39:d3"],
            "config": "cfg",
            "action": "action",
            "status": "ok",
        },
    }
    return monkeypatch_request(mp, mock_response(mocked_empty_page))


@pytest.fixture
def mock_ipmiproxy_request(mp):
    monkeypatch_config(mp, "ipmiproxy.access_token", "mock-ipmiproxy-access-token")
    patch_attr(mp, walle.clients.dns.slayer_dns_api.DnsClient, "__init__", return_value=None)
    patch_attr(mp, walle.clients.dns.local_dns_resolver.LocalDNSResolver, "get_a", return_value=["ipmi-ip-mock"])
    patch_attr(mp, walle.clients.dns.local_dns_resolver.LocalDNSResolver, "get_aaaa", return_value=["ipmi-ip-mock"])
    mocked_empty_page = {
        "message": "foo bar",
        "host": "some-host",
        "success": True,
    }
    return monkeypatch_request(mp, mock_response(mocked_empty_page))


@pytest.fixture
def mock_eine_request(mp):
    mocked_empty_page = {
        "paginate": {
            "count": 0,
            "has_next": False,
            "has_previous": False,
            "per_page": 10,
            "pages": 0,
        },
        "result": [],
    }
    return monkeypatch_request(mp, mock_response(mocked_empty_page))


def test_default_internal_eine(mp, mock_eine_request):
    EINE_DEFAULT_URL = "default_eine_url"
    monkeypatch_config(mp, "eine.host", EINE_DEFAULT_URL)

    eine.get_client(eine.get_eine_provider(box_id=None)).get_profile_stat()

    assert EINE_DEFAULT_URL in mock_eine_request.call_args[0][1]


def test_custom_eine_client_endpoint_for_box(mp, mock_eine_request):
    EINE_BOX_URL = "special_eine_box_host"
    BOX_ID = "id"
    mock_box_config(mp, monkeypatch_config, boxes.EINE_BOXES_SECTION, BOX_ID, {"host": EINE_BOX_URL})

    eine.get_client(eine.get_eine_provider(BOX_ID)).get_profile_stat()

    assert EINE_BOX_URL in mock_eine_request.call_args[0][1]


def test_default_internal_deploy(mp, mock_deploy_request):
    LUI_DEFAULT_URL = "default_deploy_url"
    monkeypatch_config(mp, "lui.host", LUI_DEFAULT_URL)

    deploy.get_client(deploy.get_deploy_provider(box_id=None)).get_deploy_status("some-host")

    assert LUI_DEFAULT_URL in mock_deploy_request.call_args[0][1]


def test_custom_deploy_client_endpoint_for_box(mp, mock_deploy_request):
    LUI_BOX_URL = "special_lui_box_host"
    BOX_ID = "id"
    mock_box_config(mp, monkeypatch_config, boxes.DEPLOY_BOXES_SECTION, BOX_ID, {"host": LUI_BOX_URL})

    deploy.get_client(deploy.get_deploy_provider(BOX_ID)).get_deploy_status("some-host")

    assert LUI_BOX_URL in mock_deploy_request.call_args[0][1]


def test_default_internal_lui(mp, mock_deploy_request):
    LUI_DEFAULT_URL = "default_lui_url"
    monkeypatch_config(mp, "lui.host", LUI_DEFAULT_URL)

    deploy.get_lui_client(deploy.get_deploy_provider(box_id=None)).get_deploy_log("some-host")

    assert LUI_DEFAULT_URL in mock_deploy_request.call_args[0][1]


def test_custom_lui_client_endpoint_for_box(mp, mock_deploy_request):
    LUI_BOX_URL = "special_lui_box_host"
    BOX_ID = "id"
    mock_box_config(mp, monkeypatch_config, boxes.DEPLOY_BOXES_SECTION, BOX_ID, {"host": LUI_BOX_URL})

    deploy.get_lui_client(deploy.get_deploy_provider(BOX_ID)).get_deploy_log("some-host")

    assert LUI_BOX_URL in mock_deploy_request.call_args[0][1]


def test_default_internal_ipmiproxy(mp, mock_ipmiproxy_request):
    IPMIPROXY_DEFAULT_URL = "default_ipmiproxy_url"
    monkeypatch_config(mp, "ipmiproxy.host", IPMIPROXY_DEFAULT_URL)

    ipmiproxy.get_client(ipmiproxy.get_yandex_internal_provider("some-mac")).reset()

    assert IPMIPROXY_DEFAULT_URL in mock_ipmiproxy_request.call_args[0][1]


def test_custom_ipmiproxy_client_endpoint_for_box(mp, mock_ipmiproxy_request):
    IPMIPROXY_BOX_URL = "special_ipmiproxy_box_host"
    BOX_ID = "id"
    mock_box_config(mp, monkeypatch_config, boxes.IPMIPROXY_BOXES_SECTION, BOX_ID, {"host": IPMIPROXY_BOX_URL})

    ipmiproxy.get_client(ipmiproxy.get_yandex_box_provider(BOX_ID, inv=911)).reset()

    assert IPMIPROXY_BOX_URL in mock_ipmiproxy_request.call_args[0][1]
