"""Test physical location API"""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase
from walle.hosts import HostLocation
from walle.physical_location_tree import (
    LocationTree,
    LocationCountry,
    LocationCity,
    LocationDatacenter,
    LocationQueue,
    LocationRack,
    LocationNamesMap,
)


@pytest.fixture
def test(request):
    test = TestCase.create(request)
    return test


def mock_tree(tree_id, timestamp, paths):
    tree_dict = {}
    for path in paths:
        country, city, datacenter, queue, rack = path.split("|")
        tree_dict.setdefault(country, {}).setdefault(city, {}).setdefault(datacenter, {}).setdefault(queue, set())
        tree_dict[country][city][datacenter][queue].add(rack)

    tree_model = LocationTree(id=tree_id, timestamp=timestamp)
    for country, cities in tree_dict.items():
        country_model = LocationCountry(name=country)
        tree_model.countries.append(country_model)
        for city, datacenters in cities.items():
            city_model = LocationCity(name=city)
            country_model.cities.append(city_model)
            for datacenter, queues in datacenters.items():
                datacenter_model = LocationDatacenter(name=datacenter)
                city_model.datacenters.append(datacenter_model)
                for queue, racks in queues.items():
                    queue_model = LocationQueue(name=queue)
                    datacenter_model.queues.append(queue_model)
                    for rack in racks:
                        rack_model = LocationRack(name=rack)
                        queue_model.racks.append(rack_model)
    tree_model.save()
    return tree_model


def test_get_physical_tree_empty(test):
    """404 if there's no location's tree in the database"""
    result = test.api_client.get("/v1/physical-location-tree")
    assert result.status_code == http.client.NOT_FOUND

    response = result.json

    assert set(response.keys()) == {"message", "errors", "result"}
    assert response["result"] == "FAIL"


def test_get_physical_tree_filled(test):
    timestamp = 1
    mock_tree(
        "the_only_tree",
        timestamp,
        [
            "country|city|datacenter|queue|rack",
        ],
    )
    result = test.api_client.get("/v1/physical-location-tree")
    assert result.status_code == http.client.OK

    response = result.json

    expected_tree = {
        "result": [
            {
                "path": "country",
                "name": "country",
                "nodes": [
                    {
                        "path": "country|city",
                        "name": "city",
                        "nodes": [
                            {
                                "path": "country|city|datacenter",
                                "name": "datacenter",
                                "nodes": [
                                    {
                                        "path": "country|city|datacenter|queue",
                                        "name": "queue",
                                        "nodes": [
                                            {
                                                "path": "country|city|datacenter|queue|rack",
                                                "name": "rack",
                                            }
                                        ],
                                    }
                                ],
                            }
                        ],
                    }
                ],
            }
        ]
    }

    assert response == expected_tree


def test_get_physical_tree_filled_for_projects(test):
    # these names must appear in the tree
    LocationNamesMap.objects.create(path="country_1|city_1|datacenter_1", name="mockdc")
    LocationNamesMap.objects.create(path="country_1|city_1|datacenter_1|queue_1", name="q-1")

    project1 = test.mock_project({"id": "project-1"})
    project2 = test.mock_project({"id": "project-2"})
    test.mock_host(
        {
            "inv": "1",
            "name": "host-1",
            "project": project1.id,
            "location": {
                "country": "country_1",
                "city": "city_1",
                "datacenter": "datacenter_1",
                "queue": "queue_1",
                "rack": "rack_1",
                "unit": "unit_1",
            },
        }
    )
    test.mock_host(
        {
            "inv": "2",
            "name": "host-2",
            "project": project2.id,
            "location": {
                "country": "country_1",
                "city": "city_1",
                "datacenter": "datacenter_1",
                "queue": "queue_2",
                "rack": "rack_1",
                "unit": "unit_1",
            },
        }
    )
    test.mock_host(
        {
            "inv": "3",
            "name": "host-3",
            "project": "project-2",
            "location": {
                "country": "country_2",
                "city": "city_1",
                "datacenter": "datacenter_1",
                "queue": "queue_1",
                "rack": "rack_1",
                "unit": "unit_1",
            },
        }
    )

    result = test.api_client.get("/v1/physical-location-tree?project=project-1,project-2")
    assert result.status_code == http.client.OK

    response = result.json

    expected_tree = {
        "result": [
            {
                "path": "country_1",
                "name": "country_1",
                "nodes": [
                    {
                        "path": "country_1|city_1",
                        "name": "city_1",
                        "nodes": [
                            {
                                "path": "country_1|city_1|datacenter_1",
                                "name": "datacenter_1",
                                "short_name": "mockdc",
                                "nodes": [
                                    {
                                        "path": "country_1|city_1|datacenter_1|queue_1",
                                        "name": "queue_1",
                                        "short_name": "q-1",
                                        "nodes": [
                                            {
                                                "path": "country_1|city_1|datacenter_1|queue_1|rack_1",
                                                "name": "rack_1",
                                            }
                                        ],
                                    },
                                    {
                                        "path": "country_1|city_1|datacenter_1|queue_2",
                                        "name": "queue_2",
                                        "nodes": [
                                            {
                                                "path": "country_1|city_1|datacenter_1|queue_2|rack_1",
                                                "name": "rack_1",
                                            }
                                        ],
                                    },
                                ],
                            }
                        ],
                    }
                ],
            },
            {
                "path": "country_2",
                "name": "country_2",
                "nodes": [
                    {
                        "path": "country_2|city_1",
                        "name": "city_1",
                        "nodes": [
                            {
                                "path": "country_2|city_1|datacenter_1",
                                "name": "datacenter_1",
                                "nodes": [
                                    {
                                        "path": "country_2|city_1|datacenter_1|queue_1",
                                        "name": "queue_1",
                                        "nodes": [
                                            {
                                                "path": "country_2|city_1|datacenter_1|queue_1|rack_1",
                                                "name": "rack_1",
                                            }
                                        ],
                                    }
                                ],
                            }
                        ],
                    }
                ],
            },
        ]
    }

    assert response["result"] == expected_tree["result"]


def test_get_physical_tree__no_location(test):
    test.mock_project({"id": "project"})
    for i in range(3):
        test.mock_host({"inv": i, "name": "host-{}".format(i), "project": "project", "location": HostLocation()})

    result = test.api_client.get("/v1/physical-location-tree?project=project")
    assert result.status_code == http.client.OK

    response = result.json

    expected_tree = {
        "result": [
            {
                "path": "Unknown",
                "name": "Unknown",
                "nodes": [
                    {
                        "path": "Unknown|Unknown",
                        "name": "Unknown",
                        "nodes": [
                            {
                                "path": "Unknown|Unknown|Unknown",
                                "name": "Unknown",
                                "nodes": [
                                    {
                                        "path": "Unknown|Unknown|Unknown|Unknown",
                                        "name": "Unknown",
                                        "nodes": [
                                            {
                                                "path": "Unknown|Unknown|Unknown|Unknown|Unknown",
                                                "name": "Unknown",
                                            }
                                        ],
                                    },
                                ],
                            }
                        ],
                    }
                ],
            },
        ]
    }

    assert response == expected_tree
