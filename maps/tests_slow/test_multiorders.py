import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_multiorders_azbuka():
    """
    Checks mvrp request of Azbuka with small multiorders:
    2-5 orders per multiorder (90 uniq locations out of 300).

    Solver deviation results (30 runs):
    log2_iterations  cost_best  cost_avr   cost_2std(%)  cost_99err(%)
    10000000.000     85272.120  88674.671  3.900         8.090
    """
    route = mvrp_checker.solve_and_check(
        open(tools.data_path('data/azbuka_request.json')).read(),
        open(tools.data_path('data/azbuka_distances.json')).read(),
        solver_arguments={'sa_iterations': 10000000},
        runs_count=5)

    expected_metrics = {
        "assigned_locations_count": 267.0,
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
        "early_shifts_count": 0.08,
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
        "failed_time_window_shifts_count": 0.24,
        "failed_time_window_shifts_count_penalty": 240.0,
        "failed_time_window_shifts_duration_penalty": 44.155,
        "failed_time_window_shifts_duration_s": 155.84,
        "failed_work_duration_count": 0.0,
        "failed_work_duration_count_penalty": 0.0,
        "failed_work_duration_penalty": 0.0,
        "failed_work_duration_s": 0.0,
        "global_proximity": 26.279,
        "late_depot_count": 0.0,
        "late_locations_count": 0.0,
        "late_shifts_count": 0.16,
        "lateness_risk_locations_count": 18.48,
        "max_vehicle_runs": 5.28,
        "number_of_routes": 35.96,
        "objective_minimum": 4912819.28,
        "operations_per_second": 10920.326,
        "optimization_steps": 10000000.0,
        "overtime_duration_penalty": 0.0,
        "overtime_duration_s": 0.0,
        "overtime_penalty": 0.0,
        "overtime_shifts_count": 0.0,
        "overtime_shifts_count_penalty": 0.0,
        "proximity": 14.665,
        "total_cost": 88390.516,
        "total_cost_with_penalty": 88674.671,
        "total_depot_penalty": 0.0,
        "total_drop_penalty": 0.0,
        "total_duration_cost": 15484.674,
        "total_duration_s": 557448.28,
        "total_early_count": 0.08,
        "total_early_duration_s": 92.0,
        "total_early_penalty": 106.067,
        "total_failed_delivery_deadline_count": 0.0,
        "total_failed_delivery_deadline_duration_s": 0.0,
        "total_failed_delivery_deadline_penalty": 0.0,
        "total_failed_time_window_count": 0.24,
        "total_failed_time_window_duration_s": 155.84,
        "total_failed_time_window_penalty": 284.155,
        "total_fixed_cost": 29760.0,
        "total_global_proximity_distance_m": 2104591.84,
        "total_global_proximity_duration_s": 177055.36,
        "total_global_proximity_penalty": 0.0,
        "total_guaranteed_penalty": 284.155,
        "total_late_count": 0.16,
        "total_late_duration_s": 63.84,
        "total_late_penalty": 178.088,
        "total_lateness_risk_probability": 791.589,
        "total_locations_cost": 0.0,
        "total_mileage_penalty": 0.0,
        "total_penalty": 284.155,
        "total_probable_penalty": 11033.494,
        "total_proximity_distance_m": 5060774.68,
        "total_proximity_duration_s": 335759.225,
        "total_proximity_penalty": 0.0,
        "total_runs_cost": 0.0,
        "total_service_duration_s": 206220.0,
        "total_stop_count_penalty": 0.0,
        "total_stops": 90.0,
        "total_transit_distance_cost": 43145.842,
        "total_transit_distance_m": 5393230.2,
        "total_transit_duration_s": 351167.0,
        "total_unfeasibility_count": 0.0,
        "total_unfeasibility_penalty": 0.0,
        "total_waiting_duration_s": 61.28,
        "total_work_breaks": 0.0,
        "used_vehicles": 9.92
    }

    rel_accuracies = {
        "early_shifts_count": 6.9222,
        "failed_time_window_shifts_count": 3.6324,
        "failed_time_window_shifts_count_penalty": 3.6324,
        "failed_time_window_shifts_duration_penalty": 4.342,
        "failed_time_window_shifts_duration_s": 4.342,
        "global_proximity": 0.18899999999999997,
        "late_shifts_count": 4.677099999999999,
        "lateness_risk_locations_count": 0.5429999999999999,
        "max_vehicle_runs": 0.1736,
        "number_of_routes": 0.037599999999999995,
        "objective_minimum": 0.0416,
        "operations_per_second": 0.0264,
        "proximity": 0.2155,
        "total_cost": 0.035699999999999996,
        "total_cost_with_penalty": 0.039,
        "total_early_count": 6.9222,
        "total_early_duration_s": 6.9293,
        "total_early_penalty": 6.9226,
        "total_failed_time_window_count": 3.6324,
        "total_failed_time_window_duration_s": 4.342,
        "total_failed_time_window_penalty": 3.6512000000000002,
        "total_fixed_cost": 0.1152,
        "total_global_proximity_distance_m": 0.1917,
        "total_global_proximity_duration_s": 0.18469999999999998,
        "total_guaranteed_penalty": 3.6512000000000002,
        "total_late_count": 4.677099999999999,
        "total_late_duration_s": 4.9638,
        "total_late_penalty": 4.6800999999999995,
        "total_lateness_risk_probability": 0.4489,
        "total_penalty": 3.6512000000000002,
        "total_probable_penalty": 0.4636,
        "total_proximity_distance_m": 0.0406,
        "total_proximity_duration_s": 0.0379,
        "total_transit_distance_cost": 0.0301,
        "total_transit_distance_m": 0.0301,
        "total_transit_duration_s": 0.029900000000000003,
        "total_waiting_duration_s": 6.9067,
        "used_vehicles": 0.1152
    }

    tools.check_metrics_are_close(route["metrics"], expected_metrics, rel_accuracies)


