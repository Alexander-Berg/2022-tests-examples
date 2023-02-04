import base64
import datetime
import decimal
import json

import pytest

from intranet.trip.src.api.schemas import (
    AviaSearchFilter,
    HotelSearchFilter,
    RailSearchFilter,
    SearchAviaRequestIn,
    SearchHotelRequestIn,
    SearchRailRequestIn,
)
from intranet.trip.src.api.schemas.provider.search import (
    AviaDetailResponse,
    AviaLocation,
    AviaSearchInfo,
    AviaSearchResult,
    BaseFlight,
    BaseInfo,
    Flight,
    FlightBaggage,
    GeoPosition,
    Hotel,
    HotelDetail,
    HotelDetailResponse,
    HotelLocation,
    HotelSearchInfo,
    HotelSearchResult,
    Loc,
    ProviderLeg,
    ProviderSearchResultCount,
    ProviderSegment,
    RailDetailResponse,
    RailLocation,
    RailSearchInfo,
    RailSearchResult,
    Room,
    Train,
    TrainCarriage,
    TrainCarriageDetail,
    TrainCarriagePlace,
    TransportBaggage,
)
from intranet.trip.src.config import settings
from intranet.trip.src.enums import ObjectType, SearchOrdering, SearchStatus, ServiceType
from intranet.trip.src.lib import aviacenter
from intranet.trip.src.lib.aviacenter.enums import (
    AviaClassType,
    CabinGender,
    SortDirection,
    TrainCarType,
    TrainCategory,
    TrainPlaceType,
    TrainSortField,
)
from intranet.trip.src.lib.aviacenter.models import (
    AviaSearchRequest,
    AviaSearchSegment,
    HotelSearchGuest,
    HotelSearchParams,
    HotelSearchRequest,
    HotelSearchRoom,
    TimeInterval,
    TrainSearchRequest,
    TrainSort,
)
from intranet.trip.src.logic.providers.aviacenter import converters

from .data import (
    TEST_AVIA_DETAIL_RESPONSE,
    TEST_AVIA_RESULTS_RESPONSE,
    TEST_HOTEL_DETAIL_RESPONSE,
    TEST_HOTEL_RESULTS_RESPONSE,
    TEST_RAIL_DETAIL_RESPONSE,
    TEST_RAIL_RESULTS_RESPONSE,
)


def test_rail_key_converter_in():
    key_data = {
        'train_number': '1',
        'departure_time': '2022-05-05 01:01',
        'test': 'test',
    }
    key = base64.b64encode(json.dumps(key_data).encode()).decode()

    converter = converters.AviacenterRailKeyConverterIn(lang='ru')
    result = converter.convert(key)

    expected_data = {
        'train_number': '1',
        'departure_time': '2022-05-05 01:01',
    }
    assert result == expected_data


def test_rail_search_request_converter_in():
    request_in = SearchRailRequestIn(
        from_id='1',
        to_id='2',
        departure_on=datetime.date(1900, 1, 1),
    )

    converter = converters.AviacenterRailSearchRequestConverterIn(lang='ru')
    result = converter.convert(request_in)

    assert isinstance(result, TrainSearchRequest)
    assert result.company_id == settings.AVIACENTER_COMPANY_ID
    assert result.from_id == 1
    assert result.to_id == 2
    assert result.date == datetime.date(1900, 1, 1)
    assert result.adult == 1
    assert result.infant == 0
    assert result.child == 0


