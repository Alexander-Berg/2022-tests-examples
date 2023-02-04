import json
from datetime import datetime
from app.schemas.enums import Transmission, EngineType, GearType, Color, Source, SteeringWheel
from app.converters.import_converters.auto.converter import ImportConverter
from app.parsers.parsed_value import Expected, NonParsed, NoValue
from app.parsers.s3.auto.scrapinghub.cars.drom.parser import ScrapingHubDromCarsParser

from tests.parsers.base import TestParser


URL = "https://spb.drom.ru/renault/kaptur/34019777.html"


class TestScrapinghubCarsDrom(TestParser):
    def setUp(self):
        test_json = json.load(open("tests/resources/test_scrapinghub_drom.json"))
        self.parser2 = ScrapingHubDromCarsParser(URL, test_json)
        self.ici = ImportConverter(ScrapingHubDromCarsParser(URL, test_json), Source.SCRAPING_HUB_FRESH, None)

    async def test_all_parsing(self):
        await self.ici.convert()

    async def test_parse_raw_body_type(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_body": "\u0421\u0435\u0434\u0430\u043d"})
        self.assert_parsed_value(Expected("Седан"), parser.parse_raw_body_type())
        parser = ScrapingHubDromCarsParser(URL, {"car_body": "Седан"})
        self.assert_parsed_value(Expected("Седан"), parser.parse_raw_body_type())

    async def test_parse_horse_power(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_engine_power": "105 \u043b.\u0441."})
        self.assert_parsed_value(Expected(105), parser.parse_horse_power())

    async def test_parse_transmission(self):
        parser = ScrapingHubDromCarsParser(
            URL, {"car_transmission": "\u0430\u0432\u0442\u043e\u043c\u0430\u0442"}
        )
        self.assert_parsed_value(Expected(Transmission.AUTOMATIC), parser.parse_transmission())

    async def test_parse_displacement(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_engine": "1.6"})
        self.assert_parsed_value(Expected(1600), parser.parse_displacement())

    async def test_parse_engine_type(self):
        parser = ScrapingHubDromCarsParser(
            URL, {"car_engine": "\u0431\u0435\u043d\u0437\u0438\u043d, 0.7 \u043b"}
        )
        self.assert_parsed_value(Expected(EngineType.GASOLINE), parser.parse_engine_type())

    async def test_parse_gear_type(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_drive": "4WD"})
        self.assert_parsed_value(Expected(GearType.ALL), parser.parse_gear_type())

    async def test_is_4_wd(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_drive": "4WD"})
        self.assert_parsed_value(Expected(True), parser.is_4_wd())

    async def test_parse_fn(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_name": "Volkswagen Jetta, 2012"})
        self.assert_parsed_value(Expected("Volkswagen Jetta, 2012"), parser.parse_fn())

    async def test_parse_mark(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_name": "ВАЗ (LADA) 2114 Samara"})
        self.assert_parsed_value(NonParsed(), parser.parse_mark())

    async def test_parse_model(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_name": "ВАЗ (LADA) 2114 Samara"})
        self.assert_parsed_value(NonParsed(), parser.parse_model())

    async def test_parse_color(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_color": "\u0431\u0435\u043b\u044b\u0439"})
        self.assert_parsed_value(Expected(Color.WHITE), parser.parse_color())

    async def test_parse_steering_wheel(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_leftrighthand": "\u041b\u0435\u0432\u044b\u0439"})
        self.assert_parsed_value(Expected(SteeringWheel.LEFT), parser.parse_steering_wheel())

    async def test_parse_address(self):
        parser = ScrapingHubDromCarsParser(
            URL, {"listing_city": "\u041c\u0443\u0440\u043c\u0430\u043d\u0441\u043a"}
        )
        addresses = parser.parse_address()
        self.assertEqual(2, len(addresses))
        self.assert_parsed_value(Expected("Мурманск"), addresses[0])
        self.assert_parsed_value(NoValue(), addresses[1])

    async def test_parse_price(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_price": "605000"})
        self.assert_parsed_value(Expected(605000), parser.parse_price())

    async def test_parse_owner_name(self):
        parser = ScrapingHubDromCarsParser(URL, {"dealership": {"dealership_name": "test_name"}})
        self.assert_parsed_value(Expected("test_name"), parser.parse_owner_name())
        parser = ScrapingHubDromCarsParser(URL, {})
        self.assert_parsed_value(NoValue(), parser.parse_owner_name())

    async def test_parse_owners_count(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_owners_number": 4})
        self.assert_parsed_value(NonParsed(), parser.parse_owners_count())
        parser = ScrapingHubDromCarsParser(URL, {"car_owners_number": "4"})
        self.assert_parsed_value(NonParsed(), parser.parse_owners_count())
        parser = ScrapingHubDromCarsParser(URL, {})
        self.assert_parsed_value(NonParsed(), parser.parse_owners_count())

    async def test_parse_phones(self):
        parser = ScrapingHubDromCarsParser(URL, {"listing_phone": "+7(986)889-3033, +7(986)889-3022"})
        self.assert_parsed_phones(["79868893033", "79868893022"], parser.parse_phones())
        parser = ScrapingHubDromCarsParser(URL, {"listing_phone": "+79062896472"})
        self.assert_parsed_phones(["79062896472"], parser.parse_phones())
        parser = ScrapingHubDromCarsParser(URL, {})
        self.assert_parsed_value(NoValue(), parser.parse_phones())

    async def test_parse_description(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_extrainfo": "В отличном состоянии!"})
        self.assert_parsed_value(Expected("В отличном состоянии!"), parser.parse_description())

    async def test_parse_vin(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_vin": "X9F3XXEED35****19"})
        self.assert_parsed_value(Expected("X9F3XXEED35****19"), parser.parse_vin())

    async def test_parse_year(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_year": "2012"})
        self.assert_parsed_value(Expected(2012), parser.parse_year())

    async def test_parse_mileage(self):
        parser = ScrapingHubDromCarsParser(URL, {"car_mileage": "202 000 \u043a\u043c"})
        self.assert_parsed_value(Expected(202000), parser.parse_mileage())
        parser = ScrapingHubDromCarsParser(URL, {"car_mileage": "220000"})
        self.assert_parsed_value(Expected(220000), parser.parse_mileage())

    async def test_parse_photos(self):
        parser = ScrapingHubDromCarsParser(
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
        parser = ScrapingHubDromCarsParser(URL, {"seller_type": "private"})
        self.assert_parsed_value(Expected(False), parser.parse_is_dealer())
        parser = ScrapingHubDromCarsParser(URL, {"seller_type": "dealership"})
        self.assert_parsed_value(Expected(True), parser.parse_is_dealer())

    async def test_parse_displayed_publish_date(self):
        parser = ScrapingHubDromCarsParser(URL, {"listing_date": "01-04-2021"})
        self.assert_parsed_value(Expected(datetime(2021, 4, 1)), parser.parse_displayed_publish_date())

    async def test_parse_views(self):
        parser = ScrapingHubDromCarsParser(URL, {"listing_views": "885"})
        views = parser.parse_views()
        self.assert_parsed_value(Expected(885), views["total"])
        self.assert_parsed_value(NonParsed(), views["daily"])

    async def test_parse_dealer_name(self):
        parser = ScrapingHubDromCarsParser(URL, {"dealership": {"dealership_name": "test_name"}})
        self.assert_parsed_value(Expected("test_name"), parser.parse_dealer_name())
        parser = ScrapingHubDromCarsParser(URL, {})
        self.assert_parsed_value(NoValue(), parser.parse_dealer_name())

    async def test_parse_is_phone_protected(self):
        parser = ScrapingHubDromCarsParser(URL, {"listing_phone_protected": "true"})
        self.assert_parsed_value(NonParsed(), parser.parse_is_phone_protected())
