
search_info_departure_city = {
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
    'image_url': 'some_scheme://some_url',
    'code': 'MOW',
    'name': {'ru': 'Москва', 'en': 'Moscow'},
}

search_info_arrival_city = {
    'id': 9620,
    'type': 'City',
    'terminal': None,
    'address': None,
    'located_in': {
        'id': 10,
        'type': 'Country',
        'terminal': None,
        'address': None,
        'located_in': None,
        'time_zone_offset': None,
        'image_url': None,
        'code': 'BY',
        'name': {'ru': 'Беларусь', 'en': 'Belarus'},
    },
    'time_zone_offset': '03:00:00',
    'image_url': 'some_scheme://some_url',
    'code': 'MSQ',
    'name': {'ru': 'Минск', 'en': 'Minsk'},
}

search_info_profile = {
    'id': 734379,
    'first_name': {'ru': 'Альберт', 'en': 'Albert'},
    'middle_name': {'ru': 'Яндексович', 'en': None},
    'last_name': {'ru': 'Айнштайн', 'en': 'Einstein'},
    'company_info': {
        'id': 42436,
        'company_name': {'ru': 'ЯНДЕКС', 'en': 'YANDEX'},
        'holding_name': {'ru': 'ЯНДЕКС ХОЛДИНГ', 'en': 'YANDEX HOLDING'},
    },
    'avatar_url': None,
    'sex': 'Male',
}

TEST_COUNT_RESPONSE = {
    'data': {
        'count': 10,
        'expires_at_utc': '',
    },
    'errors': [],
    'is_success': False,
    'message': 'All good',
}

TEST_RAIL_RESULTS_RESPONSE = {
    'items_per_page': 20,
    'page_number': 0,
    'items_count': 2,
    'message': None,
    'errors': None,
    'is_success': True,
    'data': [
        {
            'client_id': 42436,
            'profile_grade_id': None,
            'departure_at': '2022-06-10T00:35:00',
            'departure_at_utc': '2022-06-09T21:35:00Z',
            'is_local_departure_at': True,
            'arrival_at': '2022-06-17T06:03:00',
            'arrival_at_utc': '2022-06-16T20:03:00Z',
            'is_local_arrival_at': True,
            'train_code': '002Э',
            'train_name': 'Россия',
            'electronic_registration': True,
            'duration': '6.22:28:00',
            'tariff_total': 10636.4,
            'tariff_service_fee': 260.0,
            'key': 'somekey==',
            'option_number': 1,
            'sequence_number': None,
            'currency': 'RUB',
            'is_corporate_tariff': False,
            'service_type': 'Rail',
            'is_favourite': False,
            'carriages_info': [
                {
                    'min_price': 10636.4,
                    'type': 'Reserved',
                    'carriage_owner': 'ФПК',
                    'is_travel_policy_compliant': True,
                    'travel_policy_violations': ["\rAAAAA\n\n"],
                    'seat_count': 302,
                },
            ],
            'departure_from': {
                'city': {
                    'time_zone_offset': '03:00:00',
                    'country': {
                        'id': 156,
                        'code': 'RUS',
                        'name': {
                            'ru': 'Россия',
                            'en': 'Russian Federation',
                        },
                    },
                    'id': 16381,
                    'code': None,
                    'name': {
                        'ru': 'Москва',
                        'en': 'Moscow',
                    },
                },
                'id': 19378,
                'code': None,
                'name': {
                    'ru': 'Москва-Ярославская',
                    'en': 'Moscow Yaroslavskaya',
                },
            },
            'arrival_to': {
                'city': {
                    'time_zone_offset': '10:00:00',
                    'country': {
                        'id': 156,
                        'code': 'RUS',
                        'name': {
                            'ru': 'Россия',
                            'en': 'Russian Federation',
                        },
                    },
                    'id': 15111,
                    'code': None,
                    'name': {
                        'ru': 'Владивосток',
                        'en': 'Vladivostok',
                    },
                },
                'id': 23623,
                'code': None,
                'name': {
                    'ru': 'Владивосток',
                    'en': 'Vladivostok',
                },
            },
        },
    ],
}

