from maps.b2bgeo.ya_courier.backend.test_lib import util
from dateutil.parser import parse


def parse_daytime(str_time):
    if "." in str_time:
        day, time = str_time.split(".")
    else:
        day = 0
        time = str_time
    day = int(day)
    time = (parse(time) - parse("00:00:00")).total_seconds()
    return day * 24 * 3600 + time


def interval_to_sec(inverval_str):
    begin_str = inverval_str.split(" - ")[0]
    end_str = inverval_str.split(" - ")[1]
    return [parse_daytime(begin_str), parse_daytime(end_str)]


class TestTimeZoneConsistency(object):
    def test(self, system_env_with_db):
        courier_num, depot_num, route_num, order_num = \
            util.generate_numbers("timezone")
        util.cleanup_state(system_env_with_db, courier_num, route_num, depot_num)
        try:
            courier = util.create_courier(system_env_with_db, courier_num)
            depot = util.create_depot(system_env_with_db, depot_num)
            route = util.create_route(
                system_env_with_db, route_num,
                courier_id=courier['id'],
                depot_id=depot['id'])
            util.create_orders(
                system_env_with_db,
                order_num, route_id=route['id']
            )
            routed_orders = util.query_routed_orders(
                system_env_with_db, courier["id"], route["id"])

            assert len(routed_orders["route"]) > 0
            for order in routed_orders["route"]:
                assert order["time_interval_secs"] == interval_to_sec(order["time_interval"])
        finally:
            util.cleanup_state(system_env_with_db, courier_num, route_num, depot_num)
