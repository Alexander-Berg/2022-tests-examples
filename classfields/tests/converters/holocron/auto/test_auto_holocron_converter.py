import pytest
import random
from datetime import datetime
from typing import Optional

from auto.api.catalog_model_pb2 import TechInfo
from auto.api import api_offer_model_pb2
from vertis.holocron import utils_pb2
from vertis.holocron.broker import holo_car_offer_pb2
from pytest_mock import MockerFixture

from app.schemas.enums import AutoCategory
from app.errors  import HoloUnableToConvertError
from app.converters.holocron.auto.converter import AutoHolocronConverter
from app.proto import parsing_auto_model_pb2
from tests.helpers import get_test_auto_offer


# All test coroutines will be treated as marked.
pytestmark = pytest.mark.asyncio

TEST1 = "test1"


@pytest.fixture(autouse=True)
def mock_cars_catalog_data(mocker: MockerFixture):
    yield mocker.patch("app.clients.searcher.SearcherClient.cars_catalog_data", new=new_cars_catalog_data)


async def new_cars_catalog_data(self, tech_param_id: int) -> TechInfo:
    tech_info = TechInfo()
    tech_info.tech_param.id = tech_param_id
    tech_info.mark_info.name = "MERCEDES"
    tech_info.model_info.name = "814"
    return tech_info


async def test_searcher_client_no_call_if_no_tech_param_id(avito_cars_url: str, mocker: MockerFixture):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    update_parsed_offer(new_parsed_offer, section=api_offer_model_pb2.Section.USED)
    new_offer = get_test_auto_offer(avito_cars_url, parsed_offer=new_parsed_offer, category=AutoCategory.CARS)
    assert not new_offer.data_proto.offer.car_info.tech_param_id
    converter = AutoHolocronConverter(new_offer)
    spy = mocker.spy(converter.searcher_client, "cars_catalog_data")
    holo_car_offer = await converter.convert()
    assert holo_car_offer
    assert holo_car_offer.section == api_offer_model_pb2.Section.USED
    spy.assert_not_called()


async def test_searcher_client_call_if_tech_param_id(avito_cars_url: str, mocker: MockerFixture):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    update_parsed_offer(new_parsed_offer, section=api_offer_model_pb2.Section.USED)
    tech_param_id = random.randint(1000000, 10000000)
    new_parsed_offer.offer.car_info.tech_param_id = tech_param_id
    new_offer = get_test_auto_offer(avito_cars_url, parsed_offer=new_parsed_offer, category=AutoCategory.CARS)
    assert new_offer.data_proto.offer.car_info.tech_param_id
    converter = AutoHolocronConverter(new_offer)
    spy = mocker.spy(converter.searcher_client, "cars_catalog_data")
    holo_car_offer = await converter.convert()
    assert holo_car_offer
    assert holo_car_offer.section == api_offer_model_pb2.Section.USED
    spy.assert_called()
    assert isinstance(spy.spy_return, TechInfo)
    assert spy.spy_return.tech_param.id == tech_param_id


async def test_section_set_in_offer(avito_cars_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    update_parsed_offer(new_parsed_offer, section=api_offer_model_pb2.Section.USED)
    new_offer = get_test_auto_offer(avito_cars_url, parsed_offer=new_parsed_offer, category=AutoCategory.CARS)
    converter = AutoHolocronConverter(new_offer)
    holo_car_offer = await converter.convert()
    assert holo_car_offer
    assert holo_car_offer.section == api_offer_model_pb2.Section.USED


async def test_section_not_set_year_old(avito_cars_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    update_parsed_offer(new_parsed_offer, year=datetime.now().year - 2)
    new_offer = get_test_auto_offer(avito_cars_url, parsed_offer=new_parsed_offer, category=AutoCategory.CARS)
    converter = AutoHolocronConverter(new_offer)
    holo_car_offer = await converter.convert()
    assert holo_car_offer
    assert holo_car_offer.section == api_offer_model_pb2.Section.USED


async def test_section_not_set_year_not_old_mileage_above_zero(avito_cars_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    update_parsed_offer(new_parsed_offer, year=datetime.now().year - 1, mileage=1000)
    new_offer = get_test_auto_offer(avito_cars_url, parsed_offer=new_parsed_offer, category=AutoCategory.CARS)
    converter = AutoHolocronConverter(new_offer)
    holo_car_offer = await converter.convert()
    assert holo_car_offer
    assert holo_car_offer.section == api_offer_model_pb2.Section.USED


async def test_section_not_set_year_not_old_mileage_zero(avito_cars_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    update_parsed_offer(new_parsed_offer, year=datetime.now().year - 1)
    new_offer = get_test_auto_offer(avito_cars_url, parsed_offer=new_parsed_offer, category=AutoCategory.CARS)
    converter = AutoHolocronConverter(new_offer)
    holo_car_offer = await converter.convert()
    assert holo_car_offer
    assert holo_car_offer.section == api_offer_model_pb2.Section.NEW


async def test_section_not_set_year_not_set_mileage_above_zero(avito_cars_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    update_parsed_offer(new_parsed_offer, mileage=1000)
    new_offer = get_test_auto_offer(avito_cars_url, parsed_offer=new_parsed_offer, category=AutoCategory.CARS)
    converter = AutoHolocronConverter(new_offer)
    holo_car_offer = await converter.convert()
    assert holo_car_offer
    assert holo_car_offer.section == api_offer_model_pb2.Section.USED


async def test_section_not_set_year_not_set_mileage_zero(avito_cars_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    update_parsed_offer(new_parsed_offer)
    new_offer = get_test_auto_offer(avito_cars_url, parsed_offer=new_parsed_offer, category=AutoCategory.CARS)
    converter = AutoHolocronConverter(new_offer)
    holo_car_offer = await converter.convert()
    assert holo_car_offer
    assert holo_car_offer.section == api_offer_model_pb2.Section.NEW


async def test_action_active_on_empty_status_history(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    update_parsed_offer(new_parsed_offer, TEST1)
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer, category=AutoCategory.TRUCKS)
    converter = AutoHolocronConverter(new_offer)
    car_offer = holo_car_offer_pb2.HoloCarOffer()
    converter.set_action(car_offer)
    assert car_offer.action == utils_pb2.Action.ACTIVATE


async def test_non_cars_offer_unable_to_convert_error(avito_trucks_url: str):
    new_parsed_offer = parsing_auto_model_pb2.ParsedOffer()
    update_parsed_offer(new_parsed_offer, TEST1)
    new_offer = get_test_auto_offer(avito_trucks_url, parsed_offer=new_parsed_offer, category=AutoCategory.TRUCKS)
    converter = AutoHolocronConverter(new_offer)
    with pytest.raises(HoloUnableToConvertError) as exc_info:
        await converter.convert()
    assert len(exc_info.value.args) == 1
    assert exc_info.value.args[0] == "non_cars_category"


def update_parsed_offer(
    parsed_offer: parsing_auto_model_pb2.ParsedOffer,
    text: str = "",
    parse_date: Optional[datetime] = None,
    section: int = api_offer_model_pb2.Section.SECTION_UNKNOWN,
    year: int = 0,
    mileage: int = 0,
) -> None:
    if not parse_date:
        parse_date = datetime.now()
    parsed_offer.offer.description = text
    parsed_offer.offer.section = section
    parsed_offer.offer.documents.year = year
    parsed_offer.offer.state.mileage = mileage
    parsed_offer.parse_date.FromDatetime(parse_date)
    parsed_offer.max_parse_date.FromDatetime(max(parse_date, parsed_offer.max_parse_date.ToDatetime()))
