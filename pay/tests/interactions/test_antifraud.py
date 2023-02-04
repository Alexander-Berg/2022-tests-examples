from unittest import mock

import pytest
from aiohttp import TCPConnector

from pay.lib.interactions.antifraud.client import AbstractAntifraudClient, CashbackAntifraudStatus, ChallengeStatus

ANTIFRAUD_URL = 'https://antifraud.yandex.net/antifraud'


class AntifraudTestClient(AbstractAntifraudClient):
    BASE_URL = ANTIFRAUD_URL
    DEBUG = False
    REQUEST_RETRY_TIMEOUTS = ()
    CONNECTOR = TCPConnector()


@pytest.fixture
def common_args() -> dict:
    return {
        'external_id': 'some_external_id',
        'amount': 1,
        'trust_card_id': 'card_id',
        'timestamp': 2323,
        'uid': 11,
        'user_agent': 'python',
        'user_ip': 'some_ip',
        'login_id': 'some_login_id',
        'currency_number': '643',
        'device_id': 'some_device_id',
    }


@pytest.fixture
def challenge_args(common_args) -> dict:
    common_args['return_path'] = 'test_path'
    return common_args


@pytest.fixture
async def antifraud_client(create_interaction_client) -> AntifraudTestClient:
    client = create_interaction_client(AntifraudTestClient)
    yield client
    await client.close()


@pytest.mark.asyncio
async def test_get_challenge_request_sent_with_expected_body(
    aioresponses_mocker,
    antifraud_client,
    challenge_args,
):
    def assert_body_callback(url, **kwargs):
        body = kwargs['json']
        assert body == {
            'external_id': challenge_args['external_id'],
            'channel': 'acquiring',
            'sub_channel': 'yandex_pay',
            'service_id': 1042,
            'amount': challenge_args['amount'],
            'card_id': challenge_args['trust_card_id'],
            't': challenge_args['timestamp'],
            'uid': challenge_args['uid'],
            'user_agent': challenge_args['user_agent'],
            'ip': challenge_args['user_ip'],
            'login_id': challenge_args['login_id'],
            'currency': challenge_args['currency_number'],
            'retpath': challenge_args['return_path'],
            'device_id': challenge_args['device_id'],
            'request': 'PAY',
        }

    aioresponses_mocker.post(
        f'{ANTIFRAUD_URL}/score',
        status=200,
        payload={
            'status': 'success',
            'action': 'ALLOW',
            'tags': [],
        },
        callback=assert_body_callback,
    )

    await antifraud_client.get_challenge(**challenge_args)


@pytest.mark.asyncio
async def test_get_cashback_status_request_sent_with_expected_body(
    aioresponses_mocker,
    antifraud_client,
    common_args,
):
    def assert_body_callback(url, **kwargs):
        body = kwargs['json']
        assert body == {
            'external_id': common_args['external_id'],
            'channel': 'acquiring',
            'sub_channel': 'yandex_pay',
            'service_id': 1042,
            'amount': common_args['amount'],
            'card_id': common_args['trust_card_id'],
            't': common_args['timestamp'],
            'uid': common_args['uid'],
            'user_agent': common_args['user_agent'],
            'ip': common_args['user_ip'],
            'login_id': common_args['login_id'],
            'currency': common_args['currency_number'],
            'device_id': common_args['device_id'],
            'request': 'cashback-plus',
        }

    aioresponses_mocker.post(
        f'{ANTIFRAUD_URL}/score',
        status=200,
        payload={
            'status': 'success',
            'action': 'ALLOW',
            'tags': [],
        },
        callback=assert_body_callback,
    )

    await antifraud_client.get_cashback_status(**common_args)


@pytest.mark.asyncio
async def test_should_not_pass_device_id_if_none_on_get_challenge(
    aioresponses_mocker,
    antifraud_client,
    challenge_args,
):
    challenge_args['device_id'] = None

    def assert_no_device_id(url, **kwargs):
        body = kwargs['json']
        assert 'device_id' not in body

    aioresponses_mocker.post(
        f'{ANTIFRAUD_URL}/score',
        status=200,
        payload={
            'status': 'success',
            'action': 'ALLOW',
            'tags': [],
        },
        callback=assert_no_device_id,
    )

    await antifraud_client.get_challenge(**challenge_args)


@pytest.mark.asyncio
async def test_should_not_pass_device_id_if_none_on_get_cashback_status(
    aioresponses_mocker,
    antifraud_client,
    common_args,
):
    common_args['device_id'] = None

    def assert_no_device_id(url, **kwargs):
        body = kwargs['json']
        assert 'device_id' not in body

    aioresponses_mocker.post(
        f'{ANTIFRAUD_URL}/score',
        status=200,
        payload={
            'status': 'success',
            'action': 'ALLOW',
            'tags': [],
        },
        callback=assert_no_device_id,
    )

    await antifraud_client.get_cashback_status(**common_args)


@pytest.mark.asyncio
async def test_should_support_cards_with_prefixes_on_get_challenge(
    aioresponses_mocker,
    antifraud_client,
    challenge_args,
):
    challenge_args['trust_card_id'] = 'card-x123'

    def assert_no_device_id(url, **kwargs):
        body = kwargs['json']
        assert body['card_id'] == '123'

    aioresponses_mocker.post(
        f'{ANTIFRAUD_URL}/score',
        status=200,
        payload={
            'status': 'success',
            'action': 'ALLOW',
            'tags': [],
        },
        callback=assert_no_device_id,
    )

    await antifraud_client.get_challenge(**challenge_args)


