def test_duplicate_ids(item_name='', items=[]):
    seen = set()
    duplicates = []
    for item in items:
        if item.id in seen:
            duplicates.append(item.id)
        else:
            seen.add(item.id)
    assert not duplicates, f'duplicate {item_name} ids: {", ".join(duplicates)}'
