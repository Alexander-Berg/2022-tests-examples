from maps.garden.libs.masstransit_validation.tests.compare_requests import compare_requests_sets
from maps.masstransit.tools.compare_routers.lib.ammo import (
    read_pedestrian_validation_config,
    RequestsSet, Request
)

PEDESTRIAN_ROUTES_CONFIG = 'pedestrian_tester/pedestrian_config.unittest.json'


def test_pedestrian_config():
    pedestrian_config = read_pedestrian_validation_config(PEDESTRIAN_ROUTES_CONFIG)
    test_validate = \
    [
        RequestsSet(
            'Moscow',
            {
                'maximum_different_routes': 0,
                'exists_at_least': 1.0
            },
            [
                Request(
                    {
                        'from':
                        {
                            'lon': 37.593525,
                            'lat': 55.735298
                        },
                        'to':
                        {
                            'lon': 37.579128,
                            'lat': 55.774099
                        }
                    }
                ),
                Request(
                    {
                        'from':
                        {
                            'lon': 37.579128,
                            'lat': 55.774099
                        },
                        'to':
                        {
                            'lon': 37.583660,
                            'lat': 55.775061
                        }
                    }
                )
            ]
        )
    ]
    compare_requests_sets(pedestrian_config, test_validate)
