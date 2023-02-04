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
    BaseTrain,
    FilterItem,
    FilterSelectValue,
    FilterValueType,
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
from intranet.trip.src.lib.aeroclub import SearchFilter
from intranet.trip.src.lib.aeroclub.enums import (
    ACServiceType,
    AviaCabinClassType,
    AviaRules,
    HotelConfirmationType,
    HotelPaymentPlace,
    SearchResultsOrdering,
    Time,
    TrainCarriageType,
    TrainCategory,
    TrainCompartmentGender,
)
from intranet.trip.src.lib.aeroclub.models import SearchOptionIn, SearchRequestIn
from intranet.trip.src.logic.providers.aeroclub import converters

from .data import (
    TEST_AVIA_DETAIL_RESPONSE,
    TEST_AVIA_FILTERS_RESPONSE,
    TEST_AVIA_RESULTS_RESPONSE,
    TEST_AVIA_SEARCH_INFO_RESPONSE,
    TEST_COUNT_RESPONSE,
    TEST_HOTEL_DETAIL_RESPONSE,
    TEST_HOTEL_FILTERS_RESPONSE,
    TEST_HOTEL_RESULTS_RESPONSE,
    TEST_HOTEL_SEARCH_INFO_RESPONSE,
    TEST_RAIL_DETAIL_RESPONSE,
    TEST_RAIL_FILTERS_RESPONSE,
    TEST_RAIL_RESULTS_RESPONSE,
    TEST_RAIL_SEARCH_INFO_RESPONSE,
)


def test_key_converter_in():
    key_data = {
        'key': 'somekey==',
        'option_number': 1,
    }
    key = base64.b64encode(json.dumps(key_data).encode()).decode()

    converter = converters.AeroclubKeyConverterIn(lang='ru')
    result = converter.convert(key)

    expected_data = {
        'key': 'somekey==',
        'option_id': 1,
    }
    assert result == expected_data


def test_rail_search_request_converter_in():

    request_in = SearchRailRequestIn(
        from_id='1',
        to_id='2',
        departure_on=datetime.date(1900, 1, 1),
    )

    converter = converters.AeroclubRailSearchRequestConverterIn(lang='ru')
    result = converter.convert(request_in)

    assert isinstance(result, SearchRequestIn)
    assert result.departure_city_id == 1
    assert result.arrival_city_id == 2
    assert result.departure_on == datetime.date(1900, 1, 1)
    assert result.round_trip_departure_on is None
    assert result.profile_id == settings.AEROCLUB_PROFILE_ID

    assert isinstance(result.options, list)
    assert len(result.options) == 1
    option = result.options[0]
    assert isinstance(option, SearchOptionIn)
    assert option.service_type == ACServiceType.rail
    assert option.comment == ''
    assert option.departure_city_id == 1
    assert option.arrival_city_id == 2
    assert option.checkin_on is None
    assert option.checkout_on is None
    assert option.departure_on == datetime.date(1900, 1, 1)
    assert option.number == 1
    assert option.round_trip_departure_on is None
    assert option.search_mode is None


def test_rail_search_results_count_converter_out():
    converter = converters.AeroclubRailSearchCountConverterOut(lang='ru')
    result = converter.convert(TEST_COUNT_RESPONSE)

    assert isinstance(result, ProviderSearchResultCount)
    assert result.service_type == ServiceType.rail
    assert result.count == 10


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_rail_search_converter_out(language):
    converter = converters.AeroclubRailSearchConverterOut(lang=language)
    result = converter.convert(TEST_RAIL_RESULTS_RESPONSE)

    assert isinstance(result, RailSearchResult)
    assert result.service_type == ServiceType.rail
    assert result.total == 2
    assert result.page == 1
    assert result.limit == 20

    assert len(result.data) == 1
    train = result.data[0]
    assert isinstance(train, Train)
    train_id_data = {
        'key': 'somekey==',
        'option_number': 1,
    }
    assert train.id == base64.b64encode(json.dumps(train_id_data).encode()).decode()
    assert train.train_name == 'Россия'
    assert train.train_number == '002Э'
    assert train.train_category is None
    assert train.departure_at == datetime.datetime(2022, 6, 10, 0, 35)
    assert train.departure_at_utc == datetime.datetime(2022, 6, 9, 21, 35)
    assert train.arrival_at == datetime.datetime(2022, 6, 17, 6, 3)
    assert train.arrival_at_utc == datetime.datetime(2022, 6, 16, 20, 3)
    assert train.ride_duration == 9988

    assert isinstance(train.departure, RailLocation)
    assert isinstance(train.departure.city, Loc)
    assert train.departure.city.type == ObjectType.city
    assert train.departure.city.name == {'ru': 'Москва', 'en': 'Moscow'}[language]
    assert isinstance(train.departure.country, Loc)
    assert train.departure.country.type == ObjectType.country
    assert train.departure.country.name == {'ru': 'Россия', 'en': 'Russian Federation'}[language]
    assert isinstance(train.departure.train_station, Loc)
    assert train.departure.train_station.type == ObjectType.train_station
    assert train.departure.train_station.name == {
        'ru': 'Москва-Ярославская',
        'en': 'Moscow Yaroslavskaya',
    }[language]

    assert isinstance(train.arrival, RailLocation)
    assert isinstance(train.arrival.city, Loc)
    assert train.arrival.city.type == ObjectType.city
    assert train.arrival.city.name == {'ru': 'Владивосток', 'en': 'Vladivostok'}[language]
    assert isinstance(train.arrival.country, Loc)
    assert train.arrival.country.type == ObjectType.country
    assert train.arrival.country.name == {'ru': 'Россия', 'en': 'Russian Federation'}[language]
    assert isinstance(train.arrival.train_station, Loc)
    assert train.arrival.train_station.type == ObjectType.train_station
    assert train.arrival.train_station.name == {'ru': 'Владивосток', 'en': 'Vladivostok'}[language]

    assert len(train.carriages) == 1
    carriage = train.carriages[0]
    assert isinstance(carriage, TrainCarriage)
    assert carriage.min_price == decimal.Decimal('10636.4')
    assert carriage.carriage_type == TrainCarriageType.reserved.name
    assert carriage.carriage_owner == 'ФПК'
    assert carriage.is_travel_policy_compliant is True
    assert carriage.place_count == 302
    assert carriage.travel_policy_violations == ['AAAAA']


