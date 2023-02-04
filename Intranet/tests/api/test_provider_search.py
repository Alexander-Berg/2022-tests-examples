from fastapi.encoders import jsonable_encoder
from mock import patch
import pytest

from intranet.trip.src.config import settings
from intranet.trip.src.lib.utils import dict_to_b64json

pytestmark = pytest.mark.asyncio


aviacenter_response = {
    'search_id': '04T4.04T40000.0:30::ru.08052023.15052023.6308866.',
    'search': {
        'adults': 1,
        'children': [],
        'rooms': [{'guests': [{'age': 30, 'is_child': False, 'citizenship': 'ru'}]}],
        'check_in': '08.05.2023',
        'check_out': '15.05.2023',
        'city': 6308866,
        'hotel_id': None,
        'location': {
            'iata': 6308866,
            'country': 'Россия',
            'type': 'City',
            'type_description': 'Город',
            'typeRu': 'Город',
            'name': 'Белогорск, Россия',
        },
    },
    'hotels': [{
        'id': '925157',
        'name': 'Тестотель (Островок)',
        'phone': None,
        'email': None,
        'photos': [
            {
                'url': (
                    'http://static-release.htl.deac/Images/'
                    'HotelImages/925157/1024-768/Тестотель__Островок__31.jpg'
                ),
                'thumb': (
                    'http://static-release.htl.deac/Images/'
                    'HotelImages/925157/240-240/Тестотель__Островок__31.jpg'
                ),
            },
            {
                'url': (
                    'http://static-release.htl.deac/Images/'
                    'HotelImages/925157/1024-768/Тестотель__Островок__19.jpg'
                ),
                'thumb': (
                    'http://static-release.htl.deac/Images/'
                    'HotelImages/925157/240-240/Тестотель__Островок__19.jpg'
                ),
            }],
        'description': (
            'Плата за доступ в интернет в номерах: 400.00 AMD '
            '(приблизительно)(стоимость услуги может быть изменена).'
        ),
        'policy': None,
        'facilities': [{
            'name': 'Питание и напитки',
            'list': [
                'Ресторан',
                'Бар',
                'Завтрак в номер',
                'Барбекю',
                'Упакованные ланчи',
                'Снэк-бар',
                'Специальные диетические меню (по запросу)',
                'Торговый автомат (напитки)',
                'Кафе',
            ],
        }],
        'city': 'Белогорск',
        'country': 'Россия',
        'location': {'latitude': 50.921287, 'longitude': 128.473881},
        'stars': 2,
        'category_name': 'Hotel',
        'rating': [],
        'address': 'Ордос 1, Белогорск',
        'trip_advisor_rating': None,
        'check_in_time': '14:00',
        'check_out_time': '12:00',
        'max_check_in_time': None,
        'max_check_out_time': None,
        'important_information': None,
        'min_fare': 639,
        'min_fares': {'RUB': 639},
        'min_price': 639,
        'min_prices': {'RUB': 639},
        'price_details': {'RUB': {'aac_fee': 0}},
        'liked': False,
        'has_free_cancellation': False,
        'travel_policy': [],
        'search_id': '04T4.04T40000.0:30::ru.08052023.15052023.6308866.925157',
        'contract': None,
        'is_contract_only': False,
        'is_provider_contract': False,
    }],
    'total': 1,
    'offset': 0,
    'limit': 20,
    'is_completed': True,
}

aviacenter_hotel_id_data = {
    'search_id': '04T4.04T40000.0:30::ru.08052023.15052023.6308866.925157',
}

trip_aviacenter_response = {
    'service_type': 'hotel',
    'total': 1,
    'page': 1,
    'limit': 20,
    'data': [{
        'id': dict_to_b64json(aviacenter_hotel_id_data),
        'hotel_name': 'Тестотель (Островок)',
        'description': (
            'Плата за доступ в интернет в номерах: 400.00 AMD '
            '(приблизительно)(стоимость услуги может быть изменена).'
        ),
        'stars': 2,
        'image_url': (
            'http://static-release.htl.deac/Images/'
            'HotelImages/925157/1024-768/Тестотель__Островок__31.jpg'
        ),
        'currency': 'RUB',
        'min_price_per_night': 639,
        'address': 'Ордос 1, Белогорск',
        'geo_position': {
            'latitude': 50.921287,
            'longitude': 128.473881,
        },
        'location': {
            'city': {'type': 'city', 'name': 'Белогорск'},
            'country': {'type': 'country', 'name': 'Россия'},
        },
        'is_recommended': None,
    }],
}


async def test_aviacenter_hotel_results(client):
    req_path = 'intranet.trip.src.lib.aviacenter.api.aviacenter._make_request'

    async def mock_request(*a, **kw):
        return aviacenter_response

    with patch(req_path, mock_request):
        res = await client.get('api/provider/search/hotel/123/results?provider=aviacenter')
        assert res.json() == trip_aviacenter_response


