import pytest
from datetime import datetime

from app.db.models import AutoOfferHolocronSendData
from app.schemas.enums import AutoCategory
from tests.helpers import get_test_auto_offer, TestCaseAutoModeAssertSavelMixin
from app.proto import parsing_auto_model_pb2
from app.scheduler.jobs.holocron import send_offers_to_holocron


class TestHolocronJobs(TestCaseAutoModeAssertSavelMixin):
    avito_url: str

    @pytest.fixture(autouse=True)
    def add_avito_url(self, avito_trucks_url: str):
        self.avito_url = avito_trucks_url

    async def test_holo_job_send_offers(self):
        """
        При удачной отправке оффера в холокрон, work_dt выставляется в Null,
        кол-во попыток made_attempts += 1
        """
        new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
        offer = get_test_auto_offer(self.avito_url, parsed_offer=new_parsed_offer, category=AutoCategory.CARS)
        await self.assert_save_result(offer, created=True)
        holo_send_data_all = await AutoOfferHolocronSendData.all().first()

        assert isinstance(holo_send_data_all.work_dt, datetime)

        await send_offers_to_holocron()

        holo_send_data_all2 = await AutoOfferHolocronSendData.all().first()
        assert holo_send_data_all2.work_dt is None
        assert holo_send_data_all2.made_attempts == 1
