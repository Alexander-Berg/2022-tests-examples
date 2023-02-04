import pytest
from datetime import datetime

from app.db.models import AutoOfferHolocronSendData
from app.schemas.enums import AutoCategory, Action
from tests.helpers import get_test_auto_offer, TestCaseAutoModeAssertSavelMixin
from tests.converters.holocron.auto.test_auto_holocron_converter import update_parsed_offer
from app.proto import parsing_auto_model_pb2


class TestHolocronModel(TestCaseAutoModeAssertSavelMixin):
    avito_url: str

    @pytest.fixture(autouse=True)
    def add_avito_url(self, avito_trucks_url: str):
        self.avito_url = avito_trucks_url

    async def test_holo_record(self):
        """
        Проверяем, что при добавлении оффера и апдейте,
        добавится 2 записи с соответствующими action
        """
        new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
        new_parsed_offer.is_dealer.value = True
        update_parsed_offer(new_parsed_offer, year=datetime.now().year - 1)
        offer = get_test_auto_offer(self.avito_url, parsed_offer=new_parsed_offer, category=AutoCategory.CARS)

        await self.assert_save_result(offer, created=True)
        holo_send_data_all = await AutoOfferHolocronSendData.all().first()
        assert holo_send_data_all.action == Action.ACTIVATE

        new_parsed_offer.is_dealer.value = False
        offer = get_test_auto_offer(self.avito_url, parsed_offer=new_parsed_offer, category=AutoCategory.CARS)
        await self.assert_save_result(offer, updated=True)
        holo_send_data_all = await AutoOfferHolocronSendData.all()

        assert len(holo_send_data_all) == 2
        assert holo_send_data_all[-1].action == Action.UPDATE

    async def test_error_holo_record(self):
        offer = get_test_auto_offer(self.avito_url)
        await self.assert_save_result(offer, created=True)
        holo_send_data = await AutoOfferHolocronSendData.all().first()
        assert holo_send_data.error_message == "non_cars_category"
        assert holo_send_data.holo_offer is None
