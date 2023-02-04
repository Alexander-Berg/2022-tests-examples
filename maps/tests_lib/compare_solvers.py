#!/usr/bin/env python3


import argparse
import datetime
import json
import subprocess
import sys
import collections
import scipy
import scipy.stats
import tabulate
import logging
import time
import os

from multiprocessing import Pool, cpu_count


CRITICAL_METRICS = ['dropped_locations_count', 'failed_locations_count']


def parse_args(args_list):
    p = argparse.ArgumentParser()
    p.add_argument('-t', '--time-limit', type=float, help='time limit in seconds')
    p.add_argument('-1', '--solver1', help='Command line template for the first solver. Allowed variables: {path}, {time_limit}, {run_num}')
    p.add_argument('-2', '--solver2', help='Command line template for second solver', required=True)
    p.add_argument('-q', '--quiet', action='store_true', help='less output')
    p.add_argument('--critical', action='store_true', help='show only critical diffs')
    p.add_argument('-u', '--update', action='store_true', help='update metrics and test results with solver2 responses')
    p.add_argument('--must-be-equal', action='store_true', help='metrics of two solvers must be equal')
    p.add_argument('-m', '--metrics', help='average metrics must be as in this json file')
    p.add_argument('-r', '--solver-runs', type=int, default=1, help='number of times to run each solver (the best result accepted)')
    p.add_argument('-j', '--jobs-count', type=int, default=1, help='maximum number of concurrent jobs running, 0 means as many as number of CPU')
    p.add_argument('--approx-compare', action='store_true', help='allow some differences in metrics')
    p.add_argument('problems', nargs="*", help='json file(s) with problem configuration')
    return p.parse_args(args_list[1:])


def is_num(v):
    return isinstance(v, float) or isinstance(v, int)


def rel_diff(v1, v2):
    return 100. * (v1 - v2) / max(v2, 1e-6)


def are_values_close(v1, v2, eps):
    if v1 == v2:
        return True
    if v1 == 0 or v2 == 0:
        return False  # since v1 != v2
    diff = 1.0 * abs(v1 - v2)
    if max(diff/v1, diff/v2) < eps:
        return True
    return False


def metrics_equal(result1, result2):
    ignore_metrics = ['operations_per_second']
    for key in sorted(set(result1) & set(result2) - set(ignore_metrics)):
        if key[:1] != '_':  # hidden metrics
            v1 = result1[key]
            v2 = result2[key]
            if not are_values_close(v1, v2, 1e-4):
                return False
    return True


def check_metrics(results, metrics):
    for key in sorted(set(results) & set(metrics)):
        values = results[key]
        average = scipy.average(values)
        if average != metrics[key]:
            return False
    return True


def print_metrics_diff(title, results1, results2, critical_only=False, diff_only=False):
    output = []

    critical_diff = False
    for key in sorted(set(results1) & set(results2)):
        v1 = results1[key]
        v2 = results2[key]
        if is_num(v1) and is_num(v2):
            if diff_only and v2 == v1 or key[:1] == "_":
                continue
            output.append([
                key, float(v1), float(v2),
                "%+.2f%%" % rel_diff(v1, v2),
                "%+.2f%%" % rel_diff(v2, v1)
            ])
            if v2 != v1 and key in CRITICAL_METRICS:
                critical_diff = True
    if not critical_only or critical_diff:
        logging.warn("=" * len(title) + '\n' + title + '\n' + '-' * len(title))
        titles = ["measure", "value1", "value2", "diff1", "diff2"]
        logging.warn(tabulate.tabulate(output, titles, floatfmt="g", numalign='right'))
        logging.warn("")


def print_final_results(results1, results2, title):
    output = []

    for key in sorted(set(results1) & set(results2)):
        values1, values2 = results1[key], results2[key]

        rel_diffs = [rel_diff(v2, v1) for v1, v2 in zip(values1, values2)]
        non_zero_diffs = [diff for diff in rel_diffs if diff != 0]
        zeroes = [0.0] * len(non_zero_diffs)
        wx_pvalue = scipy.stats.wilcoxon(non_zero_diffs, zeroes)[1] if len(non_zero_diffs) > 1 else 1.0
        avr1, avr2 = scipy.average(values1), scipy.average(values2)
        rel_avr_diff = "%+.2f%%" % rel_diff(avr2, avr1)

        less_cnt = sum(1 for diff in non_zero_diffs if diff < 0)
        more_cnt = sum(1 for diff in non_zero_diffs if diff > 0)
        equal_cnt = len(rel_diffs) - len(non_zero_diffs)

        output.append([key, '%g' % avr1, '%g' % avr2, '%+f' % (avr2-avr1), rel_avr_diff, '%g' % wx_pvalue, less_cnt, more_cnt, equal_cnt])

    headers = ["measure", "average1", "average2", "diff", "relative_diff", "pvalue", "less", "more", "equal"]
    logging.warn("\n===%s===" % title)
    logging.warn(tabulate.tabulate(output, headers=headers, numalign='right'))


