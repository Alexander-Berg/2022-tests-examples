import logging
import json
import math
import yatest.common

from maps.automotive.radio.tools.pylib.geobase_helper import GeobaseHelper
from maps.automotive.radio.tools.pylib.utils import walk_dirs


logger = logging.getLogger("test_data_logger")


def get_distance(lat1, lon1, lat2, lon2):
    lat1, lon1, lat2, lon2 = map(math.radians, [lat1, lon1, lat2, lon2])
    d_lat = lat2 - lat1
    d_lon = lon2 - lon1
    temp = (
        math.sin(d_lat / 2) ** 2 +
        math.cos(lat1) *
        math.cos(lat2) *
        math.sin(d_lon / 2) ** 2
    )
    return 6373.0 * (2 * math.atan2(math.sqrt(temp), math.sqrt(1 - temp))) * 1000


def test_frequency_conflict():
    towers = []
    walk_dirs(
        yatest.common.source_path('maps/automotive/radio/data/broadcasts'),
        lambda f: towers.append(json.load(open(f))))

    geobase_helper = GeobaseHelper(yatest.common.binary_path("maps/automotive/radio/tools/geobase_data/geodata6.bin"))
    gids_map = json.load(open(yatest.common.source_path('maps/automotive/radio/tools/prepare_data_from_csv/gids_map.json')))
    max_radius = max(station["radius"] for tower in towers for station in tower["radiostations"])

    result = ""
    for i, tower1 in enumerate(towers):
        for j in range(i + 1, len(towers)):
            tower2 = towers[j]
            dist = get_distance(tower1["lat"], tower1["lon"], tower2["lat"], tower2["lon"])
            if dist < max_radius:
                stations1 = {station["frequency"]: station for station in tower1["radiostations"]}
                stations2 = {station["frequency"]: station for station in tower2["radiostations"]}
                conflicts = set(stations1.keys()).intersection(set(stations2.keys()))
                for freq in sorted(conflicts):
                    station1 = stations1[freq]
                    station2 = stations2[freq]
                    if station1["group_id"] != station2["group_id"] and dist < (station1["radius"] + station2["radius"]) * 0.8:
                        result += f"[WARNING] stations conflict with distance {int(dist)}m:\n"

                        def print_station(tower, station):
                            parents = geobase_helper.get_parent_region_names(tower['lat'], tower['lon'])
                            return (
                                f"\t{parents[0]}, {parents[1]}, "
                                f"({tower['lat']}, {tower['lon']}), "
                                f"Radius={station['radius']}, "
                                f"Stations={gids_map[station['group_id']][0]}({station['group_id']}), "
                                f"frequency={station['frequency']}\n")

                        result += print_station(tower1, station1)
                        result += print_station(tower2, station2)
    return result
