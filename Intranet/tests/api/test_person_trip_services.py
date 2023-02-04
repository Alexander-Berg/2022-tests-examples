import pytest

from mock import patch
from intranet.trip.src.enums import ServiceType


pytestmark = pytest.mark.asyncio


traveller = {
    'id': 8572925,
    'type': 'Adult',
    'profile': {
        'id': 2436969,
        'first_name': {'ru': 'Имя', 'en': 'First'},
        'middle_name': {'ru': 'Отчество', 'en': 'Middle'},
        'last_name': {'ru': 'Фамилия', 'en': 'Second'},
        'company_info': {
            'id': 54673,
            'company_name': {'ru': 'ЯНДЕКС', 'en': 'YANDEX TECHNOLOGY'},
            'holding_name': {'ru': 'ЯНДЕКС', 'en': 'YANDEX HOLDING'},
        },
        'avatar_url': None,
        'sex': 'Male',
    },
    'bonus_card': None,
    'documents': [
        {
            'id': 2745395,
            'type': 'NationalPassport',
            'first_name': 'Имя',
            'last_name': 'Фамилия',
            'document_number': '**1234',
        },
    ],
    'tickets': [
        {
            'number': '123-1234234234',
            'attachments': [
                {
                    'url': 'https://fs.aeroclub.ru/v1/blob/kek.pdf',
                    'name': 'kek.pdf',
                    'content_type': 'application/pdf',
                    'length': 233602,
                },
            ],
        },
    ],
}


document = {
    'ticket_token': None,
    'created_at': '2021-04-26T12:46:17',
    'deleted_at_field': None,
    'attachments': [
        {
            'url': 'https://fs.aeroclub.ru/v1/blob/kek.pdf',
            'name': 'kek1.pdf',
            'content_type': 'application/pdf',
            'length': 332080,
        },
        {
            'url': 'https://fs.aeroclub.ru/v1/blob/kek2.pdf',
            'name': 'kek2.pdf',
            'content_type': 'application/pdf',
            'length': 248053,
        },
    ],
    'location': None,
}


avia_arrival_to = {
    'id': 38045,
    'type': 'Airport',
    'terminal': None,
    'address': {
        'name': None,
        'coordinates': {'latitude': 45.052222, 'longitude': 33.975139},
    },
    'located_in': {
        'id': 12053,
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
        'image_url': None,
        'code': 'SIP',
        'name': {'ru': 'Симферополь', 'en': 'Simferopol'},
    },
    'time_zone_offset': '03:00:00',
    'image_url': None,
    'code': 'SIP',
    'name': {'ru': 'Симферополь', 'en': 'Simferopol Intl'},
}


moscow_location = {
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
    'image_url': 'https://fs.aeroclub.ru/v1/blob/kek.jpg',
    'code': 'MOW',
    'name': {'ru': 'Москва', 'en': 'Moscow'},
}


spb_location = {
    'id': 15613,
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
    'image_url': 'https://fs.aeroclub.ru/v1/blob/kek.jpg',
    'code': 'LED',
    'name': {'ru': 'Санкт-Петербург', 'en': 'Saint Petersburg'},
}


avia_departure_from = {
    'id': 39781,
    'type': 'Airport',
    'terminal': None,
    'address': {
        'name': None,
        'coordinates': {'latitude': 55.414914, 'longitude': 37.89963},
    },
    'located_in': moscow_location,
    'time_zone_offset': '03:00:00',
    'image_url': None,
    'code': 'DME',
    'name': {'ru': 'Домодедово', 'en': 'Domodedovo'},
}