def test_multiorders_kse():
    """
    Checks mvrp request of KSE with lots of multiorders -
    up to 96 orders per multiorder (494 uniq locations out of 1146).

    Solver deviation results (55 runs):
    log2_iterations  cost_best   cost_avr    cost_2std(%)  cost_99err(%)
    10000000.000     193731.861  204742.279  3.920         8.290
    """
    route = mvrp_checker.solve_and_check(
        open(tools.data_path('data/kse_request.json')).read(),
        open(tools.data_path('data/kse_distances.json')).read(),
        solver_arguments={'sa_iterations': 10000000},
        runs_count=5)

    expected_metrics = {
        "assigned_locations_count": 1137.0,
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
        "failed_time_window_shifts_count": 0.02,
        "failed_time_window_shifts_count_penalty": 20.0,
        "failed_time_window_shifts_duration_penalty": 71.275,
        "failed_time_window_shifts_duration_s": 251.56,
        "failed_work_duration_count": 0.0,
        "failed_work_duration_count_penalty": 0.0,
        "failed_work_duration_penalty": 0.0,
        "failed_work_duration_s": 0.0,
        "global_proximity": 2.171,
        "late_depot_count": 0.0,
        "late_locations_count": 0.0,
        "late_shifts_count": 0.02,
        "lateness_risk_locations_count": 10.94,
        "max_vehicle_runs": 1.0,
        "number_of_routes": 12.84,
        "objective_minimum": 11082092.105,
        "operations_per_second": 7764.465,
        "optimization_steps": 10000000.0,
        "overtime_duration_penalty": 0.0,
        "overtime_duration_s": 0.0,
        "overtime_penalty": 0.0,
        "overtime_shifts_count": 0.0,
        "overtime_shifts_count_penalty": 0.0,
        "proximity": 0.477,
        "total_cost": 204651.003,
        "total_cost_with_penalty": 204742.279,
        "total_depot_penalty": 0.0,
        "total_drop_penalty": 0.0,
        "total_duration_cost": 45350.883,
        "total_duration_s": 326526.36,
        "total_early_count": 0.0,
        "total_early_duration_s": 0.0,
        "total_early_penalty": 0.0,
        "total_failed_delivery_deadline_count": 0.0,
        "total_failed_delivery_deadline_duration_s": 0.0,
        "total_failed_delivery_deadline_penalty": 0.0,
        "total_failed_time_window_count": 0.02,
        "total_failed_time_window_duration_s": 251.56,
        "total_failed_time_window_penalty": 91.275,
        "total_fixed_cost": 38520.0,
        "total_global_proximity_distance_m": 1043589.44,
        "total_global_proximity_duration_s": 234515.02,
        "total_global_proximity_penalty": 0.0,
        "total_guaranteed_penalty": 91.275,
        "total_late_count": 0.02,
        "total_late_duration_s": 251.56,
        "total_late_penalty": 91.275,
        "total_lateness_risk_probability": 480.663,
        "total_locations_cost": 0.0,
        "total_mileage_penalty": 0.0,
        "total_penalty": 91.275,
        "total_probable_penalty": 22455.706,
        "total_proximity_distance_m": 251775.93,
        "total_proximity_duration_s": 58561.597,
        "total_proximity_penalty": 0.0,
        "total_runs_cost": 0.0,
        "total_service_duration_s": 276897.0,
        "total_stop_count_penalty": 0.0,
        "total_stops": 495.14,
        "total_transit_distance_cost": 120780.12,
        "total_transit_distance_m": 241560.24,
        "total_transit_duration_s": 49555.48,
        "total_unfeasibility_count": 0.0,
        "total_unfeasibility_penalty": 0.0,
        "total_waiting_duration_s": 73.88,
        "total_work_breaks": 0.0,
        "used_vehicles": 12.84
    }

    rel_accuracies = {
        "failed_time_window_shifts_count": 8.838812500000001,
        "failed_time_window_shifts_count_penalty": 8.838812500000001,
        "failed_time_window_shifts_duration_penalty": 8.838812500000001,
        "failed_time_window_shifts_duration_s": 8.838812500000001,
        "global_proximity": 0.1056875,
        "late_shifts_count": 8.838812500000001,
        "lateness_risk_locations_count": 0.255375,
        "number_of_routes": 0.0360625,
        "objective_minimum": 0.0243125,
        "operations_per_second": 0.07981250000000001,
        "proximity": 0.044937500000000005,
        "total_cost": 0.0249375,
        "total_cost_with_penalty": 0.0245,
        "total_failed_time_window_count": 8.838812500000001,
        "total_failed_time_window_duration_s": 8.838812500000001,
        "total_failed_time_window_penalty": 8.838812500000001,
        "total_fixed_cost": 0.0360625,
        "total_global_proximity_distance_m": 0.106125,
        "total_global_proximity_duration_s": 0.08481250000000001,
        "total_guaranteed_penalty": 8.838812500000001,
        "total_late_count": 8.838812500000001,
        "total_late_duration_s": 8.838812500000001,
        "total_late_penalty": 8.838812500000001,
        "total_lateness_risk_probability": 0.1858125,
        "total_penalty": 8.838812500000001,
        "total_probable_penalty": 0.46799999999999997,
        "total_proximity_distance_m": 0.036375000000000005,
        "total_proximity_duration_s": 0.03675,
        "total_transit_distance_cost": 0.0305,
        "total_transit_distance_m": 0.0305,
        "total_transit_duration_s": 0.03375,
        "total_waiting_duration_s": 2.75875,
        "used_vehicles": 0.0360625
    }

    tools.check_metrics_are_close(route["metrics"], expected_metrics, rel_accuracies)
