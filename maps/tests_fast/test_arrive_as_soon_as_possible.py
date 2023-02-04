import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def getIds(route):
    ids = []
    for loc in route:
        ids.append(loc["node"]["value"]["id"])
    return ids


def test_arrive_as_soon_as_possible():
    # в этом тесте 3 машины, у каждой по 4 примерно одинаковые локации
    # Для машины без штрафов ожидается маршрут с прибытием в конец окна первого заказа, для минимизации дальнейших ожиданий
    # Для машины только с штрафом average_h ожидается перестановка 2 и третьего заказа, чтобы приехать к началу третьего
    # Для машины с as_soon_as_possible = true ожидается маршрут 2 машины, но прибытие к первому заказу к началу его окна
    data = tools.get_test_json("arrival_after_start.json")

    result = mvrp_checker.solve_and_check(json.dumps(data), solver_arguments={'sa_iterations': 10000})

    vehicleToRoute = {}

    for route in result["routes"]:
        vehicleToRoute[route["vehicle_id"]] = route["route"]

    assert vehicleToRoute["normal"][1]["arrival_time_s"] == 21600
    assert getIds(vehicleToRoute["normal"]) == ["depot", "normal_1", "normal_2", "normal_3", "normal_4"]

    assert vehicleToRoute["average"][1]["arrival_time_s"] == 21600
    assert getIds(vehicleToRoute["average"]) == ["depot", "average_1", "average_3", "average_2", "average_4"]

    assert vehicleToRoute["as_soon_as_possible"][1]["arrival_time_s"] == 18000
    assert getIds(vehicleToRoute["as_soon_as_possible"]) == [
        "depot",
        "as_soon_as_possible_1",
        "as_soon_as_possible_3",
        "as_soon_as_possible_2",
        "as_soon_as_possible_4"
    ]


def test_shift_duration():
    # Тест проверяет, что мы не нарушаем длину смены, при включенной опции as_soon_as_possible

    data = tools.get_test_json("arrival_after_start.json")

    result = mvrp_checker.solve_and_check(json.dumps(data), solver_arguments={'sa_iterations': 10000})

    assert result["metrics"]["overtime_penalty"] == 0
