import argparse
import os
import sys
import json

import yatest


TIME_ACCURACY_S = 2
ACCURACY_RELATIVE = 0.02

DEFAULT_ITERATIONS = 1000000


def solver_path():
    return yatest.common.binary_path("maps/b2bgeo/mvrp_solver/annealing_mvrp/annealing-mvrp/annealing-mvrp")


def data_path(rel_path):
    return yatest.common.work_path(os.path.join("sandbox", rel_path))


def arcadia_path(rel_path):
    path_within_arcadia = os.path.join(
        "maps/b2bgeo/mvrp_solver/annealing_mvrp",
        rel_path)
    return yatest.common.source_path(path_within_arcadia)


def get_test_data(fname):
    with open(arcadia_path(os.path.join('tests_data', fname)), 'r') as f:
        return f.read()


def get_test_json(fname):
    with open(arcadia_path(os.path.join('tests_data', fname)), 'r') as f:
        return json.load(f)


def validate_format(request, response, kind="mvrp"):
    sys.path.append("pymod")  # relative path to vrp_solver module
    from vrp_solver import schema
    from jsonschema import validate

    if kind == 'mvrp':
        validate(response, schema.mvrp_response)
        validate(request, schema.mvrp_request)
    elif kind == 'svrp':
        validate(response, schema.svrp_response)
        validate(request, schema.svrp_request)
    else:
        raise ValueError("Invalid kind '" + kind + "'. Allowed kinds: ['mvrp', 'svrp']")


def is_abs_close(a, b, accuracy):
    return abs(a - b) <= accuracy


def is_rel_close(a, b, accuracy):
    return is_abs_close(a, b, accuracy * max(abs(a), abs(b)))


def is_close(a, b, abs_accuracy, rel_accuracy):
    return is_abs_close(a, b, abs_accuracy) or is_rel_close(a, b, rel_accuracy)


def check_yamake(test_group):
    tests = [f for f in os.listdir(arcadia_path(test_group))
             if f.startswith('test_') and f.endswith('.py')]
    yamake = os.path.join(test_group, 'ya.make')
    checked = set()
    with open(arcadia_path(yamake)) as ym:
        for line in ym:
            checked.add(line.strip())
    for test in tests:
        assert test in checked, \
            "Test %s is not added to %s" % (test, yamake)


def check_svrp_route(response, id_sequence):
    """
    Check that the vehicle visits the depots and locations in the
    order specified by `id_sequence`.
    """
    seq = []
    for route in response['routes']:
        for node in route['route']:
            seq.append(node['node']['value']['id'])
    if seq != id_sequence:
        msg = 'Incorrect sequence of node visits!\n'
        msg += 'The expected order: %s\n' % ', '.join(map(str, id_sequence))
        msg += 'The solver order: %s\n' % ', '.join(map(str, seq))
        raise RuntimeError(msg)


def check_metrics_are_close(metrics, expected_metrics, rel_accuracies=None):
    """
    Check that difference between values in `metrics` and `expected_metric`
    are close enough.
    Metrics not specified in `expected_metrics` are ignored.
    """
    import tabulate

    if yatest.common.context.sanitize:
        return True

    if rel_accuracies is None:
        rel_accuracies = {}
    ignore_metrics = ["operations_per_second", "_solver_duration_s"]
    failed = []
    changed = []

    for name, expected_val in sorted(expected_metrics.items()):
        if name not in metrics:
            continue
        val = metrics[name]
        diff = val - expected_val
        if diff != 0:
            accuracy_abs = TIME_ACCURACY_S if name.endswith('_s') else 1e-6
            accuracy_rel = rel_accuracies.get(name, ACCURACY_RELATIVE)

            ok = is_close(val, expected_val, accuracy_abs, accuracy_rel)
            (changed if ok or name in ignore_metrics else failed).append([
                name, val, expected_val, diff,
                '%+.3f %%' % (100.0 * diff / max(abs(val), abs(expected_val), 1e-32))
            ])

    headers = ['metric', 'current', 'expected', 'diff', 'rel_diff']
    msg = ''
    if failed:
        msg += "Metrics which are too far from expected:\n"
        msg += tabulate.tabulate(failed, headers=headers)
    if changed:
        msg += '\n' + '=' * 64 + '\n'
        msg += "Metrics which are close to expected:\n"
        msg += tabulate.tabulate(changed, headers=headers)
    assert not failed, msg
    return True


def find_requests_files(dirpath):
    tasks = []
    for path, dirs, files in os.walk(dirpath):
        for fname in files:
            if fname.endswith('_request.json'):
                tasks.append(os.path.join(path, fname))
    return tasks


def check_solver(tests, metrics, iterations, update=False, solver_runs=1, approx_compare=False):
    import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.compare_solvers as compare_solvers

    assert os.path.exists(tests), ("%s does not exist" % tests)

    tasks = find_requests_files(tests) if os.path.isdir(tests) else [tests]

    print()
    if update:
        print("Updating %d tasks with new solver results" % len(tasks))
    else:
        print("Testing solver on %d tasks" % len(tasks))

    solver_w_args = solver_path() + ' -i {path} --vrp-solver-comparison --rand-seed {run_num} -I %d' % iterations
    cmd = [
        arcadia_path('./tests_lib/compare_solvers.py'),
        '--jobs-count', '0', '--solver-runs', str(solver_runs), '--metrics', metrics, '-2', solver_w_args]
    if update:
        cmd.append('--update')
    if approx_compare:
        cmd.append('--approx-compare')
    update_cmd = ' '.join("'" + arg + "'" for arg in cmd + ['--update', '<PATH>/' + os.path.basename(tests)])
    cmd += tasks

    passed = compare_solvers.main(cmd) == 0

    print("=" * 20)
    print('Test status: ' + ("SUCCESS" if passed else "FAILED"))

    # update_cmd = '{prog} --update -t {tests} -m {metrics} -I {iterations}'.format(prog=sys.argv[0], **locals())
    assert passed, "If the changes are expected and acceptable execute the command below to update test data:\n" + update_cmd


def parse_args():
    p = argparse.ArgumentParser()
    p.add_argument('-t', '--tests', default=data_path('tests/svrp100'), help='file or directory')
    p.add_argument('-m', '--metrics', help='average metrics must be as in this json file')
    p.add_argument('-r', '--solver-runs', type=int, default=1, help='number of times to run each solver (the best result accepted)')
    p.add_argument('-u', '--update', action='store_true', help='update metrics and test data according to the solver results')
    p.add_argument('-I', '--iterations', help='solver iterations', type=int, default=DEFAULT_ITERATIONS)
    args = p.parse_args()
    if not args.metrics:
        args.metrics = args.tests + '_metrics.json'
    return args
