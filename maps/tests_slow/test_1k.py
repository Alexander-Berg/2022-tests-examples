import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_1k():
    """
    checks metrics and complete result on 1000 locations and 1 vehicle

    Solver deviation results (30 runs):
    log2_iterations  cost_best   cost_avr    cost_2std(%)  cost_99err(%)
    1500000.000      113431.801  117431.624  3.430         6.300
    """
    route = mvrp_checker.solve_and_check(
        open(tools.data_path('data/1k_locs.json')).read(),
        open(tools.data_path('data/1k_locs_distances.json')).read(),
        solver_arguments={
            'sa_iterations': 1500000,
            'sa_temperature': 500},
        runs_count=10)

    expected_metrics = {
        "assigned_locations_count": 999.0,
        "balanced_group_duration_deviation_s": 0.0,
        "balanced_group_penalty": 0.0,
        "balanced_group_stop_count_deviation": 0.0,
        "depot_throughput_violation_kg": 0.0,
        "depot_throughput_violation_kg_per_hour": 0.0,
        "depot_throughput_violation_units": 0.0,
        "depot_throughput_violation_units_per_hour": 0.0,
        "dropped_locations_count": 0.0,
        "early_depot_count": 0.0,
        "early_locations_count": 0.0,
        "early_shifts_count": 0.0,
        "failed_max_work_duration_count": 0.0,
        "failed_min_work_duration_count": 0.0,
        "failed_time_window_depot_count": 0.0,
        "failed_time_window_depot_count_penalty": 0.0,
        "failed_time_window_depot_duration_penalty": 0.0,
        "failed_time_window_depot_duration_s": 0.0,
        "failed_time_window_locations_count": 0.0,
        "failed_time_window_locations_count_penalty": 0.0,
        "failed_time_window_locations_duration_penalty": 0.0,
        "failed_time_window_locations_duration_s": 0.0,
        "failed_time_window_shifts_count": 0.0,
        "failed_time_window_shifts_count_penalty": 0.0,
        "failed_time_window_shifts_duration_penalty": 0.0,
        "failed_time_window_shifts_duration_s": 0.0,
        "failed_work_duration_count": 0.0,
        "failed_work_duration_count_penalty": 0.0,
        "failed_work_duration_penalty": 0.0,
        "failed_work_duration_s": 0.0,
        "global_proximity": 17.449,
        "late_depot_count": 0.0,
        "late_locations_count": 0.0,
        "late_shifts_count": 0.0,
        "lateness_risk_locations_count": 0.0,
        "max_vehicle_runs": 2.0,
        "number_of_routes": 27.2,
        "objective_minimum": 7209634.979,
        "operations_per_second": 5826.0,
        "optimization_steps": 1500000.0,
        "overtime_duration_penalty": 0.0,
        "overtime_duration_s": 0.0,
        "overtime_penalty": 0.0,
        "overtime_shifts_count": 0.0,
        "overtime_shifts_count_penalty": 0.0,
        "proximity": 5.161,
        "total_cost": 117416.473,
        "total_cost_with_penalty": 117431.624,
        "total_depot_penalty": 0.0,
        "total_drop_penalty": 0.0,
        "total_duration_cost": 16903.925,
        "total_duration_s": 608541.291,
        "total_early_count": 0.0,
        "total_early_duration_s": 0.0,
        "total_early_penalty": 0.0,
        "total_failed_delivery_deadline_count": 0.0,
        "total_failed_delivery_deadline_duration_s": 0.0,
        "total_failed_delivery_deadline_penalty": 0.0,
        "total_failed_time_window_count": 0.0,
        "total_failed_time_window_duration_s": 0.0,
        "total_failed_time_window_penalty": 0.0,
        "total_fixed_cost": 72240.0,
        "total_global_proximity_distance_m": 16179960.255,
        "total_global_proximity_duration_s": 2024973.915,
        "total_global_proximity_penalty": 0.0,
        "total_guaranteed_penalty": 0.0,
        "total_late_count": 0.0,
        "total_late_duration_s": 0.0,
        "total_late_penalty": 0.0,
        "total_lateness_risk_probability": 28.582,
        "total_locations_cost": 0.0,
        "total_mileage_penalty": 0.0,
        "total_penalty": 15.15,
        "total_probable_penalty": 15.15,
        "total_proximity_distance_m": 5171891.338,
        "total_proximity_duration_s": 772895.124,
        "total_proximity_penalty": 0.0,
        "total_runs_cost": 0.0,
        "total_service_duration_s": 0.0,
        "total_stop_count_penalty": 0.0,
        "total_stops": 951.36,
        "total_transit_distance_cost": 28272.549,
        "total_transit_distance_m": 3534068.564,
        "total_transit_duration_s": 575663.905,
        "total_unfeasibility_count": 0.0,
        "total_unfeasibility_penalty": 0.0,
        "total_waiting_duration_s": 32877.386,
        "total_work_breaks": 0.0,
        "used_vehicles": 24.08
    }

    rel_accuracies = {
        "global_proximity": 0.07332500000000002,
        "number_of_routes": 0.049174999999999996,
        "objective_minimum": 0.032025,
        "operations_per_second": 0.0620375,
        "proximity": 0.0485625,
        "total_cost": 0.030012500000000004,
        "total_cost_with_penalty": 0.030012500000000004,
        "total_duration_cost": 0.033424999999999996,
        "total_duration_s": 0.033424999999999996,
        "total_fixed_cost": 0.05521249999999999,
        "total_global_proximity_distance_m": 0.07350000000000001,
        "total_global_proximity_duration_s": 0.0713125,
        "total_lateness_risk_probability": 0.3285625,
        "total_penalty": 4.362487499999999,
        "total_probable_penalty": 4.362487499999999,
        "total_proximity_distance_m": 0.0469875,
        "total_proximity_duration_s": 0.0357875,
        "total_transit_distance_cost": 0.0417375,
        "total_transit_distance_m": 0.0417375,
        "total_transit_duration_s": 0.0298375,
        "total_waiting_duration_s": 0.37415,
        "used_vehicles": 0.05521249999999999
    }

    tools.check_metrics_are_close(route["metrics"], expected_metrics, rel_accuracies)