aeroclub_response = {
    'items_per_page': 20,
    'page_number': 0,
    'items_count': 1,
    'data': [{
        'hotel': {
            'code': '1957331',
            'name': {'ru': 'Авгур', 'en': 'Augures'},
        },
        'city': {
            'time_zone_offset': '03:00:00',
            'country': {
                'id': 156,
                'code': 'RUS',
                'name': {'ru': 'Россия', 'en': 'Russian Federation'},
            },
            'id': 0,
            'code': '15613',
            'name': {'ru': 'Санкт-Петербург', 'en': 'Saint Petersburg'},
        },
        'is_direct_contract': False,
        'stars': 3,
        'geo_position': {
            'latitude': 59.926877,
            'longitude': 30.342422,
        },
        'image_url': (
            'https://fs.aeroclub.ru/v1/blob/ce19f63ee33cb5dfa2a15505b3f85db1453ba7c1/0d2fa2e2-226a'
            '-4171-ac94-6732630b8dde.jpg?accessKey=aeroclub-platform-dictionaries&signature=qpCTBJ7'
            'nmNexCyQ3gDfkDWp3VBk%3d&contentType=image%2fjpeg'
        ),
        'is_recommended': True,
        'is_top_hotel': False,
        'recommendations': None,
        'recommendations_enhanced': 'Undefinded',
        'start_on': '2023-05-12T00:00:00',
        'end_on': '2023-05-17T00:00:00',
        'tariffs': [
            {
                'price_per_night': 875.26,
                'payment_place': 'Agency',
                'total_price': 4376.3,
                'service_fee': 0.0,
                'is_travel_policy_compliant': True,
                'travel_policy_violations': [],
                'is_corporate_tariff': False,
                'confirmation_type': 'InstantConfirmation',
                'index': 0,
            },
            {
                'price_per_night': 90.02,
                'payment_place': 'Agency',
                'total_price': 4376.3,
                'service_fee': 0.0,
                'is_travel_policy_compliant': True,
                'travel_policy_violations': [],
                'is_corporate_tariff': False,
                'confirmation_type': 'InstantConfirmation',
                'index': 0,
            },
        ],
        'is_travel_policy_compliant': None,
        'address': {
            'ru': 'Рубинштейна улица, 29/28',
            'en': '29/28 Rubinstein street',
        },
        'description': {
            'ru': (
                'Посетите уютный отель в самом центре Санкт-Петербурга. '
                'Отель находится на всем известной улице Рубинштейна'
            ),
            'en': (
                'Attractions:\r\nVladimir Icon Of Mother Of God Cathedral '
                '- 0,37 km, Europe Theatre - 0,41 km, Jazz Music'
            ),
        },
        'key': (
            'QWVyb2NsdWJBcGk6dHA7NF9mcjsxNTYxM190bzsxNTYxM19zdDsxMjA1MjNfZW47MTcwNTIzX3NtOzVfY2lkO'
            '3s0MjQzNn06aW5mbzozMDIwMzMwNDNiNjMyNzhjMDRlMDQ0MDA2YjI2MGVlMA=='
        ),
        'option_number': 1,
        'sequence_number': None,
        'currency': 'RUB',
        'is_corporate_tariff': False,
        'service_type': 'Hotel',
        'is_favourite': False,
    }],
    'message': None,
    'errors': None,
    'is_success': True,
}


aeroclub_hotel_id_data = {
    'key': (
        'QWVyb2NsdWJBcGk6dHA7NF9mcjsxNTYxM190bzsxNTYxM19zdDsxMjA1MjNfZW47MTcwNTIzX3NtOzVfY'
        '2lkO3s0MjQzNn06aW5mbzozMDIwMzMwNDNiNjMyNzhjMDRlMDQ0MDA2YjI2MGVlMA=='
    ),
    'option_number': 1,
}

trip_aeroclub_response = {
    'service_type': 'hotel',
    'total': 1,
    'page': 1,
    'limit': 20,
    'data': [{
        'id': dict_to_b64json(aeroclub_hotel_id_data),
        'hotel_name': 'Авгур',
        'description': (
            'Посетите уютный отель в самом центре Санкт-Петербурга. '
            'Отель находится на всем известной улице Рубинштейна'
        ),
        'stars': 3,
        'image_url': (
            'https://fs.aeroclub.ru/v1/blob/ce19f63ee33cb5dfa2a15505b3f85db1453ba7c1/0d2fa2e2-226a'
            '-4171-ac94-6732630b8dde.jpg?accessKey=aeroclub-platform-dictionaries&signature=qpCTBJ'
            '7nmNexCyQ3gDfkDWp3VBk%3d&contentType=image%2fjpeg'
        ),
        'currency': 'RUB',
        'min_price_per_night': 90.02,
        'address': 'Рубинштейна улица, 29/28',
        'geo_position': {
            'latitude': 59.926877,
            'longitude': 30.342422,
        },
        'location': {
            'city': {'name': 'Санкт-Петербург', 'type': 'city'},
            'country': {'name': 'Россия', 'type': 'country'},
        },
        'is_recommended': True,
    }],
}


async def test_aeroclub_hotel_results(client):
    req_path = 'intranet.trip.src.lib.aeroclub.api.aeroclub._make_request'

    async def mock_request(*a, **kw):
        return aeroclub_response

    with patch(req_path, mock_request):
        res = await client.get('api/provider/search/hotel/123/results?provider=aeroclub')
        assert res.json() == trip_aeroclub_response


trip_aviacenter_hotel_filters_response = [
    {
        'name': 'order_by',
        'type': 'select',
        'values': [
            {
                'caption': 'По цене',
                'target_id': 'price'
            }
        ]
    },
    {
        'name': 'is_descending',
        'type': 'boolean',
        'values': None
    },
    {
        'name': 'is_restricted_by_travel_policy',
        'type': 'boolean',
        'values': None,
    },
    {
        'name': 'price_from',
        'type': 'integer',
        'values': None,
    },
    {
        'name': 'price_to',
        'type': 'integer',
        'values': None,
    },
    {
        'name': 'stars',
        'type': 'multiselect',
        'values': [
            {
                'target_id': '1',
                'caption': '5',
            },
            {
                'target_id': '2',
                'caption': '3',
            },
        ],
    },
    {
        'name': 'hotel_types',
        'type': 'multiselect',
        'values': [
            {
                'target_id': '1',
                'caption': 'Hotel',
            },
            {
                'target_id': '2',
                'caption': 'CapsuleHotel',
            },
            {
                'target_id': '3',
                'caption': 'Villa',
            },
        ],
    },
]


async def test_aviacenter_hotel_filters(client):
    res = await client.get('api/provider/search/hotel/123/filters?provider=aviacenter')
    assert res.json() == trip_aviacenter_hotel_filters_response


