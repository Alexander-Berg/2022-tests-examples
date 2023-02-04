import pytest

from app.db.models import AutoOfferPhone
from tests.helpers import get_test_auto_offer, TestCaseAutoModeAssertSavelMixin


class TestAutoModel(TestCaseAutoModeAssertSavelMixin):
    avito_url: str

    @pytest.fixture(autouse=True)
    def add_avito_url(self, avito_trucks_url: str):
        self.avito_url = avito_trucks_url

    async def test_index_tables_insert_delete_select(self):
        offer = get_test_auto_offer(self.avito_url)
        await self.assert_save_result(offer, created=True)
        offer_phone = AutoOfferPhone(offer_id=offer.hash, value=123456)
        await offer_phone.save(force_create=True)
        offers_phones = await AutoOfferPhone.filter(offer_id=offer.hash)
        self.assertEqual(1, len(offers_phones))
        self.assertEqual(offer_phone.value, offers_phones[0].value)
        await AutoOfferPhone.filter(offer_id=offer.hash).delete()
        offers_phones = await AutoOfferPhone.filter(offer_id=offer.hash)
        self.assertEqual(0, len(offers_phones))
