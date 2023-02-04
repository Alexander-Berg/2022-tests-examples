from unittest import mock

import pytest

from infra.walle.server.contrib.yc_python_sdk.yandexcloud import _channels
from walle import yc, boxes
from walle.clients import dns as dns_clients

TEST_DNS_BOX_PROJECT = "dns-box-project"
TEST_DNS_BOX_NAME = "dns_box"
TEST_DNS_API_ENDPOINT = "dns.api.test.endpoint.com"
TEST_IAM_API_ENDPOINT = "ts.api.test.endpoint.com"
TEST_CONSOLE_URL = "https://console.test.com"
TEST_RURIKK_DNS_ZONE = "cloud.yandex.net"
TEST_FOLDER_ID = "folder_id_value"
TEST_DNS_ZONE_ID = "dns_zone_id_value"


def dns_box(mp):
    mp.config(boxes.CONFIG_BOX_MAPPING_SECTION, {TEST_DNS_BOX_PROJECT: {"dns": TEST_DNS_BOX_NAME}})
    mp.config(
        boxes.DNS_API_BOXES_SECTION,
        {
            TEST_DNS_BOX_NAME: {
                "type": yc.DnsApiType.rurikk_dns,
                "dns_endpoint": TEST_DNS_API_ENDPOINT,
                "iam_endpoint": TEST_IAM_API_ENDPOINT,
                "console_url": TEST_CONSOLE_URL,
                "service_account_id": "service_account_id_value",
                "key_id": "key_id_value",
                "private_key": "-----BEGIN PRIVATE KEY-----private_key_value",
            },
        },
    )
    mocked_dns_zone_info = yc.YcDnsZone(TEST_DNS_ZONE_ID, TEST_RURIKK_DNS_ZONE, TEST_FOLDER_ID)
    mp.method(
        dns_clients.RurikkDnsClient.get_dns_zone_info,
        dns_clients.RurikkDnsClient,
        return_value=mocked_dns_zone_info,
    )
    mp.method(_channels.Channels.channel, _channels.Channels, return_value=mock.Mock())


@pytest.fixture(name="dns_box")
def dns_box_fixture(mp):
    return dns_box(mp)
