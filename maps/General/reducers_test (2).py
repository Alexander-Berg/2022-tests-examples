import random

from collections import Counter

import yandex.maps.geolib3 as geolib

from maps.poi.statistics.altay.lib.reducers import (
    AddBldAreaInfoJoinReducer,
    AggregateOrgBuildingsReducer,
    ClusterReducer,
    GluedOrgsReducer,
    OrgConflictsReducer,
    OrgNeighboursDistReducer,
    StaticPoi2NmapsReducer,
)

from reducers_rows import (
    add_bld_area_reducer_rows,
    aggregate_org_buildings_reducer_rows,
    cluster_reducer_rows,
    glued_orgs_reducer_rows,
    org_conflicts_reducer_rows,
    org_neighbours_dist_reducer_rows,
    static_poi_2_nmaps_reducer_rows,
)


class GluedOrgsDataGenerator(object):
    LAT_MIN = -70.0
    LAT_MAX = 70.0
    LON_MIN = -180.0
    LON_MAX = 180.0 - 1e-6

    # https://www.kakras.ru/mobile/book/dlina-dugi.html#dlina-dugi-paralleli
    # for simplicity, lat and lon distances are the same
    DEGREE_MIN_DISTANCE_METERS = 38000.0  # a bit less than actual value
    GLUED_DISTANCE_STD_DEGREES = 1e-6

    def _clear(self):
        self.data = []
        self.actual_stats = {
            'n_separate_orgs': 0,
            'n_glued_orgs': 0,
            'n_region_orgs': 0
        }

    def __init__(self, eps_meters):
        self._clear()
        self.eps_meters = eps_meters

    def _make_point(self, lat, lon):
        return dict(permalink=len(self.data), lat=lat, lon=lon)

    def _add_record(self, lat, lon, glue_probability, glued_max_cluster_size, region, region_max_cluster_size):
        if region:
            n_points = random.randint(1, region_max_cluster_size)
            for i in range(n_points):
                record = self._make_point(lat, lon)
                record['region_center'] = region
                self.data.append(record)
            self.actual_stats['n_region_orgs'] += n_points
        else:
            is_glued = (random.uniform(0.0, 1.0) < glue_probability)
            if is_glued:
                n_points = random.randint(2, glued_max_cluster_size)
                for i in range(n_points):
                    record = self._make_point(lat, lon)
                    self.data.append(record)
                self.actual_stats['n_glued_orgs'] += n_points
            else:
                record = self._make_point(lat, lon)
                self.data.append(record)
                self.actual_stats['n_separate_orgs'] += 1

    def create_dataset(
        self,
        grid_frequency=1000,
        glue_probability=0.03,
        glued_max_cluster_size=20,
        n_regions=50,
        region_max_cluster_size=500
    ):
        random.seed(42)
        self._clear()
        cell_size = self.eps_meters / self.DEGREE_MIN_DISTANCE_METERS
        n_cells_lat = int((self.LAT_MAX - self.LAT_MIN) / cell_size)
        n_cells_lon = int((self.LON_MAX - self.LON_MIN) / cell_size)

        # only even indices to guarantee distance between points in different cells
        lat_cells = random.sample(range(0, n_cells_lat, 2), grid_frequency)
        lon_cells = random.sample(range(0, n_cells_lon, 2), grid_frequency)

        current_region = n_regions
        for lat_cell in lat_cells:
            for lon_cell in lon_cells:
                lat = self.LAT_MIN + cell_size * (lat_cell + 0.5)
                lat += random.uniform(-self.GLUED_DISTANCE_STD_DEGREES, self.GLUED_DISTANCE_STD_DEGREES)
                lon = self.LON_MIN + cell_size * (lon_cell + 0.5)
                lon += random.uniform(-self.GLUED_DISTANCE_STD_DEGREES, self.GLUED_DISTANCE_STD_DEGREES)
                self._add_record(
                    lat,
                    lon,
                    glue_probability,
                    glued_max_cluster_size,
                    current_region,
                    region_max_cluster_size
                )
                if current_region:
                    current_region -= 1
        random.shuffle(self.data)
        return self.data, self.actual_stats