def test_rail_search_filters_converter_in():
    converter = converters.AeroclubRailSearchFiltersConverterIn(lang='ru')
    search_filter = RailSearchFilter(
        is_restricted_by_travel_policy=False,
        train_names=['Lapochka'],
        train_categories=['high_speed'],
        carriage_types=['vip'],
        is_brand_train=True,
        has_electronic_registration=False,
        departure_there_timespan=['Night'],
        arrival_there_timespan=['Morning'],
        departure_from_there=['1'],
        arrival_to_there=['2'],
        order_by=SearchOrdering.price,
        is_descending=False,
    )

    result = converter.convert(search_filter)
    expected_filter = SearchFilter(
        departureAtThereTimes=[Time.night],
        arrivalAtThereTimes=[Time.morning],
        departureFromThere=['1'],
        arrivalToThere=['2'],
        IsTravelPolicyCompliant=True,
        railFilter_carriageTypes=[TrainCarriageType.vip],
        railFilter_trainName=['Lapochka'],
        railFilter_trainCategories=[TrainCategory.high_speed],
        railFilter_electronicRegistration=False,
        railFilter_branded=True,
        orderBy=SearchResultsOrdering.price,
        isDescending=False,
    )

    assert result == expected_filter


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_rail_search_filters_converter_out(language):

    timespan_values = [
        FilterSelectValue(target_id='Morning', caption={'ru': 'Утро', 'en': 'Morning'}[language]),
        FilterSelectValue(
            target_id='Afternoon',
            caption={'ru': 'День', 'en': 'Afternoon'}[language],
        ),
        FilterSelectValue(target_id='Evening', caption={'ru': 'Вечер', 'en': 'Evening'}[language]),
        FilterSelectValue(target_id='Night', caption={'ru': 'Ночь', 'en': 'Night'}[language]),
    ]

    order_by_values = [
        FilterSelectValue(
            target_id=SearchOrdering.price,
            caption={
                'ru': 'По цене',
                'en': 'By price',
            }[language],
        ),
        FilterSelectValue(
            target_id=SearchOrdering.duration,
            caption={
                'ru': 'По длительности',
                'en': 'By duration',
            }[language],
        ),
        FilterSelectValue(
            target_id=SearchOrdering.departure_time,
            caption={
                'ru': 'По времени начала',
                'en': 'By time',
            }[language],
        ),
    ]

    expected_filters = {
        'departure_there_timespan': FilterItem(
            name='departure_there_timespan',
            type=FilterValueType.multiselect,
            values=timespan_values,
        ),
        'arrival_there_timespan': FilterItem(
            name='arrival_there_timespan',
            type=FilterValueType.multiselect,
            values=timespan_values,
        ),
        'carriers': FilterItem(
            name='carriers',
            type=FilterValueType.multiselect,
            values=[
                FilterSelectValue(
                    target_id='ТВЕРСК',
                    caption='ТВЕРСК',
                ),
            ],
        ),
        'departure_from_there': FilterItem(
            name='departure_from_there',
            type='multiselect',
            values=[
                FilterSelectValue(
                    target_id='s285227',
                    caption={
                        'ru': 'Москва, Восточный',
                        'en': 'Moscow, Vostochny'
                    }[language],
                ),
            ],
        ),
        'arrival_to_there': FilterItem(
            name='arrival_to_there',
            type=FilterValueType.multiselect,
            values=[
                FilterSelectValue(
                    target_id='s20418',
                    caption={
                        'ru': 'Санкт-Петербург',
                        'en': 'Saint Petersburg',
                    }[language],
                ),
            ],
        ),
        'carriage_types': FilterItem(
            name='carriage_types',
            type=FilterValueType.multiselect,
            values=[
                FilterSelectValue(
                    target_id='reserved',
                    caption={'ru': 'Плацкарт', 'en': 'Reserved'}[language]),
                FilterSelectValue(
                    target_id='coupe',
                    caption={'ru': 'Купе', 'en': 'Coupe'}[language],
                ),
            ],
        ),
        'train_categories': FilterItem(
            name='train_categories',
            type=FilterValueType.multiselect,
            values=[
                FilterSelectValue(
                    target_id='fast',
                    caption={'ru': 'Скорый', 'en': 'Fast'}[language],
                ),
            ],
        ),
        'train_names': FilterItem(
            name='train_names',
            type=FilterValueType.multiselect,
            values=[
                FilterSelectValue(
                    target_id='0KHQkNCf0KHQkNCd',
                    caption='САПСАН',
                ),
            ],
        ),
        'order_by': FilterItem(
            name='order_by',
            type=FilterValueType.select,
            values=order_by_values,
        ),
        'is_descending': FilterItem(
            name='is_descending',
            type=FilterValueType.boolean,
            values=None,
        ),
    }

    converter_out = converters.AeroclubRailSearchFiltersConverterOut(language)
    result = converter_out.convert(TEST_RAIL_FILTERS_RESPONSE)

    assert len(result) == len(expected_filters.keys())
    for result_filter in result:
        assert result_filter.name in expected_filters
        expected_filter = expected_filters[result_filter.name]
        assert result_filter.name == expected_filter.name
        assert result_filter.type == expected_filter.type
        assert result_filter.values == expected_filter.values


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_rail_search_info_converter_out(language):
    converter = converters.AeroclubRailSearchInfoConverterOut(lang=language)
    result = converter.convert(TEST_RAIL_SEARCH_INFO_RESPONSE)

    assert isinstance(result, RailSearchInfo)
    assert result.service_type == ServiceType.rail
    assert result.status == SearchStatus.completed
    assert result.departure_on == datetime.date(2022, 5, 30)

    loc_from = result.location_from
    assert isinstance(loc_from, RailLocation)
    assert isinstance(loc_from.city, Loc)
    assert loc_from.city.type == ObjectType.city
    assert loc_from.city.name == {'ru': 'Москва', 'en': 'Moscow'}[language]
    assert loc_from.country.type == ObjectType.country
    assert loc_from.country.name == {'ru': 'Россия', 'en': 'Russian Federation'}[language]
    assert loc_from.train_station is None

    loc_to = result.location_to
    assert isinstance(loc_to, RailLocation)
    assert isinstance(loc_to.city, Loc)
    assert loc_to.city.type == ObjectType.city
    assert loc_to.city.name == {'ru': 'Минск', 'en': 'Minsk'}[language]
    assert loc_to.country.type == ObjectType.country
    assert loc_to.country.name == {'ru': 'Беларусь', 'en': 'Belarus'}[language]
    assert loc_to.train_station is None


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_rail_detail_converter_out(language):
    service_class = {'ru': 'Русское', 'en': 'English'}

    converter_out = converters.AeroclubRailDetailConverterOut(language)
    result = converter_out.convert(TEST_RAIL_DETAIL_RESPONSE)

    assert isinstance(result, RailDetailResponse)
    train = result.train
    assert isinstance(train, BaseTrain)
    assert train.train_name == 'САПСАН'
    assert train.train_number == '778А'
    assert train.train_category is None
    assert train.departure_at == datetime.datetime(2022, 6, 13, 17, 40)
    assert train.departure_at_utc == datetime.datetime(2022, 6, 13, 14, 40)
    assert train.arrival_at == datetime.datetime(2022, 6, 13, 21, 35)
    assert train.arrival_at_utc == datetime.datetime(2022, 6, 13, 18, 35)
    assert train.ride_duration == 235

    assert isinstance(result.data, list)
    assert len(result.data) == 1
    carriage = result.data[0]
    assert isinstance(carriage, TrainCarriageDetail)
    assert carriage.has_electronic_registration is True
    assert carriage.min_price == decimal.Decimal('18754.4')
    assert carriage.max_price == decimal.Decimal('18764.4')
    assert carriage.carriage_type == TrainCarriageType.lux.name
    assert carriage.carriage_owner == 'ДОСС'
    assert carriage.place_count == 2
    assert carriage.is_travel_policy_compliant is False
    assert set(carriage.travel_policy_violations) == {
        'Разрешены вагоны классов купе, плацкартный, сидячий, общий',
        'Разрешен вагон класса Эконом и Эконом+',
    }
    assert carriage.carriage_number == 1
    assert carriage.service_class_code == '1Е'
    assert carriage.service_class_description == service_class[language]
    assert carriage.services == []

    assert isinstance(carriage.places, list)
    assert len(carriage.places) == 2

    places_gender = {
        21: TrainCompartmentGender.wo_desc.name,
        23: TrainCompartmentGender.male.name,
    }
    for place in carriage.places:
        assert isinstance(place, TrainCarriagePlace)
        assert place.min_price is None
        assert place.max_price is None
        assert place.compartment_gender == places_gender[place.place_number]
        assert place.place_type is None
        assert place.place_type_description is None
        assert place.compartment_number is None


