from ya_courier_backend.logic.segment_distance import filter_positions


def test_filtering():
    """
        Fourth point deviates from the track
        so it's noise.
            * check: fourth point doesn't contain in filtered positions
    """
    positions = [
        {
            "lat": 55.728033,
            "lon": 37.583119,
            "time": 1533644320
        },
        {
            "lat": 55.729074,
            "lon": 37.584965,
            "time": 1533644330
        },
        {
            "lat": 55.729764,
            "lon": 37.586831,
            "time": 1533644350
        },
        {
            "lat": 55.735390,
            "lon": 37.581242,
            "time": 1533644360
        },
        {
            "lat": 55.730793,
            "lon": 37.589600,
            "time": 1533644370
        },
        {
            "lat": 55.731629,
            "lon": 37.591638,
            "time": 1533644380
        }
    ]
    filtered_positions = filter_positions(positions)
    for pos in filtered_positions:
        print(pos["time"])
    assert filtered_positions == positions[:3] + positions[4:]


def test_inv_direction_on_the_line():
    """
        Points are located on the line.
        Third point is located between first and second, fourth is set after second so third is noise.
            * check: third point doesn't contain in filtered positions
    """
    positions = [
        {
            "lat": 55.728809,
            "lon": 37.584703,
            "time": 1533644320
        },
        {
            "lat": 55.729366,
            "lon": 37.586248,
            "time": 1533644330
        },
        {
            "lat": 55.729220,
            "lon": 37.585841,
            "time": 1533644340
        },
        {
            "lat": 55.729886,
            "lon": 37.587600,
            "time": 1533644350
        }
    ]
    filtered_positions = filter_positions(positions)
    assert filtered_positions == positions[:2] + positions[3:]


def test_max_speed_contraint():
    """
    Points are located on the line.
    Third point is noise because it's jump forward with high speed.
        * check: third point doesn't contain in filtered positions
    """
    positions = [
        {
            "lat": 55.728809,
            "lon": 37.584703,
            "time": 1533644365
        },
        {
            "lat": 55.729366,
            "lon": 37.586248,
            "time": 1533644370
        },
        {
            "lat": 55.731037,
            "lon": 37.590709,
            "time": 1533644375
        },
        {
            "lat": 55.729886,
            "lon": 37.587600,
            "time": 1533644380
        },
        {
            "lat": 55.730553,
            "lon": 37.589293,
            "time": 1533644385
        }
    ]
    filtered_positions = filter_positions(positions)
    assert filtered_positions == positions[:2] + positions[3:]


def test_two_noises_in_same_location():
    """
        Two points of a track are deviated and located close to each other.
        But they are far from real track.
            * check: these two points don't contain in filtered positions
    """
    positions = [
        {
            "lat": 55.728033,
            "lon": 37.583119,
            "time": 1533644320
        },
        {
            "lat": 55.729074,
            "lon": 37.584965,
            "time": 1533644330
        },
        {
            "lat": 55.729764,
            "lon": 37.586831,
            "time": 1533644350
        },
        {
            "lat": 55.735390,
            "lon": 37.581242,
            "time": 1533644360
        },
        {
            "lat": 55.735506,
            "lon": 37.581487,
            "time": 1533644365,
        },
        {
            "lat": 55.730793,
            "lon": 37.589600,
            "time": 1533644370
        },
        {
            "lat": 55.731629,
            "lon": 37.591638,
            "time": 1533644380
        }
    ]
    filtered_positions = filter_positions(positions)
    assert filtered_positions == positions[:3] + positions[5:]


def test_first_positions_noise():
    """
        First two points of track are noise. Speed on between track point is more than 100 km/h and less than 144 km/h
            * check: this point don't contain in filtered positions
    """
    positions = [
        {
            "time": 1551271235.688,
            "lat": 56.0016317,
            "lon": 46.05162
        },
        {
            "time": 1551271334,
            "lat": 55.764169,
            "lon": 46.231877
        },
        {
            "time": 1551272414,
            "lat": 56.096095,
            "lon": 47.010945
        },
        {
            "time": 1551272426,
            "lat": 56.0934483,
            "lon": 47.0048917
        },
        {
            "time": 1551272443,
            "lat": 56.0897533,
            "lon": 47.000385
        }
    ]
    filtered_positions = filter_positions(positions)
    assert filtered_positions == positions[2:]
