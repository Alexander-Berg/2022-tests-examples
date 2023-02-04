from decimal import Decimal

import pytest

from hamcrest import assert_that, equal_to, has_entries, has_properties, instance_of

from billing.yandex_pay_plus.yandex_pay_plus.interactions.saturn import SaturnClient
from billing.yandex_pay_plus.yandex_pay_plus.interactions.saturn.entities import UserScore
from billing.yandex_pay_plus.yandex_pay_plus.interactions.saturn.exceptions import (
    SaturnNotFoundError,
    SaturnResponseError,
)


@pytest.fixture
async def saturn_client(create_client):
    client = create_client(SaturnClient)
    client.REQUEST_RETRY_TIMEOUTS = ()
    yield client
    await client.close()


@pytest.fixture
def uid(randn):
    return randn()


class TestGetUserScore:
    @pytest.fixture
    def endpoint_url(self, yandex_pay_plus_settings):
        base_url = yandex_pay_plus_settings.SATURN_API_URL
        service_name = yandex_pay_plus_settings.SATURN_SERVICE_NAME
        return f'{base_url}/api/v1/{service_name}/search'

    @pytest.mark.asyncio
    async def test_get_user_score(self, saturn_client, uid, endpoint_url, aioresponses_mocker):
        payload = {
            'request_id': 'fake_request_id',
            'uid': uid,
            'score': 0.3867742419,
            'formula_id': '2489197606',
        }
        aioresponses_mocker.post(endpoint_url, payload=payload)

        response = await saturn_client.get_user_score(uid=uid)

        assert_that(response, instance_of(UserScore))
        expected_response = UserScore(
            request_id='fake_request_id',
            uid=uid,
            score=Decimal('0.3867742419'),
            formula_id='2489197606',
            score_raw=None,
            formula_description=None,
            data_source=None,
            status=None,
        )
        assert_that(response, equal_to(expected_response))

    @pytest.mark.asyncio
    async def test_validate_client_request(self, saturn_client, uid, endpoint_url, aioresponses_mocker):
        payload = {
            'request_id': 'fake_request_id',
            'uid': uid,
            'score': 0.3867742419,
            'formula_id': '2489197606',
        }
        mock = aioresponses_mocker.post(endpoint_url, payload=payload)

        await saturn_client.get_user_score(uid=uid)

        expected_request = {
            'request_id': saturn_client.request_id,
            'puid': uid,
            'service': saturn_client.SATURN_SERVICE_NAME,
        }
        mock.assert_called_once()
        assert_that(mock.call_args[1], has_entries(json=expected_request))

    @pytest.mark.asyncio
    async def test_user_not_found(self, saturn_client, uid, endpoint_url, aioresponses_mocker):
        payload = {'reqid': 'fake_request_id', 'puid': uid}
        aioresponses_mocker.post(endpoint_url, status=404, payload=payload)

        with pytest.raises(SaturnNotFoundError) as exc_info:
            await saturn_client.get_user_score(uid=uid)

        assert_that(
            exc_info.value,
            has_properties(
                params={'response': payload},
                status_code=404,
                method='post',
                service=saturn_client.SERVICE,
            )
        )

    @pytest.mark.asyncio
    async def test_generic_error(self, saturn_client, uid, endpoint_url, aioresponses_mocker):
        body = 'Server Error'
        aioresponses_mocker.post(endpoint_url, status=500, body=body)

        with pytest.raises(SaturnResponseError) as exc_info:
            await saturn_client.get_user_score(uid=uid)

        assert_that(
            exc_info.value,
            has_properties(
                params={'response_text': body},
                status_code=500,
                method='post',
                service=saturn_client.SERVICE,
            )
        )
