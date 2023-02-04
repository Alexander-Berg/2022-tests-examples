# import json
# from datetime import datetime
# import pytest
# from app.schemas.enums import Transmission, EngineType, GearType, Color, SteeringWheel
# from app.converters.import_converter import ImportConverter
# from app.deactivate.auto.haraba.cars.avito.parser import HarabaAvitoCarsParser
# from app.scheduler.jobs.deactivation_haraba import get_objects_from_last
#
#
# class TestHarabaCarsAvito:
#     def setup(self):
#         test_json = json.load(open("tests/resources/test_haraba_avito.json"))
#         self.parser2 = HarabaAvitoCarsParser("test_url", test_json)
#         self.ici = ImportConverter(parser=HarabaAvitoCarsParser("test_url", test_json))
#
#     @pytest.mark.asyncio
#     async def test_all_parsing(self, mocker):
#         mocker.patch(__name__ + ".get_objects_from_last", return_value=9)
#         d = await get_objects_from_last()
#         assert d == 9
