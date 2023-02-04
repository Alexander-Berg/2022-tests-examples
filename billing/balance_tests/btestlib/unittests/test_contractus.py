# coding: utf-8
import random

import pytest

from btestlib import dictdiff, reporter, contractus


def collector_mock(**kwargs):
    return lambda: dict(**kwargs)


def test_collect_contract():
    with reporter.step(u'Cценарий до'):
        pass
    # contractus.collect(id_='prototype_unique_id', collector=collector_mock(**{key: ord(key) for key in 'asd;flkasdfkjhasdf;lkahsdf;lasidfaoishdfkjladhsf'}))
    contractus.collect(id_='prototype_unique_id', collector=collector_mock(**{key: random.randint(ord('a'), ord('z'))
                                                                              for key in
                                                                              'asd;flkasdfkasdfafqwefvashdfkjladhsf'}))
    with reporter.step(u'Сценарий после'):
        pass


@pytest.mark.parametrize('contractus_id, filters', [
    ('prototype_unique_id', [])
])
def test_contract(contractus_id, filters):
    contractus.check(id_=contractus_id, differ=dictdiff.dictdiff, filters=filters, diff_reporter=dictdiff.report)