aeroclub_hotel_filters_response = {
    'data': [
        {
            'parameter_name': 'hoteltypes',
            'values': [
                {
                    'code': '3',
                    'name': {
                        'ru': 'Отель',
                        'en': 'Hotel',
                    },
                },
                {
                    'code': '4',
                    'name': {
                        'ru': 'Апарт-отель',
                        'en': 'Apartment Hotel',
                    },
                },
                {
                    'code': '8',
                    'name': {
                        'ru': 'Кровать и завтрак',
                        'en': 'Bed And Breakfast',
                    },
                },
            ],
        },
        {
            'parameter_name': 'confirmationtype',
            'values': [
                {
                    'code': 'OnRequest',
                    'name': {
                        'ru': 'Под запрос',
                        'en': 'On request',
                    },
                },
                {
                    'code': 'InstantConfirmation',
                    'name': {
                        'ru': 'Мгновенное',
                        'en': 'Instant',
                    },
                },
            ],
        },
        {
            'parameter_name': 'pricerange',
            'values': [
                {
                    'code': 'min',
                    'name': {
                        'ru': '4219,3200',
                        'en': '4219,3200'
                    },
                },
                {
                    'code': 'max',
                    'name': {
                        'ru': '146089,5500',
                        'en': '146089,5500'
                    },
                },
            ],
        },
    ],
}


trip_aeroclub_hotel_filters_response = [
    {
        'name': 'is_travel_policy_compliant',
        'type': 'boolean',
        'values': None,
    },
    {
        'name': 'is_recommended',
        'type': 'boolean',
        'values': None,
    },
    {
        'name': 'hotel_name',
        'type': 'string',
        'values': None,
    },
    {
        'name': 'stars',
        'type': 'multiselect',
        'values': [
            {
                'target_id': '1',
                'caption': '1',
            },
            {
                'target_id': '2',
                'caption': '2',
            },
            {
                'target_id': '3',
                'caption': '3',
            },
            {
                'target_id': '4',
                'caption': '4',
            },
            {
                'target_id': '5',
                'caption': '5',
            },
        ],
    },
    {
        'name': 'payment_places',
        'type': 'multiselect',
        'values': [
            {
                'target_id': 'Checkin',
                'caption': 'При регистрации',
            },
            {
                'target_id': 'Agency',
                'caption': 'Агентством',
            },
        ],
    },
    {
        'name': 'order_by',
        'type': 'select',
        'values': [
            {
                'caption': 'По цене',
                'target_id': 'price'
            }
        ]
    },
    {
        'name': 'is_descending',
        'type': 'boolean',
        'values': None
    },
    {
        'name': 'hotel_types',
        'type': 'multiselect',
        'values': [
            {
                'target_id': '3',
                'caption': 'Отель',
            },
            {
                'target_id': '4',
                'caption': 'Апарт-отель',
            },
            {
                'target_id': '8',
                'caption': 'Кровать и завтрак',
            },
        ],
    },
    {
        'name': 'confirmation_type',
        'type': 'multiselect',
        'values': [
            {
                'target_id': 'OnRequest',
                'caption': 'Под запрос',
            },
            {
                'target_id': 'InstantConfirmation',
                'caption': 'Мгновенное',
            },
        ],
    },
    {
        'name': 'price_range',
        'type': 'multiselect',
        'values': [
            {
                'caption': '4219,3200',
                'target_id': 'min',
            },
            {
                'caption': '146089,5500',
                'target_id': 'max',
            },
        ],
    },
]


async def test_aeroclub_hotel_filters(client):
    req_path = 'intranet.trip.src.lib.aeroclub.api.aeroclub._make_request'

    async def mock_request(*a, **kw):
        return aeroclub_hotel_filters_response

    with patch(req_path, mock_request):
        res = await client.get('api/provider/search/hotel/123/filters?provider=aeroclub')
        assert res.json() == trip_aeroclub_hotel_filters_response


request_to_aeroclub = {
    'hotelFilter_stars': ['1', '2'],
    'hotelFilter_priceFrom': 100,
    'hotelFilter_priceTo': 500,
    'hotelFilter_hotelTypes': ['t1', 't2'],
    'IsTravelPolicyCompliant': False,
    'hotelFilter_paymentPlaces': ['Agency'],
    'hotelFilter_confirmationTypes': ['OnRequest'],
    'hotelFilter_isRecommended': False,
    'hotelName': 'asd',

    'maxCount': 20,
    'orderBy': 'Optimal',
    'pageNumber': 0,
}


aeroclub_request_with_filters = {
    'stars': ['1', '2'],
    'price_from': 100,
    'price_to': 500,
    'hotel_types': ['t1', 't2'],
    'is_restricted_by_travel_policy': 1,
    'confirmation_types': ['OnRequest'],
    'is_recommended': 0,
    'hotel_name': 'asd',

    'provider': 'aeroclub',
}


async def test_aeroclub_hotel_results_with_filters(client):
    req_path = 'intranet.trip.src.lib.aeroclub.api.aeroclub._make_request'
    called_with = None

    async def mock_request(*a, params=None, **kw):
        nonlocal called_with
        called_with = jsonable_encoder(params, exclude_none=True)
        return aeroclub_response

    with patch(req_path, mock_request):
        await client.get(
            'api/provider/search/hotel/123/results',
            params=aeroclub_request_with_filters,
        )
        assert called_with == request_to_aeroclub


request_to_aviacenter = {
    'is_restricted_by_travel_policy': 1,
    'min_price': 234,
    'max_price': 345,
    'stars': ['1'],
    'hotel_categories': ['some_cat'],
    'new_data_only': 0,
}


aviacenter_request_with_filters = {
    'is_restricted_by_travel_policy': 1,
    'price_from': 234,
    'price_to': 345,
    'stars': ['1'],
    'hotel_types': ['some_cat'],

    'provider': 'aviacenter',
}


async def test_aviacenter_hotel_results_with_filters(client):
    req_path = 'intranet.trip.src.lib.aviacenter.api.aviacenter._make_request'
    called_with = None

    async def mock_request(*a, params=None, **kw):
        nonlocal called_with
        called_with = params['post_filter']
        return aviacenter_response

    with patch(req_path, mock_request):
        await client.get(
            'api/provider/search/hotel/123/results',
            params=aviacenter_request_with_filters,
        )
        assert called_with == request_to_aviacenter