def test_rail_search_results_count_converter_out():
    converter = converters.AviacenterRailSearchCountConverterOut(lang='ru')
    result = converter.convert(TEST_RAIL_RESULTS_RESPONSE)

    assert isinstance(result, ProviderSearchResultCount)
    assert result.service_type == ServiceType.rail
    assert result.count == TEST_RAIL_RESULTS_RESPONSE['total']


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_rail_search_converter_out(language):
    converter = converters.AviacenterRailSearchConverterOut(lang=language)

    result = converter.convert(TEST_RAIL_RESULTS_RESPONSE)

    assert isinstance(result, RailSearchResult)
    assert result.service_type == ServiceType.rail
    assert result.total == 20
    assert result.page == 2
    assert result.limit == 10

    assert len(result.data) == 1
    train = result.data[0]
    assert isinstance(train, Train)

    train_id_data = {
        'train_number': '002Э',
        'departure_time': '2022-05-25 00:35',
    }
    assert train.id == base64.b64encode(json.dumps(train_id_data).encode()).decode()

    assert train.train_name == 'Россия'
    assert train.train_number == '002Э'
    assert train.train_category == TrainCategory.fast.name

    departure_at = datetime.datetime(2022, 5, 25, 1, 35)
    assert train.departure_at == departure_at
    assert train.departure_at_utc == departure_at - datetime.timedelta(hours=4)
    arrival_at = datetime.datetime(2022, 6, 1, 1, 3)
    assert train.arrival_at == arrival_at
    assert train.arrival_at_utc == arrival_at - datetime.timedelta(hours=5)
    assert train.ride_duration == 9988

    assert isinstance(train.departure, RailLocation)
    assert isinstance(train.departure.city, Loc)
    assert train.departure.city.type == ObjectType.city
    assert train.departure.city.name == 'Москва'
    assert isinstance(train.departure.country, Loc)
    assert train.departure.country.type == ObjectType.country
    assert train.departure.country.name == 'RU'
    assert train.departure.train_station.type == ObjectType.train_station
    assert train.departure.train_station.name == 'Москва Ярославская'

    assert isinstance(train.arrival, RailLocation)
    assert isinstance(train.arrival.city, Loc)
    assert train.arrival.city.type == ObjectType.city
    assert train.arrival.city.name == 'Владивосток'
    assert isinstance(train.arrival.country, Loc)
    assert train.arrival.country.type == ObjectType.country
    assert train.arrival.country.name == 'RU'
    assert isinstance(train.arrival.train_station, Loc)
    assert train.arrival.train_station.type == ObjectType.train_station
    assert train.arrival.train_station.name == 'Владивосток'

    assert len(train.carriages) == 1
    carriage = train.carriages[0]
    assert isinstance(carriage, TrainCarriage)
    assert carriage.min_price == decimal.Decimal('10376.4')
    assert carriage.max_price == decimal.Decimal('10386.4')
    assert carriage.carriage_type == TrainCarType.reserved.name
    assert carriage.carriage_owner == 'ФПК'
    assert carriage.is_travel_policy_compliant is True
    assert carriage.place_count == 303
    assert carriage.travel_policy_violations == ['AAAAAA']


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_rail_search_filters_converter_in(language):
    converter = converters.AviacenterRailSearchFiltersConverterIn(lang=language)
    search_filter = RailSearchFilter(
        is_restricted_by_travel_policy=False,
        train_names=['Lapochka'],
        train_categories=['high_speed'],
        carriage_types=['lux'],
        departure_from_there=['1'],
        arrival_to_there=['2'],
        departure_time_from=datetime.time(0, 0),
        departure_time_to=datetime.time(1, 1),
        arrival_time_from=datetime.time(2, 2),
        arrival_time_to=datetime.time(3, 3),
        from_stations=[1],
        to_stations=[2],
        order_by=SearchOrdering.price,
        is_descending=False,
    )

    result = converter.convert(search_filter)
    expected_filter = aviacenter.TrainSearchFilter(
        is_restricted_by_travel_policy=0,
        from_stations=[1],
        to_stations=[2],
        car_groups=[TrainCarType.lux],
        train_names=['Lapochka'],
        train_categories=[TrainCategory.high_speed],
        departure_time_intervals=[
            TimeInterval(from_time=datetime.time(0, 0), to_time=datetime.time(1, 1)),
        ],
        arrival_time_intervals=[
            TimeInterval(from_time=datetime.time(2, 2), to_time=datetime.time(3, 3)),
        ],
        carriers=None,
        sort=[
            TrainSort(
                field=TrainSortField.price,
                direction=SortDirection.asc,
            )
        ],
    )
    assert result == expected_filter
    expected_dict = {
        'is_restricted_by_travel_policy': 0,
        'departure_time_intervals': [
            {
                'from': datetime.time(0, 0),
                'to': datetime.time(1, 1),
            },
        ],
        'arrival_time_intervals': [
            {
                'from': datetime.time(2, 2),
                'to': datetime.time(3, 3),
            },
        ],
        'carriers': None,
        'train_categories': ['Скоростной'],
        'car_groups': ['ЛЮКС'],
        'train_names': ['Lapochka'],
        'from': [1],
        'to': [2],
        'sort': [
            {
                'field': TrainSortField.price,
                'direction': SortDirection.asc,
            },
        ],
    }

    assert result.dict() == expected_dict


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_rail_search_info_converter_out(language):
    converter = converters.AviacenterRailSearchInfoConverterOut(lang=language)
    result = converter.convert(TEST_RAIL_RESULTS_RESPONSE)

    assert isinstance(result, RailSearchInfo)
    assert result.service_type == ServiceType.rail
    assert result.status == SearchStatus.completed
    assert result.departure_on == datetime.date(2022, 5, 25)

    loc_from = result.location_from
    assert isinstance(loc_from, RailLocation)
    assert isinstance(loc_from.city, Loc)
    assert loc_from.city.type == ObjectType.city
    assert loc_from.city.name == 'Москва'
    assert isinstance(loc_from.country, Loc)
    assert loc_from.country.type == ObjectType.country
    assert loc_from.country.name == 'RU'
    assert loc_from.train_station is None

    loc_to = result.location_to
    assert isinstance(loc_to, RailLocation)
    assert isinstance(loc_to.city, Loc)
    assert loc_to.city.type == ObjectType.city
    assert loc_to.city.name == 'Владивосток'
    assert isinstance(loc_to.country, Loc)
    assert loc_to.country.type == ObjectType.country
    assert loc_to.country.name == 'RU'
    assert loc_to.train_station is None


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_rail_detail_converter_out(language):
    converter_out = converters.AviacenterRailDetailConverterOut(language)
    result = converter_out.convert(TEST_RAIL_DETAIL_RESPONSE)

    assert isinstance(result, RailDetailResponse)
    assert not result.train
    assert isinstance(result.data, list)
    assert len(result.data) == 1

    carriage = result.data[0]
    assert isinstance(carriage, TrainCarriageDetail)
    assert carriage.has_electronic_registration is True
    assert carriage.min_price == decimal.Decimal('2940.1')
    assert carriage.max_price == decimal.Decimal('2950.1')
    assert carriage.carriage_type == TrainCarType.coupe.name
    assert carriage.carriage_owner == 'ТВЕРСК'
    assert carriage.place_count == 1
    assert carriage.is_travel_policy_compliant is False
    assert carriage.travel_policy_violations == ['Превышение длительности пребывания']
    assert carriage.carriage_number == 11
    assert carriage.service_class_code == '2Л'
    assert carriage.service_class_description == '4-х местные купе. В стоимость'
    assert carriage.services == [
        'air_conditioner',
        'bed',
        'bio_toilet',
        'multimedia_portal',
        'restaurant_car',
        'socket_220',
        'wifi',
    ]

    assert isinstance(carriage.places, list)
    assert len(carriage.places) == 1
    place = carriage.places[0]
    assert isinstance(place, TrainCarriagePlace)
    assert place.place_number == 10
    assert place.min_price == decimal.Decimal('2930.1')
    assert place.max_price == decimal.Decimal('2980.1')
    assert place.compartment_number == 3
    assert place.compartment_gender == CabinGender.mixed.name
    assert place.place_type == TrainPlaceType.upper.name
    assert place.place_type_description == 'Верхнее'


