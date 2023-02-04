import pytest
from datetime import datetime

import fakeredis.aioredis

from app.parsers.s3.auto.scrapinghub.cars.avito.parser import ScrapingHubAvitoCarsParser
from app.scheduler.jobs.deactivation_sh import get_objects_from_last
from app.schemas.enums import RedisLastObjectsS3, BucketNameS3
from app.db.models import AutoOfferDeactivatorQueue, AutoOffer
from tests.helpers import (
    AsyncIterator, AsyncIteratorObjs,
    BucketObj, get_test_auto_offer
)


class TestSHDeactivationAvito:

    @pytest.mark.asyncio
    async def test_redis_save_last_key_exists_full(self, mocker):
        """
        Если после сохранненого ключа бакета есть фуловые ключи,
        то делаем закрузку и обновляем название последнего
        ключа бакета в редисе
        """
        await AutoOfferDeactivatorQueue().all().delete()
        mocked_redis = fakeredis.aioredis.FakeRedis(decode_responses=True)
        await mocked_redis.set(RedisLastObjectsS3.SH_AVITO_DEACTIVATIONS_LAST_OBJ, "123-full.json")

        mocker.patch("app.scheduler.jobs.deactivation_sh.get_redis_cli", return_value=mocked_redis)
        mocker.patch("app.scheduler.jobs.deactivation_sh.S3Resource.get_all_objects",
                     return_value=AsyncIterator([BucketObj("123-full.json"),
                                                 BucketObj("124-full.json"),
                                                 BucketObj("125-fresh.json")]))

        mocker.patch("ijson.items", return_value=AsyncIterator([]))

        listing_id = "testId"
        listing_url = "https://www.avito.ru/vladivostok/avtomobili/isuzu_bighorn_1996_testId"
        offer = get_test_auto_offer(url=listing_url, listing_id=listing_id)
        await offer.save()

        await get_objects_from_last(BucketNameS3.AVITO_AUTO,
                                    RedisLastObjectsS3.SH_AVITO_DEACTIVATIONS_LAST_OBJ,
                                    ScrapingHubAvitoCarsParser)
        len_deactivate_queue = len(await AutoOfferDeactivatorQueue().all())
        assert len_deactivate_queue == 1
        current_key = await mocked_redis.get(RedisLastObjectsS3.SH_AVITO_DEACTIVATIONS_LAST_OBJ)
        assert current_key == "125-fresh.json"

    @pytest.mark.asyncio
    async def test_redis_save_last_key_not_exists_full(self, mocker):
        """
        Если после сохранненого ключа бакета нет фуловых ключей,
        то не делаем заuрузку и не обновляем название последнего
        ключа бакета в редисе
        """
        await AutoOfferDeactivatorQueue().all().delete()
        mocked_redis = fakeredis.aioredis.FakeRedis(decode_responses=True)
        await mocked_redis.set(RedisLastObjectsS3.SH_AVITO_DEACTIVATIONS_LAST_OBJ, "123-full.json")

        mocker.patch("app.scheduler.jobs.deactivation_sh.get_redis_cli", return_value=mocked_redis)
        mocker.patch("app.scheduler.jobs.deactivation_sh.S3Resource.get_all_objects",
                     return_value=AsyncIterator([BucketObj("123-full.json"),
                                                 BucketObj("124-fresh.json"),
                                                 BucketObj("125-fresh.json")]))

        mocker.patch("ijson.items", return_value=AsyncIterator([]))

        listing_id = "testId"
        listing_url = "https://www.avito.ru/vladivostok/avtomobili/isuzu_bighorn_1996_testId"
        offer = get_test_auto_offer(url=listing_url, listing_id=listing_id)
        await offer.save()

        await get_objects_from_last(BucketNameS3.AVITO_AUTO,
                                    RedisLastObjectsS3.SH_AVITO_DEACTIVATIONS_LAST_OBJ,
                                    ScrapingHubAvitoCarsParser)
        len_deactivate_queue = len(await AutoOfferDeactivatorQueue().all())
        assert len_deactivate_queue == 0
        current_key = await mocked_redis.get(RedisLastObjectsS3.SH_AVITO_DEACTIVATIONS_LAST_OBJ)
        assert current_key == "123-full.json"

    @pytest.mark.asyncio
    async def test_redis_without_key(self, mocker):
        """
        Если нет сохраненного ключа для бакета в редис,
        то после отработки скрипта сохраняется последний.
        При этом из s3 загружаются все данные после последнего full включительно
        """
        await AutoOfferDeactivatorQueue().all().delete()
        listing_id = "testId"
        listing_url = "https://www.avito.ru/vladivostok/avtomobili/isuzu_bighorn_1996_testId"
        offer = get_test_auto_offer(url=listing_url, listing_id=listing_id)
        await offer.save()

        s3_json = []
        for idx in range(3):
            s3_json.append({"listing_id": str(idx), "listing_url": "https://www.avito.ru/" + str(idx)})
            await get_test_auto_offer(url="https://www.avito.ru/" + str(idx), listing_id=str(idx)).save()

        mocked_redis = fakeredis.aioredis.FakeRedis(decode_responses=True)
        mocker.patch("app.scheduler.jobs.deactivation_sh.get_redis_cli", return_value=mocked_redis)
        mocker.patch("app.scheduler.jobs.deactivation_sh.S3Resource.get_all_objects",
                     return_value=AsyncIterator([BucketObj("2021-04-02-04-40-47-fresh.json"),
                                                 BucketObj("2021-04-03-04-40-47-full.json"),
                                                 BucketObj("2021-04-04-04-40-47-fresh.json")]))

        mocker.patch("ijson.items", return_value=AsyncIteratorObjs([
            [
                {"listing_id": "bla3", "listing_url": "https://www.avito.ru/bla4"},
            ],
            [
                {"listing_id": "bla2", "listing_url": "https://www.avito.ru/bla2"},
                {"listing_id": "bla3", "listing_url": "https://www.avito.ru/bla3"},
                {"listing_id": listing_id, "listing_url": listing_url},
            ],
            [
                {"listing_id": "bla3", "listing_url": "https://www.avito.ru/bla5"},
            ],
            s3_json,
        ]))

        await get_objects_from_last(BucketNameS3.AVITO_AUTO,
                                    RedisLastObjectsS3.SH_AVITO_DEACTIVATIONS_LAST_OBJ,
                                    ScrapingHubAvitoCarsParser)
        len_deactivate_queue = len(await AutoOfferDeactivatorQueue().all())
        assert len_deactivate_queue == 3
        current_key = await mocked_redis.get(RedisLastObjectsS3.SH_AVITO_DEACTIVATIONS_LAST_OBJ)
        assert current_key == "2021-04-04-04-40-47-fresh.json"

    @pytest.mark.asyncio
    async def test_save_duplicate(self, mocker):
        """
        Если из s3 придут дубликаты, то сохраняется только один хэш на деактивацию.
        """
        await AutoOfferDeactivatorQueue().all().delete()
        await AutoOffer().all().delete()

        for idx in range(3):
            offer = get_test_auto_offer(url="https://www.avito.ru/" + str(idx), listing_id=str(idx))
            await offer.save()
            await AutoOfferDeactivatorQueue(hash_id=offer.hash, added=datetime.now()).save()
        len_deactivate_queue = len(await AutoOfferDeactivatorQueue().all())

        mocked_redis = fakeredis.aioredis.FakeRedis(decode_responses=True)
        mocker.patch("app.scheduler.jobs.deactivation_sh.get_redis_cli", return_value=mocked_redis)
        mocker.patch("app.scheduler.jobs.deactivation_sh.S3Resource.get_all_objects",
                     return_value=AsyncIterator([BucketObj("2021-04-02-04-40-47-full.json")]))

        mocker.patch("ijson.items", return_value=AsyncIteratorObjs([[], ]))

        await get_objects_from_last(BucketNameS3.AVITO_AUTO,
                                    RedisLastObjectsS3.SH_AVITO_DEACTIVATIONS_LAST_OBJ,
                                    ScrapingHubAvitoCarsParser)
        len_deactivate_queue_new = len(await AutoOfferDeactivatorQueue().all())
        assert len_deactivate_queue_new == len_deactivate_queue