async def test_aviacenter_hotel_info(client):
    req_path = 'intranet.trip.src.lib.aviacenter.api.aviacenter._make_request'

    async def mock_request(*a, **kw):
        return aviacenter_response

    expected_reponse = {
        'service_type': 'hotel',
        'check_in': '2023-05-08',
        'check_out': '2023-05-15',
        'status': 'completed',
        'location': {
            'city': {'name': 'Белогорск', 'type': 'city'},
            'country': {'name': 'Россия', 'type': 'country'},
        },
    }

    with patch(req_path, mock_request):
        res = await client.get(
            'api/provider/search/hotel/123/',
            params=aviacenter_request_with_filters,
        )
        assert res.json() == expected_reponse


async def test_aviacenter_hotel_count(client):
    req_path = 'intranet.trip.src.lib.aviacenter.api.aviacenter._make_request'

    async def mock_request(*a, **kw):
        return aviacenter_response

    with patch(req_path, mock_request):
        res = await client.get(
            'api/provider/search/hotel/123/count',
            params=aviacenter_request_with_filters,
        )
        assert res.json() == {'count': 1, 'service_type': 'hotel'}


aeroclub_hotel_search_object_response = {'data': {
    'number': 670069,
    'created_at_utc': '2022-05-30T12:29:57Z',
    'updated_at_utc': '2022-05-30T12:30:31Z',
    'status': 'Completed',
    'requested_by': {
        'id': 2431035,
        'first_name': {'ru': 'Интеграция', 'en': 'Integration'},
        'middle_name': {'ru': None, 'en': None},
        'last_name': {'ru': 'Яндекс', 'en': 'Yandex'},
        'company_info': {
            'id': 42436,
            'company_name': {'ru': 'ЯНДЕКС', 'en': 'YANDEX'},
            'holding_name': {'ru': 'ЯНДЕКС ХОЛДИНГ', 'en': 'YANDEX HOLDING'},
        },
        'avatar_url': None,
        'sex': 'Male',
    },
    'options': [{
        'service_type': 'Hotel',
        'comment': None,
        'departure_city': None,
        'arrival_city': {
            'id': 16381,
            'type': 'City',
            'terminal': None,
            'address': None,
            'located_in': {
                'id': 156,
                'type': 'Country',
                'terminal': None,
                'address': None,
                'located_in': None,
                'time_zone_offset': None,
                'image_url': None,
                'code': 'RU',
                'name': {'ru': 'Россия', 'en': 'Russian Federation'},
            },
            'time_zone_offset': '03:00:00',
            'image_url': 'https://fs.aeroclub.ru/v1/blob/4d368b24ff4fc1f2815/strip',
            'code': 'MOW',
            'name': {'ru': 'Москва', 'en': 'Moscow'},
        },
        'departure_on': None,
        'round_trip_departure_on': None,
        'checkin_on': '2023-05-08T00:00:00',
        'checkout_on': '2023-05-15T00:00:00',
        'sequence_number': None,
        'number': 1,
        'status': 'Completed',
        'search_mode': ['CorporateAndFast', 'NoncorporateAndFast'],
        'avia_search_mode': {'cabin_class': 'NotSet'},
    }],
    'company_id': 42436,
    'favourites_count': 0,
    'results_count': {
        'variants_count': 112,
        'variants_count_avia': 0,
        'variants_count_rail': 0,
        'variants_count_rail_round_trip': 0,
        'variants_count_hotel': 112,
        'expires_at_utc': '2022-05-30T13:30:08Z',
    },
    'profile_level': None,
    'expires_at_utc': '2022-05-30T13:30:08Z',
    'profile': {
        'id': 2431035,
        'first_name': {'ru': 'Интеграция', 'en': 'Integration'},
        'middle_name': {'ru': None, 'en': None},
        'last_name': {'ru': 'Яндекс', 'en': 'Yandex'},
        'company_info': {
            'id': 42436,
            'company_name': {'ru': 'ЯНДЕКС', 'en': 'YANDEX'},
            'holding_name': {'ru': 'ЯНДЕКС ХОЛДИНГ', 'en': 'YANDEX HOLDING'},
        },
        'avatar_url': None,
        'sex': 'Male',
    },
    'is_active': True,
    'is_read': False,
    'departure_on': '2023-05-08T00:00:00',
    'round_trip_departure_on': '2023-05-15T00:00:00',
    'departure_city': None,
    'arrival_city': {
        'id': 16381,
        'type': 'City',
        'terminal': None,
        'address': None,
        'located_in': {
            'id': 156,
            'type': 'Country',
            'terminal': None,
            'address': None,
            'located_in': None,
            'time_zone_offset': None,
            'image_url': None,
            'code': 'RU',
            'name': {'ru': 'Россия', 'en': 'Russian Federation'},
        },
        'time_zone_offset': '03:00:00',
        'image_url': 'https://fs.aeroclub.ru/v1/blob/4d368b24ff4fc1f2815abb7302b1/strip',
        'code': 'MOW',
        'name': {'ru': 'Москва', 'en': 'Moscow'},
    },
    'is_expired': False},
    'message': None,
    'errors': None,
    'is_success': True,
}


async def test_aeroclub_hotel_info(client):
    req_path = 'intranet.trip.src.lib.aeroclub.api.aeroclub._make_request'

    async def mock_request(*a, **kw):
        return aeroclub_hotel_search_object_response

    expected_reponse = {
        'service_type': 'hotel',
        'check_in': '2023-05-08',
        'check_out': '2023-05-15',
        'status': 'completed',
        'location': {
            'city': {'name': 'Москва', 'type': 'city'},
            'country': {'name': 'Россия', 'type': 'country'},
        },
    }

    with patch(req_path, mock_request):
        res = await client.get(
            'api/provider/search/hotel/123/',
            params=aeroclub_request_with_filters,
        )
        assert res.json() == expected_reponse


aeroclub_hotel_search_count_response = {
    'data': {'count': 112, 'expires_at_utc': None},
    'message': None,
    'errors': None,
    'is_success': True,
}


