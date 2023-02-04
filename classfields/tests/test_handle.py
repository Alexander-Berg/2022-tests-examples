import csv
import json
from typing import Any, Dict, List
from unittest import IsolatedAsyncioTestCase
from unittest.mock import AsyncMock

import pytest
from pytest_mock import MockerFixture

from cm_avito_converter.config import settings
from cm_avito_converter.main import handle
from cm_avito_converter.models.sh import SHAvitoOffer
from tests.helpers.events import (
    build_http_event,
    build_ymq_event,
    build_ymq_event_message,
)


class TestHandle(IsolatedAsyncioTestCase):
    boto_client: AsyncMock
    producer: AsyncMock
    m: MockerFixture
    correct_phones: List[List[Any]]
    active_dealer_offer: Dict[str, Any]
    active_private_offer: Dict[str, Any]
    archive_offer: Dict[str, Any]
    invalid_source_offer: Dict[str, Any]

    def setUp(self) -> None:
        with open("tests/resources/phones.csv") as r:
            reader = csv.reader(r)
            self.correct_phones = list(reader)
        for attr in ("active_dealer_offer", "active_private_offer", "archive_offer", "invalid_source_offer"):
            with open(f"tests/resources/{attr}.json") as r:
                setattr(self, attr, json.load(r))

    @pytest.fixture(autouse=True)
    def _set_boto_client(self, boto_client_mock: AsyncMock):
        self.boto_client = boto_client_mock

    @pytest.fixture(autouse=True)
    def _set_producer(self, producer_mock: AsyncMock):
        self.producer = producer_mock

    @pytest.fixture(autouse=True)
    def _set_mocker(self, mocker: MockerFixture):
        self.m = mocker

    def validate_active_private_offer(self, offer: SHAvitoOffer):
        self.assertEqual("Volkswagen Multivan", offer.car_name)
        self.assertEqual(len(self.active_private_offer["raw"]["PROP_ADVERT_EXTERNAL_IMAGE_URL"]), len(offer.images))
        self.assertFalse(False, offer.seller_is_dealer)
        self.assertEqual("Частное лицо", offer.seller_type)
        self.assertIsNotNone(offer.seller_name)
        self.assertIsNotNone(offer.seller_url)
        self.assertIsNone(offer.dealership_name)
        self.assertIsNone(offer.dealership_url)

    def validate_active_dealer_offer(self, offer: SHAvitoOffer):
        self.assertEqual("Porsche Cayenne", offer.car_name)
        self.assertEqual(len(self.active_dealer_offer["raw"]["PROP_ADVERT_EXTERNAL_IMAGE_URL"]), len(offer.images))
        self.assertTrue(offer.seller_is_dealer)
        self.assertEqual("Автодилер", offer.seller_type)
        self.assertIsNone(offer.seller_name)
        self.assertIsNone(offer.seller_url)
        self.assertIsNotNone(offer.dealership_name)
        self.assertIsNotNone(offer.dealership_url)

    async def test_handle_http_event_active_offer(self):
        for i, attr in enumerate(("active_private_offer", "active_dealer_offer")):
            active_offer = getattr(self, attr)
            await handle(build_http_event({}, json.dumps(active_offer)).dict(), None)
            send_call_args = self.producer.method_calls[i * 3].args
            self.assertEqual(settings.KAFKA_AVITO_TOPIC, send_call_args[0])
            getattr(self, f"validate_{attr}")(SHAvitoOffer.parse_raw(send_call_args[1]))

    async def test_handle_http_event_archive_offer(self):
        await handle(build_http_event({}, json.dumps(self.archive_offer)).dict(), None)
        send_call_args = self.producer.method_calls[0].args
        self.assertEqual(settings.KAFKA_AVITO_DEACTIVATIONS_TOPIC, send_call_args[0])
        self.assertEqual(self.archive_offer["url"], send_call_args[1].decode())

    async def test_handle_http_event_invalid_source_offer(self):
        await handle(build_http_event({}, json.dumps(self.invalid_source_offer)).dict(), None)
        self.assertEqual(0, len(self.producer.method_calls))

    async def test_handle_ymq_event_offer(self):
        evt = build_ymq_event(
            [
                build_ymq_event_message(json.dumps(self.active_private_offer)),
                build_ymq_event_message(json.dumps(self.active_dealer_offer)),
                build_ymq_event_message(json.dumps(self.archive_offer)),
                build_ymq_event_message(json.dumps(self.invalid_source_offer)),
            ]
        )
        await handle(evt.dict(), None)
        send_call_args = self.producer.method_calls[0].args
        self.assertEqual(settings.KAFKA_AVITO_TOPIC, send_call_args[0])
        self.validate_active_private_offer(SHAvitoOffer.parse_raw(send_call_args[1]))
        send_call_args = self.producer.method_calls[3].args
        self.assertEqual(settings.KAFKA_AVITO_TOPIC, send_call_args[0])
        self.validate_active_dealer_offer(SHAvitoOffer.parse_raw(send_call_args[1]))
        self.assertEqual(8, len(self.producer.method_calls))
