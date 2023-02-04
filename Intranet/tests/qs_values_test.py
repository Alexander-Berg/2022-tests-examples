from copy import deepcopy


def test_extract_related():
    from staff.lib.utils.qs_values import extract_related

    related = {
        'foo': 'foo',
        'bar': 100,
    }

    data = {
        'related__foo': 'foo',
        'related__bar': 100,
        'field': 'value',
    }
    data_copy = deepcopy(data)

    assert extract_related(data_dict=data, related_name='related', pop=False) == related
    assert data == data_copy

    result = extract_related(data_dict=data, related_name='related', prefix='pref', pop=False)
    assert result == {'pref_' + k: v for k, v in related.items()}
    assert data == data_copy

    assert extract_related(data_dict=data, related_name='related') == related
    assert data == {'field': 'value'}
