import pytest
from tests.converters.holocron.auto.test_auto_holocron_converter import update_parsed_offer
from app.proto import parsing_auto_model_pb2
from auto.api import api_offer_model_pb2
from app.converters.holocron.auto.converter import AutoHolocronConverter
from app.schemas.enums import AutoCategory
from tests.helpers import get_test_auto_offer
from app.clients.holocron import send_offer_to_holocron


class TestHolocronClient:
    car_url: str = "https://www.drom.ru/catalog/daihatsu/hijet/g_2015_6986/"
    # TODO по хорошему замокать отправки в холокрон
    @pytest.mark.asyncio
    async def test_send_proto(self):
        new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
        update_parsed_offer(new_parsed_offer, section=api_offer_model_pb2.Section.USED)
        new_offer = get_test_auto_offer(self.car_url, parsed_offer=new_parsed_offer, category=AutoCategory.CARS)
        converter = AutoHolocronConverter(new_offer)
        holo_car_offer_proto = await converter.convert()
        resp = await send_offer_to_holocron(new_offer.hash, holo_car_offer_proto)
        assert resp is True

    @pytest.mark.asyncio
    async def test_send_proto_faild(self):
        new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
        update_parsed_offer(new_parsed_offer, section=api_offer_model_pb2.Section.USED)
        new_offer = get_test_auto_offer(self.car_url, parsed_offer=new_parsed_offer, category=AutoCategory.CARS)
        converter = AutoHolocronConverter(new_offer)
        holo_car_offer_proto = await converter.convert()
        holo_car_offer_proto.sent_timestamp.Clear()
        resp = await send_offer_to_holocron(new_offer.hash, holo_car_offer_proto)
        assert resp is False
