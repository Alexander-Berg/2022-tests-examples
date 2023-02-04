from maps.garden.libs.masstransit_validation.tests.compare_requests import compare_requests_sets
from maps.masstransit.tools.compare_routers.lib.ammo import (
    read_masstransit_validation_config,
    read_masstransit_data_validation_config,
    RequestsSet, Request
)

MASSTRANSIT_ROUTES_CONFIG = 'masstransit_tester/config.unittest.json'


def test_masstransit_config():
    masstransit_config = read_masstransit_validation_config(MASSTRANSIT_ROUTES_CONFIG)
    test_validate = \
    [
        RequestsSet(
            'masstransit_tester/requests_unittest',
            {
                'maximum_different_routes': 0,
                'exists_at_least': 1.0
            },
            [
                Request(
                    {
                        'name': 'Moscow1',
                        'from': {
                            'lon': 37.593525,
                            'lat': 55.735298
                        },
                        'to': {
                            'lon': 37.579128,
                            'lat': 55.774099
                        },
                        'important': True,
                        'avoid_types': []
                    }
                ),
                Request(
                    {
                        'name': 'Moscow2',
                        'from': {
                            'lon': 37.579128,
                            'lat': 55.774099
                        },
                        'to': {
                            'lon': 37.583660,
                            'lat': 55.775061
                        },
                        'important': True,
                        'avoid_types': ['bus']
                    }
                )
            ]
        )
    ]
    compare_requests_sets(masstransit_config, test_validate)


def test_masstransit_data_config():
    masstransit_data_config = read_masstransit_data_validation_config(MASSTRANSIT_ROUTES_CONFIG)
    assert masstransit_data_config.stops_max_relative_difference == 0.05
    assert masstransit_data_config.routes_max_relative_difference == 0.05
    assert masstransit_data_config.threads_max_relative_difference == 0.05
