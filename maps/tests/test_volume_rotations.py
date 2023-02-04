#!/usr/bin/python3
# Run this file as a python script to dump the test jsons.
import json
import os.path as op
from math import sqrt
import random
if __name__ != '__main__':
    import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker


DATA_PATH = '../tests_data'
TT_PATH = 'volume_types_and_alignments.json'
TT_SEED = 324965
MERGE_PATH = 'volume_merge.json'
MERGE_SEED = 757839

VEHICLE_CNT = 10
LOCATION_CNT = 240
DEPOT_LAT, DEPOT_LON = 57.7, 37.7
GEO_DEVIATION = 0.0005
GEO_THRESHOLD = 1e-6
VOLUME_THRESHOLD = 1e-6
VOLUME_OFFSET = 1 + VOLUME_THRESHOLD
TIME_WINDOW = "00:00-2.23:59"
DEPOT = {
    "id": -1,
    "point": {
        "lat": DEPOT_LAT,
        "lon": DEPOT_LON
    },
    "time_window": TIME_WINDOW
}
OPTIONS = {
    "date": "2020-03-12",
    "time_zone": 3.0,
    "default_speed_km_h": 120
}
DIMENSIONS = ["width_m", "height_m", "depth_m"]


def random_vehicles(gen, vehicle_cnt, need_tags):
    vehicle_list = []
    for idx in range(vehicle_cnt):
        vehicle = {"id": idx, "max_runs": LOCATION_CNT}
        vehicle["capacity"] = {
            "volume": dict((dim, 1 + 9 * gen.random())
                           for dim in DIMENSIONS),
            "limits": {
                "volume_perc": gen.randint(50, 150)
            }
        }
        if need_tags:
            vehicle["tags"] = ["tag" + str(idx)]
        vehicle_list.append(vehicle)
    return vehicle_list


def random_geo_locations(seed, total, max_repeat):
    used = {(0, 0)}  # (0, 0) is reserved for depot
    yielded = []
    choice_range = int(round(GEO_DEVIATION / GEO_THRESHOLD))
    gen = random.Random(seed)
    while len(yielded) < total:
        choice = (gen.randint(-choice_range, choice_range),
                  gen.randint(-choice_range, choice_range))
        if choice in used:
            continue
        used.add(choice)
        repeat = gen.randint(1, max_repeat)
        yielded += repeat * [choice]
    gen.shuffle(yielded)
    for choice in yielded:
        yield {
            "lat": DEPOT_LAT + choice[0] * GEO_THRESHOLD,
            "lon": DEPOT_LON + choice[1] * GEO_THRESHOLD,
        }


def random_rotated(gen, w, h):
    # w and h are sides of outer box
    if w < h:
        w, h = h, w
    # longer side of rotated inner box
    a = gen.uniform(w, sqrt(w ** 2 + h ** 2))
    # binary search
    s = left = 0.
    r = right = w - sqrt(a ** 2 - h ** 2)
    while right - left > 1e-12:
        # a point on the longer side of outer box where inner box touches
        r = (left + right) / 2
        # a point on the shorter side of outer box where inner box touches
        s = h - sqrt(a ** 2 - (w - r) ** 2)
        # for rectangle inner box r * (w - r) = s * (h - s)
        if r * (w - r) > s * (h - s):
            right = r
        else:
            left = r
    # shorter side of rotated inner box
    b = sqrt(r ** 2 + s ** 2)
    if gen.random() < 0.5:
        return (a, b)
    else:
        return (b, a)


def random_rigid_unfitting(gen, max_allowed_dims):
    dims = len(max_allowed_dims)
    dim_vals = sorted([dim * VOLUME_OFFSET for dim in max_allowed_dims])
    save_dim = gen.randrange(dims)
    for dim in range(dims):
        # randomly decrease dimensions except save_dim
        if dim != save_dim:
            dim_vals[dim] *= gen.random()
    # restore sorting if needed
    for dim in range(1, dims):
        dim_vals[dim] = max(dim_vals[dim - 1], dim_vals[dim])
    gen.shuffle(dim_vals)
    return dim_vals


