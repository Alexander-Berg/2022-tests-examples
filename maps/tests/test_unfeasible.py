import json
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import _solver as solver


def check(request_file, unfeasible_types, unfeasible_count):
    request = open(tools.arcadia_path(request_file)).read()
    task = solver.SolverTask(
        vrp_json=request,
        sa_iterations=100000)
    task.distanceMatrixFromJson("")
    result_python = task.solve()
    assert isinstance(result_python.solution, str)

    result = mvrp_checker.remove_internal_params(json.loads(result_python.solution), ["iterations"])
    assert result["solver_status"] == "UNFEASIBLE"
    assert len(result["routes"]) == 1
    assert len(result["routes"][0]["route"]) == 12

    tools.validate_format(json.loads(request), result)

    count = 0
    for node in result["routes"][0]["route"]:
        if "unfeasible_reasons" not in node["node"]["value"]:
            continue
        count += 1
        assert len(node["node"]["value"]["unfeasible_reasons"]) == len(unfeasible_types)
        for unfeasible in node["node"]["value"]["unfeasible_reasons"]:
            assert unfeasible["type"] in unfeasible_types
            if unfeasible["type"] in ["OVERLOAD_WEIGHT", "OVERLOAD_SIZE", "OVERLOAD_VOLUME"]:
                assert len(unfeasible) == 3
                assert unfeasible["overload"] > 0
                assert len(unfeasible["text"]) > 0
            elif unfeasible["type"] == "FAILED_HARD_TIME_WINDOW":
                assert len(unfeasible) == 3
                assert len(unfeasible["text"]) > 0
                assert unfeasible["late_duration_s"] > 0
    assert count == unfeasible_count


def test_overload_weight():
    check("tests_data/unfeasible_overload_weight.json", ["OVERLOAD_WEIGHT"], 6)


def test_overload_size():
    check("tests_data/unfeasible_overload_size.json", ["OVERLOAD_SIZE"], 6)


def test_overload_volume():
    check("tests_data/unfeasible_overload_volume.json", ["OVERLOAD_VOLUME"], 6)


def test_hard_time_window():
    check("tests_data/unfeasible_hard_time_window.json", ["FAILED_HARD_TIME_WINDOW"], 3)


def test_tags():
    request = tools.get_test_data("unfeasible_tags.json")
    task = solver.SolverTask(
        vrp_json=request,
        sa_iterations=100000)
    task.distanceMatrixFromJson("")
    result_python = task.solve()
    assert isinstance(result_python.solution, str)

    result = mvrp_checker.remove_internal_params(json.loads(result_python.solution), ["iterations"])

    assert result["solver_status"] == "UNFEASIBLE"
    assert len(result["routes"]) == 1
    assert len(result["routes"][0]["route"]) == 4

    tools.validate_format(json.loads(request), result)

    unfeasible_reasons = [node["node"]["value"].get("unfeasible_reasons", None) for node in result["routes"][0]["route"]]

    expected_unfeasible_reasons = [
        None,
        [
            {
                "tags": [
                    "tag3"
                ],
                "text": "Order is not compatible with the vehicle because it does not have the following required tags: \"tag3\"",
                "type": "REQUIRED_TAGS_VIOLATION"
            },
            {
                "tags": [
                    "tag3"
                ],
                "text": "Order is not compatible with the vehicle because it has the following incompatible tags: \"tag3\"",
                "type": "EXCLUDED_TAGS_VIOLATION"
            }
        ],
        [
            {
                "tags": [
                    "tag1"
                ],
                "text": "Order is not compatible with the vehicle because it does not have the following required tags: \"tag1\"",
                "type": "REQUIRED_TAGS_VIOLATION"
            }
        ],
        None
    ]

    assert unfeasible_reasons == expected_unfeasible_reasons


