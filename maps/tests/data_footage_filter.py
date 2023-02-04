STREETVIEW = [
    {'lon': 20.0001, 'lat': 30.0001},
    {'lon': 37.15, 'lat': 48.15},
]

# Wkb mercator point in hex
# e.g. wkb.dumps(Point(2782987.2698318390175700, 4139372.7622473053634167), hex=True)
MIRROR = [
    {'pos': b'010100000086D989A2853B4541D8519161B6944F41'},
]

DATA = [
    {'type': 'test1', 'lon': 20.000101, 'lat': 30.000101, 'text': 'Near streetview'},
    {'type': 'test2', 'lon': 24.9999999, 'lat': 34.9999999, 'text': 'Near mirror'},
    {'type': 'test3', 'lon': 31.0, 'lat': 41.0, 'text': 'Not near anything'},
    {'type': 'test4', 'lon': 37.15, 'lat': 48.15, 'text': 'Double'},
    {'type': 'test4', 'lon': 37.15, 'lat': 48.15, 'text': 'Double'},
]

RESULT = [
    {'type': 'test1', 'lon': 20.000101, 'lat': 30.000101, 'text': 'Near streetview'},
    {'type': 'test2', 'lon': 24.9999999, 'lat': 34.9999999, 'text': 'Near mirror'},
    {'type': 'test4', 'lon': 37.15, 'lat': 48.15, 'text': 'Double'},
]