def execute_json(cmdline):
    start_time = time.time()

    proc = subprocess.Popen(
        ['bash', '-o', 'pipefail', '-c', cmdline],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE)
    out, err = proc.communicate()

    exec_time = time.time() - start_time

    if proc.poll():
        logging.info(err.decode('utf-8'))
        return None, exec_time
    try:
        return json.loads(out.decode('utf-8')), exec_time
    except json.decoder.JSONDecodeError:
        return None, exec_time


def get_metrics(result):
    result = result.get('result', result)
    metrics = result.get('metrics', result)
    metrics['failed_locations_count'] = \
            metrics.get('failed_time_window_locations_count', 0) + \
            metrics.get('dropped_locations_count', 0)
    return metrics


def cut_tail(s, tail):
    return s[:-len(tail)] if s.endswith(tail) else s


def get_response_path(request_path):
    return cut_tail(cut_tail(request_path, '.json'), '_request') + '_response.json'


def get_distances_path(request_path):
    return cut_tail(cut_tail(request_path, '.json'), '_request') + '_distances.json'


def run_solver(params):
    solver, solver_runs, format_args, path, solver_num = params

    exec_time = 0.
    if solver:
        dist_path = get_distances_path(path)
        dist_exists = os.path.exists(dist_path)
        opts = (' -d ' + dist_path) if dist_exists else ''
        if not dist_exists:
            logging.warn('distance matrix file %s does not exist, using haversine distance' % dist_path)
        results = []
        for run_num in range(solver_runs):
            cmd = solver.format(path=path, run_num=run_num, **format_args) + opts
            logging.info('[%d] %s' % (solver_num, cmd))
            result, exec_time = execute_json(cmd)
            if not result:
                return None, exec_time, path
            results.append((result, exec_time))
        result, exec_time = min(results, key=lambda r: get_metrics(r[0])['objective_minimum'])
    else:
        try:
            with open(get_response_path(path)) as f_in:
                result = json.load(f_in)
        except:
            result = None
    return result, exec_time, path


def update_test_data(result, request_path):
    response_path = get_response_path(request_path)
    if result:
        with open(response_path, 'w') as f_out:
            json.dump(result, f_out, indent=4, sort_keys=True)
        return 1
    if os.path.exists(response_path):
        os.unlink(response_path)
    return 0


class AverageDict:
    def __init__(self):
        self.values = collections.defaultdict(list)
        self.count = 0

    def add(self, dct):
        for key, value in dct.items():
            if is_num(value):
                self.values[key].append(value)
        self.count += 1

    def get(self):
        return dict(
            (key, scipy.average(values))
            for key, values in self.values.items() if is_num(values[0]))

    def __len__(self):
        return self.count


