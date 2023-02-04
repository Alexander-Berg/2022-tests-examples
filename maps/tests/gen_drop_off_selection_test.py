#!/usr/bin/env python3

import json
import argparse
import random


def is_inside_segment(x, a, b):
    return x >= min(a, b) and x <= max(a, b)


class Point:
    def __init__(self, lat, lon):
        self.lat = lat
        self.lon = lon

    def __add__(self, p):
        return Point(self.lat + p.lat, self.lon + p.lon)

    def __sub__(self, p):
        return Point(self.lat - p.lat, self.lon - p.lon)

    def __mul__(self, k):
        return Point(k * self.lat, k * self.lon)

    def as_dict(self):
        return {"lat": self.lat, "lon": self.lon}


class Rectangle:
    def __init__(self, a, b):
        self.a = a
        self.b = b

    def stretch(self, scale):
        k = max(0, (scale - 1.0) / 2)
        vector = (self.b - self.a) * k
        return Rectangle(self.a - vector, self.b + vector)

    def includes(self, p):
        return is_inside_segment(p.lat, self.a.lat, self.b.lat) and \
            is_inside_segment(p.lon, self.a.lon, self.b.lon)

    def random_point(self):
        return Point(random.uniform(self.a.lat, self.b.lat), random.uniform(self.a.lon, self.b.lon))


def parse_args():
    p = argparse.ArgumentParser()
    p.add_argument("-o", "--output", required=True)
    p.add_argument("-d", "--drop-offs")
    p.add_argument("--num-drop-off", "--ndo", type=int, default=50)
    p.add_argument("--num-delivery-to-any", "--ndta", type=int, default=10)
    p.add_argument("--num-pickup", "--np", type=int, default=10)
    p.add_argument("--num-pickup-and-delivery", "--npad", type=int, default=10)
    p.add_argument("--num-delivery", "--nd", type=int, default=10)
    p.add_argument("--num-vehicles", "--nv", type=int, default=1)
    return p.parse_args()


def gen_drop_off_coord(rectangle):
    big_rectangle = rectangle.stretch(10)
    rectangle_x2 = rectangle.stretch(2)
    while True:
        p = big_rectangle.random_point()
        if not rectangle_x2.includes(p):
            return p.as_dict()


def gen_locations(rectangle, delivery_count, pickup_count, pud_count, delivery_to_any_count, drop_off_count):
    locations = []

    def add_location(loc_type, ref):
        loc = {
            "id": str(len(locations)),
            "ref": ref,
            "type": loc_type,
            "point": rectangle.random_point().as_dict(),
            "service_duration_s": 300,
            "shipment_size": { "weight_kg": 1 },
            "time_window": "00:00 - 1.00:00",
        }
        if loc_type == 'drop_off':
            del loc["shipment_size"]
        locations.append(loc)
        return loc

    for i in range(delivery_count):
        add_location("delivery", "delivery_%d"%i)

    for i in range(pickup_count):
        add_location("pickup", "pickup_%d"%i)

    for i in range(pud_count):
        loc = add_location("pickup" if i%2 == 1 else "delivery", "pud_%d"%i)
        if i%2 == 1:
            loc["delivery_to"] = locations[-2]["id"]

    best_drop_off_id = None
    drop_offs = []
    for i in range(drop_off_count):
        loc = add_location("drop_off", "drop_off_%d"%i)
        if i == 0:
            best_drop_off_id = loc["id"]
        else:
            loc["point"] = gen_drop_off_coord(rectangle)
            drop_offs.append(loc["id"])

    for i in range(delivery_to_any_count):
        loc = add_location("pickup", "dta_%d"%i)
        loc["delivery_to_any"] = [best_drop_off_id] + list(random.sample(drop_offs, 5))
        random.shuffle(loc["delivery_to_any"])

    return locations


def gen_vehicle(index):
    return {
        "capacity": {
            "weight_kg": 10
        },
        "id": index,
        "shifts": [
            {
                "id": "morning",
                "time_window": "10:00 - 15:00",
                "max_duration_s": 4 * 3600
            }
        ]
    }


def convert_drop_off(loc):
    loc["type"] = "delivery"
    return loc


def gen_request(
    delivery_count,
    pickup_count,
    pickup_and_delivery_count,
    delivery_to_any_count,
    drop_off_count,
    vehicles_count
):
    random.seed(0)

    rectangle = Rectangle(Point(55.725994, 37.625667), Point(55.744627, 37.665298))

    request = {
        'depot': {
            "id": "aurora",
            "point": Point(55.735739, 37.642518).as_dict(),
            "time_window": "00:00-1.00:00"
        },
        "locations": gen_locations(
            rectangle,
            delivery_count,
            pickup_count,
            pickup_and_delivery_count,
            delivery_to_any_count,
            drop_off_count
        ),
        "vehicles": [gen_vehicle(index) for index in range(vehicles_count)],
        "options": {
            "date": "2019-12-25",
            "time_zone": 3,
            "solver_time_limit_s": 10
        }
    }
    return request


def main():
    args = parse_args()

    request = gen_request(
        args.num_delivery,
        args.num_pickup,
        args.num_pickup_and_delivery,
        args.num_delivery_to_any,
        args.num_drop_off,
        args.num_vehicles
    )

    with open(args.output, 'w') as f_out:
        json.dump(request, f_out, indent=4, sort_keys=True)

    if args.drop_offs:
        with open(args.drop_offs, 'w') as f_out:
            request["locations"] = [convert_drop_off(loc) for loc in request["locations"] if loc["type"] == "drop_off"]
            json.dump(request, f_out, indent=4, sort_keys=True)


if __name__ == "__main__":
    main()
