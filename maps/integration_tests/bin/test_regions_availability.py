import json
import unittest
import os
import logging

from .common import (
    start_task,
    get_task_result,
    API_ENDPOINT,
    ADD_SVRP_TASK_QUERY,
    TASK_SVRP_RESULT_QUERY_PATTERN
)
from .locs import REGIONS


def _prepare_task(locations, time_window, time_zone_shift_hours, plan_date):
    return {
        "depot": {
            "id": 0,
            "point": locations[0],
            "time_window": time_window,
            "service_duration_s": 0
        },
        "locations": [
            {
                "id": 1,
                "point": locations[1],
                "time_window": time_window,
                "service_duration_s": 0,
                "hard_window": False,
                "shipment_size": {
                    "volume": {
                        "width_m": 0,
                        "depth_m": 0,
                        "height_m": 0
                    },
                    "weight_kg": 1
                }
            }
        ],
        "options": {
            "date": plan_date,
            "time_zone": time_zone_shift_hours,
            "minimize": "combined",
            "default_speed_km_h": 20,
            "solver_time_limit_s": 1,
            "thread_count": 1,
            "task_count": 1
        },
        "vehicle": {
            "id": 0,
            "capacity": {
                "limits": {
                    "volume_perc": 90,
                    "weight_perc": 100
                },
                "volume": {
                    "width_m": 5,
                    "depth_m": 5,
                    "height_m": 5
                },
                "weight_kg": 100
            },
            "ref": "Vehicle_1",
        }
    }


def _run_mvrp_task(task):
    task_id = start_task(API_ENDPOINT + ADD_SVRP_TASK_QUERY, task)
    return task_id, get_task_result(API_ENDPOINT + TASK_SVRP_RESULT_QUERY_PATTERN.format(task_id), task_id)


class RegionsAvailabilityTest(unittest.TestCase):
    def test_regions(self):
        selected_region_name = os.environ.get('SELECTED_REGION')
        if selected_region_name:
            selected_regions = [region for region in REGIONS if region.name == selected_region_name]
            self.assertTrue(selected_regions)
        else:
            selected_regions = REGIONS

        skipped_regions = set(os.environ.get('SKIPPED_REGIONS', '').split(','))
        logging.info(f"Skip regions: {skipped_regions}")
        selected_regions = [r for r in selected_regions if r.name not in skipped_regions]

        for region in selected_regions:
            logging.info("Test service availability in region {}".format(region.name))
            total_durations = {}
            time_windows = ["03:00:00-23:59:59", "10:00:00-23:59:59"]
            for time_window in time_windows:
                task = _prepare_task(region.locations, time_window, region.time_zone_shift_hours, "2018-07-13")
                logging.debug(json.dumps(task, indent=4))
                task_id, result = _run_mvrp_task(task)
                logging.debug(json.dumps(result, indent=4))
                self.assertTrue(
                    "result" in result,
                    "Failed to solve task {} for region {}: {}".format(task_id, region.name, result.get('message', 'No message')))
                total_duration = result["result"]["metrics"]["total_transit_duration_s"]
                total_durations[time_window] = total_duration
                logging.info("Total transit duration for region {} and time window {} is {}".format(region.name, time_window, total_duration))

            t0 = total_durations[time_windows[0]]
            t1 = total_durations[time_windows[1]]

            self.assertTrue(
                t0 != t1,
                msg="No difference in durations in region {} ({}s in time windows {} and {})".format(
                    region.name, t0, time_windows[0], time_windows[1])
            )
            logging.info("Checked difference of durations in region {}".format(region.name))
