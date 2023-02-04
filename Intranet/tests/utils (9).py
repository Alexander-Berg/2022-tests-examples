def assert_queryset_equals_list(qs, list):
    assert set(qs.values_list('id', flat=True)) == set(list)
