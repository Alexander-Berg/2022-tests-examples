import binascii
import json

import pytest
from async_asgi_testclient import TestClient
from furl import furl
from tortoise.contrib.test import TestCase

from app.core.config import settings
from app.db.models import CallCenter, Offer
from app.schemas.offer import OfferUploadInSchema
from tests.helpers.random_data import random_lower_string

_prefix = furl(settings.API_INTERNAL_STR) / "offers/"


@pytest.mark.usefixtures("async_client_class")
class TestInternalOffers(TestCase):
    async_client: TestClient

    OFFERS_JSON = json.load(open("tests/resources/parsing_offers.json"))
    OFFERS_PROTO = {
        key: binascii.unhexlify(val)
        for key, val in json.load(open("tests/resources/parsing_offers_proto.json")).items()
    }

    async def test_upload_offers_all_good(self):
        cc = await CallCenter.create(name=random_lower_string(), bunker_name=random_lower_string(), region_id=1)
        offers_in = []
        for offer in self.OFFERS_JSON:
            offers_in.append(
                OfferUploadInSchema(
                    hash=offer["hash"], photo=offer["photo"], offer=offer["offer"], call_center=cc.bunker_name
                ).dict()
            )
        r = await self.async_client.post((_prefix / "upload").url, json=offers_in)
        r_json = r.json()
        self.assertEqual(r.status_code, 200)
        self.assertTrue(all((res["url"] is not None for res in r_json)))
        self.assertTrue(all((res["success"] for res in r_json)))
        self.assertTrue(all((res["error"] is None for res in r_json)))
        self.assertEqual(await Offer.all().count(), len(self.OFFERS_JSON))

    async def test_upload_offers_no_bunker(self):
        cc = await CallCenter.create(name=random_lower_string(), bunker_name=random_lower_string(), region_id=1)
        offers_in = []
        for i, offer in enumerate(self.OFFERS_JSON[:3]):
            if i == 1:
                call_center = random_lower_string()
            else:
                call_center = cc.bunker_name
            offers_in.append(
                OfferUploadInSchema(
                    hash=offer["hash"], photo=offer["photo"], offer=offer["offer"], call_center=call_center
                ).dict()
            )
        r = await self.async_client.post((_prefix / "upload").url, json=offers_in)
        r_json = r.json()
        self.assertEqual(r.status_code, 200)
        self.assertFalse(r_json[1]["success"])
        self.assertIsNotNone(r_json[1]["error"])
        self.assertTrue(r_json[1]["error"].startswith("No call call center with bunker name"))
        self.assertEqual(len(list(filter(lambda x: x["success"] is True, r_json))), 2)
        self.assertEqual(await Offer.all().count(), 2)

    async def test_upload_offers_big_chunk(self):
        cc = await CallCenter.create(name=random_lower_string(), bunker_name=random_lower_string(), region_id=1)
        offers_in = []
        c = 0
        for i in range(500):
            for offer in self.OFFERS_JSON:
                c += 1
                offers_in.append(
                    OfferUploadInSchema(
                        hash=str(c), photo=offer["photo"], offer=offer["offer"], call_center=cc.bunker_name
                    ).dict()
                )
        r = await self.async_client.post((_prefix / "upload").url, json=offers_in)
        self.assertEqual(r.status_code, 200)
        self.assertEqual(500 * len(self.OFFERS_JSON), await Offer.all().count())
