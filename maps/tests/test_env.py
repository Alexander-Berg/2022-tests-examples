import pytest

import maps.analyzer.pylibs.envkit.env as env


def test_env():
    e = env.Vars()
    assert not e.declared('env')
    with pytest.raises(ValueError):
        e.has('env')
    with pytest.raises(ValueError):
        e.put('env', 10)

    e.declare('env')
    assert e.declared('env')
    assert not e.has('env')

    e.put('env', 10)
    assert e.has('env')
    assert e.get('env') == 10

    e.clear('env')
    assert not e.has('env')
    assert e.get('env') is None
    with pytest.raises(ValueError):
        e.declare('env')

    # same with operators
    assert 'env' in e
    e['env'] = 10
    assert e['env'] == 10
    del e['env']
    assert 'env' in e
    assert e['env'] is None


def test_derived_env():
    root = env.Vars()
    child = root.derive()
    root.declare('env', 'value')
    assert child['env'] == 'value'

    child['env'] = 'other-value'
    assert root['env'] == 'value', "set child value shouldn't affect parent"
    assert child['env'] == 'other-value', "child value should be updated"

    del child['env']
    assert child['env'] == 'value', "clearing child value should lead to using parent one"

    del root['env']
    assert root['env'] is None
    assert child['env'] is None


def test_computed_env():
    root = env.Vars()
    child = root.derive()
    root.declare('graph', 'regular')
    root.declare('root', lambda e: 'data/osm' if e['graph'] == 'osm' else 'data')

    assert root['root'] == 'data'
    assert child['root'] == 'data'

    root['graph'] = 'osm'
    assert root['root'] == 'data/osm'
    assert child['root'] == 'data/osm'

    child['graph'] = 'regular'
    assert root['root'] == 'data/osm'
    assert child['root'] == 'data'