def test_hotel_search_request_converter_in():

    request_in = SearchHotelRequestIn(
        target_id='1',
        check_in_on=datetime.date(1900, 1, 1),
        check_out_on=datetime.date(1901, 1, 1),
        search_mode=['CorporateAnSlow'],
    )

    converter = converters.AeroclubHotelSearchRequestConverterIn(lang='ru')
    result = converter.convert(request_in)

    assert isinstance(result, SearchRequestIn)
    assert result.departure_city_id == -1
    assert result.arrival_city_id == 1
    assert result.departure_on == datetime.date(1900, 1, 1)
    assert result.round_trip_departure_on == datetime.date(1901, 1, 1)
    assert result.profile_id == settings.AEROCLUB_PROFILE_ID

    assert isinstance(result.options, list)
    assert len(result.options) == 1
    option = result.options[0]
    assert isinstance(option, SearchOptionIn)
    assert option.service_type == ACServiceType.hotel
    assert option.comment == ''
    assert option.departure_city_id is None
    assert option.arrival_city_id == 1
    assert option.checkin_on == datetime.date(1900, 1, 1)
    assert option.checkout_on == datetime.date(1901, 1, 1)
    assert option.departure_on == datetime.date(1900, 1, 1)
    assert option.number == 1
    assert option.round_trip_departure_on == datetime.date(1901, 1, 1)
    assert option.search_mode == ['CorporateAnSlow']


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_hotel_search_results_count_converter_out(language):
    converter = converters.AeroclubHotelSearchCountConverterOut(lang=language)
    result = converter.convert(TEST_COUNT_RESPONSE)

    assert isinstance(result, ProviderSearchResultCount)
    assert result.service_type == ServiceType.hotel
    assert result.count == 10


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_hotel_search_converter_out(language):
    converter = converters.AeroclubHotelSearchConverterOut(lang=language)
    result = converter.convert(TEST_HOTEL_RESULTS_RESPONSE)

    assert isinstance(result, HotelSearchResult)
    assert result.service_type == ServiceType.hotel
    assert result.total == 20
    assert result.page == 1
    assert result.limit == 15

    assert isinstance(result.data, list)
    assert len(result.data) == 1
    hotel = result.data[0]

    assert isinstance(hotel, Hotel)
    hotel_id_data = {
        'key': 'somekey==',
        'option_number': 1,
    }
    assert hotel.id == base64.b64encode(json.dumps(hotel_id_data).encode()).decode()
    assert hotel.hotel_name == {'ru': 'Друзья', 'en': 'Druzya'}[language]
    assert hotel.description == {'ru': 'Описание', 'en': 'Eng'}[language]
    assert hotel.stars == 0
    assert hotel.image_url is None
    assert hotel.currency == 'RUB'
    assert hotel.min_price_per_night == decimal.Decimal('1204.0')
    assert hotel.address == {'ru': 'Санкт-Петербург', 'en': 'Spb'}[language]

    assert isinstance(hotel.geo_position, GeoPosition)
    assert hotel.geo_position.latitude == decimal.Decimal('59.93357467651367')
    assert hotel.geo_position.longitude == decimal.Decimal('30.36038589477539')

    assert isinstance(hotel.location, HotelLocation)
    assert isinstance(hotel.location.city, Loc)
    assert hotel.location.city.type == ObjectType.city
    assert hotel.location.city.name == {'ru': 'Спб', 'en': 'Spb'}[language]
    assert isinstance(hotel.location.country, Loc)
    assert hotel.location.country.type == ObjectType.country
    assert hotel.location.country.name == {'ru': 'Россия', 'en': 'Russian'}[language]
    assert hotel.is_recommended is None


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_hotel_search_filters_converter_in(language):
    converter = converters.AeroclubHotelSearchFiltersConverterIn(lang=language)
    search_filter = HotelSearchFilter(
        is_restricted_by_travel_policy=False,
        stars=['99'],
        price_from=1,
        price_to=2,
        hotel_types=['high_speed'],
        confirmation_types=[HotelConfirmationType.instant],
        is_recommended=False,
        hotel_name='Hotel',
        order_by=SearchOrdering.price,
        is_descending=False,
    )

    result = converter.convert(search_filter)
    expected_filter = SearchFilter(
        hotelFilter_stars=['99'],
        hotelFilter_priceFrom=1,
        hotelFilter_priceTo=2,
        hotelFilter_hotelTypes=['high_speed'],
        hotelFilter_paymentPlaces=[HotelPaymentPlace.agency],
        hotelFilter_confirmationTypes=[HotelConfirmationType.instant],
        hotelFilter_isRecommended=False,
        hotelName='Hotel',
        IsTravelPolicyCompliant=True,
        orderBy=SearchResultsOrdering.price,
        isDescending=False,
    )

    assert result == expected_filter


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_hotel_search_filters_converter_out(language):
    order_by_values = [
        FilterSelectValue(
            target_id=SearchOrdering.price,
            caption={
                'ru': 'По цене',
                'en': 'By price',
            }[language],
        ),
    ]
    expected_filters = {
        'is_travel_policy_compliant': FilterItem(
            name='is_travel_policy_compliant',
            type=FilterValueType.boolean,
            values=None,
        ),
        'is_recommended': FilterItem(
            name='is_recommended',
            type=FilterValueType.boolean,
            values=None
        ),
        'hotel_name': FilterItem(
            name='hotel_name',
            type=FilterValueType.string,
            values=None,
        ),
        'stars': FilterItem(
            name='stars',
            type='multiselect',
            values=[
                FilterSelectValue(
                    target_id='1',
                    caption='1',
                ),
                FilterSelectValue(
                    target_id='2',
                    caption='2',
                ),
                FilterSelectValue(
                    target_id='3',
                    caption='3',
                ),
                FilterSelectValue(
                    target_id='4',
                    caption='4',
                ),
                FilterSelectValue(
                    target_id='5',
                    caption='5',
                ),
            ],
        ),
        'payment_places': FilterItem(
            name='payment_places',
            type=FilterValueType.multiselect,
            values=[
                FilterSelectValue(
                    target_id=HotelPaymentPlace.check_in,
                    caption={
                        'ru': 'При регистрации',
                        'en': 'During check-in',
                    }[language],
                ),
                FilterSelectValue(
                    target_id=HotelPaymentPlace.agency,
                    caption={
                        'ru': 'Агентством',
                        'en': 'By agency',
                    }[language],
                ),
            ],
        ),
        'hotel_types': FilterItem(
            name='hotel_types',
            type=FilterValueType.multiselect,
            values=[
                FilterSelectValue(
                    target_id='7',
                    caption={'ru': 'Мотель', 'en': 'Motel'}[language]),
                FilterSelectValue(
                    target_id='22',
                    caption={'ru': 'Курорт', 'en': 'Resort'}[language],
                ),
            ],
        ),
        'confirmation_type': FilterItem(
            name='confirmation_type',
            type=FilterValueType.multiselect,
            values=[
                FilterSelectValue(
                    target_id='OnRequest',
                    caption={'ru': 'Под запрос', 'en': 'On request'}[language],
                ),
            ],
        ),
        'price_range': FilterItem(
            name='price_range',
            type=FilterValueType.multiselect,
            values=[
                FilterSelectValue(target_id='min', caption='1204,000'),
                FilterSelectValue(target_id='max', caption='64311,5900'),
            ],
        ),
        'order_by': FilterItem(
            name='order_by',
            type=FilterValueType.select,
            values=order_by_values,
        ),
        'is_descending': FilterItem(
            name='is_descending',
            type=FilterValueType.boolean,
            values=None,
        ),
    }

    converter_out = converters.AeroclubHotelSearchFiltersConverterOut(language)
    result = converter_out.convert(TEST_HOTEL_FILTERS_RESPONSE)

    result_filters = {}

    for result_filter in result:
        assert result_filter.name in expected_filters
        expected_filter = expected_filters[result_filter.name]
        assert result_filter.name == expected_filter.name
        assert result_filter.type == expected_filter.type
        assert result_filter.values == expected_filter.values
        result_filters[result_filter.name] = result_filter

    for expected_filter_name, expected_filter in expected_filters.items():
        assert expected_filter_name in result_filters
        result_filter = result_filters[expected_filter_name]
        assert expected_filter.name == result_filter.name
        assert expected_filter.type == result_filter.type
        assert expected_filter.values == result_filter.values


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_hotel_search_info_converter_out(language):
    converter = converters.AeroclubHotelSearchInfoConverterOut(lang=language)
    result = converter.convert(TEST_HOTEL_SEARCH_INFO_RESPONSE)

    assert isinstance(result, HotelSearchInfo)
    assert result.service_type == ServiceType.hotel
    assert result.status == SearchStatus.completed
    assert result.check_in == datetime.date(2022, 7, 25)
    assert result.check_out == datetime.date(2022, 7, 31)

    assert isinstance(result.location, HotelLocation)
    assert result.location.city
    assert isinstance(result.location.city, Loc)
    assert result.location.city.type == ObjectType.city
    assert result.location.city.name == {'ru': 'Минск', 'en': 'Minsk'}[language]
    assert result.location.country.type == ObjectType.country
    assert result.location.country.name == {'ru': 'Беларусь', 'en': 'Belarus'}[language]


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_hotel_search_detail_converter_out(language):
    converter = converters.AeroclubHotelDetailConverterOut(lang=language)
    result = converter.convert(TEST_HOTEL_DETAIL_RESPONSE)

    assert isinstance(result, HotelDetailResponse)
    assert result.service_type == ServiceType.hotel

    assert isinstance(result.hotel, HotelDetail)
    assert result.hotel.hotel_name == {'ru': 'Друзья', 'en': 'Druzya'}[language]
    assert result.hotel.stars == 0
    assert result.hotel.address == {'ru': 'Спб', 'en': 'Spb'}[language]

    assert isinstance(result.hotel.geo_position, GeoPosition)
    assert result.hotel.geo_position.latitude == decimal.Decimal('59.93357467651367')
    assert result.hotel.geo_position.longitude == decimal.Decimal('30.36038589477539')

    assert isinstance(result.hotel.location, HotelLocation)
    assert isinstance(result.hotel.location.country, Loc)
    assert result.hotel.location.country.type == ObjectType.country
    assert result.hotel.location.country.name == {'ru': 'Россия', 'en': 'Russian'}[language]
    assert isinstance(result.hotel.location.city, Loc)
    assert result.hotel.location.city.type == ObjectType.city
    assert result.hotel.location.city.name == {'ru': 'Санкт', 'en': 'Saint'}[language]
    assert result.hotel.is_recommended is None
    assert result.hotel.images == []
    assert result.hotel.check_in == datetime.date(2022, 7, 25)
    assert result.hotel.check_out == datetime.date(2022, 7, 31)
    assert result.hotel.num_of_nights == 6
    assert result.hotel.website is None

    assert isinstance(result.data, list)
    assert len(result.data) == 1
    room = result.data[0]
    assert isinstance(room, Room)
    assert room.index == 3
    assert room.images == []
    assert room.description == {'ru': 'описание', 'en': 'descr'}[language]
    assert room.name == 'койко'
    assert room.is_meal_included is False
    assert room.meal_names == []
    assert room.is_travel_policy_compliant is True
    assert room.currency == 'RUB'
    assert room.price_total == decimal.Decimal('4614.0')
    assert room.price_per_night == decimal.Decimal('769.0')
    assert room.is_booking_by_request is True