ac_avia_service = {
    'status': 'Active',
    'authorization_status': 'Authorized',
    'authorization_assertion': 'Required',
    'available_actions': 'ContactOperator, Cancel, Change',
    'service_request': 'Unknown',
    'service_state': 'Execution',
    'analytics': {
        'is_corporate_tariff': False,
        'is_travel_policy_compliant': True,
        'travel_policy_violation_reason': None,
        'travel_policy_violations': [],
    },
    'tariff_total': 5601,
    'reservation_expires_at_utc': None,
    'end_at_utc': '2021-04-27T19:20:00Z',
    'end_at': '2021-04-27T22:20:00',
    'start_at_utc': '2021-04-27T16:45:00Z',
    'start_at': '2021-04-27T19:45:00',
    'order_number': 1,
    'type': 'Avia',
    'number': 1,
    'id': 21612373,
    'reservation_token': 'W4D8C9',
    'reservation_expires_at': None,
    'travellers': [
        traveller,
    ],
    'documents': [
        document,
    ],
    'avia_leg': [
        {
            'transfers': 0,
            'arrival_to': avia_arrival_to,
            'departure_from': avia_departure_from,
            'duration': '02:35:00',
            'arrival_at_utc': '2021-04-27T19:20:00Z',
            'arrival_at': '2021-04-27T22:20:00',
            'departure_at_utc': '2021-04-27T16:45:00Z',
            'departure_at': '2021-04-27T19:45:00',
            'id': 22816898,
            'segments': [
                {
                    'duration': '02:35:00',
                    'booking_class': {'code': 'Q', 'name': None},
                    'flight_number': '2013',
                    'airline': {
                        'code': 'S7',
                        'code_ru': 'С7',
                        'name': {'ru': 'S7 Airlines', 'en': 'S7 Airlines'},
                        'registration_links': {
                            'web_link': {
                                'ru': 'https://webcheckin.s7.ru/login.action',
                                'en': 'https://webcheckin.s7.ru/login.action',
                            },
                            'pda_link': {
                                'ru': 'https://webcheckin.s7.ru/login.action',
                                'en': 'https://webcheckin.s7.ru/login.action',
                            },
                        },
                    },
                    'baggage': {
                        'type': 'Seats',
                        'quantity': 0,
                        'weigh_measurement': None,
                    },
                    'class_of_service': {
                        'code': 'Economy',
                        'name': {'ru': 'Эконом', 'en': 'Economy'},
                    },
                    'arrival_at_utc': '2021-04-27T19:20:00Z',
                    'arrival_at': '2021-04-27T22:20:00',
                    'departure_at_utc': '2021-04-27T16:45:00Z',
                    'departure_at': '2021-04-27T19:45:00',
                    'arrival_to': avia_arrival_to,
                    'departure_from': avia_departure_from,
                    'operating_airline_code': 'S7',
                    'marketing_airline_code': 'S7',
                    'id': 29120613,
                    'air_company_reservation_control_number': 'W4D8C9',
                    'flight_status': 'Unknown',
                    'actual_arrival_at_utc': None,
                    'actual_arrival_at': None,
                    'actual_departure_at_utc': None,
                    'actual_departure_at': None,
                    'aircraft': {'ru': 'Боинг 737-800', 'en': 'Boeing 737-800'},
                },
            ],
        },
    ],
}


ac_rail_service = {
    'status': 'Active',
    'authorization_status': 'Authorized',
    'authorization_assertion': 'Required',
    'available_actions': 'ContactOperator, Cancel, Change',
    'service_request': 'Unknown',
    'service_state': 'Execution',
    'analytics': {
        'is_corporate_tariff': False,
        'is_travel_policy_compliant': True,
        'travel_policy_violation_reason': None,
        'travel_policy_violations': [],
    },
    'tariff_total': 4310.3,
    'reservation_expires_at_utc': None,
    'end_at_utc': '2021-04-30T11:46:00Z',
    'end_at': '2021-04-30T14:46:00',
    'start_at_utc': '2021-04-30T08:00:00Z',
    'start_at': '2021-04-30T11:00:00',
    'order_number': 2,
    'type': 'Rail',
    'number': 1,
    'id': 21628045,
    'reservation_token': None,
    'reservation_expires_at': '2021-04-26T15:59:00',
    'travellers': [
        traveller,
    ],
    'documents': [
        document,
    ],
    'duration': '03:46:00',
    'is_local_time': True,
    'carriage': {
        'type': {'ru': 'Сидячий', 'en': 'Seat'},
        'number': '06',
        'class_of_service': {'code': '2С', 'name': None},
        'owner': 'ДОСС',
        'electronic_registration_allowed': True,
    },
    'train': {
        'name': {'ru': 'САПСАН', 'en': None},
        'category_name': None,
        'number': '761А',
    },
    'arrival_to': {
        'id': 21469,
        'type': 'RailwayStation',
        'terminal': None,
        'address': {
            'name': None,
            'coordinates': {'latitude': 55.776127, 'longitude': 37.655347},
        },
        'located_in': moscow_location,
        'time_zone_offset': '03:00:00',
        'image_url': None,
        'code': None,
        'name': {
            'ru': 'Москва-Октябрьская (Ленинградский вокзал)',
            'en': 'Moscow Oktyabrskaya (Leningradsky Station)',
        },
    },
    'departure_from': {
        'id': 20418,
        'type': 'RailwayStation',
        'terminal': None,
        'address': {
            'name': None,
            'coordinates': {'latitude': 59.928672, 'longitude': 30.362545},
        },
        'located_in': spb_location,
        'time_zone_offset': '03:00:00',
        'image_url': None,
        'code': None,
        'name': {
            'ru': 'Санкт-Петербург-Московский',
            'en': 'Saint Petersburg Moskovsky',
        },
    },
    'arrival_at_utc': '2021-04-30T11:46:00Z',
    'arrival_at': '2021-04-30T14:46:00',
    'departure_at_utc': '2021-04-30T08:00:00Z',
    'departure_at': '2021-04-30T11:00:00',
}


