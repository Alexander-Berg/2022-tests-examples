import pytest
from datetime import datetime, timezone

from app.core import settings
from app.schemas.enums import Action
from app.proto.parsing_auto_model_pb2 import ParsedOffer
from tests.helpers import get_test_auto_offer, TestCaseAutoModeAssertSavelMixin
from app.db.models import AutoOffer, AutoOfferHolocronSendData


class TestDeactivation(TestCaseAutoModeAssertSavelMixin):
    test_url = "https://auto.youla.ru/ir-renault-ekaterinburg--234104e56fdcc3e1"

    @pytest.mark.asyncio
    async def test_deactivation_offer(self):
        """
        При деактивации оффера сохраняется дата деактивации и ставится статус - деактивировано.
        В таблицу с данными для холокрона добавляется оффер со статусом - деактивирован
        """
        avito_trucks_url = self.test_url

        parsed_offer_pb = ParsedOffer()
        offer = get_test_auto_offer(avito_trucks_url, parsed_offer=parsed_offer_pb)
        await offer.save()
        holo_data = await AutoOfferHolocronSendData.filter(hash_id=offer.hash,
                                                           action=Action.DEACTIVATE)
        assert len(holo_data) == 0

        deactivation_dt = datetime.now(tz=timezone.utc)
        new_parsed_offer_pb = ParsedOffer()
        new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer_pb,
                                        deactivation_dt=deactivation_dt)
        await new_offer.save()
        offer_from_db = await AutoOffer.filter(hash=offer.hash).first()
        assert offer_from_db.deactivation_dt == deactivation_dt
        holo_data = await AutoOfferHolocronSendData.filter(hash_id=offer.hash,
                                                           action=Action.DEACTIVATE)
        assert len(holo_data) == 1

    @pytest.mark.asyncio
    async def test_activation_offer(self):
        """
        При активации деактивированного оффера дата деактивации сбрасывается на дату по умолчанию
        """
        avito_trucks_url = self.test_url

        parsed_offer_pb = ParsedOffer()
        offer = get_test_auto_offer(avito_trucks_url, parsed_offer=parsed_offer_pb)
        await offer.save()

        new_parsed_offer_pb = ParsedOffer()
        deactivation_dt = datetime.now(tz=timezone.utc)
        new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer_pb,
                                        deactivation_dt=deactivation_dt)
        await new_offer.save()
        offer_from_db = await AutoOffer.filter(hash=offer.hash, deactivation_dt=deactivation_dt).first()
        assert offer_from_db.deactivation_dt == deactivation_dt

        await AutoOfferHolocronSendData.all().delete()
        new_parsed_offer_pb = ParsedOffer()
        new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer_pb)

        await new_offer.save()

        offer_from_db = await AutoOffer.filter(hash=offer.hash).first()
        assert offer_from_db.deactivation_dt == settings.DEFAULT_DATETIME
        holo_send_datas = await AutoOfferHolocronSendData.filter(hash_id=offer.hash)
        assert len(holo_send_datas) == 1
        assert holo_send_datas[0].action == Action.ACTIVATE