TEST_RAIL_SEARCH_INFO_RESPONSE = {
    'data': {
        'number': 64717,
        'created_at_utc': '2022-05-23T12:02:37Z',
        'updated_at_utc': '2022-05-23T12:02:40Z',
        'status': 'Completed',
        'requested_by': search_info_profile,
        'options': [
            {
                'service_type': 'Rail',
                'comment': None,
                'departure_city': search_info_departure_city,
                'arrival_city': search_info_arrival_city,
                'departure_on': '2022-05-30T00:00:00',
                'round_trip_departure_on': None,
                'checkin_on': None,
                'checkout_on': None,
                'sequence_number': None,
                'number': 1,
                'status': 'Completed',
                'search_mode': None,
                'avia_search_mode': {'cabin_class': 'NotSet'},
            },
        ],
        'company_id': 42436,
        'favourites_count': 0,
        'results_count': {
            'variants_count': 6,
            'variants_count_avia': 0,
            'variants_count_rail': 6,
            'variants_count_rail_round_trip': 0,
            'variants_count_hotel': 0,
            'expires_at_utc': '2022-05-23T13:02:40Z',
        },
        'profile_level': None,
        'expires_at_utc': '2022-05-23T13:02:40Z',
        'profile': search_info_profile,
        'is_active': True,
        'is_read': False,
        'departure_on': '2022-05-30T00:00:00',
        'round_trip_departure_on': None,
        'departure_city': search_info_departure_city,
        'arrival_city': search_info_arrival_city,
        'is_expired': False,
    },
    'message': None,
    'errors': None,
    'is_success': True,
}

TEST_RAIL_FILTERS_RESPONSE = {
    'data': [
        {
            'parameter_name': 'traincarriageowner',
            'values': [{'code': 'ТВЕРСК', 'name': {'ru': 'ТВЕРСК', 'en': None}}]},
        {
            'parameter_name': 'departurefromthere',
            'values': [
                {
                    'code': 's285227',
                    'name': {
                        'ru': 'Москва, Восточный',
                        'en': 'Moscow, Vostochny'
                    },
                },
            ],
        },
        {
            'parameter_name': 'arrivaltothere',
            'values': [
                {
                    'code': 's20418',
                    'name': {
                        'ru': 'Санкт-Петербург',
                        'en': 'Saint Petersburg',
                    },
                },
            ],
        },
        {
            'parameter_name': 'carriagetype',
            'values': [
                {'code': 'Reserved', 'name': {'ru': 'Плацкарт', 'en': 'Reserved'}},
                {'code': 'Coupe', 'name': {'ru': 'Купе', 'en': 'Coupe'}},
            ],
        },
        {
            'parameter_name': 'traincategory',
            'values': [{'code': 'Fast', 'name': {'ru': 'Скорый', 'en': 'Fast'}}]},
        {
            'parameter_name': 'trainname',
            'values': [
                {'code': '0KHQkNCf0KHQkNCd', 'name': {'ru': 'САПСАН', 'en': None}},
            ],
        },
    ],
    'message': None,
    'errors': None,
    'is_success': True,
}