def calculate_glued_orgs_stats(reducer, rows):
    clusters = Counter()
    output_rows = list(reducer(dict(region_id=0), rows))
    for row in output_rows:
        clusters[row['cluster']] += 1

    n_separate_orgs = 0
    n_glued_orgs = 0
    n_region_orgs = 0
    for k, v in clusters.items():
        if '_' not in k:
            n_region_orgs += v
        elif v > 1:
            n_glued_orgs += v
        else:
            n_separate_orgs += 1
    return {
        'n_separate_orgs': n_separate_orgs,
        'n_glued_orgs': n_glued_orgs,
        'n_region_orgs': n_region_orgs
    }


def org_conflicts_data_generator(
        dataset_size=3000,
        region_center_proba=0.1
):
    random.seed(42)
    MIN_LAT, MIN_LON = 55.748914, 37.612587
    MAX_LAT, MAX_LON = 55.753665, 37.619856
    CENTER_LAT, CENTER_LON = 55.753229, 37.622503

    data = []
    for i in range(dataset_size):
        org = {}
        org['permalink'] = i
        if random.uniform(0, 1) < region_center_proba:
            org['lat'], org['lon'] = CENTER_LAT, CENTER_LON
            org['region_center'] = 213
        else:
            org['lat'] = random.uniform(MIN_LAT, MAX_LAT)
            org['lon'] = random.uniform(MIN_LON, MAX_LON)
        data.append(org)
    return data


def org_conflicts_bruteforce(data, distances):
    results = []
    for org in data:
        org_result = {}
        org_result['permalink'] = org['permalink']
        for column_name, distance in distances.items():
            org_result[column_name] = -1
            for another_org in data:
                d = geolib.geodistance(
                    geolib.Point2(org['lon'], org['lat']),
                    geolib.Point2(another_org['lon'], another_org['lat']),
                )
                if d < distance:
                    org_result[column_name] += 1
        results.append(org_result)

    for result, org in zip(results, data):
        for column_name in distances:
            result[column_name + '_no_region_centers'] = 0 if 'region_center' in org else result[column_name]

    return results


def reducer_test(reducer, rows):
    for row in rows:
        result = list(reducer(row['input']['keys'], iter(row['input']['rows'])))
        assert result == row['output']


def test_cluster_reducer():
    reducer_test(ClusterReducer(), cluster_reducer_rows)


def test_glued_orgs_reducer_small():
    reducer_test(GluedOrgsReducer(), glued_orgs_reducer_rows)


def test_glued_orgs_reducer_large():
    eps_meters = 1.0
    data_generator = GluedOrgsDataGenerator(eps_meters)
    data, actual_stats = data_generator.create_dataset()

    reducer = GluedOrgsReducer(eps_meters)
    stats = calculate_glued_orgs_stats(reducer, data)
    assert stats == actual_stats


def test_add_bld_area_info_reducer():
    reducer_test(AddBldAreaInfoJoinReducer(), add_bld_area_reducer_rows)


def test_aggregate_org_buildings_reducer():
    reducer_test(AggregateOrgBuildingsReducer(), aggregate_org_buildings_reducer_rows)


def test_org_neighbours_dist_reducer():
    reducer_test(OrgNeighboursDistReducer(), org_neighbours_dist_reducer_rows)


def test_org_conflicts_reducer_small():
    distances = {
        '8m': 8.0,
        '4m': 4.0
    }
    reducer_test(OrgConflictsReducer(distances), org_conflicts_reducer_rows)


def test_org_conflicts_reducer_large():
    distances = {
        '8m': 8.0,
        '4m': 4.0,
        '2m': 2.0,
        '1m': 1.0,
    }
    data = org_conflicts_data_generator()
    reference_result = org_conflicts_bruteforce(data, distances)
    reference_result = sorted(reference_result, key=lambda x: x['permalink'])

    reducer = OrgConflictsReducer(distances)
    result = list(reducer({'region_id': 0}, data))
    result = sorted(result, key=lambda x: x['permalink'])

    errors_count = 0
    for res, ref_res in zip(result, reference_result):
        if res != ref_res:
            errors_count += 1
    # there are a small percent of errors, since sklearn haversive slightly differs from geolib.geodistance
    assert float(errors_count) / len(data) < 0.02


def test_static_poi_2_nmaps_reducer():
    reducer_test(StaticPoi2NmapsReducer(), static_poi_2_nmaps_reducer_rows)