def main(args_list):
    args = parse_args(args_list)
    logging.basicConfig(format='%(asctime)-15s %(message)s', level=logging.WARN if args.quiet else logging.INFO)

    format_args = {}
    if args.time_limit:
        format_args.update({
            "time_limit": str(args.time_limit)
        })

    results1 = AverageDict()
    results2 = AverageDict()

    results1_with_same_fails = AverageDict()
    results2_with_same_fails = AverageDict()

    fails1, fails2, skipped, updated = 0, 0, 0, 0

    metrics = None
    if args.metrics:
        if args.update and not os.path.exists(args.metrics):
            metrics = {}
        else:
            with open(args.metrics) as f_in:
                metrics = json.load(f_in)

    jobs_count = args.jobs_count
    if jobs_count == 0:
        jobs_count = cpu_count() - 1

    pool = Pool(jobs_count)
    print(f'Created pool with {jobs_count} worker(s)', file=sys.stderr, flush=True)

    def run_tasks(solver, solver_num):
        tasks = [(solver, args.solver_runs, format_args, fpath, solver_num) for fpath in args.problems]
        results = []
        print(f'Running solver {solver_num}: {solver}', file=sys.stderr, flush=True)
        for result in pool.imap_unordered(run_solver, tasks):
            results.append(result)
            done = 100.0 * len(results) / len(args.problems)
            dt = datetime.datetime.now().replace(microsecond=0).isoformat()
            print(f'{dt} [{done:.2f}%] {len(results)}/{len(args.problems)}: {result[1]:.3f}s, {result[2]}', end='\r', file=sys.stderr, flush=True)
        print('', file=sys.stderr, flush=True)
        results.sort(key=lambda r: r[2])
        return results

    try:
        solver1_results = run_tasks(args.solver1, 1)
        solver2_results = run_tasks(args.solver2, 2)

        assert len(solver1_results) == len(args.problems)
        assert len(solver1_results) == len(solver2_results)

        for idx, test_data in enumerate(zip(solver1_results, solver2_results)):
            logging.info("===test #{0}===".format(idx+1))
            assert test_data[0][2] == test_data[1][2]
            fpath = test_data[0][2]
            result1, time1 = test_data[0][:2]
            result2, time2 = test_data[1][:2]

            if args.update:
                updated += update_test_data(result2, fpath)

            if not result1:
                fails1 += 1

            if not result2:
                fails2 += 1

            if not result1:
                logging.info("failed to load json output of solver (1), test skipped")
                skipped += 1
                continue

            if not result2:
                logging.info("failed to load json output of solver (2), test skipped")
                skipped += 1
                continue

            result1 = get_metrics(result1)
            result2 = get_metrics(result2)

            equal = metrics_equal(result1, result2)

            if args.must_be_equal and not equal:
                logging.error('metrics are not equal')
                print_metrics_diff(fpath, result1, result2, args.critical, diff_only=True)
                return 1

            result1['_solver_duration_s'] = time1
            result2['_solver_duration_s'] = time2

            results1.add(result1)
            results2.add(result2)

            if result1.get('failed_locations_count', 0) == result2.get('failed_locations_count', 0) and \
               result1.get('total_failed_time_window_duration_s', 0) == result2.get('total_failed_time_window_duration_s', 0):
                results1_with_same_fails.add(result1)
                results2_with_same_fails.add(result2)

            if not equal:
                print_metrics_diff(fpath, result1, result2, args.critical, diff_only=True)

    except KeyboardInterrupt:
        pass

    rel_accuracies = {
        "dropped_locations_count": 0.3,
        "early_locations_count": 0.15,
        "failed_time_window_depot_count": 0.3,
        "failed_time_window_depot_count_penalty": 0.2,
        "failed_time_window_depot_duration_penalty": 0.3,
        "failed_time_window_depot_duration_s": 0.3,
        "failed_time_window_locations_count": 0.1,
        "failed_time_window_locations_count_penalty": 0.1,
        "failed_time_window_locations_duration_penalty": 0.1,
        "failed_time_window_locations_duration_s": 0.1,
        "global_proximity": 0.2,
        "late_depot_count": 0.2,
        "late_locations_count": 0.2,
        "lateness_risk_locations_count": 0.2,
        "objective_minimum": 0.3,
        "proximity": 0.05,
        "total_cost_with_penalty": 0.3,
        "total_drop_penalty": 0.3,
        "total_early_count": 0.15,
        "total_early_duration_s": 0.1,
        "total_early_penalty": 0.1,
        "total_failed_time_window_count": 0.1,
        "total_failed_time_window_duration_s": 0.1,
        "total_failed_time_window_penalty": 0.1,
        "total_global_proximity_distance_m": 0.05,
        "total_global_proximity_duration_s": 0.05,
        "total_guaranteed_penalty": 0.3,
        "total_late_count": 0.1,
        "total_late_duration_s": 0.1,
        "total_late_penalty": 0.1,
        "total_lateness_risk_probability": 0.15,
        "total_penalty": 0.3,
        "total_probable_penalty": 0.15,
        "total_proximity_distance_m": 0.5,
        "total_proximity_duration_s": 0.5,
        "total_transit_distance_cost": 0.075,
        "total_transit_distance_m": 0.075,
        "total_transit_duration_s": 0.075,
        "multiorders_extra_points": 0.2,
        "multiorders_extra_visits": 0.15
    }

    if args.approx_compare:
        import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
        check_similarity = lambda expected_metrics, metrics: tools.check_metrics_are_close(
            expected_metrics, metrics, rel_accuracies)
    else:
        check_similarity = metrics_equal

    if not check_similarity(results1.get(), results2.get()):
        print_final_results(results1.values, results2.values, 'all tests')

    if not check_similarity(results1_with_same_fails.get(), results2_with_same_fails.get()):
        print_final_results(
            results1_with_same_fails.values, results2_with_same_fails.values,
            'same number of drops/failed windows')

    logging.warn("Solver 1 results: %d" % len(results1))
    logging.warn("Solver 2 results: %d" % len(results2))
    if skipped > 0:
        logging.warn("")
        logging.warn("Solver 1 failed %d times" % fails1)
        logging.warn("Solver 2 failed %d times" % fails2)
        logging.warn("Skipped tests: %d" % skipped)
    else:
        logging.warn("No skipped tests")

    if updated > 0:
        logging.warn("Updated tests: %d" % updated)

    if args.metrics:
        if args.update:
            new_metrics = results2.get()
            with open(args.metrics, 'w') as f_out:
                json.dump(new_metrics, f_out, sort_keys=True, indent=4)
            logging.warning('Average metrics has been updated according to solver 2 results')
        else:
            for results in (results1, results2):
                metrics_new = results.get()
                if not check_similarity(metrics, metrics_new):
                    print_metrics_diff("average metrics diff", metrics, metrics_new, diff_only=True)
                    return 1

    return 0 if skipped == 0 else 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
