from typing import Dict
from unittest import TestCase

import pytest

from offer_status_parser.models.parsing import PageCategories, ParsingStatuses
from offer_status_parser.parsers import get_parser


@pytest.mark.usefixtures("pages_class")
class TestParser(TestCase):
    pages: Dict[str, str]

    def test_avito_auto_page(self):
        url = "https://www.avito.ru/kazan/avtomobili/lada_kalina_2011_1394425125"
        parser = get_parser(url, PageCategories.AUTO)(self.pages["auto/avito_inactive.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_drom_auto_page(self):
        url = "https://vladivostok.drom.ru/hummer/h2/28386969.html"
        parser = get_parser(url, PageCategories.AUTO)(self.pages["auto/drom_inactive.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_amru_auto_page(self):
        url = "https://auto.youla.ru/advert/used/vaz_lada/2110/prv--1931a290b94baae0/"
        parser = get_parser(url, PageCategories.AUTO)(self.pages["auto/amru_inactive.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_bad_page(self):
        url = "https://auto.bad.ru/advert/used/vaz_lada/2110/prv--1931a290b94baae0/"
        with pytest.raises(ValueError):
            get_parser(url, PageCategories.AUTO)

    def test_avito_realty_page(self):
        url = "https://www.avito.ru/novyy_oskol/kvartiry/3-k_kvartira_60_m_45_et._1268052339"
        parser = get_parser(url, PageCategories.REALTY)(self.pages["realty/avito_realty_offer_deact.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)

    def test_cian_realty_page(self):
        url = "https://rostov.cian.ru/rent/flat/199155944/"
        parser = get_parser(url, PageCategories.REALTY)(self.pages["realty/cian-realty-offer-deact-1.html"])
        result, reason, additional_info = parser.offer_status()
        self.assertEqual(result, ParsingStatuses.INACTIVE)