async def test_aeroclub_hotel_count(client):
    req_path = 'intranet.trip.src.lib.aeroclub.api.aeroclub._make_request'

    async def mock_request(*a, **kw):
        return aeroclub_hotel_search_count_response

    with patch(req_path, mock_request):
        res = await client.get(
            'api/provider/search/hotel/123/count',
            params=aeroclub_request_with_filters,
        )
        assert res.json() == {'count': 112, 'service_type': 'hotel'}


aeroclub_hotel_details_response = {'data': {
    "hotel": {
        "code": "1910519",
        "name": {
            "ru": "Друзья На Банковском",
            "en": "Druz'ya Na Bankovskom"
        }
    },
    "city": {
        "code": "15613",
        "name": {
            "ru": "Санкт-Петербург",
            "en": "Saint Petersburg"
        },
        "id": 0,
        "time_zone_offset": "03:00:00",
        "country": {
            "code": "RUS",
            "name": {
                "ru": "Россия",
                "en": "Russian Federation"
            },
            "id": 156
        }
    },
    "stars": 2,
    "rooms": [
        {
            "room_name": "койко-место в 7 местном женском номере Long Stay",
            "total_price": 11979,
            "actual_checkin_at": "2022-06-09T12:00:00",
            "actual_checkin_at_utc": "2022-06-09T09:00:00Z",
            "actual_checkout_at": "2022-06-23T12:00:00",
            "actual_checkout_at_utc": "2022-06-23T09:00:00Z",
            "service_type": "Hotel",
            "additional_room_info": (
                "Стоимость комплекта белья/полотенец в многоместных номерах: 150 рублей."
            ),
            "is_pay_on_site": False,
            "asim_room_commission": None,
            "booking_code": (
                "gUC0A723glvoXtFANA7kZmNBL1JNFnt2tyqw6pOUF"
                "Fj0L9UkeunZeGbNQTIpwGzE5rYJK7o61"
            ),
            "room_rate_id": "0AF6D3154CEC8BC1DDF8174F313F4672",
            "sale_type": "FreeSale",
            "meals": [
                {
                    "code": "3",
                    "name": "Завтрак \"Шведский стол\"",
                    "meal_type": "MealIncludedInPrice",
                    "is_included_in_price": True,
                    "price": 0
                },
                {
                    "code": "3",
                    "name": "Завтрак \"Континентальный\"",
                    "meal_type": "MealIncludedInPrice",
                    "is_included_in_price": True,
                    "price": 0
                },
                {
                    "code": "3",
                    "name": "Бонба",
                    "meal_type": "MealIncludedInPrice",
                    "is_included_in_price": False,
                    "price": 0
                },
            ],
            "currency": "RUB",
            "tariff_per_night": 891.3571428571429,
            "index": 0,
            "tariff_expected_cancellation_penalty": 623.7,
            "tariff_vat": 0,
            "tariff_additional_fee_percent": None,
            "tariff_discount_percent": None,
            "tariff_rack_rate": None,
            "allocation_is_limited": True,
            "check_in_at": "2022-06-09T12:00:00",
            "check_in_at_utc": "2022-06-09T09:00:00Z",
            "check_out_at": "2022-06-23T12:00:00",
            "check_out_at_utc": "2022-06-23T09:00:00Z",
            "room_id": "1abc78f6-96c6-430d-b73d-187459b229a8",
            "tariff_total": 12479,
            "tariff_adult": 11979,
            "is_travel_policy_compliant": True,
            "tariff_service_fee": 500,
            "is_corporate_tariff": False,
            "supplier_unique_identifier": None,
            "supplier_room_id": "132151",
            "corporate_tariff_code": "ACB",
            "allocation_adults": 1,
            "reservation_cancellation_policy": None,
            "travel_policy_violation_reason": None,
            "is_hidden_price": False,
            "images": [
                (
                    "https://fs.aeroclub.ru/v1/blob/1dd5d5b7b14b4e2586f662913ced39db05dae382/"
                    "64af27fa-ee99-4732-9ef3-7e93a8c7a0cf.jpg?accessKey=aeroclub-platform-"
                    "dictionaries&signature=qMeBDE8pzddCf%2baZh2PK2KlvjEk%3d"
                    "&contentType=image%2fjpeg"
                ),
                (
                    "https://fs.aeroclub.ru/v1/blob/43ef2859930d72504481d54163c37a236b53d7a1/"
                    "bb6ec8b7-3b2b-43df-ab21-8e9c2611185d.jpg?accessKey=aeroclub-platform-"
                    "dictionaries&signature=xj8C%2f0XyZfaiYUteIrTXyfwwjuc%3d"
                    "&contentType=image%2fjpeg"
                ),
            ],
            "room_description": {
                "russian": "Стандартная категория номеров",
                "english": "Стандартная категория номеров",
            },
            "number_of_free_rooms": None,
            "is_discount_offer": False,
        },
        {
            "room_name": "койко-место в 7 местном женском номере Невозвратный",
            "total_price": 12258.4,
            "actual_checkin_at": "2022-06-09T12:00:00",
            "actual_checkin_at_utc": "2022-06-09T09:00:00Z",
            "actual_checkout_at": "2022-06-23T12:00:00",
            "actual_checkout_at_utc": "2022-06-23T09:00:00Z",
            "service_type": "Hotel",
            "additional_room_info": (
                "Стоимость комплекта белья/полотенец в "
                "многоместных номерах: 150 рублей."
            ),
            "is_pay_on_site": False,
            "asim_room_commission": None,
            "booking_code": "gUC0A723glvoXtFANA7kZmNBL1JNFnt2tyqw6pOUFFj0L9UkeunZeGbNQTIpwGzE5",
            "room_rate_id": "BED2BAC6578EA4953C07CAD192DAF6E4",
            "sale_type": "OnRequest",
            "meals": [],
            "currency": "RUB",
            "tariff_per_night": 911.3142857142857,
            "index": 3,
            "tariff_expected_cancellation_penalty": 12258.4,
            "tariff_vat": 0,
            "tariff_additional_fee_percent": None,
            "tariff_discount_percent": None,
            "tariff_rack_rate": None,
            "allocation_is_limited": True,
            "check_in_at": "2022-06-09T12:00:00",
            "check_in_at_utc": "2022-06-09T09:00:00Z",
            "check_out_at": "2022-06-23T12:00:00",
            "check_out_at_utc": "2022-06-23T09:00:00Z",
            "room_id": "52f2c6cc-abcf-4388-8aaf-e007c77d8676",
            "tariff_total": 12758.4,
            "tariff_adult": 12258.4,
            "is_travel_policy_compliant": True,
            "tariff_service_fee": 500,
            "is_corporate_tariff": False,
            "supplier_unique_identifier": None,
            "supplier_room_id": "132151",
            "corporate_tariff_code": "ACB",
            "allocation_adults": 1,
            "reservation_cancellation_policy": None,
            "travel_policy_violation_reason": None,
            "is_hidden_price": False,
            "images": [],
            "room_description": {
                "russian": "Стандартная категория",
                "english": "Стандартная категория",
            },
            "number_of_free_rooms": None,
            "is_discount_offer": False,
        },
    ],
    "address": {
        "name": 'asdfgwq',
        "coordinates": None
    },
    "geo_position": {
        "latitude": 59.930908203125,
        "longitude": 30.323514938354492
    },
    "images": [],
    "hotel_images": [
        {
            'url': (
                'http://static-release.htl.deac/Images/'
                'HotelImages/925157/1024-768/Тестотель__Островок__19.jpg'
            ),
            'thumb': (
                'http://static-release.htl.deac/Images/'
                'HotelImages/925157/240-240/Тестотель__Островок__19.jpg'
            ),
        },
    ],
    "web_site": 'https://google.com',
    "is_recommended": True,
    "rating": 0,
    "is_top_hotel": False,
    "departure_from": None,
    "arrival_to": None,
    "departure_at": None,
    "departure_at_utc": None,
    "is_local_departure_at": None,
    "arrival_at": None,
    "arrival_at_utc": None,
    "duration": None,
    "is_local_arrival_at": None,
    "train_code": None,
    "train_name": None,
    "train_category": None,
    "branded": None,
    "currency": None,
    "carriage_details": None,
    "legs": None,
    "validating_airline_code": None,
    "is_segment_discount_variant": None,
    "is_segment_discount_as_exact_value": None,
    "service_type": "Hotel",
    "key": (
        "QWVyb2NsdWJBcGk6dHA7NF9mcjsxNTYxM190bzsxNTYxM19zdDswOTA2MjJfZW47MjMwNjIyX3NtOzV"
        "fY2lkO3s0MjQzNn06aW5mbzo4OGRiODVlM2ExNjU0ZTU2YmFlY2NmZGIyMmM0NWM4Nzsw"
    ),
    "is_corporate_tariff": False,
    "start_on": "2022-06-09T00:00:00",
    "end_on": "2022-06-23T00:00:00",
    "start_on_utc": "2022-06-08T21:00:00Z",
    "end_on_utc": "2022-06-22T21:00:00Z",
    "tariff_total": 0,
    "is_travel_policy_compliant": None,
    "tariff_tax": None,
    "corporate_tariff_code": None,
    "reservation_cancellation_policy": None,
    "grade_custom_list_property_value_id": None,
    "travel_policy_violation_reason": None,
    "tariff_service_fee": 0
}}