def random_rigid_fitting(gen, max_allowed_dims):
    dim_vals = []
    for dim in max_allowed_dims:
        dice = gen.random()
        dim_vals.append(dim / VOLUME_OFFSET * (1 if dice < 0.5 else dice))
    gen.shuffle(dim_vals)
    return dim_vals


def random_rigid_template(fitting, *args):
    return (random_rigid_fitting if fitting else random_rigid_unfitting)(*args)


def random_rotated_fitting(gen, max_allowed_dims, may_shuffle):
    a, b = random_rotated(gen, max_allowed_dims[0], max_allowed_dims[2])
    dim_vals = [a / VOLUME_OFFSET, max_allowed_dims[1] / VOLUME_OFFSET, b / VOLUME_OFFSET]
    if may_shuffle:
        gen.shuffle(dim_vals)
    return dim_vals


def random_rotated_unfitting(gen, width, height, depth, may_shuffle):
    a, b = random_rotated(gen, width, depth)
    dim_vals = [a * VOLUME_OFFSET, height / VOLUME_OFFSET, b * VOLUME_OFFSET]
    if may_shuffle:
        gen.shuffle(dim_vals)
    return dim_vals


def generate_bulk_volume(volume, gen, fitting, vehicle_cap):
    loc_cbm = 1.
    for dim in DIMENSIONS:
        loc_cbm *= vehicle_cap["volume"][dim]
    loc_cbm *= vehicle_cap["limits"]["volume_perc"] * 0.01
    if fitting:
        loc_cbm /= VOLUME_OFFSET
    else:
        loc_cbm *= VOLUME_OFFSET
    rand_vol = 1.
    for dim in DIMENSIONS:
        volume[dim] = gen.random() + VOLUME_THRESHOLD
        rand_vol *= volume[dim]
    k = (loc_cbm / rand_vol) ** (1./3)
    for dim in DIMENSIONS:
        volume[dim] *= k


def generate_rigid_volume(gen, fitting, allowed_dims, align):
    if align == 'all_axes' or (fitting and gen.random() < 0.1):
        return random_rigid_template(fitting, gen, allowed_dims)
    elif align == 'height':
        if fitting:
            return random_rotated_fitting(gen, allowed_dims, may_shuffle=True)
        else:
            # A rigid box with height-only alignment give much freedom,
            # making it hard to generate arbitrary unfitting.
            # However, suppose the following conditions are satisfied:
            # 1) The longest side of inner box (C) is longer than the longest side of outer box;
            # 2) One side of inner box (A) equals to the shortest side of outer box, while two other
            # inner sides (B and C) cannot be fit into two other outer sides even with rotation.
            # Then even with rigid & height-alignment freedom fitting is impossible.
            # Indeed, condition 1) basically says that we need rotation involving the longest
            # inner side. Rotation involving B & C is impossible because of condition 2).
            # Therefore, we have rotation involving A & C, and thus A < B (otherwise why A & C is
            # better than B & C?), and also we cannot use outer shortest side in rotation, as A
            # equals to it and C is greater than any other. It follows that we must fit the remaining
            # B side into the shortest, which is impossible due to A < B and condition 2).
            ad = sorted(allowed_dims)
            return random_rotated_unfitting(gen, ad[1], ad[0], ad[2], may_shuffle=True)
            # Here A = ad[0], (B, C) are slightly greater than random_rotated(gen, ad[1], ad[2]),
            # so the conditions 1) and 2) are satisfied.
    else:
        raise RuntimeError('Invalid volume alignment')


def generate_fixed_bottom_volume(gen, dice_0_1_2, fitting, allowed_dims, align):
    height_fitting = bottom_fitting = True
    if not fitting:
        if dice_0_1_2 <= 1:
            height_fitting = False
        if dice_0_1_2 >= 1:
            bottom_fitting = False
    if height_fitting:
        height = allowed_dims[1] / VOLUME_OFFSET * gen.random()
    else:
        height = allowed_dims[1] * VOLUME_OFFSET
    if align == 'all_axes' or (fitting and gen.random() < 0.1):
        dim_vals = random_rigid_template(bottom_fitting, gen, list(allowed_dims[0::2]))
        dim_vals.insert(1, height)
    elif align == 'height':
        if bottom_fitting:
            dim_vals = random_rotated_fitting(gen, allowed_dims, may_shuffle=False)
        else:
            dim_vals = random_rotated_unfitting(gen, *allowed_dims, may_shuffle=False)
        dim_vals[1] = height
    else:
        raise RuntimeError('Invalid volume alignment')
    return dim_vals