ac_hotel_service = {
    'status': 'Archived',
    'authorization_status': 'Authorized',
    'authorization_assertion': 'Required',
    'available_actions': 'ContactOperator, Cancel, Change',
    'service_request': 'Unknown',
    'service_state': 'Execution',
    'analytics': {
        'is_corporate_tariff': True,
        'is_travel_policy_compliant': True,
        'travel_policy_violation_reason': None,
        'travel_policy_violations': [],
    },
    'tariff_total': 13300,
    'reservation_expires_at_utc': None,
    'end_at_utc': '2021-04-30T09:00:00Z',
    'end_at': '2021-04-30T12:00:00',
    'start_at_utc': '2021-04-28T11:00:00Z',
    'start_at': '2021-04-28T14:00:00',
    'order_number': 3,
    'type': 'Hotel',
    'number': 1,
    'id': 21628072,
    'reservation_token': '6697867',
    'reservation_expires_at': None,
    'travellers': [
        traveller,
    ],
    'documents': [
        document,
    ],
    'tariff_per_night': 6650,
    'nights': 2,
    'is_payment_in_place': False,
    'actual_checkout_at_utc': '2021-04-30T09:00:00Z',
    'actual_checkout_at': '2021-04-30T12:00:00',
    'actual_checkin_at_utc': '2021-04-28T11:00:00Z',
    'actual_checkin_at': '2021-04-28T14:00:00',
    'address': {
        'name': {
            'ru': 'улица Чайковского, 17',
            'en': 'ulitsa Chaykovskogo, 17',
        },
        'coordinates': {'latitude': 59.9463768005371, 'longitude': 30.347515106201},
    },
    'location': spb_location,
    'image': None,
    'stars': 5,
    'name': {
        'ru': 'Индиго Санкт Петербург Чайковского',
        'en': 'Indigo Saint Petersburg Chaykovskogo',
    },
    'relations': [],
    'room': {
        'meal': [
            {
                'name': 'Завтрак Шведский стол',
                'code': '0',
                'price': None,
                'is_included_in_price': True,
            },
        ],
        'sale_type': 'FreeSale',
        'room_category': {'ru': 'Стандарт', 'en': 'Standard'},
        'name': {
            'ru': 'Стандартный с видом в Атриум DBL',
            'en': 'standard Double with Atrium view',
        },
    },
    'additional_comment': None,
    'execution_comment': None,
    'hotel_conformation_code': 'ФИО',
    'is_late_check_out': True,
    'is_early_checkin': True,
    'allocation_adults': 1,
    'category': {'ru': 'Отель', 'en': 'Hotel'},
    'rating': 0,
    'images': [
        {
            'url': 'https://fs.aeroclub.ru/v1/blob/kek.jpg',
            'preview_url': 'https://fs.aeroclub.ru/v1/blob/kek2.jpg',
        },
    ],
}


class MockedAeroclubClient:

    async def get_service(self, order_id: int, service_id: int):
        if order_id == 1:
            return ac_avia_service
        if order_id == 2:
            return ac_rail_service
        if order_id == 3:
            return ac_hotel_service


@pytest.mark.parametrize('service_type, provider_order_id', (
    (ServiceType.avia, 1),
    (ServiceType.rail, 2),
    (ServiceType.hotel, 3),
))
async def test_person_trip_services(f, client, service_type, provider_order_id):
    person_id = 1
    trip_id = 1
    await f.create_person(person_id=person_id)
    await f.create_trip(trip_id=trip_id, person_ids=[person_id])
    await f.create_service(
        trip_id=trip_id,
        person_id=1,
        service_id=1,
        type=service_type,
        provider_service_id=1,
        provider_order_id=provider_order_id,
    )
    with patch('intranet.trip.src.api.endpoints.trips.aeroclub', MockedAeroclubClient()):
        response = await client.get(f'api/trips/{trip_id}/persons/{person_id}/services')

    assert response.status_code == 200
