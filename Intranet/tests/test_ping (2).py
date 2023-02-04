import asyncio
import json
import uuid
from unittest import mock

import pytest
import pytest_asyncio

from intranet.ims.staff_connector.staff_connector import sqs, settings
from intranet.ims.staff_connector.staff_connector.handlers import ping


@pytest_asyncio.fixture
async def sqs_client():
    async with sqs.get_sqs_context() as _client:
        yield _client


@mock.patch(
    'intranet.ims.staff_connector.staff_connector.handlers.ping.process_message',
    side_effect=asyncio.CancelledError,
)
@pytest.mark.asyncio
async def test_push_pull(process_message, sqs_client):
    queue = await sqs.get_or_create_queue(
        ping.QUEUE,
        sqs_client=sqs_client,
        **settings.QUEUES_ATTRIBUTES[ping.QUEUE].dict(),
    )
    await queue.purge()

    ping_id = uuid.uuid4()
    with mock.patch('uuid.uuid4', return_value=ping_id):
        await ping.produce_messages(queue)

    with pytest.raises(asyncio.CancelledError):
        await sqs.consume_messages(queue, ping.process_message)

    process_message.assert_awaited_once()
    body = json.loads(await process_message.await_args.args[0].body)
    assert body == {'message': 'Hello, world!', 'id': str(ping_id)}
