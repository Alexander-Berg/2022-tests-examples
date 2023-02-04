import textwrap
from gosky.version import LooseVersion as V

import pytest


def test_loose_version(a, b, comp):
    assert comp in ('<', '>', '=='), 'Unknown comparison!'
    if comp == '<':
        comp = '>'
        a, b = b, a

    if comp == '>':
        assert V(a) > V(b)
        assert V(b) < V(a)

    elif comp == '==':
        assert V(a) == V(b)


def pytest_generate_tests(metafunc):
    testdata = []
    for line in textwrap.dedent('''
        1 < 2
        a < b
        1 == 1
        1.0 == 1
        1.0.0.0 == 1
        1a2 == 1.a.2
        1.0.1 > 1.0
        1.0a > 1a       # corner case: 1.0a != 1a, because 1.0.1 != 1.1
        2 > 1
        1.99+ < 2.0
        text < 0        # any garbage should be lower than real versions
        1.2.3 == 1.2.3.0
        1.2.3 < 1.2.3.1
        1.2.4 > 1.2.3.1
        1.2a == 1.2a
        1.2a < 1.2b
        1.2a < 1.3
        1.2a < 1.2
        1.2.a == 1.2.a
        1.2.a < 1.2.b
        1.2.a < 1.3
        1.2.a < 1.2
        1.2.a < 1.3a
        1.2.a == 1.2a
    ''').strip().split('\n'):
        a, comp, b = line.split()[:3]
        testdata.append(pytest.param(a, b, comp, id=f'{a} {comp} {b}'))

    metafunc.parametrize("a,b,comp", testdata)
