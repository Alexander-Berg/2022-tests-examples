import re
from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to, has_entries, none

from billing.yandex_pay_plus.yandex_pay_plus.interactions import YtClient


@pytest.fixture
async def yt_client(create_client):
    client = create_client(YtClient)
    client.REQUEST_RETRY_TIMEOUTS = ()
    client.YT_OAUTH_TOKEN = 'fake_token'
    yield client
    await client.close()


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def table():
    return '//yt/table/path'


class TestLookupRows:
    @pytest.fixture
    def endpoint_url(self, yandex_pay_plus_settings):
        return re.compile(f'{yandex_pay_plus_settings.YT_API_URL}/lookup_rows.*')

    @pytest.fixture
    def payload(self, uid):
        return {
            'uid': uid,
            'trusted_devices': [str(uuid4())],
        }

    @pytest.mark.asyncio
    async def test_lookup_succeeded(
        self, yt_client, uid, table, payload, endpoint_url, aioresponses_mocker
    ):
        aioresponses_mocker.put(endpoint_url, payload=payload)

        response = await yt_client.lookup_rows(table, {'uid': uid})

        assert_that(response, equal_to(payload))

    @pytest.mark.asyncio
    async def test_validate_client_request(
        self, yt_client, uid, table, payload, endpoint_url, aioresponses_mocker, mocker
    ):
        mock = aioresponses_mocker.put(endpoint_url, payload=payload)

        await yt_client.lookup_rows(table, {'uid': uid})

        mock.assert_called_once()
        _, call_kwargs = mock.call_args
        assert_that(
            call_kwargs,
            has_entries(
                json={'uid': uid},
                params={'path': table},
                headers=has_entries({'Accept': 'application/json'}),
            ),
        )

    @pytest.mark.asyncio
    async def test_row_not_found(
        self, yt_client, uid, table, endpoint_url, aioresponses_mocker
    ):
        aioresponses_mocker.put(endpoint_url)

        response = await yt_client.lookup_rows(table, {'uid': uid})

        assert_that(response, none())
