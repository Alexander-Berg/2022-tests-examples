import http.client

import pytest

from infra.walle.server.tests.lib import dns as dns_test_lib
from infra.walle.server.tests.lib.util import TestCase, monkeypatch_config, mock_box_config
from walle import boxes, yc

TEST_IPMIPROXY_BOX_URL = "special_ipmiproxy_box_host"
TEST_DEPLOY_BOX_URL = "special_deploy_box_host"
TEST_EINE_BOX_URL = "special_eine_box_host"
TEST_EINE_BOX_ID = "id"
TEST_BOX_PROJECT = "some_project"


@pytest.fixture
def test(request):
    return TestCase.create(request)


@pytest.fixture
def eine_box(mp):
    mock_box_config(
        mp, monkeypatch_config, boxes.IPMIPROXY_BOXES_SECTION, TEST_EINE_BOX_ID, {"host": TEST_IPMIPROXY_BOX_URL}
    )
    mock_box_config(mp, monkeypatch_config, boxes.EINE_BOXES_SECTION, TEST_EINE_BOX_ID, {"host": TEST_EINE_BOX_URL})
    mock_box_config(mp, monkeypatch_config, boxes.DEPLOY_BOXES_SECTION, TEST_EINE_BOX_ID, {"host": TEST_DEPLOY_BOX_URL})


def test_get_boxes(test, mp, dns_box, eine_box):
    result = test.api_client.get("/v1/boxes")
    assert result.status_code == http.client.OK
    dns_boxes_response = result.json[boxes.BoxType.dns]
    assert len(dns_boxes_response) == 1
    dns_box_response = dns_boxes_response[dns_test_lib.TEST_DNS_BOX_NAME]
    assert dns_box_response["type"] == yc.DnsApiType.rurikk_dns
    assert dns_box_response["console_url"] == dns_test_lib.TEST_CONSOLE_URL
    assert dns_box_response["dns_endpoint"] == dns_test_lib.TEST_DNS_API_ENDPOINT
    assert dns_box_response["iam_endpoint"] == dns_test_lib.TEST_IAM_API_ENDPOINT
    assert "private_key" not in dns_box_response
    eine_boxes_response = result.json[boxes.BoxType.eine]
    assert len(eine_boxes_response) == 1
    eine_box_response = eine_boxes_response[TEST_EINE_BOX_ID]
    assert eine_box_response["deploy_host"] == TEST_DEPLOY_BOX_URL
    assert eine_box_response["eine_host"] == TEST_EINE_BOX_URL
    assert eine_box_response["ipmiproxy_host"] == TEST_IPMIPROXY_BOX_URL
