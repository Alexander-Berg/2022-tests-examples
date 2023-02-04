from typing import Dict
from unittest import TestCase

import pytest

from app.schemas.enums import ParsingStatuses
from app.parsers.deactivate import DromCarsParser


@pytest.mark.usefixtures("pages_class")
class TestDromCarsParser(TestCase):
    pages: Dict[str, str]

    def test_active_page(self):
        parser = DromCarsParser(self.pages["auto/drom_active.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.ACTIVE)

    def test_active_page_2(self):
        parser = DromCarsParser(self.pages["auto/drom_active2.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.ACTIVE)

    def test_inactive_page(self):
        parser = DromCarsParser(self.pages["auto/drom_inactive.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_inactive_page_2(self):
        parser = DromCarsParser(self.pages["auto/drom_inactive2.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_inactive_page_3(self):
        parser = DromCarsParser(self.pages["auto/drom_inactive3.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_inactive_page_4(self):
        parser = DromCarsParser(self.pages["auto/drom_inactive4.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_inactive_page_5(self):
        parser = DromCarsParser(self.pages["auto/drom_inactive5.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_inactive_page_6(self):
        parser = DromCarsParser(self.pages["auto/drom_inactive6.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_inactive_page_7(self):
        parser = DromCarsParser(self.pages["auto/drom_inactive7.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_inactive_page_8(self):
        parser = DromCarsParser(self.pages["auto/drom_inactive8.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_pre_moderation_page(self):
        parser = DromCarsParser(self.pages["auto/drom_premoderation.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_moderation_deleted_pag(self):
        parser = DromCarsParser(self.pages["auto/drom_moderation_deleted.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_moderation_deleted_page_2(self):
        parser = DromCarsParser(self.pages["auto/drom_moderation_deleted2.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)
