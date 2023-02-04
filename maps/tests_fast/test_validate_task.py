import _solver as solver
import ujson
import copy
import pytest


TASK = {
    "depot": {
        "id": 10000,
        "point": {"lon": 37.619427, "lat": 55.754466},
        "time_window": "00:00-23:59"
    },
    "vehicles": [
        {'id': 1}
    ],
    "locations": [
        {
            "id": 0,
            "point": {"lon": 37.775692, "lat": 55.620095},
            "time_window": "00:00-23:59"
        }
    ]
}


def test_validate_intervals():
    task = copy.deepcopy(TASK)
    solver.SolverTask(vrp_json=ujson.dumps(task))

    task["locations"][0]["time_window"] = "12:00-12:00"
    solver.SolverTask(vrp_json=ujson.dumps(task))

    task["locations"][0]["time_window"] = "18:00-06:00"
    with pytest.raises(solver.SolverLocalizedException):
        solver.SolverTask(vrp_json=ujson.dumps(task)).solve()


def test_unknown_minimization_target():
    task = copy.deepcopy(TASK)
    task["options"] = {"time_zone": 3, "minimize": "unknown_minimization_target"}
    task = solver.SolverTask(vrp_json=ujson.dumps(task))
    task.distanceMatrixFromJson("")
    with pytest.raises(
            solver.SolverLocalizedException,
            match="Unknown target unknown"):
        task.solve()