async def test_aeroclub_hotel_details(client):
    expected_answer = {
        'service_type': 'hotel',
        'hotel': {
            'hotel_name': 'Друзья На Банковском',
            'stars': 2,
            'address': 'asdfgwq',
            'location': {
                'city': {'type': 'city', 'name': 'Санкт-Петербург'},
                'country': {'type': 'country', 'name': 'Россия'},
            },
            'geo_position': {
                'latitude': 59.930908203125,
                'longitude': 30.323514938354492
            },
            'images': [(
                'http://static-release.htl.deac/Images/'
                'HotelImages/925157/1024-768/Тестотель__Островок__19.jpg'
            )],
            'check_in': '2022-06-09',
            'check_out': '2022-06-23',
            'num_of_nights': 14,
            'website': 'https://google.com',
            'is_recommended': True,
        },
        'data': [
            {
                'index': 0,
                'images': [
                    (
                        'https://fs.aeroclub.ru/v1/blob/1dd5d5b7b14b4e2586f662913ced39db05dae382/'
                        '64af27fa-ee99-4732-9ef3-7e93a8c7a0cf.jpg?accessKey=aeroclub-platform-'
                        'dictionaries&signature=qMeBDE8pzddCf%2baZh2PK2KlvjEk%3d'
                        '&contentType=image%2fjpeg'
                    ),
                    (
                        'https://fs.aeroclub.ru/v1/blob/43ef2859930d72504481d54163c37a236b53d7a1/'
                        'bb6ec8b7-3b2b-43df-ab21-8e9c2611185d.jpg?accessKey=aeroclub-platform-'
                        'dictionaries&signature=xj8C%2f0XyZfaiYUteIrTXyfwwjuc%3d'
                        '&contentType=image%2fjpeg'
                    ),
                ],
                'description': 'Стандартная категория номеров',
                'name': 'койко-место в 7 местном женском номере Long Stay',
                'is_meal_included': True,
                'meal_names': ['Завтрак "Шведский стол"', 'Завтрак "Континентальный"'],
                'is_travel_policy_compliant': True,
                'is_booking_by_request': False,
                'currency': 'RUB',
                'price_total': 12479.0,
                'price_per_night': 891.3571428571429,
            },
            {
                'index': 3,
                'images': [],
                'description': 'Стандартная категория',
                'name': 'койко-место в 7 местном женском номере Невозвратный',
                'is_meal_included': False,
                'meal_names': [],
                'is_travel_policy_compliant': True,
                'is_booking_by_request': True,
                'currency': 'RUB',
                'price_total': 12758.4,
                'price_per_night': 911.3142857142857,
            },
        ],
    }

    req_path = 'intranet.trip.src.lib.aeroclub.api.aeroclub._make_request'

    search_id = 'wqr'
    search_object_id = 'asdwqw'
    coded_id = dict_to_b64json({'key': search_object_id, 'option_number': 1})

    async def mock_request(*, method, path, params):
        assert method == 'get'
        assert path == f'search/{search_id}/options/1/details/{search_object_id}'
        assert not params
        return aeroclub_hotel_details_response

    with patch(req_path, mock_request):
        res = await client.get(
            f'api/provider/search/hotel/{search_id}/results/{coded_id}',
            params={'provider': 'aeroclub'},
        )
        assert res.json() == expected_answer


