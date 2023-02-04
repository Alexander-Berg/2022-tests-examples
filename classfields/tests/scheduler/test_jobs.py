import random
import re
from typing import List, Tuple

import pytest
from aioresponses import aioresponses
from mock import AsyncMock
from pytest_mock import MockerFixture
from tortoise.contrib.test import TestCase

from app.core import redis
from app.db.models import ImageRequestQueue
from app.scheduler.jobs.images import fill_request_queue, load_images
from app.scheduler.jobs.offers import fill_parse_queue, parse_offers
from app.wrappers.redis import RedisWrapper


class TestSchedulerJobs(TestCase):
    mocker: MockerFixture
    redis_wrapper: RedisWrapper

    @pytest.fixture(autouse=True)
    def __inject_fixtures(self, mocker: MockerFixture):
        self.mocker = mocker
        self.redis_wrapper = RedisWrapper(redis)

    def generate_ids(self, length: int = 100) -> List[Tuple[int]]:
        return [(i,) for i in range(length)]

    def generate_urls(self, length: int = 100) -> List[Tuple[str]]:
        return [(f"https://www.avito.ru/{i}",) for i in range(length)]

    async def fill_request_queue(self, img_ids: List[Tuple[int]]):
        yql_mock = self.mocker.patch("app.helpers.yql.YqlClient")
        yql_mock().query().run().get_results().table.rows = img_ids
        await fill_request_queue.run()

    async def fill_parse_queue(self, img_ids: List[Tuple[str]]):
        yql_mock = self.mocker.patch("app.helpers.yql.YqlClient")
        yql_mock().query().run().get_results().table.rows = img_ids
        await fill_parse_queue.run()

    async def test_fill_request_queue(self):
        await redis.flushdb()
        img_ids = self.generate_ids()
        await self.fill_request_queue(img_ids)
        self.assertEqual(await ImageRequestQueue.all().count(), len(img_ids))

    async def test_fill_parse_queue(self):
        await redis.flushdb()
        offer_urls = self.generate_urls()
        await self.fill_parse_queue(offer_urls)
        self.assertEqual(await self.redis_wrapper.count_parse_queue(), len(offer_urls))

    async def test_parse_offers(self):
        await redis.flushdb()
        with open("tests/resources/offer_1.html", "rb") as r:
            content = r.read()
        write_mock = self.mocker.patch("app.scheduler.jobs.offers.parse_offers.write_rows")
        tvm_mock = self.mocker.patch("app.clients.zora._tvm")
        tvm_mock.get_service_ticket = AsyncMock()
        tvm_mock.get_service_ticket.return_value = "ticket"
        bad_responses = 0
        good_responses = 0
        error_responses = 0
        offer_urls = self.generate_urls()
        offer_urls_unpacked = [row[0] for row in offer_urls]
        await self.fill_parse_queue(offer_urls)
        with aioresponses() as m:
            for offer_url in offer_urls_unpacked:
                exception = None
                if bool(random.getrandbits(1)):
                    body = content
                    status = 200
                    good_responses += 1
                else:
                    if bool(random.getrandbits(1)):
                        status = 500
                        exception = Exception("Something nasty happened here!")
                        error_responses += 1
                    else:
                        status = 404
                        bad_responses += 1
                    body = b""
                m.get(
                    offer_url.replace("https", "http"),
                    body=body,
                    status=status,
                    exception=exception,
                    repeat=True,
                )
            await parse_offers.run(False)
        self.assertEqual(await self.redis_wrapper.count_parse_queue(), 0)
        img_rows = write_mock.mock_calls[0].args[0]
        image_urls = [
            "https://www.avito.ru/img/share/auto/13710229894",
            "https://www.avito.ru/img/share/auto/13710230026",
            "https://www.avito.ru/img/share/auto/13710230146",
            "https://www.avito.ru/img/share/auto/13710230288",
            "https://www.avito.ru/img/share/auto/13710230398",
            "https://www.avito.ru/img/share/auto/13710223826",
            "https://www.avito.ru/img/share/auto/13710223976",
            "https://www.avito.ru/img/share/auto/13710224071",
            "https://www.avito.ru/img/share/auto/13710224152",
            "https://www.avito.ru/img/share/auto/13710224268",
            "https://www.avito.ru/img/share/auto/13710224410",
            "https://www.avito.ru/img/share/auto/13710224557",
            "https://www.avito.ru/img/share/auto/13710224658",
            "https://www.avito.ru/img/share/auto/13710229894",
        ]
        self.assertEqual(good_responses * len(image_urls), len(img_rows))

        for img_row in img_rows:
            self.assertIn(img_row["url"], offer_urls_unpacked)
            self.assertIn(img_row["image_url"], image_urls)

    async def test_load_images(self):
        await redis.flushdb()
        write_mock = self.mocker.patch("app.scheduler.jobs.images.load_images.write_rows")
        tvm_mock = self.mocker.patch("app.clients.zora._tvm")
        tvm_mock.get_service_ticket = AsyncMock()
        tvm_mock.get_service_ticket.return_value = "ticket"
        bad_responses = 0
        good_responses = 0
        error_responses = 0
        img_ids = self.generate_ids()
        await self.fill_request_queue(img_ids)
        with aioresponses() as m:
            for img_id in img_ids:
                exception = None
                if bool(random.getrandbits(1)):
                    body = b"image"
                    status = 200
                    good_responses += 1
                else:
                    if bool(random.getrandbits(1)):
                        status = 500
                        exception = Exception("Something nasty happened here!")
                        error_responses += 1
                    else:
                        status = 404
                        bad_responses += 1
                    body = b""
                m.get(
                    re.compile(rf"http://\d{{2}}\.img\.avito\.st/1280x960/{img_id[0]}\.jpg"),
                    body=body,
                    status=status,
                    exception=exception,
                    repeat=True,
                )
            await load_images.run_images_push()
            await load_images.run_images_load(False)
        self.assertEqual(await ImageRequestQueue.all().count(), error_responses)
        self.assertEqual(await ImageRequestQueue.filter(task_try__gt=0).count(), error_responses)
        log_rows = write_mock.mock_calls[0].args[0]
        content_rows = write_mock.mock_calls[1].args[0]
        self.assertEqual(len(img_ids) - error_responses, len(log_rows))
        self.assertEqual(bad_responses, len(list(filter(lambda x: x["status"] == 404, log_rows))))
        self.assertEqual(good_responses, len(list(filter(lambda x: x["status"] == 200, log_rows))))
        self.assertEqual(good_responses, len(content_rows))
