import logging

import pytest
from aioresponses import CallbackResult

from hamcrest import assert_that, has_item, has_properties

from billing.yandex_pay.yandex_pay.interactions import UnifiedAgentMetricPushClient


@pytest.fixture
async def unified_agent_metric_client(create_client):
    client: UnifiedAgentMetricPushClient = create_client(UnifiedAgentMetricPushClient)
    client.REQUEST_RETRY_TIMEOUTS = ()

    yield client
    await client.close()


@pytest.mark.asyncio
async def test_send_success(
    unified_agent_metric_client: UnifiedAgentMetricPushClient,
    aioresponses_mocker,
    caplog,
):
    caplog.set_level(logging.ERROR, logger=unified_agent_metric_client.logger.name)
    caplog.clear()  # Аркадийный pytest успевает записать INFO запись

    ts = 12345678

    def callback(url, **kwargs):
        request = kwargs['json']

        assert request['ts'] == ts
        assert request['metrics'][0]['value'] == 123
        assert request['metrics'][0]['labels'] == {'name': 'some-metric', 'label1': 'aaa', 'label2': 'bbb'}

        return CallbackResult(
            payload={},
            status=200
        )

    aioresponses_mocker.post(
        f'{unified_agent_metric_client.BASE_URL}',
        callback=callback,
    )
    await unified_agent_metric_client.send(
        123, 'some-metric', {'label1': 'aaa', 'label2': 'bbb'}, timestamp=ts,
    )

    assert len(caplog.records) == 0


@pytest.mark.asyncio
async def test_send_failed(
    unified_agent_metric_client: UnifiedAgentMetricPushClient,
    aioresponses_mocker,
    caplog,
    dummy_logger,
):
    caplog.set_level(logging.ERROR, logger=dummy_logger.logger.name)

    aioresponses_mocker.post(
        f'{unified_agent_metric_client.BASE_URL}',
        status=500,
    )
    await unified_agent_metric_client.send(
        123, 'some-metric'
    )

    logs = [r for r in caplog.records if r.name == dummy_logger.logger.name]
    assert_that(
        logs,
        has_item(has_properties(message='UnifiedAgent metric send error')),
    )