def test_hotel_search_results_count_converter_out():
    converter = converters.AviacenterHotelSearchCountConverterOut(lang='ru')
    result = converter.convert(TEST_HOTEL_RESULTS_RESPONSE)

    assert isinstance(result, ProviderSearchResultCount)
    assert result.service_type == ServiceType.hotel
    assert result.count == 2


def test_hotel_search_request_converter_in():
    request_in = SearchHotelRequestIn(
        target_id='1',
        check_in_on=datetime.date(1900, 1, 1),
        check_out_on=datetime.date(1901, 1, 1),
    )
    converter = converters.AviacenterHotelSearchRequestConverterIn(lang='ru')
    result = converter.convert(request_in)

    assert isinstance(result, HotelSearchRequest)
    assert result.company_id == settings.AVIACENTER_COMPANY_ID
    assert isinstance(result.search, HotelSearchParams)
    assert result.search.city == 1
    assert result.search.check_in == datetime.date(1900, 1, 1)
    assert result.search.check_out == datetime.date(1901, 1, 1)
    assert isinstance(result.search.rooms, list)
    assert len(result.search.rooms) == 1
    room = result.search.rooms[0]
    assert isinstance(room, HotelSearchRoom)
    assert isinstance(room.guests, list)
    assert len(room.guests) == 1
    guest = room.guests[0]
    assert isinstance(guest, HotelSearchGuest)
    assert guest.citizenship == 'ru'


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_hotel_search_converter_out(language):
    converter = converters.AviacenterHotelSearchConverterOut(lang=language)
    result = converter.convert(TEST_HOTEL_RESULTS_RESPONSE)

    assert isinstance(result, HotelSearchResult)
    assert result.service_type == ServiceType.hotel
    assert result.total == 2
    assert result.page == 1
    assert result.limit == 20

    assert len(result.data) == 1
    hotel = result.data[0]
    assert isinstance(hotel, Hotel)

    hotel_id_data = {
        'search_id': 'some_hotel_id',
    }
    assert hotel.id == base64.b64encode(json.dumps(hotel_id_data).encode()).decode()
    assert hotel.hotel_name == 'Тестотель (Островок)'
    assert hotel.description == 'Плата за доступ в номерах'
    assert hotel.stars == 2
    assert hotel.image_url == 'some_url'
    assert hotel.currency == 'RUB'
    assert hotel.min_price_per_night == decimal.Decimal('487')
    assert hotel.address == 'Белогорск, Ордос 1'
    assert isinstance(hotel.geo_position, GeoPosition)
    assert hotel.geo_position.latitude == decimal.Decimal('50.921287')
    assert hotel.geo_position.longitude == decimal.Decimal('128.473881')

    assert isinstance(hotel.location, HotelLocation)
    assert isinstance(hotel.location.city, Loc)
    assert hotel.location.city.type == ObjectType.city
    assert hotel.location.city.name == 'Белогорск'
    assert isinstance(hotel.location.country, Loc)
    assert hotel.location.country.type == ObjectType.country
    assert hotel.location.country.name == 'Россия'
    assert hotel.is_recommended is None


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_hotel_search_filters_converter_in(language):
    converter = converters.AviacenterHotelSearchFiltersConverterIn(lang=language)
    search_filter = HotelSearchFilter(
        is_restricted_by_travel_policy=True,
        stars=['99'],
        price_from=1,
        price_to=2,
        hotel_types=['high_speed'],
        order_by=SearchOrdering.price,
        is_descending=False,
    )

    result = converter.convert(search_filter)
    expected_filter = aviacenter.HotelSearchFilter(
        is_restricted_by_travel_policy=True,
        min_price=1,
        max_price=2,
        stars=['99'],
        hotel_categories=['high_speed'],
        new_data_only=0,
        sort=[
            TrainSort(
                field=TrainSortField.price,
                direction=SortDirection.asc,
            )
        ],
    )

    assert result == expected_filter

    expected_dict = {
        'hotel_categories': ['high_speed'],
        'is_restricted_by_travel_policy': 1,
        'max_price': 2,
        'min_price': 1,
        'new_data_only': 0,
        'stars': ['99'],
        'sort': [
            {
                'field': TrainSortField.price,
                'direction': SortDirection.asc,
            },
        ],
    }

    assert result.dict() == expected_dict


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_hotel_search_info_converter_out(language):
    converter = converters.AviacenterHotelSearchInfoConverterOut(lang=language)
    result = converter.convert(TEST_HOTEL_RESULTS_RESPONSE)

    assert isinstance(result, HotelSearchInfo)
    assert result.service_type == ServiceType.hotel
    assert result.status == SearchStatus.completed

    assert result.check_in == datetime.date(2022, 6, 26)
    assert result.check_out == datetime.date(2022, 7, 1)

    assert isinstance(result.location, HotelLocation)
    assert isinstance(result.location.city, Loc)
    assert result.location.city.type == ObjectType.city
    assert result.location.city.name == 'Белогорск'
    assert isinstance(result.location.country, Loc)
    assert result.location.country.type == ObjectType.country
    assert result.location.country.name == 'Россия'


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_hotel_search_detail_converter_out(language):
    converter = converters.AviacenterHotelSearchDetailsConverterOut(lang=language)
    result = converter.convert(TEST_HOTEL_DETAIL_RESPONSE)

    assert isinstance(result, HotelDetailResponse)
    assert result.service_type == ServiceType.hotel

    assert isinstance(result.hotel, HotelDetail)
    assert result.hotel.hotel_name == 'Домик Спанч Боба test'
    assert result.hotel.stars == 0
    assert result.hotel.address == 'Белогорск, Набережная ул., 4'

    assert isinstance(result.hotel.geo_position, GeoPosition)
    assert result.hotel.geo_position.latitude == decimal.Decimal('38.5907739013')
    assert result.hotel.geo_position.longitude == decimal.Decimal('4.1785051625')

    assert isinstance(result.hotel.location, HotelLocation)
    assert isinstance(result.hotel.location.country, Loc)
    assert result.hotel.location.country.type == ObjectType.country
    assert result.hotel.location.country.name == 'Россия'
    assert isinstance(result.hotel.location.city, Loc)
    assert result.hotel.location.city.type == ObjectType.city
    assert result.hotel.location.city.name == 'Белогорск'

    assert result.hotel.is_recommended is None
    assert result.hotel.images == ['http://some.url']
    assert result.hotel.check_in == datetime.date(2022, 7, 4)
    assert result.hotel.check_out == datetime.date(2022, 7, 9)
    assert result.hotel.num_of_nights == 5
    assert result.hotel.website is None

    assert isinstance(result.data, list)
    assert len(result.data) == 1
    room = result.data[0]
    assert isinstance(room, Room)
    assert room.index == 0
    assert room.images == []
    assert room.description == ''
    assert room.name == 'Без категории, тип кровати может измениться'
    assert room.is_meal_included is True
    assert room.meal_names == ['Континентальный завтрак']
    assert room.is_travel_policy_compliant is False
    assert room.currency == 'RUB'
    assert room.price_total == decimal.Decimal('5822.0')
    assert room.price_per_night == decimal.Decimal('1164.4')
    assert room.is_booking_by_request is False


