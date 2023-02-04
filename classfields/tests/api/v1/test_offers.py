import binascii
import json
from typing import Any, Dict, Tuple
from unittest.mock import AsyncMock

import pytest
from aioresponses import aioresponses
from async_asgi_testclient import TestClient
from mock import AsyncMock
from pytest_mock import MockerFixture
from starlette import status
from tortoise.contrib.test import TestCase

from app.clients.common import BadResponseError
from app.clients.parsing_api import client as parsing_api_client
from app.constants import errors
from app.core.config import settings
from app.db.models import CallCenter, Offer, User
from app.helpers.serializers import to_md5
from tests.helpers.random_data import random_integer, random_lower_string

USER_ID = random_integer()
LICENSE_PLATE = "К486НХ67"


@pytest.mark.usefixtures("create_superuser", "async_client_class", "superuser_token_headers_class", "http_mock_class")
class TestOffers(TestCase):
    m: MockerFixture
    async_client: TestClient

    DRAFTS_JSON = json.load(open("tests/resources/draft_offers.json"))
    OFFERS_JSON = json.load(open("tests/resources/parsing_offers.json"))
    OFFERS_PROTO = {
        key: binascii.unhexlify(val)
        for key, val in json.load(open("tests/resources/parsing_offers_proto.json")).items()
    }
    http_mock: aioresponses

    @pytest.fixture(autouse=True)
    def _set_mocker(self, mocker: MockerFixture):
        self.m = mocker

    async def asyncSetUp(self):
        await super().asyncSetUp()
        call_center = await CallCenter.create(
            name=random_lower_string(), bunker_name=random_lower_string(), region_id=225
        )
        await User.filter(id=1).update(call_center_id=call_center.id)
        offer_send_url = (parsing_api_client.url / "offer/send").url
        self.http_mock.clear()
        for offer in self.OFFERS_JSON:
            self.http_mock.get(
                f"{offer_send_url}?call-center={call_center.bunker_name}&limit=1", payload={"offers": [offer]}
            )
        for offer_hash, content in self.OFFERS_PROTO.items():
            offer_url = (parsing_api_client.url / "offer").url
            self.http_mock.get(f"{offer_url}?hash={offer_hash}", body=content)

    async def next_offer(self, limit: int = 1):
        resp = await self.async_client.get(
            f"{settings.API_V1_STR}/offers/next", query_string={"limit": limit}, headers=self.superuser_token_headers
        )
        # del self.OFFERS_JSON[0]
        return resp

    async def read_offers(self):
        response = await self.async_client.get(f"{settings.API_V1_STR}/offers/", headers=self.superuser_token_headers)
        return response

    async def test_read_offers(self):
        code = (await self.next_offer()).json()[0]["code"]
        r = await self.read_offers()
        self.assertEqual(r.status_code, status.HTTP_200_OK)
        self.assertEqual(len(r.json()["items"]), 1)
        self.assertEqual(r.json()["items"][0]["code"], code)

    async def test_read_offer_by_code(self):
        code = (await self.next_offer()).json()[0]["code"]
        response = await self.async_client.get(f"{settings.API_V1_STR}/offers/form/{code}")
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    async def test_read_offer(self):
        await self.next_offer()
        offer = (await self.read_offers()).json()["items"][0]
        response = await self.async_client.get(
            f"{settings.API_V1_STR}/offers/{offer['hash']}", headers=self.superuser_token_headers
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(offer["price"], response.json()["priceInfo"]["price"])
        response = await self.async_client.get(f"{settings.API_V1_STR}/offers/{offer['hash']}")
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    async def test_from_parsing_good(self):
        offer_json = self.OFFERS_JSON[0]
        self.m.patch("app.clients.parsing_api.client.offer_full", AsyncMock(return_value=offer_json))
        response = await self.async_client.get(f"{settings.API_V1_STR}/offers/form/from-parsing/{offer_json['hash']}")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        j = response.json()
        self.assertEqual(j["hash"], offer_json["hash"])
        self.assertIsNotNone(j["code"])
        offer = await Offer.filter(hash=offer_json["hash"]).first()
        self.assertIsNotNone(offer)
        self.assertIsNone(offer.call_center_id)

    async def test_from_parsing_bad_hash(self):
        response = await self.async_client.get(f"{settings.API_V1_STR}/offers/form/from-parsing/123")
        self.assertEqual(response.status_code, status.HTTP_422_UNPROCESSABLE_ENTITY)

    async def test_from_parsing_bad_response(self):
        self.m.patch(
            "app.clients.parsing_api.client.offer_full",
            AsyncMock(side_effect=BadResponseError(status=status.HTTP_403_FORBIDDEN, content=b"")),
        )
        response = await self.async_client.get(f"{settings.API_V1_STR}/offers/form/from-parsing/{'1' * 32}")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        j = response.json()
        self.assertEqual(j["detail"], errors.FORM_BY_HASH_ERROR)

    async def test_from_draft_good(self):
        draft_json = self.DRAFTS_JSON[0]
        draft_id = random_lower_string(32)
        self.m.patch("app.clients.vos_auto.client.get_draft_without_user", AsyncMock(return_value=draft_json))
        response = await self.async_client.get(f"{settings.API_V1_STR}/offers/form/from-draft/{draft_id}")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        j = response.json()
        offer_hash = to_md5(draft_id)
        self.assertIsNotNone(j["code"])
        offer = await Offer.filter(hash=offer_hash).first()
        self.assertIsNotNone(offer)
        self.assertIsNone(offer.call_center_id)

    async def test_from_draft_bad_response(self):
        draft_id = random_lower_string(32)
        self.m.patch(
            "app.clients.vos_auto.client.get_draft_without_user",
            AsyncMock(side_effect=BadResponseError(status=status.HTTP_403_FORBIDDEN, content=b"")),
        )
        response = await self.async_client.get(f"{settings.API_V1_STR}/offers/form/from-draft/{draft_id}")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        j = response.json()
        self.assertEqual(j["detail"], errors.FORM_BY_DRAFT_ERROR)

    async def mock_create_offer(self):
        get_user_id_mock = self.m.patch("app.api.helpers.offers.get_user_id")
        get_user_id_mock.return_value = USER_ID
        vos_auto_client_mock = self.m.patch("app.api.helpers.offers.vos_auto_client")
        vos_auto_client_mock.update_draft = AsyncMock()
        vos_auto_client_mock.update_draft.return_value = {"status": "OK", "offerId": random_lower_string()}
        vos_auto_client_mock.create_draft = AsyncMock()
        vos_auto_client_mock.create_draft.return_value = {"status": "OK", "offerId": random_lower_string()}
        personal_api_client_mock = self.m.patch("app.api.helpers.offers.personal_api_client")
        personal_api_client_mock.update_settings = AsyncMock()
        is_free_mock = self.m.patch("app.api.v1.routes.offers.is_free_placement_available")
        is_free_mock.return_value = True
        publish_draft_mock = self.m.patch("app.api.v1.routes.offers.publish_draft")
        publish_draft_mock.return_value = {"status": "OK", "offer": {"id": random_lower_string()}}

    async def get_offer_for_creation(self) -> Tuple[str, Dict[str, Any]]:
        code = (await self.next_offer()).json()[0]["code"]
        offer = (await self.async_client.get(f"{settings.API_V1_STR}/offers/form/{code}")).json()["data"]
        offer["documents"]["vin"] = None
        offer["documents"]["_vin"] = None
        offer["documents"]["_frame"] = None
        offer["documents"]["licensePlate"] = LICENSE_PLATE
        return code, offer

    async def create_offer_get(self, code: str, offer: Dict[str, Any]) -> Dict[str, Any]:
        response = await self.async_client.post(f"{settings.API_V1_STR}/offers/form/{code}", json=offer)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        response = await self.async_client.get(f"{settings.API_V1_STR}/offers/form/{code}")
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        response = await self.async_client.get(
            f"{settings.API_V1_STR}/offers/form/{code}", headers=self.superuser_token_headers
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        return response.json()

    async def test_create_offer(self):
        await self.mock_create_offer()
        code, offer = await self.get_offer_for_creation()
        created_offer = await self.create_offer_get(code, offer)
        self.assertEqual(created_offer["data"]["documents"]["licensePlate"], LICENSE_PLATE)

    async def test_create_offer_with_seller_name(self):
        await self.mock_create_offer()
        code, offer = await self.get_offer_for_creation()
        offer["seller"]["name"] = random_lower_string()
        created_offer = await self.create_offer_get(code, offer)
        self.assertEqual(created_offer["data"]["seller"]["name"], offer["seller"]["name"])

    async def test_create_offer_without_seller_name(self):
        await self.mock_create_offer()
        code, offer = await self.get_offer_for_creation()
        offer["seller"]["name"] = None
        created_offer = await self.create_offer_get(code, offer)
        self.assertEqual(created_offer["data"]["seller"]["name"], str(USER_ID))

    async def test_create_offer_with_invalid_seller_name(self):
        await self.mock_create_offer()
        code, offer = await self.get_offer_for_creation()
        offer["seller"]["name"] = "Пользователь"
        created_offer = await self.create_offer_get(code, offer)
        self.assertEqual(created_offer["data"]["seller"]["name"], str(USER_ID))
