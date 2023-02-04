import json
from datetime import datetime
from app.schemas.enums import Transmission, EngineType, GearType, Color, Source, SteeringWheel
from app.converters.import_converters.auto.converter import ImportConverter
from app.parsers.parsed_value import Expected, NonParsed, NoValue
from app.parsers.s3.auto.scrapinghub.cars.amru.parser import ScrapingHubAmruCarsParser

from tests.parsers.base import TestParser


URL = "https://auto.youla.ru/advert/used/kia/rio/avs-btsr-motors-na-komsomolskoy--2c089a0374660fb5/"


class TestScrapinghubCarsAmru(TestParser):
    def setUp(self):
        test_json = json.load(open("tests/resources/test_scrapinghub_amru.json"))
        self.parser2 = ScrapingHubAmruCarsParser(URL, test_json)
        self.ici = ImportConverter(ScrapingHubAmruCarsParser(URL, test_json), Source.SCRAPING_HUB_FRESH, None)

    async def test_all_parsing(self):
        await self.ici.convert()

    async def test_parse_raw_body_type(self):
        parser = ScrapingHubAmruCarsParser(URL, {"car_body": "\u0421\u0435\u0434\u0430\u043d"})
        self.assert_parsed_value(Expected("Седан"), parser.parse_raw_body_type())
        parser = ScrapingHubAmruCarsParser(URL, {"car_body": "Седан"})
        self.assert_parsed_value(Expected("Седан"), parser.parse_raw_body_type())

    async def test_parse_horse_power(self):
        parser = ScrapingHubAmruCarsParser(URL, {"car_engine_power": "87 \u043b. \u0441."})
        self.assert_parsed_value(Expected(87), parser.parse_horse_power())

    async def test_parse_transmission(self):
        parser = ScrapingHubAmruCarsParser(
            URL, {"car_transmission": "\u041c\u0435\u0445\u0430\u043d\u0438\u043a\u0430"}
        )
        self.assert_parsed_value(Expected(Transmission.MECHANICAL), parser.parse_transmission())

    async def test_parse_displacement(self):
        parser = ScrapingHubAmruCarsParser(
            URL, {"car_engine": "\u0411\u0435\u043d\u0437\u0438\u043d / 1.6 \u043b"}
        )
        self.assert_parsed_value(Expected(1600), parser.parse_displacement())

    async def test_parse_engine_type(self):
        parser = ScrapingHubAmruCarsParser(
            URL, {"car_engine": "\u0411\u0435\u043d\u0437\u0438\u043d / 1.6 \u043b"}
        )
        self.assert_parsed_value(Expected(EngineType.GASOLINE), parser.parse_engine_type())

    async def test_parse_gear_type(self):
        parser = ScrapingHubAmruCarsParser(
            URL, {"car_drive": "\u041f\u0435\u0440\u0435\u0434\u043d\u0438\u0439"}
        )
        self.assert_parsed_value(Expected(GearType.FRONT), parser.parse_gear_type())

    async def test_is_4_wd(self):
        parser = ScrapingHubAmruCarsParser(
            URL, {"car_drive": "\u041f\u0435\u0440\u0435\u0434\u043d\u0438\u0439"}
        )
        self.assert_parsed_value(Expected(False), parser.is_4_wd())

    async def test_parse_fn(self):
        parser = ScrapingHubAmruCarsParser(
            URL,
            {
                "car_name": "\u0412\u0410\u0417 (Lada) Granta 1 \u043f\u043e\u043a\u043e\u043b\u0435\u043d\u0438\u0435 [\u0440\u0435\u0441\u0442\u0430\u0439\u043b\u0438\u043d\u0433], \u043b\u0438\u0444\u0442\u0431\u044d\u043a 5 \u0434\u0432."
            },
        )
        self.assert_parsed_value(Expected("ВАЗ (Lada) Granta"), parser.parse_fn())

    async def test_parse_mark(self):
        parser = ScrapingHubAmruCarsParser(URL, {"car_name": "ВАЗ (LADA) 2114 Samara"})
        self.assert_parsed_value(NonParsed(), parser.parse_mark())

    async def test_parse_model(self):
        parser = ScrapingHubAmruCarsParser(URL, {"car_name": "ВАЗ (LADA) 2114 Samara"})
        self.assert_parsed_value(NonParsed(), parser.parse_model())

    async def test_parse_color(self):
        parser = ScrapingHubAmruCarsParser(URL, {"car_color": "\u0411\u0435\u043b\u044b\u0439"})
        self.assert_parsed_value(Expected(Color.WHITE), parser.parse_color())

    async def test_parse_steering_wheel(self):
        parser = ScrapingHubAmruCarsParser(URL, {"car_leftrighthand": "\u041b\u0435\u0432\u044b\u0439"})
        self.assert_parsed_value(Expected(SteeringWheel.LEFT), parser.parse_steering_wheel())

    async def test_parse_address(self):
        parser = ScrapingHubAmruCarsParser(
            URL, {"listing_city": "\u041c\u0443\u0440\u043c\u0430\u043d\u0441\u043a"}
        )
        addresses = parser.parse_address()
        self.assertEqual(1, len(addresses))
        self.assert_parsed_value(Expected("Мурманск"), addresses[0])

    async def test_parse_price(self):
        parser = ScrapingHubAmruCarsParser(URL, {"car_price": "605000"})
        self.assert_parsed_value(Expected(605000), parser.parse_price())

    async def test_parse_owner_name(self):
        parser = ScrapingHubAmruCarsParser(URL, {})
        self.assert_parsed_value(NonParsed(), parser.parse_owner_name())

    async def test_parse_owners_count(self):
        parser = ScrapingHubAmruCarsParser(URL, {"car_owners_number": 4})
        self.assert_parsed_value(NonParsed(), parser.parse_owners_count())
        parser = ScrapingHubAmruCarsParser(URL, {"car_owners_number": "4"})
        self.assert_parsed_value(NonParsed(), parser.parse_owners_count())
        parser = ScrapingHubAmruCarsParser(URL, {})
        self.assert_parsed_value(NonParsed(), parser.parse_owners_count())

    async def test_parse_phones(self):
        parser = ScrapingHubAmruCarsParser(
            URL, {"listing_phone": "+7(986)889-3033, +7(986)889-3022, 89297292929"}
        )
        self.assert_parsed_phones(["79868893033", "79868893022", "79297292929"], parser.parse_phones())
        parser = ScrapingHubAmruCarsParser(URL, {"listing_phone": "+79062896472"})
        self.assert_parsed_phones(["79062896472"], parser.parse_phones())

    async def test_parse_description(self):
        parser = ScrapingHubAmruCarsParser(URL, {"car_extrainfo": "В отличном состоянии!"})
        self.assert_parsed_value(Expected("В отличном состоянии!"), parser.parse_description())

    async def test_parse_vin(self):
        parser = ScrapingHubAmruCarsParser(URL, {"car_vin": "WVWZ*************"})
        self.assert_parsed_value(Expected("WVWZ*************"), parser.parse_vin())

    async def test_parse_year(self):
        parser = ScrapingHubAmruCarsParser(URL, {"car_year": "2012"})
        self.assert_parsed_value(Expected(2012), parser.parse_year())

    async def test_parse_mileage(self):
        parser = ScrapingHubAmruCarsParser(URL, {"car_mileage": "202 000 \u043a\u043c"})
        self.assert_parsed_value(Expected(202000), parser.parse_mileage())
        parser = ScrapingHubAmruCarsParser(URL, {"car_mileage": "220000"})
        self.assert_parsed_value(Expected(220000), parser.parse_mileage())

    async def test_parse_photos(self):
        parser = ScrapingHubAmruCarsParser(
            URL,
            {
                "images": [
                    "https://67.img.avito.st/640x480/10148199967.jpg",
                    "https://66.img.avito.st/640x480/10148199966.jpg",
                ]
            },
        )
        self.assert_parsed_value(
            Expected(
                [
                    "https://67.img.avito.st/640x480/10148199967.jpg",
                    "https://66.img.avito.st/640x480/10148199966.jpg",
                ]
            ),
            parser.parse_photos(),
        )

    async def test_parse_is_dealer(self):
        parser = ScrapingHubAmruCarsParser(URL, {"seller_type": "dealership"})
        self.assert_parsed_value(NonParsed(), parser.parse_is_dealer())

    async def test_parse_displayed_publish_date(self):
        parser = ScrapingHubAmruCarsParser(URL, {"listing_date": "01-04-21"})
        self.assert_parsed_value(Expected(datetime(2021, 4, 1)), parser.parse_displayed_publish_date())

    async def test_parse_views(self):
        parser = ScrapingHubAmruCarsParser(URL, {"listing_views_total": "17", "listing_views_today": "16"})
        views = parser.parse_views()
        self.assert_parsed_value(Expected(17), views["total"])
        self.assert_parsed_value(Expected(16), views["daily"])

    async def test_parse_dealer_name(self):
        parser = ScrapingHubAmruCarsParser(
            URL,
            {
                "dealership": {
                    "dealership_url": "https://auto.youla.ru/cardealers/salon-transavto-m-5164/",
                    "dealership_name": "test_name",
                }
            },
        )
        self.assert_parsed_value(Expected("test_name"), parser.parse_dealer_name())
        parser = ScrapingHubAmruCarsParser(URL, {})
        self.assert_parsed_value(NoValue(), parser.parse_dealer_name())

    async def test_parse_is_phone_protected(self):
        parser = ScrapingHubAmruCarsParser(URL, {"listing_phone_protected": "true"})
        self.assert_parsed_value(NonParsed(), parser.parse_is_phone_protected())
        parser = ScrapingHubAmruCarsParser(URL, {"listing_phone_protected": "false"})
        self.assert_parsed_value(NonParsed(), parser.parse_is_phone_protected())
        parser = ScrapingHubAmruCarsParser(URL, {"listing_phone_protected": True})
        self.assert_parsed_value(NonParsed(), parser.parse_is_phone_protected())
