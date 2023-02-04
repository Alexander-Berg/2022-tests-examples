import pytest
from datetime import datetime

from auto.api.api_offer_model_pb2 import Phone

from app.core import cur_tz
from app.constants import filters_reasons
from app.db.models import (
    AutoOfferRegion,
    AutoOfferVin,
    AutoOfferPhone,
    AutoOfferFilteredReasons,
    AutoOffersLastSentDateForPhone,
)
from app.proto.parsing_auto_model_pb2 import ParsedOffer
from app.scheduler.jobs.renew_last_sent_dt_for_phone import renew_last_sent_dt
from tests.helpers import get_test_auto_offer, TestCaseAutoModeAssertSavelMixin


class TestIndexesTable(TestCaseAutoModeAssertSavelMixin):
    test_url = "https://auto.youla.ru/ir-renault-ekaterinburg--234104e56fdcc3e1"

    @pytest.mark.asyncio
    async def test_auto_offer_region(self):
        """
        Проверка сохранения региона в индексную таблицу
        """
        parsed_offer_pb = ParsedOffer()
        parsed_offer_pb.offer.seller.location.address = "Екатеринбург"
        offer = get_test_auto_offer(self.test_url, parsed_offer=parsed_offer_pb)
        await offer.save()
        region = await AutoOfferRegion.filter(hash=offer.hash).first()
        assert region.value == 54

    @pytest.mark.asyncio
    async def test_auto_offer_vin(self):
        """
        Проверка сохранения vin в индексную таблицу
        """
        parsed_offer_pb = ParsedOffer()
        parsed_offer_pb.offer.documents.vin = "X96A*************"
        offer = get_test_auto_offer(self.test_url, parsed_offer=parsed_offer_pb)
        await offer.save()
        vin = await AutoOfferVin.filter(hash=offer.hash).first()
        assert vin.value == "X96A*************"

    @pytest.mark.asyncio
    async def test_auto_offer_phone(self):
        """
        Проверка сохранения телефона в индексную таблицу
        """
        parsed_offer_pb = ParsedOffer()
        parsed_offer_pb.offer.seller.phones.append(Phone(phone="79267777777"))
        parsed_offer_pb.offer.seller.phones.append(Phone(phone="79265555555"))
        offer = get_test_auto_offer(self.test_url, parsed_offer=parsed_offer_pb)
        await offer.save()
        phones = await AutoOfferPhone.filter(offer=offer.hash)
        assert len(phones) == 2

    @pytest.mark.asyncio
    async def test_auto_offer_filtered_reason(self):
        """
        Проверка сохранения filtered_reason в индексную таблицу
        """
        parsed_offer_pb = ParsedOffer()
        parsed_offer_pb.filter_reason.append(filters_reasons.OLDER_20_DAYS)
        parsed_offer_pb.filter_reason.append(filters_reasons.PROCESSED_BY_SCALA)
        offer = get_test_auto_offer(self.test_url, parsed_offer=parsed_offer_pb)
        await offer.save()
        filtered_reasons = await AutoOfferFilteredReasons.filter(hash=offer.hash)
        assert len(filtered_reasons) == 2

    @pytest.mark.asyncio
    async def test_auto_offer_last_sent_date_for_phone(self):
        """
        Проверка сохранения последней даты отправки по телефону в индексную таблицу
        """
        test_url = "https://auto.youla.ru/ir-renault-ekaterinburg--234104e56fdcc3e1"
        parsed_offer_pb = ParsedOffer()
        parsed_offer_pb.offer.seller.phones.append(Phone(phone="79267777777"))
        parsed_offer_pb.offer.seller.phones.append(Phone(phone="79265555555"))
        offer = get_test_auto_offer(test_url, parsed_offer=parsed_offer_pb)
        offer.sent_dt = datetime(2021, 9, 1, 1, 1, 1, 1)
        await offer.save()

        test_url2 = "https://auto.youla.ru/advert/new/vaz_lada/granta/avs-avtoregion--53e4b900aaad19b"
        parsed_offer_pb = ParsedOffer()
        parsed_offer_pb.offer.seller.phones.append(Phone(phone="79267777777"))
        offer = get_test_auto_offer(test_url2, parsed_offer=parsed_offer_pb)
        offer.sent_dt = datetime(2021, 7, 1, 1, 1, 1, 1)
        await offer.save()

        await renew_last_sent_dt()
        all_phones = await AutoOffersLastSentDateForPhone.all()
        assert len(all_phones) == 2
        date_for_phone = await AutoOffersLastSentDateForPhone.filter(phone="79267777777").first()
        assert date_for_phone.last_sent_dt == cur_tz.localize(datetime(2021, 9, 1, 1, 1, 1, 1))
