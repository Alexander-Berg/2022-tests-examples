#!/usr/bin/env python3


import argparse
import json
import sys
import copy
import logging


def parse_args():
    p = argparse.ArgumentParser()
    p.add_argument('-r', '--request', required=True)
    p.add_argument('-i', '--response', required=True)
    return p.parse_args()


def get_vehicle_capacity_kg(vehicle):
    capacity = vehicle['capacity']
    return capacity['weight_kg'] * capacity.get("limits", {}).get("weight_perc", 100) / 100.0


def try_change_vehicle(route, vehicles, vehicle, title):
    used_vehicle = vehicles[route['vehicle_id']]
    utilization_kg = route['metrics']['utilization_weight_kg']
    if get_vehicle_capacity_kg(vehicle) >= utilization_kg and \
       used_vehicle['cost']['run'] > vehicle['cost']['run']:
        logging.error(
            "[%s] better vehicle id %d => %d; ton %g => %g (%g used); $ %g => %g." % (
                title,
                used_vehicle['id'],
                vehicle['id'],
                1e-3 * get_vehicle_capacity_kg(used_vehicle),
                1e-3 * get_vehicle_capacity_kg(vehicle),
                1e-3 * utilization_kg,
                used_vehicle['cost']['run'],
                vehicle['cost']['run']
            )
        )
        sys.stderr.flush()
        route['vehicle_id'] = copy.copy(vehicle['id'])
        return True
    return False


def try_change_vehicles(request, response, title='FOUND'):
    vehicles = {}

    used_vehicles = set()
    for route in response['routes']:
        used_vehicles.add(route['vehicle_id'])

    free_vehicles = []
    for vehicle in request['vehicles']:
        vehicles[vehicle['id']] = vehicle
        if vehicle['id'] not in used_vehicles:
            free_vehicles.append(vehicle)
    free_vehicles = sorted(free_vehicles, key=lambda v: v['cost']['run'])

    better_vehicles_found = 0
    while free_vehicles:
        free_vehicle = free_vehicles.pop(0)
        for route in response['routes']:
            current_vehicle_id = route['vehicle_id']
            if try_change_vehicle(route, vehicles, free_vehicle, title):
                free_vehicles.append(vehicles[current_vehicle_id])
                better_vehicles_found += 1
                break

    return better_vehicles_found


def try_improve_solution(request_path, response_path, title):
    with open(request_path) as f_in:
        request = json.load(f_in)

    with open(response_path) as f_in:
        response = json.load(f_in)
        response = response.get('result', response)

    return try_change_vehicles(request, response, title)


def main():
    args = parse_args()
    sys.exit(try_improve_solution(args.request, args.response, 'FOUND'))


if __name__ == "__main__":
    main()