def test_avia_search_request_converter_in():
    request_in = SearchAviaRequestIn(
        from_id='1',
        to_id='2',
        departure_on=datetime.date(1900, 1, 1),
        departure_back_on=datetime.date(1901, 1, 1),
    )

    converter = converters.AeroclubAviaSearchRequestConverterIn(lang='ru')
    result = converter.convert(request_in)

    assert isinstance(result, SearchRequestIn)
    assert result.departure_city_id == 1
    assert result.arrival_city_id == 2
    assert result.departure_on == datetime.date(1900, 1, 1)
    assert result.round_trip_departure_on == datetime.date(1901, 1, 1)
    assert result.profile_id == settings.AEROCLUB_PROFILE_ID

    assert isinstance(result.options, list)
    assert len(result.options) == 1
    option = result.options[0]
    assert isinstance(option, SearchOptionIn)
    assert option.service_type == ACServiceType.avia
    assert option.comment == ''
    assert option.departure_city_id == 1
    assert option.arrival_city_id == 2
    assert option.checkin_on is None
    assert option.checkout_on is None
    assert option.departure_on == datetime.date(1900, 1, 1)
    assert option.number == 1
    assert option.round_trip_departure_on == datetime.date(1901, 1, 1)
    assert option.search_mode is None


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_avia_search_results_count_converter_out(language):
    converter = converters.AeroclubAviaSearchCountConverterOut(lang=language)
    result = converter.convert(TEST_COUNT_RESPONSE)

    assert isinstance(result, ProviderSearchResultCount)
    assert result.service_type == ServiceType.avia
    assert result.count == 10


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_avia_search_converter_out(language):
    converter = converters.AeroclubAviaSearchConverterOut(lang=language)
    result = converter.convert(TEST_AVIA_RESULTS_RESPONSE)

    assert isinstance(result, AviaSearchResult)
    assert result.service_type == ServiceType.avia
    assert result.total == 15
    assert result.page == 1
    assert result.limit == 20

    assert isinstance(result.data, list)
    assert len(result.data) == 1
    flight = result.data[0]

    assert isinstance(flight, Flight)
    flight_id_data = {
        'key': 'somekey==',
        'option_number': 1,
    }
    assert flight.id == base64.b64encode(json.dumps(flight_id_data).encode()).decode()
    assert flight.price == decimal.Decimal('685.0')
    assert flight.is_refundable is False
    assert flight.is_changeable is True
    assert flight.is_travel_policy_compliant is True

    assert isinstance(flight.legs, list)
    assert len(flight.legs) == 1
    leg = flight.legs[0]
    assert isinstance(leg, ProviderLeg)
    assert leg.segments_count == 1
    assert leg.route_duration == 85

    assert isinstance(leg.segments, list)
    assert len(leg.segments) == 1
    segment = leg.segments[0]
    assert isinstance(segment, ProviderSegment)
    assert segment.departure_at == datetime.datetime(2022, 7, 25, 10, 15)
    assert segment.departure_at_utc == datetime.datetime(2022, 7, 25, 7, 15)
    assert segment.departure_at_timezone_offset == 3
    assert segment.arrival_at == datetime.datetime(2022, 7, 25, 11, 40)
    assert segment.arrival_at_utc == datetime.datetime(2022, 7, 25, 8, 40)
    assert segment.arrival_at_timezone_offset == 3
    assert segment.seats == 9
    assert segment.flight_number == '105'
    assert segment.flight_duration is None  # FIXME ?
    assert segment.transfer_duration is None
    assert segment.comment is None
    assert isinstance(segment.baggage, FlightBaggage)
    assert segment.baggage.hand_baggage is None
    assert segment.baggage.baggage is None
    assert segment.flight_class == AviaCabinClassType.economy

    assert isinstance(segment.carrier, BaseInfo)
    assert segment.carrier.type == ObjectType.carrier
    assert segment.carrier.id == '4G'
    assert segment.carrier.name == {'ru': 'Газпром авиа', 'en': 'Gazpromavia'}[language]
    assert segment.fare_code == 'Y'

    assert segment.aircraft is None


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_avia_search_filters_converter_in(language):
    converter = converters.AeroclubAviaSearchFiltersConverterIn(lang=language)
    search_filter = AviaSearchFilter(
        is_restricted_by_travel_policy=True,
        without_transfer=False,
        maximum_transfers_count=25,
        has_baggage=False,
        is_changeable=False,
        is_refundable=True,
        departure_there_timespan=['Night'],
        arrival_there_timespan=['Morning'],
        departure_back_timespan=['Evening'],
        arrival_back_timespan=['Afternoon'],
        departure_from_there=['1'],
        arrival_to_there=['2'],
        departure_from_back=['3'],
        arrival_to_back=['4'],
        cabin_classes=['class'],
        air_companies=['avia'],
        order_by=SearchOrdering.price,
        is_descending=False,
    )

    result = converter.convert(search_filter)
    expected_filter = SearchFilter(
        IsTravelPolicyCompliant=False,
        departureAtThereTimes=[Time.night],
        arrivalAtThereTimes=[Time.morning],
        departureAtBackTimes=[Time.evening],
        arrivalAtBackTimes=[Time.afternoon],
        departureFromThere=['1'],
        arrivalToThere=['2'],
        departureFromBack=['3'],
        arrivalToBack=['4'],
        aviaFilter_airCompanies=['avia'],
        aviaFilter_classes=['class'],
        aviaFilter_transfer=None,
        aviaFilter_rules=[AviaRules.refundable],
        aviaFilter_baggage=False,
        orderBy=SearchResultsOrdering.price,
        isDescending=False,
    )

    assert result == expected_filter


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_avia_search_filters_converter_out(language):

    timespan_values = [
        FilterSelectValue(target_id='Morning', caption={'ru': 'Утро', 'en': 'Morning'}[language]),
        FilterSelectValue(
            target_id='Afternoon',
            caption={'ru': 'День', 'en': 'Afternoon'}[language],
        ),
        FilterSelectValue(target_id='Evening', caption={'ru': 'Вечер', 'en': 'Evening'}[language]),
        FilterSelectValue(target_id='Night', caption={'ru': 'Ночь', 'en': 'Night'}[language]),
    ]
    order_by_values = [
        FilterSelectValue(
            target_id=SearchOrdering.price,
            caption={
                'ru': 'По цене',
                'en': 'By price',
            }[language],
        ),
        FilterSelectValue(
            target_id=SearchOrdering.duration,
            caption={
                'ru': 'По длительности',
                'en': 'By duration',
            }[language],
        ),
        FilterSelectValue(
            target_id=SearchOrdering.departure_time,
            caption={
                'ru': 'По времени начала',
                'en': 'By time',
            }[language],
        ),
    ]
    bool_filters = [
        'without_transfer',
        'has_baggage',
        'is_restricted_by_travel_policy',
        'is_changeable',
        'is_refundable',
        'is_descending',
    ]
    expected_filters = {
        name: FilterItem(name=name, type=FilterValueType.boolean, values=None)
        for name in bool_filters
    }
    expected_filters |= {
        'departure_there_timespan': FilterItem(
            name='departure_there_timespan',
            type=FilterValueType.multiselect,
            values=timespan_values,
        ),
        'arrival_there_timespan': FilterItem(
            name='arrival_there_timespan',
            type=FilterValueType.multiselect,
            values=timespan_values,
        ),
        'departure_back_timespan': FilterItem(
            name='departure_back_timespan',
            type=FilterValueType.multiselect,
            values=timespan_values,
        ),
        'arrival_back_timespan': FilterItem(
            name='arrival_back_timespan',
            type=FilterValueType.multiselect,
            values=timespan_values,
        ),
        'air_companies': FilterItem(
            name='air_companies',
            type=FilterValueType.multiselect,
            values=[
                FilterSelectValue(
                    target_id='SU',
                    caption={'ru': 'Аэрофлот', 'en': 'Aeroflot'}[language],
                ),
            ],
        ),
        'departure_from_there': FilterItem(
            name='departure_from_there',
            type='multiselect',
            values=[
                FilterSelectValue(
                    target_id='a39780',
                    caption={
                        'ru': 'Внуково',
                        'en': 'Vnukovo',
                    }[language],
                ),
            ],
        ),
        'arrival_to_there': FilterItem(
            name='arrival_to_there',
            type=FilterValueType.multiselect,
            values=[
                FilterSelectValue(
                    target_id='a37081',
                    caption={
                        'ru': 'Минск, Интернэйшнл, MSQ',
                        'en': 'Minsk, Minsk 2, MSQ',
                    }[language],
                ),
            ],
        ),
        'cabin_classes': FilterItem(
            name='cabin_classes',
            type=FilterValueType.multiselect,
            values=[
                FilterSelectValue(
                    target_id='Business',
                    caption={'ru': 'Бизнес', 'en': 'Business'}[language],
                ),
                FilterSelectValue(
                    target_id='Econom',
                    caption={'ru': 'Эконом', 'en': 'Econom'}[language],
                ),
            ],
        ),
        'order_by': FilterItem(
            name='order_by',
            type=FilterValueType.select,
            values=order_by_values,
        ),
    }

    converter_out = converters.AeroclubAviaSearchFiltersConverterOut(language)
    result = converter_out.convert(TEST_AVIA_FILTERS_RESPONSE)

    assert len(result) == len(expected_filters.keys())
    for result_filter in result:
        assert result_filter.name in expected_filters
        expected_filter = expected_filters[result_filter.name]
        assert result_filter.name == expected_filter.name
        assert result_filter.type == expected_filter.type
        assert result_filter.values == expected_filter.values


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_avia_search_info_converter_out(language):
    converter = converters.AeroclubAviaSearchInfoConverterOut(lang=language)
    result = converter.convert(TEST_AVIA_SEARCH_INFO_RESPONSE)

    assert isinstance(result, AviaSearchInfo)
    assert result.service_type == ServiceType.avia
    assert result.status == SearchStatus.completed
    assert result.departure_on == datetime.date(2022, 7, 25)
    assert result.departure_back_on == datetime.date(2022, 7, 26)

    loc_from = result.location_from
    assert isinstance(loc_from, AviaLocation)
    assert isinstance(loc_from.city, Loc)
    assert loc_from.city.type == ObjectType.city
    assert loc_from.city.name == {'ru': 'Москва', 'en': 'Moscow'}[language]
    assert loc_from.country.type == ObjectType.country
    assert loc_from.country.name == {'ru': 'Россия', 'en': 'Russian Federation'}[language]
    assert loc_from.airport is None
    assert loc_from.terminal is None

    loc_to = result.location_to
    assert isinstance(loc_to, AviaLocation)
    assert isinstance(loc_to.city, Loc)
    assert loc_to.city.type == ObjectType.city
    assert loc_to.city.name == {'ru': 'Минск', 'en': 'Minsk'}[language]
    assert loc_to.country.type == ObjectType.country
    assert loc_to.country.name == {'ru': 'Беларусь', 'en': 'Belarus'}[language]
    assert loc_to.airport is None
    assert loc_to.terminal is None


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_avia_detail_converter_out(language):
    converter_out = converters.AeroClubAviaDetailConverterOut(language)
    result = converter_out.convert(TEST_AVIA_DETAIL_RESPONSE)

    assert isinstance(result, AviaDetailResponse)
    assert result.service_type == ServiceType.avia
    assert isinstance(result.flight, BaseFlight)
    assert result.flight.is_travel_policy_compliant is True
    assert result.flight.is_changeable is False
    assert result.flight.is_refundable is False
    assert result.flight.price == decimal.Decimal('1170.0')
    assert isinstance(result.data, list)
    assert len(result.data) == 2

    to_spb = result.data[0]
    assert isinstance(to_spb, ProviderLeg)
    assert to_spb.segments_count == 1
    assert to_spb.route_duration == 85

    assert isinstance(to_spb.segments, list)
    assert len(to_spb.segments) == 1

    segment = to_spb.segments[0]
    assert isinstance(segment, ProviderSegment)

    assert isinstance(segment.departure, AviaLocation)
    assert isinstance(segment.departure.city, Loc)
    assert segment.departure.city.type == ObjectType.city
    assert segment.departure.city.name == {'ru': 'Москва', 'en': 'Moscow'}[language]
    assert segment.departure.country.type == ObjectType.country
    assert segment.departure.country.name == {'ru': 'Россия', 'en': 'Russian Federation'}[language]

    assert isinstance(segment.departure.airport, Loc)
    assert segment.departure.airport.type == ObjectType.airport
    assert segment.departure.airport.name == {'ru': 'Домодедово', 'en': 'Domodedovo'}[language]
    assert segment.departure.terminal is None

    assert segment.departure_at == datetime.datetime(2022, 7, 25, 10, 15)
    assert segment.departure_at_utc == datetime.datetime(2022, 7, 25, 7, 15)
    assert segment.departure_at_timezone_offset == 3

    assert isinstance(segment.arrival, AviaLocation)
    assert isinstance(segment.arrival.city, Loc)
    assert segment.arrival.city.type == ObjectType.city
    assert segment.arrival.city.name == {'ru': 'Санкт', 'en': 'Saint'}[language]
    assert segment.arrival.country.type == ObjectType.country
    assert segment.arrival.country.name == {'ru': 'Россия', 'en': 'Russian Federation'}[language]

    assert isinstance(segment.arrival.airport, Loc)
    assert segment.arrival.airport.type == ObjectType.airport
    assert segment.arrival.airport.name == {'ru': 'Пулково', 'en': 'Pulkovo'}[language]
    assert segment.arrival.terminal is None

    assert segment.arrival_at == datetime.datetime(2022, 7, 25, 11, 40)
    assert segment.arrival_at_utc == datetime.datetime(2022, 7, 25, 8, 40)
    assert segment.arrival_at_timezone_offset == 3

    assert segment.seats == 9
    assert segment.flight_number == '105'
    assert segment.flight_duration is None
    assert segment.transfer_duration is None
    assert segment.comment is None
    assert segment.flight_class == 'Econom'

    assert isinstance(segment.baggage, FlightBaggage)
    assert isinstance(segment.baggage.baggage, TransportBaggage)
    assert segment.baggage.baggage.weight == 100
    assert segment.baggage.baggage.quantity == 15
    assert segment.baggage.hand_baggage is None

    assert isinstance(segment.carrier, BaseInfo)
    assert segment.carrier.type == ObjectType.carrier
    assert segment.carrier.id == '4G'
    assert segment.carrier.name == {'ru': 'Газпром авиа', 'en': 'Gazpromavia'}[language]

    assert isinstance(segment.aircraft, BaseInfo)
    assert segment.aircraft.type == ObjectType.aircraft
    assert segment.aircraft.id == '737'
    assert segment.aircraft.name == {'ru': 'Боинг 737', 'en': 'Boeing 737'}[language]
    assert segment.fare_code == 'Y'

    to_msk = result.data[1]
    assert isinstance(to_msk, ProviderLeg)
    assert to_msk.segments_count == 1
    assert to_msk.route_duration == 155

    assert isinstance(to_msk.segments, list)
    assert len(to_msk.segments) == 1

    other_segment = to_msk.segments[0]
    assert isinstance(other_segment, ProviderSegment)

    assert other_segment.departure == segment.arrival
    assert other_segment.departure_at == datetime.datetime(2022, 7, 31, 19)
    assert other_segment.departure_at_utc == datetime.datetime(2022, 7, 31, 16)
    assert other_segment.departure_at_timezone_offset == 3

    assert other_segment.arrival == segment.departure
    assert other_segment.arrival_at == datetime.datetime(2022, 7, 31, 21, 35)
    assert other_segment.arrival_at_utc == datetime.datetime(2022, 7, 31, 18, 35)
    assert other_segment.arrival_at_timezone_offset == 3

    assert other_segment.seats == 9
    assert other_segment.flight_number == '106'
    assert other_segment.flight_duration is None
    assert other_segment.transfer_duration is None
    assert other_segment.comment is None
    assert other_segment.flight_class == AviaCabinClassType.economy

    assert isinstance(other_segment.baggage, FlightBaggage)
    assert other_segment.baggage.baggage is None
    assert other_segment.baggage.hand_baggage is None

    assert isinstance(other_segment.carrier, BaseInfo)
    assert other_segment.carrier.type == ObjectType.carrier
    assert other_segment.carrier.id == '4G'
    assert other_segment.carrier.name == {'ru': 'Газпром авиа', 'en': 'Gazpromavia'}[language]

    assert isinstance(other_segment.aircraft, BaseInfo)
    assert other_segment.aircraft.type == ObjectType.aircraft
    assert other_segment.aircraft.id == '737'
    assert other_segment.aircraft.name == {'ru': 'Боинг 737', 'en': 'Boeing 737'}[language]

    assert other_segment.fare_code == 'Y'
