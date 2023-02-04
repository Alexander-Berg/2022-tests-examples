import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools

import json


depot = {
    "id": 10000,
    "point": {"lon": 37.729377, "lat": 55.799087},
    "time_window": "07:00-23:59"
}

Data = {
    "depot": depot,
    "options": {
        "time_zone": 3
    },
    "vehicles": [
        {
            "id": 0,
            "capacity": {
                "weight_kg": 49,
                "volume": {"width_m": 3, "height_m": 2, "depth_m": 1}
            }
        },
        {
            "id": 1,
            "capacity": {
                "weight_kg": 51,
                "volume": {"width_m": 3, "height_m": 2, "depth_m": 0.99}
            }
        },
        {
            "id": 2,
            "capacity": {
                "weight_kg": 50,
                "volume": {"width_m": 1, "height_m": 2, "depth_m": 3}
            }
        },
        {
            "id": 3,
            "capacity": {
                "weight_kg": 150,
                "volume": {"width_m": 9, "height_m": 9, "depth_m": 9}
            }
        }
    ],
    "locations": [
        {
            "id": 2,
            "point": {"lon": 37.722388, "lat": 55.780846},
            "time_window": "10:00-15:00",
            "shipment_size": {
                "weight_kg": 50,
                "volume": {"width_m": 3, "height_m": 2, "depth_m": 1}
            }
        },
        {
            "id": 1,
            "point": {"lon": 37.722388, "lat": 55.780846},
            "time_window": "16:00-18:00",
            "shipment_size": {
                "weight_kg": 51,
                "volume": {"width_m": 3, "height_m": 2, "depth_m": 0.5}
            }
        },
        {
            "id": 0,
            "point": {"lon": 37.722390, "lat": 55.780875},
            "time_window": "09:00-10:00",
            "shipment_size": {
                "weight_kg": 48.5,
                "volume": {"width_m": 2.9, "height_m": 0.99, "depth_m": 1.99}
            }
        }
    ]
}

DataVolume = {
    "depot": depot,
    "options": {
        "time_zone": 3
    },
    "vehicles": [
        {
            "id": 0,
            "capacity": {
                "volume": {"width_m": 5, "height_m": 5, "depth_m": 5}
            }
        }
    ],
    "locations": [
        {
            "id": 2,
            "point": {"lon": 37.722388, "lat": 55.780846},
            "time_window": "10:00-15:00",
            "shipment_size": {
                "volume": {"width_m": 1, "height_m": 2, "depth_m": 3}
            }
        },
        {
            "id": 1,
            "point": {"lon": 37.722388, "lat": 55.780846},
            "time_window": "16:00-18:00",
            "shipment_size": {
                "volume": {"width_m": 3, "height_m": 2, "depth_m": 1}
            }
        },
        {
            "id": 0,
            "point": {"lon": 37.722390, "lat": 55.780875},
            "time_window": "09:00-10:00",
            "shipment_size": {
                "volume": {"width_m": 1, "height_m": 3, "depth_m": 3}
            }
        }
    ]
}

DataBigVolumeCbm = {
    "depot": depot,
    "options": {
        "time_zone": 3
    },
    "vehicles": [
        {
            "id": 0,
            "capacity": {
                "volume": {"width_m": 5, "height_m": 5, "depth_m": 5}
            }
        }
    ],
    "locations": [
        {
            "id": 2,
            "point": {"lon": 37.722388, "lat": 55.780846},
            "time_window": "10:00-15:00",
            "shipment_size": {
                "volume_cbm": 600
            }
        },
        {
            "id": 1,
            "point": {"lon": 37.722388, "lat": 55.780846},
            "time_window": "16:00-18:00",
            "shipment_size": {
                "volume_cbm": 600
            }
        },
        {
            "id": 0,
            "point": {"lon": 37.722390, "lat": 55.780875},
            "time_window": "09:00-10:00",
            "shipment_size": {
                "volume_cbm": 900
            }
        }
    ]
}

DataVolumeCbm = {
    "depot": depot,
    "options": {
        "time_zone": 3
    },
    "vehicles": [
        {
            "id": 0,
            "capacity": {
                "volume": {"width_m": 5, "height_m": 5, "depth_m": 5}
            }
        }
    ],
    "locations": [
        {
            "id": 2,
            "point": {"lon": 37.722388, "lat": 55.780846},
            "time_window": "10:00-15:00",
            "shipment_size": {
                "volume": {"width_m": 10, "height_m": 20, "depth_m": 3},
                "volume_cbm": 6
            }
        },
        {
            "id": 1,
            "point": {"lon": 37.722388, "lat": 55.780846},
            "time_window": "16:00-18:00",
            "shipment_size": {
                "volume": {"width_m": 30, "height_m": 20, "depth_m": 1},
                "volume_cbm": 6
            }
        },
        {
            "id": 0,
            "point": {"lon": 37.722390, "lat": 55.780875},
            "time_window": "09:00-10:00",
            "shipment_size": {
                "volume": {"width_m": 10, "height_m": 30, "depth_m": 3},
                "volume_cbm": 9
            }
        }
    ]
}


def test_volume_and_weight():
    """
    volume and weight restrictions are checked in mvrp_checker
    """
    mvrp_checker.solve_and_check(
        json.dumps(Data), solver_arguments={'sa_iterations': 1000})


def test_volume():
    """
    volume and weight restrictions are checked in mvrp_checker
    """
    mvrp_checker.solve_and_check(
        json.dumps(DataVolume), solver_arguments={'sa_iterations': 1000})


def test_big_volume_cbm():
    """
    this test verifies that a large "volume_cbm" value overrides a small "volume" value
    """
    mvrp_checker.solve_and_check(
        json.dumps(DataBigVolumeCbm), solver_arguments={'sa_iterations': 1000}, expected_status="PARTIAL_SOLVED")


def test_volume_cbm():
    """
    this test verifies that a small "volume_cbm" value overrides a large "volume" value
    """
    mvrp_checker.solve_and_check(
        json.dumps(DataVolumeCbm), solver_arguments={'sa_iterations': 1000}, expected_status="SOLVED")


def test_custom_units():
    """
    units restrictions are checked in mvrp_checker
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data("custom_units.json"),
        solver_arguments={'sa_iterations': 10000})