aviacenter_hotel_details_response = {
    'hotel': {
        'id': '925157',
        'name': 'Тестотель (Островок)',
        'phone': None,
        'email': None,
        'photos': [
            {
                'url': 'http://static-release.htl.deac/925157/1024-768/Тестотель__Островок__31.jpg',
                'thumb': (
                    'http://static-release.htl.deac/925157/240-240/'
                    'Тестотель__Островок__31.jpg'
                ),
            },
            {
                'url': (
                    'http://static-release.htl.deac/925157/1024-768/'
                    'Тестотель__Островок__19.jpg'
                ),
                'thumb': (
                    'http://static-release.htl.deac/925157/240-240/'
                    'Тестотель__Островок__19.jpg'
                ),
            },
        ],
        'description': (
            'Плата за доступ в интернет в номерах: 400.00 AMD (приблизительно)'
            '(стоимость услуги может быть изменена).'
            'Плата за беспроводной доступ в интернет в общественных зонах не взимается.\n'
            'Дополнительные кровати могут предоставляться бесплатно при размещении с детьми.'
            'За более подробной информацией необходимо обратиться в отель, '
            'используя контактную информацию,'
            'указанную в подтверждении бронирования. '
            'Количество дополнительных кроватей зависит от категории номера.'
            'Необходимо ознакомиться с информацией о вместимости выбранного номера.\n'
            'В этом отеле можно бесплатно оставить багаж в камере хранения.\n\n'
            'Предоставляется платная парковка на территории отеля. '
            'Стоимость: 200.00 RUB в день.\n\n'
            'Предоставляется трансфер от и до аэропорта. Стоимость: 100.00 RUB в обе стороны.\n'
            'Расположение Внимание! Данная страница является тестовой. '
            'Размещенный на странице отель является вымышленным и не подлежит бронированию. '
            'В случае бронирования данного отеля размещение'
            'не может быть предоставлено.\nГости в возрасте старше 13 лет считаются взрослыми.'
            'Услуги раннего заезда и/или позднего выезда предоставляются за дополнительную плату.\n'
        ),
        'policy': None,
        'facilities': [
            {
                'name': 'Питание и напитки',
                'list': [
                    'Ресторан',
                    'Бар',
                    'Завтрак в номер',
                    'Барбекю',
                    'Упакованные ланчи',
                    'Снэк-бар',
                    'Специальные диетические меню (по запросу)',
                    'Торговый автомат (напитки)',
                    'Кафе',
                ],
            },
            {
                'name': 'Услуги бизнес-центра',
                'list': [
                    'Конференц-зал/Банкетный зал',
                    'Бизнес-центр',
                    'Факс/Ксерокопирование',
                ],
            },
            {
                'name': 'Стойка регистрации',
                'list': [
                    'Круглосуточная стойка регистрации',
                    'Ускоренная регистрация заезда/отъезда',
                    'Обмен валюты',
                    'Экскурсионное бюро',
                    'Услуги по продаже билетов',
                    'Камера хранения багажа',
                    'Банкомат на территории отеля',
                    'Услуги консьержа',
                    'Индивидуальная регистрация заезда/отъезда',
                    'Запирающиеся шкафчики',
                ],
            },
        ],
        'city': 'Белогорск',
        'country': 'Россия',
        'location': {'latitude': 50.921287, 'longitude': 128.473881},
        'stars': 2,
        'category_name': 'Hotel',
        'rating': [],
        'address': 'Белогорск, Ордос 1',
        'trip_advisor_rating': None,
        'check_in_time': '14:00',
        'check_out_time': '12:00',
        'max_check_in_time': None,
        'max_check_out_time': None,
        'important_information': None,
        'rooms': [
            {
                'rate_id': (
                    '925157..bedBreakfast..Улучшенный..07062022..'
                    '2c23b311165ae5e6223861ccda26ab32..30'
                ),
                'is_non_refundable': False,
                'price': 140,
                'prices': {'RUB': 140},
                'vendor': '',
                'type': 'Вид на море',
                'name': 'Улучшенный, тип кровати может измениться',
                'description': 'asdf1',
                'fare_sum': 140,
                'fare_sums': {'RUB': 140},
                'meal_type': 'Завтрак',
                'meal_type_code': 'bedBreakfast',
                'cancellation_rules': [
                    {
                        'is_possible': True,
                        'amount': 0,
                        'date_from': '02.06.2022 00:00',
                        'date_to': '07.06.2022 04:00',
                        'fare': None,
                    },
                    {
                        'is_possible': True,
                        'amount': 25,
                        'date_from': '07.06.2022 04:00',
                        'date_to': '08.06.2022 08:00',
                        'fare': None,
                    },
                    {
                        'is_possible': True,
                        'amount': 146,
                        'date_from': '08.06.2022 08:00',
                        'date_to': '08.06.2022 14:00',
                        'fare': None,
                    },
                ],
                'surcharges': [
                    {
                        'name': 'Городской налог',
                        'price': 2321.89,
                        'currency': 'RUB',
                        'is_required': True,
                    },
                    {
                        'name': 'Сбор за уборку',
                        'price': 70,
                        'currency': 'RUB',
                        'is_required': True,
                    },
                    {
                        'name': 'Сервисный сбор',
                        'price': 1240.42,
                        'currency': 'RUB',
                        'is_required': True,
                    },
                ],
                'data': {
                    'id': 26243504,
                    'name': 'Superior',
                    'nameEn': 'Superior',
                    'description': '',
                    'images': [{
                        'src': ('https://cdn.ostrovok.ru/t/1024x768/ext/06/90/'
                                '0690f13918e6808c03212b40619f466123c5d966.jpeg'),
                        'thumb': ('https://cdn.ostrovok.ru/t/240x240/ext/06/90/'
                                  '0690f13918e6808c03212b40619f466123c5d966.jpeg'),
                    }],
                },
                'price_details': {'RUB': {'aac_fee': 0}},
                'is_contract': False,
                'travel_policy': [],
                'rates': [
                    {
                        'guest_ages': [30],
                        'rate_id': '925157....07062022..2c23b311165ae5e6223861ccda26ab32..30',
                    },
                ],
                'available_room_count': None,
                'has_vat': True
            },
            {
                'rate_id': '925157....08062022..44a8f902a1eeda68696494cdb22b0f4b..30',
                'is_non_refundable': True,
                'price': 707,
                'prices': {'RUB': 707},
                'vendor': '',
                'type': '',
                'name': 'Без категории, тип кровати может измениться',
                'description': 'asdf2',
                'fare_sum': 707,
                'fare_sums': {'RUB': 707},
                'meal_type': 'Без питания',
                'meal_type_code': ' rooomOnly',
                'cancellation_rules': [
                    {
                        'is_possible': True,
                        'amount': 584,
                        'date_from': '02.06.2022 00:00',
                        'date_to': '08.06.2022 14:00',
                        'fare': None,
                    },
                ],
                'surcharges': [
                    {
                        'name': 'Городской налог',
                        'price': 2321.89,
                        'currency': 'RUB',
                        'is_required': True,
                    },
                    {
                        'name': 'Сбор за уборку',
                        'price': 70,
                        'currency': 'RUB',
                        'is_required': True,
                    },
                    {
                        'name': 'Сервисный сбор',
                        'price': 1240.42,
                        'currency': 'RUB',
                        'is_required': True,
                    },
                ],
                'data': [],
                'price_details': {'RUB': {'aac_fee': 0}},
                'is_contract': False,
                'travel_policy': ['YOU SHALL NOT PASS'],
                'rates': [
                    {
                        'guest_ages': [30],
                        'rate_id': '925157....08062022..44a8f902a1eeda68696494cdb22b0f4b..30',
                    },
                ],
                'available_room_count': None,
                'has_vat': True,
            },
        ],
        'min_fare': 176,
        'min_fares': {'RUB': 176},
        'min_price': 176,
        'min_prices': {'RUB': 176},
        'currencies_rates': [],
        'price_details': {'RUB': {'aac_fee': 0}},
        'search_id': '04T4.04T40000.0:30::ru.08062022.15062022.6308866.925157',
        'contract': None,
        'is_contract_only': False,
        'liked': False},
    'search': {
        'adults': 1,
        'children': [],
        'rooms': [{'guests': [{'age': 30, 'is_child': False, 'citizenship': 'ru'}]}],
        'check_in': '08.06.2022',
        'check_out': '15.06.2022',
        'city': 6308866,
        'hotel_id': '925157',
    },
    'location_search_id': '04T4.04T40000.0:30::ru.08062022.15062022.6308866.',
    'is_completed': True,
}