TEST_RAIL_DETAIL_RESPONSE = {
    'data': {
        'departure_from': {
            'city': {
                'time_zone_offset': '03:00:00',
                'country': {
                    'id': 156,
                    'code': 'RUS',
                    'name': {
                        'ru': 'Россия',
                        'en': 'Russian Federation',
                    },
                },
                'id': 16381,
                'code': None,
                'name': {'ru': 'Москва', 'en': 'Moscow'},
            },
            'id': 21469,
            'code': None,
            'name': {
                'ru': 'Ленинградский вокзал (Москва-Октябрьская)',
                'en': 'Leningradsky railway station (Moscow Oktyabrskaya)'},
        },
        'arrival_to': {
            'city': {
                'time_zone_offset': '03:00:00',
                'country': {
                    'id': 156,
                    'code': 'RUS',
                    'name': {
                        'ru': 'Россия',
                        'en': 'Russian Federation',
                    },
                },
                'id': 15613,
                'code': None,
                'name': {'ru': 'Санкт-Петербург', 'en': 'Saint Petersburg'},
            },
            'id': 20418,
            'code': None,
            'name': {'ru': 'Санкт-Петербург-Московский', 'en': 'Saint Petersburg Moskovsky'},
        },
        'departure_at': '2022-06-13T17:40:00',
        'departure_at_utc': '2022-06-13T14:40:00Z',
        'is_local_departure_at': True,
        'arrival_at': '2022-06-13T21:35:00',
        'arrival_at_utc': '2022-06-13T18:35:00Z',
        'duration': '03:55:00',
        'is_local_arrival_at': True,
        'train_code': '778А',
        'train_name': 'САПСАН',
        'train_category': None,
        'branded': False,
        'currency': 'RUB',
        'carriage_details': [
            {
                'index': 0,
                'is_travel_policy_compliant': False,
                'travel_policy_violation_reason': None,
                'travel_policy_violations': [
                    'Разрешены вагоны классов купе, плацкартный, сидячий, общий',
                    'Разрешен вагон класса Эконом и Эконом+\r\n',
                ],
                'carriage_type': 'Lux',
                'carriage_owner': 'ДОСС',
                'carriage_num': '001',
                'places': [
                    {'number': 21, 'compartment_type': 'WoDesc'},
                    {'number': 23, 'compartment_type': 'Man'},
                ],
                'is_electronic_registration_allowed': True,
                'carriage_class_options': [],
                'services_description': [],
                'class': {'code': '1Е', 'name': {'ru': 'Русское', 'en': 'English'}},
                'places_summary': {
                    'bottom': 2,
                    'bottom_side': 0,
                    'female': 0,
                    'man': 0,
                    'mixed': 0,
                    'not_defined': 0,
                    'top': 0,
                    'top_side': 0,
                    'wo_desc': 0,
                },
                'min_price_for_class': 18754.4,
                'max_price_for_class': 18764.4,
                'is_two_floor': False,
                'schema': 'SAPSAN_W1_V2',
            },
        ],
        'service_type': 'Rail',
        'key': 'somekey',
        'start_on': None,
        'end_on': None,
        'tariff_total': 0.0,
        'is_corporate_tariff': False,
        'is_travel_policy_compliant': None,
        'tariff_tax': None,
        'corporate_tariff_code': None,
        'start_on_utc': None,
        'end_on_utc': None,
        'reservation_cancellation_policy': None,
        'grade_custom_list_property_value_id': None,
        'travel_policy_violation_reason': None,
        'travel_policy_violations': [],
        'tariff_service_fee': 0.0,
    },
    'message': None,
    'errors': None,
    'is_success': True,
}

TEST_HOTEL_RESULTS_RESPONSE = {
    'items_per_page': 15,
    'page_number': 0,
    'items_count': 20,
    'data': [
        {
            'hotel': {
                'code': '1906328',
                'name': {'ru': 'Друзья', 'en': 'Druzya'},
            },
            'city': {
                'time_zone_offset': '03:00:00',
                'country': {
                    'id': 156,
                    'code': 'RUS',
                    'name': {'ru': 'Россия', 'en': 'Russian'},
                },
                'id': 0,
                'code': '15613',
                'name': {'ru': 'Спб', 'en': 'Spb'},
            },
            'is_direct_contract': False,
            'stars': 0,
            'geo_position': {
                'latitude': 59.93357467651367,
                'longitude': 30.36038589477539,
            },
            'image_url': None,
            'is_recommended': None,
            'is_top_hotel': False,
            'recommendations': None,
            'recommendations_enhanced': 'Undefinded',
            'start_on': '2022-07-25T00:00:00',
            'end_on': '2022-07-26T00:00:00',
            'tariffs': [
                {
                    'price_per_night': 1204.0,
                    'payment_place': 'Agency',
                    'total_price': 1204.0,
                    'service_fee': 500.0,
                    'is_travel_policy_compliant': True,
                    'travel_policy_violations': [],
                    'is_corporate_tariff': False,
                    'confirmation_type': 'InstantConfirmation',
                    'index': 0,
                },
            ],
            'is_travel_policy_compliant': None,
            'address': {
                'ru': 'Санкт-Петербург',
                'en': 'Spb',
            },
            'description': {'ru': 'Описание', 'en': 'Eng'},
            'key': 'somekey==',
            'option_number': 1,
            'sequence_number': None,
            'currency': 'RUB',
            'is_corporate_tariff': False,
            'service_type': 'Hotel',
            'is_favourite': False
        },
    ],
    'message': None,
    'errors': None,
    'is_success': True,
}

