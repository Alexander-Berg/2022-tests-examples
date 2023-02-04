# coding: utf-8

import json

from infra.yasm.unistat import Unistat, AggregationType, SuffixType


def test_push_with_object():
    unistat = Unistat()
    metric = unistat.create_float('sig')
    metric.push(1)
    metric.push(2)
    assert json.loads(unistat.to_json()) == [['sig_summ', 3]]


def test_push_last_value():
    unistat = Unistat()
    metric = unistat.create_float('sig', SuffixType.Sum, aggregation_type=AggregationType.LastValue)
    metric.push(1.0)
    metric.push(2.0)
    assert json.loads(unistat.to_json()) == [['sig_summ', 2]]


def test_push_with_signal_name():
    unistat = Unistat()
    unistat.create_float('sig', SuffixType.Absolute, aggregation_type=AggregationType.LastValue)
    assert unistat.push('sig', 1.0)
    assert unistat.push('sig', 2.0)
    assert json.loads(unistat.to_json()) == [['sig_axxx', 2]]


def test_info():
    unistat = Unistat()
    unistat.create_float('sig', SuffixType.Sum)
    assert json.loads(unistat.to_info()) == {
        'sig': {
            'Priority': 10,
            'Suffix': 'summ',
            'Tags': '',
            'Type': 'summ',
            'Value': 0
        }
    }


def test_histogram_with_custom_buckets():
    unistat = Unistat()
    unistat.create_histogram('sig', SuffixType.Histogram, [0, 1, 2]).push(1)
    assert json.loads(unistat.to_json()) == [['sig_dhhh', [[0, 0], [1, 1], [2, 0]]]]


def test_default_histogram():
    unistat = Unistat()
    unistat.create_histogram('sig').push(1)
    assert 'sig_dhhh' in dict(json.loads(unistat.to_json()))


def test_push_with_tags():
    unistat = Unistat()
    unistat.create_float('sig', SuffixType.Sum, tags=(('first', 'alice'), ('second', 'bob'))).push(1)
    assert json.loads(unistat.to_json()) == [['first=alice;second=bob;sig_summ', 1]]
