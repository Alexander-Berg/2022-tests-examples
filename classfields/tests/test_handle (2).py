import json
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List
from unittest import IsolatedAsyncioTestCase
from unittest.mock import AsyncMock, Mock

import pytest
from parameterized import parameterized
from pytest_mock import MockerFixture

from smartagent_realty_converter.config import settings
from smartagent_realty_converter.main import handle, DATE_FMT, DT_FMT
from tests.helpers.events import (
    build_object_event,
    build_object_event_message,
)
from tests.helpers.random_data import random_small_integer


@dataclass
class XMLOffers:
    name: str
    content: bytes


class TestHandle(IsolatedAsyncioTestCase):
    boto_client: AsyncMock
    m: MockerFixture
    avito_offers: List[XMLOffers]
    cian_offers: List[XMLOffers]
    avito_deactivations: List[Dict[str, Any]]
    cian_deactivations: List[Dict[str, Any]]

    def setUp(self) -> None:
        self.avito_offers = []
        self.cian_offers = []
        for folder in ("tests/resources/avito_offers", "tests/resources/cian_offers"):
            for path in sorted(Path(folder).glob("*.xml")):
                with open(path, "rb") as r:
                    getattr(self, folder.split("/")[-1]).append(XMLOffers(name=path.name, content=r.read()))
        for folder in ("tests/resources/avito_deactivations", "tests/resources/cian_deactivations"):
            for path in sorted(Path(folder).glob("*.json")):
                with open(path) as r:
                    setattr(self, folder.split("/")[-1], json.load(r))

    @pytest.fixture(autouse=True)
    def _set_boto_client(self, boto_client_mock: AsyncMock):
        self.boto_client = boto_client_mock

    @pytest.fixture(autouse=True)
    def _set_mocker(self, mocker: MockerFixture):
        self.m = mocker

    def build_object_mock(self, content: bytes) -> Dict[str, Any]:
        obj = {"Body": Mock()}
        obj["Body"].read = AsyncMock(return_value=content)
        return obj

    async def test_handle_object_event_invalid_key(self):
        xml_offers = self.avito_offers[0]
        self.boto_client.get_object = AsyncMock(return_value=self.build_object_mock(xml_offers.content))
        res = await handle(build_object_event([build_object_event_message("test", "test")]).dict(), None)
        assert not res["output_keys"]

    async def test_handle_object_event_valid_key(self):
        object_event_messages = [
            build_object_event_message("test", xml_offers.name)
            for xml_offers in [*self.avito_offers, *self.cian_offers]
        ]
        self.boto_client.get_object = AsyncMock(return_value=self.build_object_mock(self.avito_offers[0].content))
        res = await handle(build_object_event(object_event_messages).dict(), None)
        assert res["output_keys"]
        self.assertEqual(len(res["output_keys"]), 4)

    async def validate_offers_parsing(
        self, xml_offers: XMLOffers, source_bucket: str, bucket: str
    ) -> List[Dict[str, Any]]:
        self.boto_client.get_object = AsyncMock(return_value=self.build_object_mock(xml_offers.content))
        now = datetime.now()
        key = f"{now.strftime(DT_FMT)}-sale-{random_small_integer()}.xml"
        res = await handle(build_object_event([build_object_event_message(source_bucket, key)]).dict(), None)
        self.assertIsNotNone(res["output_keys"])
        self.assertEqual(len(res["output_keys"]), 1)
        output_key = res["output_keys"][0]
        self.assertEqual(output_key[0], bucket)
        self.assertEqual(output_key[1][:19], now.strftime(DT_FMT))
        self.assertEqual(len(self.boto_client.method_calls), 3)
        self.assertEqual(self.boto_client.method_calls[1][0], "upload_fileobj")
        return json.loads(self.boto_client.method_calls[1].args[0].read())

    async def test_handle_object_event_avito_offers_rent(self):
        xml_offers = self.avito_offers[0]
        offers = await self.validate_offers_parsing(
            xml_offers, settings.S3_AVITO_SOURCE_BUCKET, settings.S3_AVITO_BUCKET
        )
        offer = offers[0]
        self.assertEqual(
            offer["listing_url"], "https://www.avito.ru/moskva/komnaty/komnata_20m_v_3-k._2222et._2465883075"
        )
        self.assertEqual(offer["seller_name"], "Светлана")
        self.assertEqual(offer["seller_url"], "https://www.avito.ru/user/604eff275074d5c19f4eb06c7545ca33/profile")
        self.assertEqual(offer["seller_type"], "Частное лицо")
        self.assertIsNone(offer.get("listing_username"))
        self.assertIsNone(offer.get("listing_userid"))
        self.assertEqual(offer["realty_price"], "20000")
        self.assertEqual(offer["realty_location"], "Москва, ул. Мусы Джалиля, 8к3")
        self.assertEqual(offer["realty_type"], "Сдам")
        self.assertEqual(offer["realty_subtype"], "На длительный срок")
        self.assertEqual(offer["realty_property_type"], "Комнаты")
        self.assertIsNone(offer["realty_property_new"])
        self.assertEqual(offer["realty_deposit"], "20000.00")
        self.assertEqual(offer["realty_building_type"], "Панельный")
        self.assertEqual(offer["realty_building_totalfloors"], 22)
        self.assertEqual(offer["realty_floor"], 22)
        self.assertEqual(offer["realty_rooms"], "21")
        self.assertIsNone(offer["realty_space_kitchen"])
        self.assertEqual(offer["realty_space_living"], "20")
        self.assertIsNone(offer["realty_space_total"])
        self.assertIsNone(offer["listing_phone_protected"])
        self.assertIsNone(offer["listing_phone"])
        self.assertEqual(len(offer["images"]), 1)
        self.assertEqual(len(offers), 101)

    async def test_handle_object_event_avito_offers_sell(self):
        xml_offers = self.avito_offers[1]
        offers = await self.validate_offers_parsing(
            xml_offers, settings.S3_AVITO_SOURCE_BUCKET, settings.S3_AVITO_BUCKET
        )
        offer = offers[0]
        self.assertEqual(
            offer["listing_url"], "https://www.avito.ru/lyubertsy/komnaty/komnata_214m_v_2-k._25et._2439462157"
        )
        self.assertEqual(offer["seller_name"], "Павел Немов")
        self.assertEqual(offer["seller_url"], "https://www.avito.ru/user/dbab69a4dcb401fb03b0e2c05ec49bd6/profile")
        self.assertEqual(offer["seller_type"], "Частное лицо")
        self.assertIsNone(offer.get("listing_username"))
        self.assertIsNone(offer.get("listing_userid"))
        self.assertEqual(offer["realty_price"], "4000000")
        self.assertEqual(offer["realty_location"], "Московская область, Люберцы, Октябрьский пр-т, 403к1")
        self.assertEqual(offer["realty_type"], "Продам")
        self.assertIsNone(offer["realty_subtype"])
        self.assertEqual(offer["realty_property_type"], "Комнаты")
        self.assertIsNone(offer["realty_property_new"])
        self.assertEqual(offer["realty_building_type"], "Кирпичный")
        self.assertEqual(offer["realty_building_totalfloors"], 5)
        self.assertEqual(offer["realty_floor"], 2)
        self.assertEqual(offer["realty_rooms"], "21")
        self.assertIsNone(offer["realty_space_kitchen"])
        self.assertEqual(offer["realty_space_living"], "21")
        self.assertIsNone(offer["realty_space_total"])
        self.assertIsNone(offer["listing_phone_protected"])
        self.assertIsNone(offer["listing_phone"])
        self.assertEqual(len(offer["images"]), 0)
        self.assertEqual(len(offers), 101)

    async def test_handle_object_event_cian_offers_rent(self):
        xml_offers = self.cian_offers[0]
        offers = await self.validate_offers_parsing(xml_offers, settings.S3_CIAN_SOURCE_BUCKET, settings.S3_CIAN_BUCKET)
        offer = offers[0]
        self.assertEqual(offer["listing_url"], "https://spb.cian.ru/rent/flat/275236611/")
        self.assertEqual(offer["seller_name"], "АДРЕС ПЛЮС")
        self.assertEqual(offer["seller_url"], "https://www.cian.ru/company/12477972/")
        self.assertEqual(offer["seller_type"], "agency")
        self.assertIsNone(offer["listing_username"])
        self.assertIsNone(offer["listing_userid"])
        self.assertEqual(offer["realty_price"], "23000")
        self.assertEqual(offer["realty_location"], "Санкт-Петербург, Светлановское, Дрезденская улица, 6К2")
        self.assertEqual(offer["realty_type"], "Сдам")
        self.assertEqual(offer["realty_subtype"], "На длительный срок")
        self.assertEqual(offer["realty_property_type"], "Квартиры")
        self.assertIsNone(offer["realty_property_new"])
        self.assertEqual(offer["realty_building_type"], "Кирпичный")
        self.assertEqual(offer["realty_building_totalfloors"], 5)
        self.assertEqual(offer["realty_floor"], 4)
        self.assertEqual(offer["realty_rooms"], "2")
        self.assertEqual(offer["realty_space_kitchen"], "6")
        self.assertEqual(offer["realty_space_living"], "32")
        self.assertEqual(offer["realty_space_total"], "45")
        self.assertTrue(offer["listing_phone_protected"])
        self.assertEqual(offer["listing_phone"], "79811872219")
        self.assertEqual(len(offer["images"]), 17)
        self.assertEqual(len(offers), 101)

    async def test_handle_object_event_cian_offers_sell(self):
        xml_offers = self.cian_offers[1]
        offers = await self.validate_offers_parsing(xml_offers, settings.S3_CIAN_SOURCE_BUCKET, settings.S3_CIAN_BUCKET)
        offer = offers[0]
        self.assertEqual(offer["listing_url"], "https://spb.cian.ru/sale/flat/275212121/")
        self.assertEqual(offer["seller_name"], "GloraХ")
        self.assertIsNone(offer["seller_url"])
        self.assertEqual(offer["seller_type"], "agency")
        self.assertIsNone(offer["listing_username"])
        self.assertIsNone(offer["listing_userid"])
        self.assertIsNone(offer["realty_subtype"])
        self.assertEqual(offer["realty_price"], "6899999")
        self.assertEqual(offer["realty_location"], "Санкт-Петербург, Малая Охта, переулок Уткин")
        self.assertEqual(offer["realty_type"], "Продам")
        self.assertEqual(offer["realty_property_type"], "Квартира студия")
        self.assertIsNone(offer["realty_building_type"])
        self.assertEqual(offer["realty_property_new"], "Вторичка")
        self.assertEqual(offer["realty_building_totalfloors"], 15)
        self.assertEqual(offer["realty_floor"], 12)
        self.assertEqual(offer["realty_rooms"], "24")
        self.assertIsNone(offer["realty_space_kitchen"])
        self.assertEqual(offer["realty_space_living"], "19")
        self.assertEqual(offer["realty_space_total"], "23")
        self.assertIsNone(offer["listing_phone_protected"])
        self.assertIsNone(offer["listing_phone"], "79126417808")
        self.assertEqual(len(offer["images"]), 21)
        self.assertEqual(len(offers), 101)

    @parameterized.expand(
        [
            (
                "avito_deactivations",
                settings.S3_AVITO_SOURCE_BUCKET,
                settings.S3_AVITO_DEACTIVATIONS_BUCKET,
                4787,
                "avito.ru",
            ),
            (
                "cian_deactivations",
                settings.S3_CIAN_SOURCE_BUCKET,
                settings.S3_CIAN_DEACTIVATIONS_BUCKET,
                783,
                "cian.ru",
            ),
        ]
    )
    async def test_handle_object_event_deactivations(
        self, deactivations_attr: str, source_bucket: str, bucket: str, deactivated_count: int, host: str
    ):
        items = getattr(self, deactivations_attr)

        class AsyncIter:
            async def __aiter__(self):
                for item in items:
                    yield item

        self.m.patch("smartagent_realty_converter.s3.get_array_items", return_value=AsyncIter())
        self.m.patch("smartagent_realty_converter.main.get_array_items", return_value=AsyncIter())
        now = datetime.now()
        key = f"{now.strftime(DATE_FMT)}-deactivations-00001.json"
        res = await handle(build_object_event([build_object_event_message(source_bucket, key)]).dict(), None)
        self.assertIsNotNone(res["output_keys"])
        self.assertEqual(len(res["output_keys"]), 1)
        output_key = res["output_keys"][0]
        self.assertEqual(output_key[0], bucket)
        self.assertIsInstance(datetime.strptime(output_key[1][:19], DT_FMT), datetime)
        self.assertEqual(len(self.boto_client.method_calls), 2)
        self.assertEqual(self.boto_client.method_calls[0][0], "upload_fileobj")
        deactivations = self.boto_client.method_calls[0].args[0].read().decode().split("\n")

        self.assertEqual(len(deactivations), deactivated_count)
        self.assertTrue(all(host in url for url in deactivations))