TEST_HOTEL_SEARCH_INFO_RESPONSE = {
    'data': {
        'number': 65305,
        'created_at_utc': '2022-06-21T14:49:04Z',
        'updated_at_utc': '2022-06-21T14:49:51Z',
        'status': 'Completed',
        'requested_by': search_info_profile,
        'options': [
            {
                'service_type': 'Hotel',
                'comment': None,
                'departure_city': None,
                'arrival_city': search_info_arrival_city,
                'departure_on': None,
                'round_trip_departure_on': None,
                'checkin_on': '2022-07-25T00:00:00',
                'checkout_on': '2022-07-31T00:00:00',
                'sequence_number': None,
                'number': 1,
                'status': 'Completed',
                'search_mode': ['CorporateAndFast', 'NoncorporateAndFast'],
                'avia_search_mode': {'cabin_class': 'NotSet'},
            },
        ],
        'company_id': 42436,
        'favourites_count': 0,
        'results_count': {
            'variants_count': 1053,
            'variants_count_avia': 0,
            'variants_count_rail': 0,
            'variants_count_rail_round_trip': 0,
            'variants_count_hotel': 1053,
            'expires_at_utc': '2022-06-21T15:49:47Z',
        },
        'profile_level': None,
        'expires_at_utc': '2022-06-21T15:49:47Z',
        'profile': search_info_profile,
        'is_active': True,
        'is_read': False,
        'departure_on': '2022-07-25T00:00:00',
        'round_trip_departure_on': '2022-07-31T00:00:00',
        'departure_city': None,
        'arrival_city': search_info_arrival_city,
        'is_expired': False,
    },
    'message': None,
    'errors': None,
    'is_success': True,
}

TEST_HOTEL_FILTERS_RESPONSE = {
    'data': [
        {
            'parameter_name': 'hoteltypes',
            'values': [
                {'code': '7', 'name': {'ru': 'Мотель', 'en': 'Motel'}},
                {'code': '22', 'name': {'ru': 'Курорт', 'en': 'Resort'}},
            ],
        },
        {
            'parameter_name': 'confirmationtype',
            'values': [
                {'code': 'OnRequest', 'name': {'ru': 'Под запрос', 'en': 'On request'}},
            ],
        },
        {
            'parameter_name': 'pricerange',
            'values': [
                {
                    'code': 'min',
                    'name': {
                        'ru': '1204,000',
                        'en': '1204,000',
                    },
                },
                {
                    'code': 'max',
                    'name': {
                        'ru': '64311,5900',
                        'en': '64311,5900',
                    },
                },
            ],
        },
    ],
    'message': None,
    'errors': None,
    'is_success': True,
}

