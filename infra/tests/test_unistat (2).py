# encoding: utf-8

import json
import cppzoom
import sys


def test_repeating_signals():
    responses = [
        [[u'logback.events_dhhh', 3101.0], [u'logback.events_dxxm', 3101.0], [u'logback.events_dhhh', 0.0], [u'logback.events_dxxm', 0.0], [u'logback.events_dhhh', 0.0]],
        [[u'logback.events_dhhh', 3103.0], [u'logback.events_dxxm', 3103.0], [u'logback.events_dhhh', 0.0], [u'logback.events_dxxm', 0.0], [u'logback.events_dhhh', 0.0]],
        [[u'logback.events_dhhh', 3105.0], [u'logback.events_dxxm', 3105.0], [u'logback.events_dhhh', 0.0], [u'logback.events_dxxm', 0.0], [u'logback.events_dhhh', 0.0]]
    ]

    loader = cppzoom.ZUnistatLoader("unistat", 10)
    for response in responses:
        assert loader.loads(json.dumps(response))[-1] is None


def test_errors():
    loader = cppzoom.ZUnistatLoader("unistat", 10)

    extra_depth_responses = [
        [[u'signal_dhhh', [[[1, 2], [2, 3]]]]],
        [[u'signal1_ahhh', [0.0, 1.0, 2.0]], [u'signal2_dhhh', [[[1, 2], [2, 3]]]], [u'signal3_ahhh', 5.0]]
    ]
    for response in extra_depth_responses:
        assert loader.loads(json.dumps(response))[-1] == 'Extra bracket detected while parsing hgram'

    unspecified_error_responses = [
        [[u'good_hgram1_dhhh', [[1, 2], [2, 3]]], [[u'bad_hgram_dhhh', [[1, 2], [2, 3]]]], [u'good_hgram2_dhhh', [[1, 2], [2, 3]]]]
    ]
    for response in unspecified_error_responses:
        assert loader.loads(json.dumps(response))[-1] == 'Unknown error'

    invalid_buckets_responses = [
        [[u'good_hgram1_dhhh', [[1, 2], [2, 3]]], [u'bad_hgram_dhhh', [[1, 2], [2, 3, 9], [7, 8]]], [u'good_hgram2_dhhh', [[1, 2], [2, 3]]]]
    ]
    for response in invalid_buckets_responses:
        assert loader.loads(json.dumps(response))[-1] == 'Too much arguments in ugram bucket detected'


def test_stats():
    loader = cppzoom.ZUnistatLoader("unistat", 10)
    response = [
        [u'signal_ahhh', [[1, 2], [2, 0]]],
        [u'signal1_ahhh', [
            [1, 1],
            [2, 1],
            [3, 1],
            [4, 1],
            [5, 1],
            [6, 1],
            [7, 1],
            [8, 1],
            [9, 1],
            [10, 1],
            [11, 1],
            [12, 1],
            [13, 1],
            [14, 1],
            [15, 1],
            [16, 1],
            [17, 1],
            [18, 1],
            [19, 1],
            [20, 1],
            [21, 0]
        ]]
    ]
    records_list, py_stats, error_message = loader.loads(json.dumps(response))
    assert sys.getrefcount(py_stats) == 2
    bucket_sizes_stats = py_stats["ugram_bucket_counts_hgram"]
    assert len(bucket_sizes_stats) == 2
    assert bucket_sizes_stats[0] == "ugram"
    assert bucket_sizes_stats[1][0:2] == [(0, 1), (20, 1)]  # expect the first stats bucket to be [0, 20]