def test_tags_regexp():
    request = tools.get_test_data("unfeasible_tags_regexp.json")
    task = solver.SolverTask(
        vrp_json=request,
        sa_iterations=100000)
    task.distanceMatrixFromJson("")
    result_python = task.solve()
    assert isinstance(result_python.solution, str)

    result = mvrp_checker.remove_internal_params(json.loads(result_python.solution), ["iterations"])

    assert result["solver_status"] == "UNFEASIBLE"
    assert len(result["routes"]) == 1
    assert len(result["routes"][0]["route"]) == 4

    tools.validate_format(json.loads(request), result)

    unfeasible_reasons = [node["node"]["value"].get("unfeasible_reasons", None) for node in result["routes"][0]["route"]]

    expected_unfeasible_reasons = [
        None,
        [
            {
                "tags": [
                    ".*3.*",
                    ".*g3.*"
                ],
                "text": "Order is not compatible with the vehicle because it has the following incompatible tags: \".*g3.*\", \".*3.*\"",
                "type": "EXCLUDED_TAGS_VIOLATION"
            }
        ],
        [
            {
                "tags": [
                    "wrong_tag"
                ],
                "text": "Order is not compatible with the vehicle because it does not have the following required tags: \"wrong_tag\"",
                "type": "REQUIRED_TAGS_VIOLATION"
            }
        ],
        None
    ]

    assert unfeasible_reasons == expected_unfeasible_reasons


def test_load_types():
    request = tools.get_test_data("unfeasible_load_types.json")
    task = solver.SolverTask(
        vrp_json=request,
        sa_iterations=1)
    task.distanceMatrixFromJson("")
    result_python = task.solve()
    assert isinstance(result_python.solution, str)

    result = mvrp_checker.remove_internal_params(json.loads(result_python.solution), ["iterations"])

    assert result["solver_status"] == "UNFEASIBLE"
    assert len(result["routes"]) == 1
    assert len(result["routes"][0]["route"]) == 6

    tools.validate_format(json.loads(request), result)

    unfeasible_reasons = [node["node"]["value"].get("unfeasible_reasons", None) for node in result["routes"][0]["route"]]

    expected_unfeasible_reasons = [
        None,
        [
            {
                "incompatibilities": [
                    {
                        "incompatible_order_id": "loc4",
                        "types": [
                            {
                                "other_type": "load_type4",
                                "self_type": "load_type3"
                            },
                            {
                                "other_type": "load_type2",
                                "self_type": "load_type1"
                            }
                        ]
                    },
                    {
                        "incompatible_order_id": "loc2",
                        "types": [
                            {
                                "other_type": "load_type2",
                                "self_type": "load_type1"
                            }
                        ]
                    }
                ],
                "text": "Orders are not compatible by load types",
                "type": "INCOMPATIBLE_LOAD_TYPES_VIOLATION"
            }
        ],
        [
            {
                "incompatibilities": [
                    {
                        "incompatible_order_id": "loc1",
                        "types": [
                            {
                                "other_type": "load_type1",
                                "self_type": "load_type2"
                            }
                        ]
                    }
                ],
                "text": "Orders are not compatible by load types",
                "type": "INCOMPATIBLE_LOAD_TYPES_VIOLATION"
            }
        ],
        [
            {
                "incompatibilities": [
                    {
                        "incompatible_order_id": "loc4",
                        "types": [
                            {
                                "other_type": "load_type4",
                                "self_type": "load_type3"
                            }
                        ]
                    }
                ],
                "text": "Orders are not compatible by load types",
                "type": "INCOMPATIBLE_LOAD_TYPES_VIOLATION"
            }
        ],
        [
            {
                "incompatibilities": [
                    {
                        "incompatible_order_id": "loc1",
                        "types": [
                            {
                                "other_type": "load_type3",
                                "self_type": "load_type4"
                            },
                            {
                                "other_type": "load_type1",
                                "self_type": "load_type2"
                            }
                        ]
                    },
                    {
                        "incompatible_order_id": "loc3",
                        "types": [
                            {
                                "other_type": "load_type3",
                                "self_type": "load_type4"
                            }
                        ]
                    }
                ],
                "text": "Orders are not compatible by load types",
                "type": "INCOMPATIBLE_LOAD_TYPES_VIOLATION"
            }
        ],
        None
    ]

    assert unfeasible_reasons == expected_unfeasible_reasons