def test_avia_search_request_converter_in():
    request_in = SearchAviaRequestIn(
        from_id='1',
        to_id='2',
        departure_on=datetime.date(1900, 1, 1),
        departure_back_on=datetime.date(1901, 1, 1),
    )

    converter = converters.AviacenterAviaSearchRequestConverterIn(lang='ru')
    result = converter.convert(request_in)

    assert isinstance(result, AviaSearchRequest)
    assert result.company_id == settings.AVIACENTER_COMPANY_ID
    assert result.is_async == 1

    assert isinstance(result.segments, list)
    assert len(result.segments) == 2
    segment_to, segment_back = result.segments

    assert isinstance(segment_to, AviaSearchSegment)
    assert segment_to.from_id == '1'
    assert segment_to.to_id == '2'
    assert segment_to.date == datetime.date(1900, 1, 1)

    assert isinstance(segment_back, AviaSearchSegment)
    assert segment_back.from_id == '2'
    assert segment_back.to_id == '1'
    assert segment_back.date == datetime.date(1901, 1, 1)


def test_avia_search_results_count_converter_out():
    converter = converters.AviacenterAviaSearchCountConverterOut(lang='ru')
    result = converter.convert(TEST_AVIA_RESULTS_RESPONSE)

    assert isinstance(result, ProviderSearchResultCount)
    assert result.service_type == ServiceType.avia
    assert result.count == 265


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_avia_search_converter_out(language):
    converter = converters.AviacenterAviaSearchConverterOut(lang=language)
    result = converter.convert(TEST_AVIA_RESULTS_RESPONSE)

    assert isinstance(result, AviaSearchResult)
    assert result.service_type == ServiceType.avia
    assert result.total == 265
    assert result.page == 1
    assert result.limit == 20

    assert isinstance(result.data, list)
    assert len(result.data) == 1
    flight = result.data[0]

    assert isinstance(flight, Flight)
    flight_id_data = {'tid': 'some_id'}
    assert flight.id == base64.b64encode(json.dumps(flight_id_data).encode()).decode()
    assert flight.price == decimal.Decimal('440')
    assert flight.is_refundable is False
    assert flight.is_changeable is True
    assert flight.is_travel_policy_compliant is True

    assert isinstance(flight.legs, list)
    assert len(flight.legs) == 1
    leg = flight.legs[0]
    assert isinstance(leg, ProviderLeg)
    assert leg.segments_count == 1
    assert leg.route_duration == 110

    assert isinstance(leg.segments, list)
    assert len(leg.segments) == 1
    segment = leg.segments[0]
    assert isinstance(segment, ProviderSegment)
    assert segment.departure_at == datetime.datetime(2022, 6, 26, 12)
    assert segment.departure_at_utc == datetime.datetime(2022, 6, 26, 9)
    assert segment.departure_at_timezone_offset == 3
    assert segment.arrival_at == datetime.datetime(2022, 6, 26, 13, 40)
    assert segment.arrival_at_utc == datetime.datetime(2022, 6, 26, 10, 40)
    assert segment.arrival_at_timezone_offset == 3
    assert segment.seats == 9
    assert segment.flight_number == '295'
    assert segment.flight_duration == 100
    assert segment.transfer_duration == 0
    assert segment.comment == 'this is a comment'
    assert isinstance(segment.baggage, FlightBaggage)
    assert isinstance(segment.baggage.hand_baggage, TransportBaggage)
    assert segment.baggage.hand_baggage.weight == 10
    assert segment.baggage.hand_baggage.quantity == 1
    assert segment.baggage.baggage is None
    assert segment.flight_class == AviaClassType.economy

    assert isinstance(segment.carrier, BaseInfo)
    assert segment.carrier.type == ObjectType.carrier
    assert segment.carrier.id == '5N'
    assert segment.carrier.name == 'Smartavia'
    assert segment.fare_code == '9'  # FIXME ?

    assert isinstance(segment.aircraft, BaseInfo)
    assert segment.aircraft.type == ObjectType.aircraft
    assert segment.aircraft.id == '738'
    assert segment.aircraft.name == 'Boeing 737-800'


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_avia_search_filters_converter_in(language):
    converter = converters.AviacenterAviaSearchFiltersConverterIn(lang=language)

    search_filter = AviaSearchFilter(
        is_restricted_by_travel_policy=True,
        maximum_transfers_count=25,
        has_baggage=False,
        is_changeable=False,
        is_refundable=True,
        air_companies=['avia'],
        departure_time_from=datetime.time(0, 5),
        departure_time_to=datetime.time(0, 10),
        arrival_time_from=datetime.time(0, 15),
        arrival_time_to=datetime.time(0, 20),
        order_by=SearchOrdering.price,
        is_descending=False,
    )

    result = converter.convert(search_filter)

    expected_filter = aviacenter.AviaSearchFilter(
        is_refundable=1,
        is_exchangeable=0,
        is_restricted_by_travel_policy=1,
        has_baggage=0,
        maximum_transfers_count=25,
        segments=[
            aviacenter.AviaSearchFilterSegment(
                departure_time_intervals=[
                    TimeInterval(from_time=datetime.time(0, 5), to_time=datetime.time(0, 10)),
                ],
                arrival_time_intervals=[
                    TimeInterval(from_time=datetime.time(0, 15), to_time=datetime.time(0, 20)),
                ],
                from_id=None,  # FIXME
                to_id=None,  # FIXME
            ),
        ],
        carriers=['avia'],
        sort=[
            TrainSort(
                field=TrainSortField.price,
                direction=SortDirection.asc,
            )
        ],
    )

    assert result == expected_filter

    expected_dict = {
        'carriers': ['avia'],
        'has_baggage': 0,
        'is_exchangeable': 0,
        'is_refundable': 1,
        'is_restricted_by_travel_policy': 1,
        'maximum_transfers_count': 25,
        'segments': [
            {
                'arrival_time_intervals': [
                    {'from': datetime.time(0, 15), 'to': datetime.time(0, 20)},
                ],
                'departure_time_intervals': [
                    {'from': datetime.time(0, 5), 'to': datetime.time(0, 10)},
                ],
                'from': None,
                'to': None,
            },
        ],
        'sort': [
            {
                'field': TrainSortField.price,
                'direction': SortDirection.asc,
            },
        ],
    }

    assert result.dict() == expected_dict


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_avia_search_info_converter_out(language):
    converter = converters.AviacenterAviaSearchInfoConverterOut(lang=language)
    result = converter.convert(TEST_AVIA_RESULTS_RESPONSE)

    assert isinstance(result, AviaSearchInfo)
    assert result.service_type == ServiceType.avia
    assert result.departure_on == datetime.date(2022, 6, 26)
    assert result.departure_back_on is None

    loc_from = result.location_from
    assert isinstance(loc_from, AviaLocation)
    assert isinstance(loc_from.city, Loc)
    assert loc_from.city.type == ObjectType.city
    assert loc_from.city.name == 'Москва'
    assert loc_from.country.type == ObjectType.country
    assert loc_from.country.name == 'Россия'
    assert loc_from.airport is None
    assert loc_from.terminal is None

    loc_to = result.location_to
    assert isinstance(loc_to, AviaLocation)
    assert isinstance(loc_to.city, Loc)
    assert loc_to.city.type == ObjectType.city
    assert loc_to.city.name == 'Санкт-Петербург'
    assert loc_to.country.type == ObjectType.country
    assert loc_to.country.name == 'Россия'
    assert loc_to.airport is None
    assert loc_to.terminal is None


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_avia_detail_converter_out(language):
    converter_out = converters.AviacenterAviaDetailConverterOut(language)
    result = converter_out.convert(TEST_AVIA_DETAIL_RESPONSE)

    assert isinstance(result, AviaDetailResponse)
    assert result.service_type == ServiceType.avia
    assert isinstance(result.flight, BaseFlight)
    assert result.flight.is_travel_policy_compliant is True
    assert result.flight.is_changeable is True
    assert result.flight.is_refundable is False
    assert result.flight.price == decimal.Decimal('440')
    assert isinstance(result.data, list)
    assert len(result.data) == 2

    to_spb = result.data[0]
    assert isinstance(to_spb, ProviderLeg)
    assert to_spb.segments_count == 1
    assert to_spb.route_duration == 100

    assert isinstance(to_spb.segments, list)
    assert len(to_spb.segments) == 1

    segment = to_spb.segments[0]
    assert isinstance(segment, ProviderSegment)

    assert isinstance(segment.departure, AviaLocation)
    assert isinstance(segment.departure.city, Loc)
    assert segment.departure.city.type == ObjectType.city
    assert segment.departure.city.name == 'Москва'
    assert segment.departure.country.type == ObjectType.country
    assert segment.departure.country.name == 'Россия'

    assert isinstance(segment.departure.airport, Loc)
    assert segment.departure.airport.type == ObjectType.airport
    assert segment.departure.airport.name == 'Домодедово'
    assert isinstance(segment.departure.terminal, Loc)
    assert segment.departure.terminal.type == ObjectType.terminal
    assert segment.departure.terminal.name == ''

    assert segment.departure_at == datetime.datetime(2022, 7, 2, 12)
    assert segment.departure_at_utc == datetime.datetime(2022, 7, 2, 9)
    assert segment.departure_at_timezone_offset == 3

    assert isinstance(segment.arrival, AviaLocation)
    assert isinstance(segment.arrival.city, Loc)
    assert segment.arrival.city.type == ObjectType.city
    assert segment.arrival.city.name == 'Санкт-Петербург'
    assert segment.arrival.country.type == ObjectType.country
    assert segment.arrival.country.name == 'Россия'

    assert isinstance(segment.arrival.airport, Loc)
    assert segment.arrival.airport.type == ObjectType.airport
    assert segment.arrival.airport.name == 'Пулково'
    assert isinstance(segment.arrival.terminal, Loc)
    assert segment.arrival.terminal.type == ObjectType.terminal
    assert segment.arrival.terminal.name == '1'

    assert segment.arrival_at == datetime.datetime(2022, 7, 2, 13, 40)
    assert segment.arrival_at_utc == datetime.datetime(2022, 7, 2, 10, 40)
    assert segment.arrival_at_timezone_offset == 3

    assert segment.seats == 9
    assert segment.flight_number == '295'
    assert segment.flight_duration == 100
    assert segment.transfer_duration == 0
    assert segment.comment == 'this is a comment'
    assert segment.flight_class == 'E'  # FIXME

    assert isinstance(segment.baggage, FlightBaggage)
    assert isinstance(segment.baggage.hand_baggage, TransportBaggage)
    assert segment.baggage.hand_baggage.weight == 10
    assert segment.baggage.hand_baggage.quantity == 1
    assert segment.baggage.baggage is None

    assert isinstance(segment.carrier, BaseInfo)
    assert segment.carrier.type == ObjectType.carrier
    assert segment.carrier.id == '5N'
    assert segment.carrier.name == 'Smartavia'
    assert segment.fare_code == '9'

    assert isinstance(segment.aircraft, BaseInfo)
    assert segment.aircraft.type == ObjectType.aircraft
    assert segment.aircraft.id == '738'
    assert segment.aircraft.name == 'Boeing 737-800'

    to_msk = result.data[1]
    assert isinstance(to_msk, ProviderLeg)
    assert to_msk.segments_count == 1
    assert to_msk.route_duration == 100

    assert isinstance(to_msk.segments, list)
    assert len(to_msk.segments) == 1

    other_segment = to_msk.segments[0]
    assert isinstance(other_segment, ProviderSegment)

    assert other_segment.departure == segment.arrival
    assert other_segment.departure_at == datetime.datetime(2022, 7, 7, 14, 40)
    assert other_segment.departure_at_utc == datetime.datetime(2022, 7, 7, 11, 40)
    assert other_segment.departure_at_timezone_offset == 3

    assert other_segment.arrival == segment.departure
    assert other_segment.arrival_at == datetime.datetime(2022, 7, 7, 16, 20)
    assert other_segment.arrival_at_utc == datetime.datetime(2022, 7, 7, 13, 20)
    assert other_segment.arrival_at_timezone_offset == 3

    assert other_segment.seats == 9
    assert other_segment.flight_number == '296'
    assert other_segment.flight_duration == 100
    assert other_segment.transfer_duration == 0
    assert other_segment.comment == 'this is some other comment'
    assert other_segment.flight_class == AviaClassType.economy

    assert isinstance(other_segment.baggage, FlightBaggage)
    assert other_segment.baggage.baggage is None
    assert other_segment.baggage.hand_baggage is None

    assert isinstance(other_segment.carrier, BaseInfo)
    assert other_segment.carrier.type == ObjectType.carrier
    assert other_segment.carrier.id == '5N'
    assert other_segment.carrier.name == 'Smartavia'
    assert other_segment.fare_code == '9'

    assert isinstance(other_segment.aircraft, BaseInfo)
    assert other_segment.aircraft.type == ObjectType.aircraft
    assert other_segment.aircraft.id == '738'
    assert other_segment.aircraft.name == 'Boeing 737-800'
