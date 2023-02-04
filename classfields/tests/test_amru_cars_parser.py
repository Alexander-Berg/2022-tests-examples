from typing import Dict
from unittest import TestCase

import pytest

from offer_status_parser.models.parsing import ParsingStatuses
from offer_status_parser.parsers import AmruCarsParser


@pytest.mark.usefixtures("pages_class")
class TestAmruCarsParser(TestCase):
    pages: Dict[str, str]

    def test_active_page(self):
        parser = AmruCarsParser(self.pages["auto/amru_active.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.ACTIVE)

    def test_inactive_page(self):
        parser = AmruCarsParser(self.pages["auto/amru_inactive.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_listing_page(self):
        parser = AmruCarsParser(self.pages["auto/amru_listing.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)
