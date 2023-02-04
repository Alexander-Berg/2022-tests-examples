# -*- coding: utf-8 -*-
__author__ = 'torvald'

import argparse
import datetime

import btestlib.reporter as reporter
import metric_custom
import metric_gap
import metric_python
import metric_python_full
import metric_quality
import metric_tails
import metric_trust_python
import metric_weighted_quality
import metric_review
import mutils
import metric_review_month


class Metrics(object):
    QUALITY = 'quality'
    W_QUALITY = 'weighted_quality'
    GAP = 'gap'
    PYTHON = 'python'
    PYTHON_TRUST = 'python_trust'
    PYTHON_FULL = 'python_full'
    CUSTOM = 'custom'
    TAILS = 'tails'
    REVIEW = 'review'
    REVIEW_MONTH = 'review_month'


if __name__ == "__main__":
    # import sys
    # argv = sys.argv
    # argv.append('--metric')
    # argv.append('gap')
    # argv.append('--base')
    # argv.append('2016-10')
    # argv.append('--n')
    # argv.append('34')
    # argv.append('--force')
    # argv.append('1')

    current_month = mutils.Utils.date_to_str(datetime.datetime.now())

    parser = argparse.ArgumentParser()
    parser.add_argument('--metric', help='metric to send')
    parser.add_argument('--base', default=current_month, help='base date')
    parser.add_argument('--force', type=int, default=0, help='force flag: recalculate cached data')
    parser.add_argument('--n', type=int, default=1, help='count of previous month to send')
    parser.add_argument('--source', default='', help='source name to override')
    parser.add_argument('--value_json', help='custom data to send in json format')
    # parser.parse_args(['--metric', 'gap', '--base', '--force', 0])

    args = parser.parse_args()
    reporter.log((args.metric, args.base, args.force, args.n, args.source, args.value_json))
    metric, base, force, n, source, value_json = args.metric, args.base, args.force, args.n, args.source, args.value_json

    params = (base, n, force)

    if metric == Metrics.CUSTOM:
        params = (value_json, source)

    metric_map = {Metrics.QUALITY: metric_quality,
                  Metrics.W_QUALITY: metric_weighted_quality,
                  Metrics.GAP: metric_gap,
                  Metrics.PYTHON: metric_python,
                  Metrics.PYTHON_TRUST: metric_trust_python,
                  Metrics.PYTHON_FULL: metric_python_full,
                  Metrics.CUSTOM: metric_custom,
                  Metrics.TAILS: metric_tails,
                  Metrics.REVIEW: metric_review,
                  Metrics.REVIEW_MONTH: metric_review_month
                  }

    metric_provider = metric_map.get(metric, None)
    if not metric_provider:
        reporter.log('No such metric')

    # if metric == Metrics.QUALITY:
    #     import metric_quality as metric
    # elif metric == Metrics.W_QUALITY:
    #     import metric_weighted_quality as metric
    # elif metric == Metrics.GAP:
    #     import metric_gap as metric
    # elif metric == Metrics.PYTHON:
    #     import metric_python as metric
    # elif metric == Metrics.PYTHON_FULL:
    #     import metric_python_full as metric
    # elif metric == Metrics.CUSTOM:
    #     import metric_custom as metric
    #     params = (value_json,)
    # else:
    #     reporter.log('No such metric')

    metric_provider.do(*params)