TEST_HOTEL_DETAIL_RESPONSE = {
    'data': {
        'hotel': {
            'code': '1906328',
            'name': {'ru': 'Друзья', 'en': 'Druzya'},
        },
        'city': {
            'time_zone_offset': '03:00:00',
            'country': {
                'id': 156,
                'code': 'RUS',
                'name': {'ru': 'Россия', 'en': 'Russian'},
            },
            'id': 0,
            'code': '15613',
            'name': {'ru': 'Санкт', 'en': 'Saint'},
        },
        'stars': 0,
        'rooms': [
            {
                'room_name': 'койко',
                'total_price': 4114.0,
                'actual_checkin_at': '2022-07-25T14:00:00',
                'actual_checkin_at_utc': '2022-07-25T11:00:00Z',
                'actual_checkout_at': '2022-07-31T12:00:00',
                'actual_checkout_at_utc': '2022-07-31T09:00:00Z',
                'service_type': 'Hotel',
                'additional_room_info': '150 рублей',
                'supplier_room_id': '133661',
                'is_pay_on_site': False,
                'booking_code': 'book-code',
                'room_rate_id': 'B891B1A16EF8C773FCE187C033FEE3AF',
                'sale_type': 'InstantConfirmation',
                'meals': [],
                'currency': 'RUB',
                'tariff_per_night': 769.0,
                'index': 3,
                'tariff_expected_cancellation_penalty': 4114.0,
                'tariff_vat': 0.0,
                'vat_type': 'Undefined',
                'allocation_is_limited': True,
                'check_in_at': '2022-07-25T14:00:00',
                'check_in_at_utc': '2022-07-25T11:00:00Z',
                'check_out_at': '2022-07-31T12:00:00',
                'check_out_at_utc': '2022-07-31T09:00:00Z',
                'room_id': '3ecf126e-13a7-4818-8ffc-28115fff6750',
                'tariff_total': 4614.0,
                'tariff_adult': 4114.0,
                'is_travel_policy_compliant': True,
                'tariff_service_fee': 500.0,
                'is_corporate_tariff': False,
                'corporate_tariff_code': 'ACB',
                'allocation_adults': 1,
                'reservation_cancellation_policy': None,
                'travel_policy_violation_reason': None,
                'travel_policy_violations': [],
                'is_hidden_price': False,
                'images': [],
                'room_description': {'russian': 'описание', 'english': 'descr'},
                'number_of_free_rooms': 11,
                'is_discount_offer': False},
        ],
        'address': {
            'ru': 'Спб',
            'en': 'Spb',
        },
        'geo_position': {'latitude': 59.93357467651367, 'longitude': 30.36038589477539},
        'images': [],
        'hotel_images': [],
        'web_site': None,
        'is_recommended': None,
        'rating': 0,
        'is_top_hotel': False,
        'recommendations': None,
        'service_type': 'Hotel',
        'key': 'some-key',
        'start_on': '2022-07-25T00:00:00',
        'end_on': '2022-07-31T00:00:00',
        'tariff_total': 0.0,
        'is_corporate_tariff': False,
        'is_travel_policy_compliant': None,
        'tariff_tax': None,
        'corporate_tariff_code': None,
        'start_on_utc': '2022-07-24T21:00:00Z',
        'end_on_utc': '2022-07-30T21:00:00Z',
        'reservation_cancellation_policy': None,
        'grade_custom_list_property_value_id': None,
        'travel_policy_violation_reason': None,
        'travel_policy_violations': [],
        'tariff_service_fee': 0.0,
    },
    'message': None,
    'errors': None,
    'is_success': True,
}


dme_avia = {
    'terminal': None,
    'city': {
        'time_zone_offset': '03:00:00',
        'country': {
            'id': 156,
            'code': 'RUS',
            'name': {'ru': 'Россия', 'en': 'Russian Federation'},
        },
        'id': 16381,
        'code': 'MOW',
        'name': {'ru': 'Москва', 'en': 'Moscow'},
    },
    'id': 39781,
    'code': 'DME',
    'name': {'ru': 'Домодедово', 'en': 'Domodedovo'},
}

led_avia = {
    'terminal': None,
    'city': {
        'time_zone_offset': '03:00:00',
        'country': {
            'id': 156,
            'code': 'RUS',
            'name': {'ru': 'Россия', 'en': 'Russian Federation'},
        },
        'id': 15613,
        'code': 'LED',
        'name': {'ru': 'Санкт', 'en': 'Saint'},
    },
    'id': 39481,
    'code': 'LED',
    'name': {'ru': 'Пулково', 'en': 'Pulkovo'},
}