def generate_json(seed, merge):
    gen = random.Random(seed)
    retval = {
        "depot": DEPOT,
        "options": OPTIONS,
    }
    locations = retval["locations"] = []
    # tags will prevent merging locations, we do not want that when checking merging logic
    vehicles = retval["vehicles"] = random_vehicles(gen, VEHICLE_CNT, need_tags=(not merge))
    for loc_id, point in zip(range(LOCATION_CNT),
                             random_geo_locations(gen.random(), LOCATION_CNT,
                                                  max_repeat=(5 if merge else 1))):
        balanced_gen = loc_id
        fitting = bool(balanced_gen % 2)
        balanced_gen //= 2

        loc_volume = {}
        volume_align = 'all_axes' if balanced_gen % 2 else 'height'
        balanced_gen //= 2
        if volume_align != 'all_axes' or gen.random() < 0.5:
            loc_volume["align"] = volume_align
        dice = balanced_gen % 10
        balanced_gen //= 10
        if dice == 0:
            volume_type = 'bulk'
        elif dice < 5:
            volume_type = 'rigid'
        else:
            volume_type = 'fixed_bottom'
        if volume_type != 'bulk' or gen.random() < 0.5:
            loc_volume["type"] = volume_type

        vehicle_id = gen.randrange(VEHICLE_CNT)
        vehicle_cap = vehicles[vehicle_id]["capacity"]
        if volume_type == 'bulk':
            generate_bulk_volume(loc_volume, gen, fitting, vehicle_cap)
        else:
            allowed_dims = [(vehicle_cap["limits"]["volume_perc"] * 0.01) ** (1./3) *
                            vehicle_cap["volume"][dim] for dim in DIMENSIONS]
            if volume_type == 'rigid':
                dim_vals = generate_rigid_volume(gen, fitting, allowed_dims, volume_align)
            elif volume_type == 'fixed_bottom':
                dim_vals = generate_fixed_bottom_volume(gen, balanced_gen % 3, fitting, allowed_dims, volume_align)
                balanced_gen //= 3
            else:
                raise RuntimeError('Invalid volume type')
            for i, dim in enumerate(DIMENSIONS):
                loc_volume[dim] = dim_vals[i]
        locations.append({
            "id": loc_id,
            "point": point,
            "time_window": TIME_WINDOW,
            "required_tags": [] if merge else ["tag" + str(vehicle_id)],
            "type": "delivery",
            "shipment_size": {
                "volume": loc_volume
            }
        })
    return retval


def solve_and_check(raw_test, all_even_dropped):
    solution = mvrp_checker.solve_and_check(
        raw_test,
        solver_arguments={'sa_iterations': 1000000},
        expected_status="PARTIAL_SOLVED")
    dropped_ids = set(loc['id'] for loc in solution['dropped_locations'])
    for loc_id in range(LOCATION_CNT):
        if loc_id % 2:
            assert loc_id not in dropped_ids
        elif all_even_dropped:
            assert loc_id in dropped_ids


def test_volume_types_and_alignments():
    """
    Check that volume types and alignments work as expected
    """
    raw_test = json.dumps(generate_json(TT_SEED, merge=False))
    solve_and_check(raw_test, all_even_dropped=True)


def test_polymorphic_merging():
    """
    Check that merging locations of different types and alignments
    does not relax the imposed restrictions
    """
    raw_test = json.dumps(generate_json(MERGE_SEED, merge=True))
    solve_and_check(raw_test, all_even_dropped=False)


if __name__ == "__main__":
    json_tt = generate_json(TT_SEED, merge=False)
    with open(op.join(DATA_PATH, TT_PATH), 'w') as ttf:
        json.dump(json_tt, ttf, indent=4)
    json_merge = generate_json(MERGE_SEED, merge=True)
    with open(op.join(DATA_PATH, MERGE_PATH), 'w') as mergef:
        json.dump(json_merge, mergef, indent=4)
