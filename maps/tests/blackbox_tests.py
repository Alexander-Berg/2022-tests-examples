import asyncio
import pytest
from datetime import timedelta

from maps.infra.sedem.lib.mongoqueue import MessageNotFoundError, MongoQueue


@pytest.mark.asyncio
async def test_consume_and_ack(queue: MongoQueue, consume_single):
    await queue.publish(message='test text 1')
    message_id, message = await consume_single(queue)
    assert message == 'test text 1'
    await queue.ack(message_id=message_id)
    await asyncio.sleep(10)
    with pytest.raises(asyncio.exceptions.TimeoutError):
        await consume_single(queue)


@pytest.mark.asyncio
async def test_consume_and_reject(queue: MongoQueue, consume_single):
    await queue.publish(message='test text 1')
    message_id, message = await consume_single(queue)
    assert message == 'test text 1'
    await queue.reject(message_id=message_id, delay=timedelta(seconds=5))
    with pytest.raises(asyncio.exceptions.TimeoutError):
        await consume_single(queue)
    await asyncio.sleep(10)
    expected_id, message = await consume_single(queue)
    assert message == 'test text 1'
    await queue.ack(message_id=expected_id)
    assert expected_id == message_id


@pytest.mark.asyncio
async def test_publish_and_reject_error(queue: MongoQueue):
    await queue.publish(message='test text 1')
    with pytest.raises(MessageNotFoundError):
        await queue.reject(message_id='123')


@pytest.mark.asyncio
async def test_publish_and_ack_error(queue: MongoQueue):
    await queue.publish(message='test text 1')
    with pytest.raises(MessageNotFoundError):
        await queue.ack(message_id='123')


@pytest.mark.asyncio
async def test_consume_with_publish(queue: MongoQueue, consume_single):
    await queue.publish(message='test text 1')
    message_id, message = await consume_single(queue)
    assert message == 'test text 1'
    await queue.ack(message_id=message_id)
    with pytest.raises(asyncio.exceptions.TimeoutError):
        await consume_single(queue)