TEST_AVIA_RESULTS_RESPONSE = {
    'items_per_page': 20,
    'page_number': 0,
    'items_count': 15,
    'data': [
        {
            'mini_rules': [
                {'category': 'Changeable', 'penalty': 0.0, 'is_empty_rule': False},
                {'category': 'Refundable', 'penalty': 0.0, 'is_empty_rule': True},
            ],
            'union_fare_family': None,
            'flight_option_id': '1044aa2f-6306-41b2-9138-720d113b741e',
            'legs': [
                {
                    'departure_from': dme_avia,
                    'arrival_to': led_avia,
                    'departure_at': '2022-07-25T10:15:00',
                    'departure_at_utc': '2022-07-25T07:15:00Z',
                    'arrival_at': '2022-07-25T11:40:00',
                    'arrival_at_utc': '2022-07-25T08:40:00Z',
                    'cabin_class': 'Econom',
                    'transfers': 1,
                    'duration': '01:25:00',
                    'segments': [
                        {
                            'flight_number': '105',
                            'marketing_airline': {
                                'code': '4G',
                                'name': {'ru': 'Газпром авиа', 'en': 'Gazpromavia'},
                            },
                            'operating_airline': {
                                'code': None,
                                'name': {
                                    'ru': None,
                                    'en': None
                                },
                            },
                            'fare_basis': 'Y',
                            'fare_family': None,
                        },
                    ],
                    'baggage': {'quantity': None, 'weight_measurement': None, 'type': 'NotSet'},
                    'seats_available': 9,
                },
            ],
            'is_travel_policy_compliant': True,
            'travel_policy_violations': [],
            'arrival_to': {
                'time_zone_offset': '03:00:00',
                'country': {
                    'id': 156,
                    'code': 'RUS',
                    'name': {'ru': 'Россия', 'en': 'Russian Federation'},
                },
                'id': 15613,
                'code': 'LED',
                'name': {'ru': 'Санкт-Петербург', 'en': 'Saint Petersburg'},
            },
            'departure_from': {
                'time_zone_offset': '03:00:00',
                'country': {
                    'id': 156,
                    'code': 'RUS',
                    'name': {'ru': 'Россия', 'en': 'Russian Federation'},
                },
                'id': 16381,
                'code': 'MOW',
                'name': {'ru': 'Москва', 'en': 'Moscow'},
            },
            'tariff_total': 685.0,
            'tariff_service_fee': 200.0,
            'key': 'somekey==',
            'option_number': 1,
            'sequence_number': None,
            'currency': 'RUB',
            'is_corporate_tariff': False,
            'service_type': 'Avia',
            'is_favourite': False,
        },
    ],
    'message': None,
    'errors': None,
    'is_success': True,
}

TEST_AVIA_SEARCH_INFO_RESPONSE = {
    'data': {
        'number': 65306,
        'created_at_utc': '2022-06-21T15:05:16Z',
        'updated_at_utc': '2022-06-21T15:05:34Z',
        'status': 'Completed',
        'requested_by': search_info_profile,
        'options': [
            {
                'service_type': 'Avia',
                'comment': None,
                'departure_city': search_info_departure_city,
                'arrival_city': search_info_arrival_city,
                'departure_on': '2022-07-25T00:00:00',
                'round_trip_departure_on': '2022-07-26T00:00:00',
                'checkin_on': None,
                'checkout_on': None,
                'sequence_number': None,
                'number': 1,
                'status': 'Completed',
                'search_mode': None,
                'avia_search_mode': {'cabin_class': 'NotSet'},
            },
        ],
        'company_id': 42436,
        'favourites_count': 0,
        'results_count': {
            'variants_count': 121,
            'variants_count_avia': 121,
            'variants_count_rail': 0,
            'variants_count_rail_round_trip': 0,
            'variants_count_hotel': 0,
            'expires_at_utc': '2022-06-21T16:05:34Z',
        },
        'profile_level': None,
        'expires_at_utc': '2022-06-21T16:05:34Z',
        'profile': search_info_profile,
        'is_active': True,
        'is_read': False,
        'departure_on': '2022-07-25T00:00:00',
        'round_trip_departure_on': None,
        'departure_city': search_info_departure_city,
        'arrival_city': search_info_arrival_city,
        'is_expired': False,
    },
    'message': None,
    'errors': None,
    'is_success': True,
}

TEST_AVIA_FILTERS_RESPONSE = {
    'data': [
        {
            'parameter_name': 'aircompany',
            'values': [
                {'code': 'SU', 'name': {'ru': 'Аэрофлот', 'en': 'Aeroflot'}},
            ],
        },
        {
            'parameter_name': 'departurefromthere',
            'values': [
                {
                    'code': 'a39780',
                    'name': {'ru': 'Внуково', 'en': 'Vnukovo'},
                },
            ],
        },
        {
            'parameter_name': 'arrivaltothere',
            'values': [
                {
                    'code': 'a37081',
                    'name': {
                        'ru': 'Минск, Интернэйшнл, MSQ',
                        'en': 'Minsk, Minsk 2, MSQ',
                    },
                },
            ],
        },
        {
            'parameter_name': 'cabinclass',
            'values': [
                {'code': 'Business', 'name': {'ru': 'Бизнес', 'en': 'Business'}},
                {'code': 'Econom', 'name': {'ru': 'Эконом', 'en': 'Econom'}},
            ],
        },
    ],
    'message': None,
    'errors': None,
    'is_success': True,
}

