from itertools import product

import math


def generate_equilateral_triangle(length):
    return [
        {'lon': 0, 'lat': length * math.sqrt(3) / 4, 'id': 0},
        {'lon': length / 2, 'lat': -length * math.sqrt(3) / 4, 'id': 1},
        {'lon': -length / 2, 'lat': -length * math.sqrt(3) / 4, 'id': 2}
    ]


def generate_square(length):
    return [
        {'lon': lon, 'lat': lat, 'id': i}
        for i, (lon, lat) in enumerate(product([-length / 2, length / 2], repeat=2))
    ]


SOUTH_POLE_STATIONS_QUERY_DISTANCE = 1_360_000  # meters
SOUTH_POLE_STATIONS = [
    {'lon': 0.0, 'lat': -90.0, 'id': 0},  # amundsen_scott
    {'lon': 77.117, 'lat': -80.4174, 'id': 1},  # kunlun
    {'lon': 106.8137, 'lat': -78.4675, 'id': 2},  # vostok
    {'lon': -34.6193, 'lat': -77.8676, 'id': 3},  # belgrano_2
    {'lon': 166.7821, 'lat': -77.8497, 'id': 4}  # scott_base
]


GIANT_CIRCLE_QUERY_DISTANCE = 8_010_000  # meters
GIANT_CIRCLE_QUERY_POINT = (0.0, 0.0)  # lon, lat
GIANT_CIRCLE_POINTS = [
    {'lon': 0.0, 'lat': 71.94572847349845, 'id': 0},
    {'lon': 61.55824035269117, 'lat': 49.40370877166775, 'id': 1},
    {'lon': 71.26802068689926, 'lat': 15.192682852767488, 'id': 2},
    {'lon': 70.7529350411336, 'lat': -19.920936833758258, 'id': 3},
    {'lon': 58.40324017971389, 'lat': -53.735314180651926, 'id': 4},
    {'lon': -14.969353373363132, 'lat': -71.28842060124974, 'id': 5},
    {'lon': -64.0269539653843, 'lat': -44.95529725976265, 'id': 6},
    {'lon': -71.63031140285582, 'lat': -10.452135960189645, 'id': 7},
    {'lon': -70.06572209787413, 'lat': 24.63190905952428, 'id': 8},
    {'lon': -54.32148279941313, 'lat': 57.901559255651556, 'id': 9}
]
BOUNDARY_POINTS = [
    {'lon': -82.0, 'lat': 0.0, 'id': 10},
    {'lon': 82.0, 'lat': 0.0, 'id': 11},
    {'lon': -82.0, 'lat': 33.0, 'id': 12},
    {'lon': 82.0, 'lat': 33.0, 'id': 13},
    {'lon': -82.0, 'lat': 66.0, 'id': 14},
    {'lon': 82.0, 'lat': 66.0, 'id': 15}
]

INNER_CIRCLE_QUERY_DISTANCE = 210  # meters
INNER_CIRCLE_QUERY_POINT = (0.0, 85.0)
INNER_CIRCLE_POINTS = [
    {'lon': 0.0, 'lat': 85.00181322846186, 'id': 0},
    {'lon': 0.016485201536182974, 'lat': 85.00109682734423, 'id': 1},
    {'lon': 0.019835836144138603, 'lat': 84.99951850555298, 'id': 2},
    {'lon': 0.007393252046384385, 'lat': 84.99833535310698, 'id': 3},
    {'lon': -0.010932750011440999, 'lat': 84.9984891467138, 'id': 4},
    {'lon': -0.02055807669598161, 'lat': 84.99985749592132, 'id': 5},
    {'lon': -0.013812698331825185, 'lat': 85.00135109153703, 'id': 6}
]
OUTER_CIRCLE_POINTS = [
    {'lon': 0.0, 'lat': 85.00271255331856, 'id': 7},
    {'lon': 0.024730472207833247, 'lat': 85.00163779719374, 'id': 8},
    {'lon': 0.029752281586357527, 'lat': 84.99927024537845, 'id': 9},
    {'lon': 0.011088021459391889, 'lat': 84.99749570923031, 'id': 10},
    {'lon': -0.016396630832835584, 'lat': 84.99772636278558, 'id': 11},
    {'lon': -0.03083663124089533, 'lat': 84.999778714345, 'id': 12},
    {'lon': -0.020721810510567755, 'lat': 85.00201923949781, 'id': 13}
]
