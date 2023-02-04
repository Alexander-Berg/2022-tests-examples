import asyncio
import pytest
from datetime import datetime, timedelta
from dateutil.tz import UTC

from motor.motor_asyncio import AsyncIOMotorCollection

from maps.infra.sedem.lib.mongoqueue import MessageNotFoundError, MongoQueue


@pytest.mark.asyncio
@pytest.mark.freeze_time(datetime(2016, 11, 23, 15, 25, 22))
async def test_publish(collection: AsyncIOMotorCollection, queue: MongoQueue):
    await queue.publish(message='test text 1')
    await queue.publish(message='test text 2', delay=timedelta(seconds=10))

    docs = collection.find()
    messages = []
    async for doc in docs:
        messages.append({'message': doc['message'], 'available_at': doc['available_at'].replace(tzinfo=None)})
    sorted_messages = sorted(messages, key=lambda x: x['available_at'])
    expected_messages = [
        {
            'message': 'test text 1',
            'available_at': datetime.utcnow(),
        },
        {
            'message': 'test text 2',
            'available_at': datetime.utcnow() + timedelta(seconds=10),
        }
    ]
    assert sorted_messages == expected_messages


@pytest.mark.asyncio
@pytest.mark.freeze_time(datetime(2016, 11, 23, 15, 25, 22, tzinfo=UTC))
async def test_publish_with_negative_time(queue: MongoQueue):
    with pytest.raises(AssertionError):
        await queue.publish(message='test error text', delay=timedelta(seconds=-10))


@pytest.mark.asyncio
@pytest.mark.freeze_time(datetime(2016, 11, 23, 15, 25, 22, tzinfo=UTC))
async def test_publish_with_duplicate_key(collection: AsyncIOMotorCollection, queue: MongoQueue):
    await queue.publish(message='test text 1', deduplication_id='123')
    await queue.publish(message='test text 2', deduplication_id='123')

    docs = collection.find()
    messages = []
    async for doc in docs:
        messages.append({'message': doc['message'], 'available_at': doc['available_at'].replace(tzinfo=None)})
    expected_message = {
        'message': 'test text 1',
        'available_at': datetime.utcnow(),
    }

    assert messages[0] == expected_message


@pytest.mark.asyncio
@pytest.mark.freeze_time(datetime(2016, 11, 23, 15, 25, 22, tzinfo=UTC))
async def test_reject(collection: AsyncIOMotorCollection, queue: MongoQueue):
    messages = [
        {
            'message': 'test text 1',
            'available_at': datetime.utcnow(),
        },
        {
            'message': 'test text 2',
            'available_at': datetime.utcnow(),
        }

    ]
    expected_ids = []
    for message in messages:
        insertion_result = await collection.insert_one(message)
        expected_ids.append(insertion_result.inserted_id)
        message['_id'] = insertion_result.inserted_id
    messages[1]["available_at"] += timedelta(10)

    await queue.reject(message_id=expected_ids[0])
    await queue.reject(message_id=expected_ids[1], delay=timedelta(10))

    docs = collection.find()
    expected_messages = []
    async for doc in docs:
        doc['available_at'] = doc['available_at'].replace(tzinfo=None)
        expected_messages.append(doc)
    sorted_messages = sorted(expected_messages, key=lambda x: x['_id'])
    assert sorted_messages == messages


@pytest.mark.asyncio
@pytest.mark.freeze_time(datetime(2016, 11, 23, 15, 25, 22, tzinfo=UTC))
async def test_reject_not_found(queue: MongoQueue):
    with pytest.raises(MessageNotFoundError):
        await queue.reject(message_id='123')


@pytest.mark.asyncio
@pytest.mark.freeze_time(datetime(2016, 11, 23, 15, 25, 22, tzinfo=UTC))
async def test_ack_with_error(queue: MongoQueue):
    with pytest.raises(MessageNotFoundError):
        await queue.ack(message_id='123')


@pytest.mark.asyncio
@pytest.mark.freeze_time(datetime(2016, 11, 23, 15, 25, 22, tzinfo=UTC))
async def test_try_acquire_message(collection: AsyncIOMotorCollection, queue: MongoQueue):
    message = {
        'message': 'test text 1',
        'available_at': datetime.utcnow() - timedelta(seconds=10),
    }
    insertion_result = await collection.insert_one(message)
    expected_id = insertion_result.inserted_id
    message['_id'] = expected_id
    await queue._try_acquire_message(message_id=expected_id)

    docs = collection.find()
    expected_messages = []
    async for doc in docs:
        doc['available_at'] = doc['available_at'].replace(tzinfo=None)
        expected_messages.append(doc)
    message["available_at"] = datetime.utcnow() + timedelta(seconds=30)
    assert expected_messages[0] == message


@pytest.mark.asyncio
async def test_consume(collection: AsyncIOMotorCollection, queue: MongoQueue, consume_single):
    message_1 = {
        'message': 'test text 1',
        'available_at': datetime.utcnow() - timedelta(10),
    }
    message_2 = {
        'message': 'test text 2',
        'available_at': datetime.utcnow() + timedelta(10),
    }
    message_3 = {
        'message': 'test text 1',
        'available_at': datetime.utcnow() - timedelta(10),
        'acknowledged_at': datetime.utcnow()
    }

    expected_ids = []

    insertion_result = await collection.insert_one(message_1)
    expected_ids.append(insertion_result.inserted_id)
    message_1['_id'] = insertion_result.inserted_id

    await collection.insert_one(message_2)
    await collection.insert_one(message_3)

    message_id, message = await consume_single(queue)
    assert message == 'test text 1'
    await queue.ack(message_id=message_id)
    with pytest.raises(asyncio.exceptions.TimeoutError):
        await consume_single(queue)