TEST_AVIA_DETAIL_RESPONSE = {
    'data': {
        'validating_airline_code': '4G',
        'is_segment_discount_variant': False,
        'is_segment_discount_as_exact_value': False,
        'mini_rules': [],
        'legs': [
            {
                'avia_variant_option_id': 0,
                'departure_from': dme_avia,
                'arrival_to': led_avia,
                'departure_at': '2022-07-25T10:15:00',
                'departure_at_utc': '2022-07-25T07:15:00Z',
                'arrival_at': '2022-07-25T11:40:00',
                'arrival_at_utc': '2022-07-25T08:40:00Z',
                'duration': '01:25:00',
                'segments': [
                    {
                        'departure_at': '2022-07-25T10:15:00',
                        'departure_at_utc': '2022-07-25T07:15:00Z',
                        'departure_from': dme_avia,
                        'arrival_at': '2022-07-25T11:40:00',
                        'arrival_at_utc': '2022-07-25T08:40:00Z',
                        'arrival_to': led_avia,
                        'flight_number': '105',
                        'cabin_class': 'Econom',
                        'duration': '01:25:00',
                        'marketing_airline': {
                            'code': '4G',
                            'name': {'ru': 'Газпром авиа', 'en': 'Gazpromavia'},
                        },
                        'operating_airline': {
                            'code': '4G',
                            'name': {'ru': 'Газпром авиа', 'en': 'Gazpromavia'},
                        },
                        'transfer': None,
                        'aircraft': {
                            'code': '737',
                            'name': {'ru': 'Боинг 737', 'en': 'Boeing 737'},
                        },
                        'fare_basis': 'Y',
                        'fare_family': None,
                        'baggage': {
                            'quantity': 15,
                            'weight_measurement': 100,
                            'type': 'Weight',
                        },
                        'seats_available': 9,
                    },
                ],
            },
            {
                'avia_variant_option_id': 0,
                'departure_from': led_avia,
                'arrival_to': dme_avia,
                'departure_at': '2022-07-31T19:00:00',
                'departure_at_utc': '2022-07-31T16:00:00Z',
                'arrival_at': '2022-07-31T21:35:00',
                'arrival_at_utc': '2022-07-31T18:35:00Z',
                'duration': '02:35:00',
                'segments': [
                    {
                        'departure_at': '2022-07-31T19:00:00',
                        'departure_at_utc': '2022-07-31T16:00:00Z',
                        'departure_from': led_avia,
                        'arrival_at': '2022-07-31T21:35:00',
                        'arrival_at_utc': '2022-07-31T18:35:00Z',
                        'arrival_to': dme_avia,
                        'flight_number': '106',
                        'cabin_class': 'Econom',
                        'duration': '02:35:00',
                        'marketing_airline': {
                            'code': '4G',
                            'name': {'ru': 'Газпром авиа', 'en': 'Gazpromavia'},
                        },
                        'operating_airline': {
                            'code': '4G',
                            'name': {'ru': 'Газпром авиа', 'en': 'Gazpromavia'},
                        },
                        'transfer': None,
                        'aircraft': {
                            'code': '737',
                            'name': {'ru': 'Боинг 737', 'en': 'Boeing 737'},
                        },
                        'fare_basis': 'Y',
                        'fare_family': None,
                        'baggage': {
                            'quantity': None,
                            'weight_measurement': None,
                            'type': 'NotSet',
                        },
                        'seats_available': 9,
                    },
                ],
            },
        ],
        'service_type': 'Avia',
        'key': 'somekey=',
        'start_on': '2022-07-25T10:15:00',
        'end_on': '2022-07-31T21:35:00',
        'tariff_total': 1170.0,
        'is_corporate_tariff': False,
        'is_travel_policy_compliant': True,
        'tariff_tax': 370.0,
        'corporate_tariff_code': None,
        'start_on_utc': '2022-07-25T07:15:00Z',
        'end_on_utc': '2022-07-31T18:35:00Z',
        'reservation_cancellation_policy': None,
        'grade_custom_list_property_value_id': None,
        'travel_policy_violation_reason': None,
        'travel_policy_violations': [],
        'tariff_service_fee': 200.0,
    },
    'message': None,
    'errors': None,
    'is_success': True,
}
