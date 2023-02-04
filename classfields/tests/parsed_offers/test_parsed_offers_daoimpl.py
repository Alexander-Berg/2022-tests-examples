import pytest
from auto.api.api_offer_model_pb2 import Phone
from app.schemas.enums import Status
from app.proto.parsing_auto_model_pb2 import ParsedOffer
from tests.helpers import get_test_auto_offer, TestCaseAutoModeAssertSavelMixin
from app.db.models import AutoOffer
from app.constants import filters_reasons
from app.schemas.enums import AutoCategory


class TestParsedOffers(TestCaseAutoModeAssertSavelMixin):
    test_url = "https://auto.youla.ru/ir-renault-ekaterinburg--234104e56fdcc3e1"

    @pytest.mark.asyncio
    async def test_parse_date_the_only_change(self):
        """
        Если parse_date единственное изменение, то не обновляем
        """
        avito_trucks_url = self.test_url

        parsed_offer_pb = ParsedOffer()
        parsed_offer_pb.parse_date.seconds = 50
        parsed_offer_pb.offer.category = AutoCategory.CARS.proto_value
        offer = get_test_auto_offer(avito_trucks_url, parsed_offer=parsed_offer_pb)
        await offer.save()
        new_parsed_offer_pb = ParsedOffer()
        new_parsed_offer_pb.parse_date.seconds = 100
        new_parsed_offer_pb.offer.category = AutoCategory.CARS.proto_value
        new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer_pb)
        await new_offer.save()
        offer_from_db = await AutoOffer.filter(hash=new_offer.hash).first()
        assert offer_from_db.data_proto.parse_date.seconds == 50

    @pytest.mark.asyncio
    async def test_less_parse_date(self):
        """
        Если спаршенная parse_date меньше, чем в БД, то не обновляем
        """
        avito_trucks_url = self.test_url

        parsed_offer_pb = ParsedOffer()
        parsed_offer_pb.parse_date.seconds = 50
        parsed_offer_pb.offer.category = AutoCategory.CARS.proto_value
        offer = get_test_auto_offer(avito_trucks_url, parsed_offer=parsed_offer_pb)
        await offer.save()
        new_parsed_offer_pb = ParsedOffer()
        new_parsed_offer_pb.parse_date.seconds = 40
        new_parsed_offer_pb.offer.category = AutoCategory.CARS.proto_value
        new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer_pb)
        await new_offer.save()
        offer_from_db = await AutoOffer.filter(hash=new_offer.hash).first()
        assert offer_from_db.data_proto.parse_date.seconds == 50

    @pytest.mark.asyncio
    async def test_is_dealer_not_ignored(self):
        """
        Если признак is_dealer был и пропал - снимаем его
        """
        avito_trucks_url = self.test_url

        parsed_offer_pb = ParsedOffer()
        parsed_offer_pb.is_dealer.value = True
        parsed_offer_pb.offer.category = AutoCategory.CARS.proto_value
        offer = get_test_auto_offer(avito_trucks_url, parsed_offer=parsed_offer_pb)
        await offer.save()
        new_parsed_offer_pb = ParsedOffer()
        new_parsed_offer_pb.is_dealer.value = False
        new_parsed_offer_pb.offer.category = AutoCategory.CARS.proto_value
        new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer_pb)
        await new_offer.save()
        offer_from_db = await AutoOffer.filter(hash=new_offer.hash).first()
        assert offer_from_db.data_proto.is_dealer.value is False

    @pytest.mark.asyncio
    async def test_save_no_phones(self):
        """
        В апдейте пропали телефоны - сохраняем, но телефоны оставляем
        """
        avito_trucks_url = self.test_url

        parsed_offer_pb = ParsedOffer()
        parsed_offer_pb.offer.seller.phones.append(Phone(phone="79267777777"))
        parsed_offer_pb.offer.seller.phones.append(Phone(phone="79265555555"))

        offer = get_test_auto_offer(avito_trucks_url, parsed_offer=parsed_offer_pb)
        await offer.save()
        new_parsed_offer_pb = ParsedOffer()
        new_parsed_offer_pb.is_dealer.value = True
        new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer_pb)
        await new_offer.save()
        offer_from_db = await AutoOffer.filter(hash=new_offer.hash).first()
        assert offer_from_db.data_proto.offer.seller == parsed_offer_pb.offer.seller

    @pytest.mark.asyncio
    async def test_same_parse_date(self):
        """
        Если parse_date не изменился, то обновляем
        """
        avito_trucks_url = self.test_url

        parsed_offer_pb = ParsedOffer()
        offer = get_test_auto_offer(avito_trucks_url, parsed_offer=parsed_offer_pb)
        await offer.save()
        new_parsed_offer_pb = ParsedOffer()
        new_parsed_offer_pb.offer.description = "test"
        new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer_pb)
        await new_offer.save()
        offer_from_db = await AutoOffer.filter(hash=new_offer.hash).first()
        assert offer_from_db.data_proto.offer.description == "test"

    @pytest.mark.asyncio
    async def test_index_update_for_phones_with_leading_zeros(self):
        """
        Если объявление отфильтровано по причине older_20_days - обновляем, мб причина пропадет
        """
        avito_trucks_url = self.test_url

        parsed_offer_pb = ParsedOffer()
        parsed_offer_pb.parse_date.seconds = 50
        parsed_offer_pb.filter_reason.append(filters_reasons.OLDER_20_DAYS)
        offer = get_test_auto_offer(avito_trucks_url, parsed_offer=parsed_offer_pb, status=Status.FILTERED)
        await offer.save()
        new_parsed_offer_pb = ParsedOffer()
        new_parsed_offer_pb.parse_date.seconds = 60
        new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer_pb)
        await self.assert_save_result(new_offer, updated=True)
        offer_from_db = await AutoOffer.filter(hash=offer.hash).first()
        assert offer_from_db.data_proto.parse_date.seconds == 60
