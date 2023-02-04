from typing import Dict
from unittest import TestCase

import pytest

from app.schemas.enums import ParsingStatuses
from app.parsers.deactivate import AvitoCarsParser
from app.parsers.deactivate.exceptions import ParsingFirewallError, ParsingRetryError


@pytest.mark.usefixtures("pages_class")
class TestAvitoCarsParser(TestCase):
    pages: Dict[str, str]

    def test_active_page(self):
        parser = AvitoCarsParser(self.pages["auto/avito_active.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.ACTIVE)

    def test_active_page_2(self):
        parser = AvitoCarsParser(self.pages["auto/avito_active2.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.ACTIVE)

    def test_active_page_3(self):
        parser = AvitoCarsParser(self.pages["auto/avito_active3.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.ACTIVE)

    def test_active_page_4(self):
        parser = AvitoCarsParser(self.pages["auto/avito_active4.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.ACTIVE)

    def test_active_page_no_phone(self):
        parser = AvitoCarsParser(self.pages["auto/avito_active_nophone.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.ACTIVE)

    def test_beaten(self):
        parser = AvitoCarsParser(self.pages["auto/avito_beaten.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_beaten_2(self):
        parser = AvitoCarsParser(self.pages["auto/avito_beaten2.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_inactive_page(self):
        parser = AvitoCarsParser(self.pages["auto/avito_inactive.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_inactive_page_2(self):
        parser = AvitoCarsParser(self.pages["auto/avito_inactive2.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_inactive_page_3(self):
        parser = AvitoCarsParser(self.pages["auto/avito_inactive3.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_listing_page(self):
        parser = AvitoCarsParser(self.pages["auto/avito_listing.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_listing_page_2(self):
        parser = AvitoCarsParser(self.pages["auto/avito_listing2.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_listing_page_3(self):
        parser = AvitoCarsParser(self.pages["auto/avito_listing2.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_services_listing_page(self):
        parser = AvitoCarsParser(self.pages["auto/avito_inactive_services_listing.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_moderation_page(self):
        parser = AvitoCarsParser(self.pages["auto/avito_moderation.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_moderation_page_2(self):
        parser = AvitoCarsParser(self.pages["auto/avito_moderation2.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_non_cars(self):
        parser = AvitoCarsParser(self.pages["auto/avito_noncars.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_500(self):
        parser = AvitoCarsParser(self.pages["auto/avito_500.html"])
        with pytest.raises(ParsingRetryError):
            parser.offer_status()

    def test_firewall(self):
        parser = AvitoCarsParser(self.pages["auto/avito_firewall_container.html"])
        with pytest.raises(ParsingFirewallError):
            parser.offer_status()
