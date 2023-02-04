from typing import Dict
from unittest import TestCase

import pytest

from app.schemas.enums import ParsingStatuses
from app.parsers.deactivate import CianRealtyParser


@pytest.mark.usefixtures("pages_class")
class TestCianRealtyParser(TestCase):
    pages: Dict[str, str]

    def test_active_page(self):
        parser = CianRealtyParser(self.pages["realty/cian-realty-offer-ok-1.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.ACTIVE)

    def test_inactive_page(self):
        parser = CianRealtyParser(self.pages["realty/cian-realty-offer-deact-1.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)
