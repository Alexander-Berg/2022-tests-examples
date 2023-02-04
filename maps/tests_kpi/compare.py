#!/usr/bin/env python

from __future__ import print_function
import json
import argparse
import os
import tabulate
import scipy
import scipy.stats
import itertools

import termcolor

PvalueBound = 0.05

TasksKpi = [
    'azbuka',
    'baltikaA',
    'baltikaB',
    'baltikaC',
    'ccola',
    'ikea',
    'lamoda',
    'sberbank',
    'utkonos',
]

Tasks = [
    'ormatek',
    'perekrestok',
    'tinkoff',
    'simplewine',
    'pony',
    'azbuka2',
    'magnit',
    'Major',
    'Myasnov',
    'optimum',
    'galamart',
    'yandex_shef_spb',
    'yandex_shef_msc',
    'kse',
]

TasksAll = TasksKpi + Tasks

Metrics = [
    ('total_cost_with_penalty', 'cost'),
    ('optimization_steps', 'steps'),
    ('dropped_locations_count', 'drops'),
    ('used_vehicles', 'vehicles'),
    ('number_of_routes', 'routes'),
    ('total_transit_distance_m', 'distance'),
    ('total_transit_duration_s', 'duration'),
]

def parse_args():
    p = argparse.ArgumentParser()
    p.add_argument('-b', "--baseline", required=True)
    p.add_argument('-t', "--test", required=True)
    p.add_argument('-f', "--filter")
    p.add_argument('-l', "--long", action='store_true')
    p.add_argument('-k', "--kpi", action='store_true', help='compare only kpi tasks')
    p.add_argument('-w', "--wiki", action='store_true', help='print colored wiki table')
    p.add_argument('-nc', "--no-colors", action='store_true', help='print non-colored terminal table')
    p.add_argument('-m', "--method", choices=['ttest', 'wilcoxon'], help='statistical method', default='ttest')
    return p.parse_args()


def load_metrics(task, path):
    with open(os.path.join(path, '%s_response.json' % task)) as f_in:
        r = json.load(f_in)
        r = r.get('result', r)
        m = r['metrics']
        metrics = [m]
        if '_tasks_summary' in m:
            metrics = [m] + [ts['metrics'] for ts in m["_tasks_summary"]]
        return metrics


def wilcoxon(a, b):
    return scipy.stats.wilcoxon(a, b, zero_method='zsplit')


StatMethods = {
    'ttest': scipy.stats.ttest_ind,
    'wilcoxon': wilcoxon
}

def compare_metric(m1, m2, metric, method):
    values1 = [m[metric] for m in m1]
    values2 = [m[metric] for m in m2]
    a = scipy.average(values1)
    b = scipy.average(values2)
    diff = b - a
    rel_diff = 100.0 * diff / max(a,b) if max(a,b) > 0 else 0 if a == b else 100.0
    return [a, b, '%+g' % diff, '%+.2f' % rel_diff, '%.2g' % StatMethods[method](values1, values2)[1]]


def print_header(title):
    print(('='*30) + ('[%s]' % title) + ('='*30))


def compare_detailed(args, tasks):
    for task in tasks:
        print()
        print_header(task)
        try:
            m1 = load_metrics(task, args.baseline)
            m2 = load_metrics(task, args.test)
            if len(m1) != len(m2):
                print("Warning! Different task count for base and test at task {}\nBase count: {}. Test count: {}".format(task, len(m1), len(m2)))
        except:
            continue
        rows = [[metric] + compare_metric(m1, m2, metric, args.method) for metric, name in Metrics]
        print(tabulate.tabulate(rows, headers=['metric', 'baseline', 'test', 'diff', 'diff %', 'pvalue']))


def average(values):
    return sum(float(v) for v in values) / len(values)


def average_vector(matrix):
    total = [0] * len(matrix[0])
    for row in matrix:
        for i, v in enumerate(row):
            total[i] += float(v)
    return [1.0 * v / len(matrix) for v in total]


def get_color(diff, pvalue):
    return 'gray' if pvalue > PvalueBound else 'red' if diff > 0 else 'green'


def transpose_matrix(columns):
    result = []
    for row in zip(*columns):
        result.append(list(row))
    return result


def print_wiki_table(header, data, transpose=False):
    print("#|")
    rows = [header] + data
    if transpose:
        rows = transpose_matrix(rows)
    for row in rows:
        print("|| " + ' | '.join(map(str, row)) + " ||")
    print("|#")


def compute_diffs_pvalue(diffs, method):
    return StatMethods[method](diffs, [0]*len(diffs))[1]


def compare_short(args, tasks):
    headers = ['task'] + list(itertools.chain(*([name + '(%)', 'pvalue'] for metric, name in Metrics[:1])))
    headers += [name + '(%)' for metric, name in Metrics[1:]]

    rows = []
    failed_tasks = []

    for task in tasks:
        try:
            m1 = load_metrics(task, args.baseline)
            m2 = load_metrics(task, args.test)
            row = list(itertools.chain(*(compare_metric(m1, m2, metric, args.method)[3:5] for metric, name in Metrics)))
        except:
            failed_tasks.append(task)
            continue
        rows.append([task] + row)
    rows = sorted(rows, key=lambda row: -abs(float(row[1])))

    average_row = ['average'] + ['%+.2f' % v for v in average_vector([row[1:] for row in rows])]
    for idx in range(len(average_row)/2):
        average_row[2*idx+2] = '%.2g' % compute_diffs_pvalue([float(row[2*idx+1]) for row in rows], args.method)
    rows.append(average_row)

    result = []
    for row_idx, row in enumerate(rows):
        is_average_row = row[0] == 'average'
        new_row = row[:1]
        for idx, (diff, pvalue) in enumerate(zip(row[1::2], row[2::2])):
            color_sign = -1 if headers[idx+2][:5] == 'steps' else 1
            color = get_color(color_sign*float(diff), float(pvalue))
            if args.wiki:
                diff = '!!(%s)**%s**!!' % (color, diff)
            elif not args.no_colors:
                diff = '%s' % (termcolor.colorize(diff, color))
            new_row.append(diff)
            if idx == 0:
                new_row.append(pvalue)
        if is_average_row and args.wiki:
            result[0:0] = [new_row]
        else:
            result.append(new_row)

    if args.wiki:
        print_wiki_table(headers, result, transpose=True)
    else:
        result[-1:-1] = [['-----' for i in range(len(Metrics)+1)]]
        print(tabulate.tabulate(result, headers=headers))

    if failed_tasks:
        print()
        print("not checked: " + str(sorted(failed_tasks)))


def main():
    args = parse_args()
    tasks = [args.filter] if args.filter else \
        TasksKpi if args.kpi else TasksAll
    if args.long:
        compare_detailed(args, tasks)
    else:
        compare_short(args, tasks)

if __name__ == "__main__":
    main()
