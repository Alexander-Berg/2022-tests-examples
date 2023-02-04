from typing import Dict
from unittest import TestCase

import pytest

from offer_status_parser.models.parsing import ParsingReasons, ParsingStatuses
from offer_status_parser.parsers import AvitoRealtyParser


@pytest.mark.usefixtures("pages_class")
class TestAvitoRealtyParser(TestCase):
    pages: Dict[str, str]

    def test_active_page(self):
        parser = AvitoRealtyParser(self.pages["realty/avito_realty_offer.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.ACTIVE)

    def test_active_page_2(self):
        parser = AvitoRealtyParser(self.pages["realty/avito_realty_offer-2.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.ACTIVE)

    def test_inactive_page(self):
        parser = AvitoRealtyParser(self.pages["realty/avito_realty_offer_deact.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_listing_page(self):
        parser = AvitoRealtyParser(self.pages["realty/avito_realty_listing.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_moderation_page(self):
        parser = AvitoRealtyParser(self.pages["realty/avito_realty_rejected_by_mod.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)
        self.assertEqual(reason, ParsingReasons.moderation_page)
