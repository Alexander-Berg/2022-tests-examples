import math

from infra.reconf_juggler.util import quorum


PROBES = 20000  # already have groups bigger than 10K instances
TABLES = []

calc = quorum.Calculator()
for name in sorted(quorum.TABLES):
    calc.set_table(name, quorum.TABLES[name])
    TABLES.append(name)


def test_builtin_tables_amount():
    assert len(TABLES) >= 50
    for name in range(99, 49, -1):  # only 50 tables for now
        assert name in TABLES


def test_builtin_tables_size():
    for name in quorum.TABLES:
        assert len(quorum.TABLES[name]['HPERC'].keys()) < 50
        assert len(quorum.TABLES[name]['QPERC'].keys()) < 50


def test_builtin_tables_values():
    for name in TABLES:
        prev = None
        for p in range(1, PROBES):
            curr = calc.calculate(p, name)
            assert curr['QABST'] + curr['QABSI'] == p
            assert curr['QPERC'] + curr['QPRCI'] == 1
            assert math.ceil(p * curr['QPERC']) == curr['QABST']

            if p == 100:
                assert name == curr['QABST']  # name is a quorum on 100

            if prev:
                assert curr['QABSI'] >= prev['QABSI']  # QABSI grows smoothly
                assert curr['HABST'] >= prev['HABST']  # HABST grows smoothly

            prev = curr
