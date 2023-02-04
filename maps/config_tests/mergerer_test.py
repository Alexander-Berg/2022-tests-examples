from maps.infra.sedem.cli.lib.utils import merge


def test_merge_scalar():
    source = {'test': 'old'}
    update = {'test': 'new'}
    assert(merge(source, update) == update)


def test_merge_list():
    source = {'test': [3, 4]}
    update = {'test': [5, 6]}
    result = {'test': [3, 4, 5, 6]}
    assert(merge(source, update) == result)


def test_merge_dict():
    source = {'test': {'list': [2, 3, 5], 'str': 'source', 'new': 0}}
    update = {'test': {'list': [4, 6], 'str': 'update', 'new2': 2}}
    result = {'test': {'list': [2, 3, 5, 4, 6], 'str': 'update', 'new': 0, 'new2': 2}}
    assert(merge(source, update) == result)