@pytest.mark.asyncio
async def test_should_support_cards_with_prefixes_on_get_cashback_status(
    aioresponses_mocker,
    antifraud_client,
    common_args,
):
    common_args['trust_card_id'] = 'card-x123'

    def assert_no_device_id(url, **kwargs):
        body = kwargs['json']
        assert body['card_id'] == '123'

    aioresponses_mocker.post(
        f'{ANTIFRAUD_URL}/score',
        status=200,
        payload={
            'status': 'success',
            'action': 'ALLOW',
            'tags': [],
        },
        callback=assert_no_device_id,
    )

    await antifraud_client.get_cashback_status(**common_args)


@pytest.mark.asyncio
async def test_challenge_is_required(
    aioresponses_mocker,
    antifraud_client,
    challenge_args,
):
    aioresponses_mocker.post(
        f'{ANTIFRAUD_URL}/score',
        status=200,
        payload={'status': 'success', 'action': 'ALLOW', 'tags': [{'url': 'some_test_url'}]},
    )

    challenge = await antifraud_client.get_challenge(**challenge_args)

    assert challenge.status == ChallengeStatus.REQUIRED
    assert challenge.url == 'some_test_url'


@pytest.mark.asyncio
async def test_challenge_is_not_required(
    aioresponses_mocker,
    antifraud_client,
    challenge_args,
):
    aioresponses_mocker.post(
        f'{ANTIFRAUD_URL}/score',
        status=200,
        payload={'status': 'success', 'action': 'ALLOW', 'tags': []},
    )

    challenge = await antifraud_client.get_challenge(**challenge_args)

    assert challenge.status == ChallengeStatus.NOT_REQUIRED
    assert challenge.url is None


@pytest.mark.asyncio
async def test_error_response_from_antifraud_on_get_challenge(
    aioresponses_mocker,
    antifraud_client,
    challenge_args,
):
    aioresponses_mocker.post(
        f'{ANTIFRAUD_URL}/score',
        status=200,
        payload={
            'status': 'error',
        },
    )

    challenge = await antifraud_client.get_challenge(**challenge_args)

    assert challenge.status == ChallengeStatus.ERROR
    assert challenge.url is None


@pytest.mark.asyncio
async def test_challenge_action_is_denied(
    aioresponses_mocker,
    antifraud_client,
    challenge_args,
):
    aioresponses_mocker.post(
        f'{ANTIFRAUD_URL}/score',
        status=200,
        payload={
            'status': 'success',
            'action': 'DENY',
        },
    )

    challenge = await antifraud_client.get_challenge(**challenge_args)

    assert challenge.status == ChallengeStatus.DENY
    assert challenge.url is None


@pytest.mark.asyncio
async def test_cashback_is_denied_by_tag(
    aioresponses_mocker,
    antifraud_client,
    common_args,
):
    aioresponses_mocker.post(
        f'{ANTIFRAUD_URL}/score',
        status=200,
        payload={'status': 'success', 'action': 'ALLOW', 'tags': ['no_plus']},
    )

    cashback_status = await antifraud_client.get_cashback_status(**common_args)

    assert cashback_status == CashbackAntifraudStatus.DENY


@pytest.mark.asyncio
async def test_cashback_ok(
    aioresponses_mocker,
    antifraud_client,
    common_args,
):
    aioresponses_mocker.post(
        f'{ANTIFRAUD_URL}/score',
        status=200,
        payload={'status': 'success', 'action': 'ALLOW', 'tags': []},
    )

    cashback_status = await antifraud_client.get_cashback_status(**common_args)

    assert cashback_status == CashbackAntifraudStatus.OK


@pytest.mark.asyncio
async def test_error_response_from_antifraud_on_get_cashback_status(
    aioresponses_mocker,
    antifraud_client,
    common_args,
):
    aioresponses_mocker.post(
        f'{ANTIFRAUD_URL}/score',
        status=200,
        payload={
            'status': 'error',
        },
    )

    cashback_status = await antifraud_client.get_cashback_status(**common_args)

    assert cashback_status == CashbackAntifraudStatus.ERROR


@pytest.mark.asyncio
async def test_cashback_is_denied_by_action(
    aioresponses_mocker,
    antifraud_client,
    common_args,
):
    aioresponses_mocker.post(
        f'{ANTIFRAUD_URL}/score',
        status=200,
        payload={
            'status': 'success',
            'action': 'DENY',
        },
    )

    cashback_status = await antifraud_client.get_cashback_status(**common_args)

    assert cashback_status == CashbackAntifraudStatus.DENY


@pytest.mark.asyncio
async def test_should_inc_metric(
    mocker,
    aioresponses_mocker,
    antifraud_client,
    common_args,
):
    metric_counter = mocker.Mock()
    antifraud_client.METRIC_COUNTER = metric_counter
    aioresponses_mocker.post(
        f'{ANTIFRAUD_URL}/score',
        status=200,
        payload={
            'status': 'success',
            'action': 'DENY',
        },
    )

    await antifraud_client.get_cashback_status(**common_args)

    assert metric_counter.mock_calls == [mock.call.labels('CASHBACK', 'DENY'), mock.call.labels().inc()]