async def test_aviacenter_hotel_details(client):
    expected_answer = {
        'service_type': 'hotel',
        'hotel': {
            'hotel_name': 'Тестотель (Островок)',
            'stars': 2,
            'address': 'Белогорск, Ордос 1',
            'location': {
                'city': {'type': 'city', 'name': 'Белогорск'},
                'country': {'type': 'country', 'name': 'Россия'},
            },
            'geo_position': {'latitude': 50.921287, 'longitude': 128.473881},
            'images': [
                'http://static-release.htl.deac/925157/1024-768/Тестотель__Островок__31.jpg',
                'http://static-release.htl.deac/925157/1024-768/Тестотель__Островок__19.jpg',
            ],
            'check_in': '2022-06-08',
            'check_out': '2022-06-15',
            'is_recommended': None,
            'num_of_nights': 7,
            'website': None,
        },
        'data': [
            {
                'index': 0,
                'description': 'asdf1',
                'name': 'Улучшенный, тип кровати может измениться',
                'meal_names': ['Завтрак'],
                'is_meal_included': True,
                'currency': 'RUB',
                'images': [('https://cdn.ostrovok.ru/t/1024x768/ext/06/90/'
                            '0690f13918e6808c03212b40619f466123c5d966.jpeg')],
                'is_travel_policy_compliant': False,
                'price_total': 140.0,
                'price_per_night': 20.0,
                'is_booking_by_request': False,
            },
            {
                'index': 1,
                'description': 'asdf2',
                'name': 'Без категории, тип кровати может измениться',
                'meal_names': [],
                'is_meal_included': False,
                'currency': 'RUB',
                'images': [],
                'is_travel_policy_compliant': True,
                'price_total': 707.0,
                'price_per_night': 101.0,
                'is_booking_by_request': False,
            },
        ],
    }

    req_path = 'intranet.trip.src.lib.aviacenter.api.aviacenter._make_request'

    search_id = 'wqr'
    search_object_id = 'asdwqw'
    coded_id = dict_to_b64json({'search_id': search_object_id})

    async def mock_request(*, method, path, params):
        assert method == 'get'
        assert path == '/hotel/search/view'
        assert params == {
            'company_id': settings.AVIACENTER_COMPANY_ID,
            'search_id': search_object_id,
        }
        return aviacenter_hotel_details_response

    with patch(req_path, mock_request):
        res = await client.get(
            f'api/provider/search/hotel/{search_id}/results/{coded_id}',
            params={'provider': 'aviacenter'},
        )
        assert res.json() == expected_answer
